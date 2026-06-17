(() => {
    'use strict';

    // Same origin by default. When the page is opened via the IntelliJ
    // built-in preview server (port 63342), redirect API calls to the
    // Spring Boot app running on port 8095.
    const API = (location.port === '63342')
        ? `${location.protocol}//${location.hostname}:8095`
        : '';
    const state = {
        agents: [],
        tools: [],
        guardrails: {}, // agentId -> { payment, limit, unsafe }
        observability: [], // { requestId, agentId, input, response, ts }
        chatHistory: [], // {role, content, requestId?}
    };

    // ---------- Utilities ----------
    const $ = (sel) => document.querySelector(sel);
    const $$ = (sel) => document.querySelectorAll(sel);
    const el = (tag, attrs = {}, ...children) => {
        const node = document.createElement(tag);
        Object.entries(attrs).forEach(([k, v]) => {
            if (k === 'class') node.className = v;
            else if (k === 'html') node.innerHTML = v;
            else if (k.startsWith('on')) node.addEventListener(k.slice(2).toLowerCase(), v);
            else if (v !== false && v != null) node.setAttribute(k, v);
        });
        children.flat().forEach(c => {
            if (c == null) return;
            node.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
        });
        return node;
    };
    const toast = (msg) => {
        const t = $('#toast');
        t.textContent = msg;
        t.hidden = false;
        clearTimeout(toast._t);
        toast._t = setTimeout(() => { t.hidden = true; }, 2400);
    };
    const truncate = (s, n = 80) => (s && s.length > n ? s.slice(0, n) + '…' : s || '');

    async function api(path, opts = {}) {
        const res = await fetch(API + path, {
            headers: { 'Content-Type': 'application/json', ...(opts.headers || {}) },
            ...opts,
        });
        const text = await res.text();
        const ctype = res.headers.get('content-type') || '';
        let data = null;
        if (text) {
            if (ctype.includes('application/json')) {
                try { data = JSON.parse(text); } catch (_) { data = null; }
            } else {
                try { data = JSON.parse(text); } catch (_) {
                    const err = new Error(`API unavailable (HTTP ${res.status})`);
                    err.status = res.status;
                    err.nonJson = true;
                    throw err;
                }
            }
        }
        if (!res.ok) {
            const err = new Error((data && (data.message || data.error)) || `HTTP ${res.status}`);
            err.status = res.status;
            err.body = data;
            throw err;
        }
        return data;
    }

    // ---------- Navigation ----------
    function showView(name) {
        $$('.view').forEach(v => v.hidden = v.id !== `view-${name}`);
        $$('.nav-item').forEach(n => n.classList.toggle('active', n.dataset.view === name));
        if (name === 'agents') loadAgents();
        if (name === 'tools') loadTools();
        if (name === 'playground') populatePlaygroundAgents();
        if (name === 'analytics') renderAnalytics();
        if (name === 'observability') renderObservability();
    }

    // ---------- Agents ----------
    async function loadAgents() {
        const grid = $('#agents-grid');
        grid.innerHTML = '<div class="empty">Loading agents…</div>';
        try {
            state.agents = await api('/agents') || [];
            renderAgents();
        } catch (e) {
            if (e.nonJson || e.status === 404 || e.status === 0 || e.name === 'TypeError') {
                state.agents = [];
                renderAgents();
            } else {
                grid.innerHTML = `<div class="empty">Failed to load agents: ${e.message}</div>`;
            }
        }
    }

    function renderAgents() {
        const grid = $('#agents-grid');
        grid.innerHTML = '';
        if (!state.agents.length) {
            grid.appendChild(el('div', { class: 'empty' }, 'No agents yet. Click "New Agent" to create one.'));
            return;
        }
        state.agents.forEach(a => {
            const tools = (a.tools || []).map(t => el('span', { class: 'tag' }, t));
            grid.appendChild(el('div', { class: 'card' },
                el('h3', {}, a.name || a.agentId),
                el('div', { class: 'meta' }, `${a.agentId} · ${a.model}`),
                el('div', { class: 'tags' }, tools.length ? tools : el('span', { class: 'meta' }, 'no tools'))
            ));
        });
    }

    function openAgentModal() {
        $('#agent-modal').hidden = false;
        $('#agent-form').reset();
        $('#agent-form-error').hidden = true;
    }
    function closeAgentModal() { $('#agent-modal').hidden = true; }

    async function submitAgent(e) {
        e.preventDefault();
        const f = e.target;
        const fd = new FormData(f);
        const tools = (fd.get('tools') || '').toString()
            .split(',').map(s => s.trim()).filter(Boolean);
        const payload = {
            agentId: fd.get('agentId').toString().trim(),
            name: fd.get('name').toString().trim(),
            model: fd.get('model').toString(),
            systemPrompt: fd.get('systemPrompt').toString(),
            tools,
        };
        const guardrails = {
            payment: !!fd.get('g_payment'),
            limit: !!fd.get('g_limit'),
            unsafe: !!fd.get('g_unsafe'),
        };
        const errBox = $('#agent-form-error');
        errBox.hidden = true;
        if (!payload.tools.length) {
            errBox.textContent = 'At least one tool is required.';
            errBox.hidden = false;
            return;
        }
        try {
            const created = await api('/agents', { method: 'POST', body: JSON.stringify(payload) });
            state.guardrails[created.agentId] = guardrails;
            closeAgentModal();
            toast(`Agent "${created.name}" created`);
            await loadAgents();
        } catch (err) {
            errBox.textContent = err.message;
            errBox.hidden = false;
        }
    }

    // ---------- Tools ----------
    async function loadTools() {
        const grid = $('#tools-grid');
        grid.innerHTML = '<div class="empty">Loading tools…</div>';
        try {
            state.tools = await api('/tools') || [];
            grid.innerHTML = '';
            if (!state.tools.length) {
                grid.appendChild(el('div', { class: 'empty' }, 'No tools registered.'));
                return;
            }
            state.tools.forEach(t => {
                grid.appendChild(el('div', { class: 'card' },
                    el('h3', {}, t.name),
                    el('div', { class: 'meta' }, `${t.method || 'POST'} · ${t.url || ''}`),
                    el('p', { class: 'muted' }, t.description || 'No description provided.')
                ));
            });
        } catch (e) {
            if (e.nonJson || e.status === 404 || e.status === 0 || e.name === 'TypeError') {
                state.tools = [];
                grid.innerHTML = '';
                grid.appendChild(el('div', { class: 'empty' }, 'No tools registered.'));
            } else {
                grid.innerHTML = `<div class="empty">Failed to load tools: ${e.message}</div>`;
            }
        }
    }

    function openToolModal() {
        $('#tool-modal').hidden = false;
        $('#tool-form').reset();
        $('#tool-form-error').hidden = true;
    }
    function closeToolModal() { $('#tool-modal').hidden = true; }

    function parseJsonField(raw, label) {
        const v = (raw || '').toString().trim();
        if (!v) return null;
        try {
            return JSON.parse(v);
        } catch (e) {
            throw new Error(`${label} is not valid JSON: ${e.message}`);
        }
    }

    async function submitTool(e) {
        e.preventDefault();
        const f = e.target;
        const fd = new FormData(f);
        const errBox = $('#tool-form-error');
        errBox.hidden = true;
        try {
            const payload = {
                name: fd.get('name').toString().trim(),
                url: fd.get('url').toString().trim(),
                method: fd.get('method').toString(),
                description: (fd.get('description') || '').toString().trim() || null,
                parameters: parseJsonField(fd.get('parameters'), 'Parameters'),
                headers: parseJsonField(fd.get('headers'), 'Headers'),
                defaultParams: parseJsonField(fd.get('defaultParams'), 'Default Params'),
            };
            const created = await api('/tools/register', { method: 'POST', body: JSON.stringify(payload) });
            closeToolModal();
            toast(`Tool "${created.name}" registered`);
            await loadTools();
        } catch (err) {
            errBox.textContent = err.message;
            errBox.hidden = false;
        }
    }

    // ---------- Playground ----------
    async function populatePlaygroundAgents() {
        const sel = $('#playground-agent');
        const prev = sel.value;
        if (!state.agents.length) {
            try { state.agents = await api('/agents') || []; } catch (_) {}
        }
        sel.innerHTML = '';
        if (!state.agents.length) {
            sel.appendChild(el('option', { value: '' }, 'No agents available'));
            return;
        }
        state.agents.forEach(a => {
            sel.appendChild(el('option', { value: a.agentId }, `${a.name} (${a.agentId})`));
        });
        if (prev && state.agents.some(a => a.agentId === prev)) sel.value = prev;
    }

    function renderMarkdown(text) {
        if (typeof marked === 'undefined') {
            const span = document.createElement('span');
            span.textContent = text;
            return span;
        }
        const wrap = document.createElement('div');
        wrap.className = 'md';
        wrap.innerHTML = marked.parse(text || '', { breaks: true, gfm: true });
        return wrap;
    }

    function renderChat() {
        const chat = $('#chat');
        chat.innerHTML = '';
        if (!state.chatHistory.length) {
            chat.appendChild(el('div', { class: 'empty' }, 'Start a conversation by sending a message.'));
            return;
        }
        state.chatHistory.forEach(m => {
            const node = el('div', { class: `msg ${m.role}` });
            if (m.role === 'assistant') {
                node.appendChild(renderMarkdown(m.content));
            } else {
                node.appendChild(document.createTextNode(m.content));
            }
            if (m.requestId) {
                node.appendChild(el('div', { class: 'meta-line' }, `requestId: ${m.requestId}`));
            }
            chat.appendChild(node);
        });
        chat.scrollTop = chat.scrollHeight;
    }

    function appendLoading() {
        const chat = $('#chat');
        const node = el('div', { class: 'msg assistant', id: 'msg-loading' });
        node.appendChild(el('span', { class: 'spinner' }));
        node.appendChild(document.createTextNode(' Thinking…'));
        chat.appendChild(node);
        chat.scrollTop = chat.scrollHeight;
    }
    function removeLoading() {
        const n = $('#msg-loading');
        if (n) n.remove();
    }

    async function sendMessage(e) {
        e.preventDefault();
        const input = $('#composer-input');
        const agentId = $('#playground-agent').value;
        const sessionId = $('#playground-session').value || 'demo-1';
        const text = input.value.trim();
        if (!text) return;
        if (!agentId) { toast('Select an agent first'); return; }

        state.chatHistory.push({ role: 'user', content: text });
        renderChat();
        input.value = '';
        appendLoading();

        try {
            const res = await api('/agents/run', {
                method: 'POST',
                body: JSON.stringify({
                    agentId,
                    input: text,
                    metadata: { sessionId },
                }),
            });
            removeLoading();
            const reply = (res && res.response) || '(empty response)';
            state.chatHistory.push({ role: 'assistant', content: reply, requestId: res && res.requestId });
            state.observability.unshift({
                requestId: res && res.requestId,
                agentId,
                input: text,
                response: reply,
                ts: Date.now(),
            });
            renderChat();
        } catch (err) {
            removeLoading();
            state.chatHistory.push({ role: 'error', content: `Error: ${err.message}` });
            renderChat();
        }
    }

    function clearChat() {
        state.chatHistory = [];
        renderChat();
    }


    // ---------- Analytics ----------
    function renderAnalytics() {
        const sessionRequests = state.observability.length;
        const baseline = { requests: 42, tokens: 1200, latency: '120ms' };
        const stats = [
            { label: 'Total Requests', value: baseline.requests + sessionRequests },
            { label: 'Tokens Used', value: (baseline.tokens + sessionRequests * 80).toLocaleString() },
            { label: 'Avg Latency', value: baseline.latency },
            { label: 'Active Agents', value: state.agents.length },
        ];
        const wrap = $('#stats');
        wrap.innerHTML = '';
        stats.forEach(s => {
            wrap.appendChild(el('div', { class: 'stat-card' },
                el('div', { class: 'stat-label' }, s.label),
                el('div', { class: 'stat-value' }, String(s.value))
            ));
        });
    }

    // ---------- Observability ----------
    function renderObservability() {
        const tbody = $('#obs-tbody');
        tbody.innerHTML = '';
        if (!state.observability.length) {
            tbody.appendChild(el('tr', {},
                el('td', { colspan: '5', class: 'muted', style: 'text-align:center; padding:24px;' },
                    'No invocations yet. Use the Playground to generate traffic.')));
            return;
        }
        state.observability.forEach((row, idx) => {
            const tr = el('tr', {},
                el('td', {}, row.requestId || '—'),
                el('td', {}, row.agentId),
                el('td', { class: 'truncate', title: row.input }, truncate(row.input, 60)),
                el('td', { class: 'truncate', title: row.response }, truncate(row.response, 80)),
                el('td', {})
            );
            const link = el('button', { class: 'link', onclick: () => showDetail(idx) }, 'View');
            tr.lastChild.appendChild(link);
            tbody.appendChild(tr);
        });
    }

    function showDetail(idx) {
        const row = state.observability[idx];
        if (!row) return;
        $('#detail-body').textContent = JSON.stringify({
            requestId: row.requestId,
            agentId: row.agentId,
            timestamp: new Date(row.ts).toISOString(),
            input: row.input,
            response: row.response,
        }, null, 2);
        $('#detail-modal').hidden = false;
    }

    // ---------- Bootstrap ----------
    function wireEvents() {
        $$('.nav-item').forEach(b => b.addEventListener('click', () => showView(b.dataset.view)));
        $('#btn-new-agent').addEventListener('click', openAgentModal);
        $('#agent-modal-close').addEventListener('click', closeAgentModal);
        $('#agent-cancel').addEventListener('click', closeAgentModal);
        $('#agent-form').addEventListener('submit', submitAgent);
        $('#btn-new-tool').addEventListener('click', openToolModal);
        $('#tool-modal-close').addEventListener('click', closeToolModal);
        $('#tool-cancel').addEventListener('click', closeToolModal);
        $('#tool-form').addEventListener('submit', submitTool);
        $('#composer').addEventListener('submit', sendMessage);
        $('#composer-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                $('#composer').requestSubmit();
            }
        });
        $('#playground-clear').addEventListener('click', clearChat);
        $('#obs-clear').addEventListener('click', () => { state.observability = []; renderObservability(); });
        $('#detail-modal-close').addEventListener('click', () => { $('#detail-modal').hidden = true; });
        [$('#agent-modal'), $('#tool-modal'), $('#detail-modal')].forEach(m => {
            m.addEventListener('click', (e) => { if (e.target === m) m.hidden = true; });
        });
    }

    function init() {
        try {
            wireEvents();
            showView('agents');
        } catch (err) {
            console.error('AI Gateway UI init failed:', err);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
