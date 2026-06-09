package com.wuxing.persona.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static String toJson(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("serialize json failed", ex);
        }
    }

    public static <T> T fromJson(ObjectMapper objectMapper, String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception ex) {
            throw new IllegalStateException("parse json failed", ex);
        }
    }
}
