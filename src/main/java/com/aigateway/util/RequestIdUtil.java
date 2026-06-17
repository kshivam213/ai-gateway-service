package com.aigateway.util;

import java.util.UUID;

public final class RequestIdUtil {

    private RequestIdUtil() {
    }

    public static String newRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
