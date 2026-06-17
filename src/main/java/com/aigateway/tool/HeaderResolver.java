package com.aigateway.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves header templates of the form {@code {{name}}} against a runtime
 * context map. Stateless and side-effect free so it can be reused across
 * concurrent tool invocations.
 * <p>
 * Designed as the single substitution point for richer header providers
 * (OAuth, JWT, signed API keys) which can pre-populate the context map before
 * resolution.
 */
@Component
public class HeaderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z0-9_.-]+)}}");

    public Map<String, String> resolve(Map<String, String> templateHeaders,
                                       Map<String, String> context) {
        if (templateHeaders == null || templateHeaders.isEmpty()) {
            return Map.of();
        }
        Map<String, String> safeContext = context == null ? Map.of() : context;
        Map<String, String> resolved = new HashMap<>(templateHeaders.size());
        for (Map.Entry<String, String> entry : templateHeaders.entrySet()) {
            resolved.put(entry.getKey(), substitute(entry.getValue(), safeContext));
        }
        return resolved;
    }

    private String substitute(String value, Map<String, String> context) {
        if (value == null || value.indexOf("{{") < 0) {
            return value;
        }
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = context.get(key);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement == null ? "" : replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}

