package com.foodscanner.application.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Слой: application.
 *
 * Повторная (серверная) маскировка секретов перед сохранением телеметрии.
 * Клиент уже маскирует, но backend не доверяет входу и страхует второй раз:
 * пароли, токены, Authorization/Cookie никогда не попадают в client_logs/server_events.
 */
@Component
public class TelemetrySanitizer {

    private static final String MASK = "********";

    /** Ключи, значение которых маскируется целиком. */
    private static final Pattern SECRET_KEY = Pattern.compile(
        "^(password|new_?password|access[_-]?token|refresh[_-]?token|token|authorization|cookie|set-cookie)$",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern BEARER = Pattern.compile("Bearer\\s+[A-Za-z0-9._~+/=-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern JWT    = Pattern.compile("eyJ[A-Za-z0-9._-]{10,}");

    private static final int MAX_DEPTH = 8;

    /** Маскирует строку (Bearer/JWT). null-безопасно. */
    public String maskString(String value) {
        if (value == null) return null;
        String out = BEARER.matcher(value).replaceAll("Bearer " + MASK);
        return JWT.matcher(out).replaceAll(MASK);
    }

    /** Рекурсивно маскирует Map (значения секретных ключей и Bearer/JWT в строках). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> maskMap(Map<String, Object> input) {
        if (input == null) return null;
        Object masked = maskValue(input, 0);
        return (Map<String, Object>) masked;
    }

    private Object maskValue(Object value, int depth) {
        if (value == null || depth > MAX_DEPTH) return value;
        if (value instanceof String s) return maskString(s);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                out.put(key, SECRET_KEY.matcher(key).matches() ? MASK : maskValue(e.getValue(), depth + 1));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) out.add(maskValue(item, depth + 1));
            return out;
        }
        return value;
    }
}
