package com.neha.paynudge;

import tools.jackson.databind.ObjectMapper;   // Spring Boot 4 ships Jackson 3 (package "tools.jackson")
import java.util.Map;

/** Tiny JSON helper so the rest of the code never touches the JSON library directly. */
public final class Json {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private Json() {}

    public static String write(Object value) {
        try { return MAPPER.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("Could not write JSON", e); }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readMap(String json) {
        try { return MAPPER.readValue(json, Map.class); }
        catch (Exception e) { throw new IllegalArgumentException("Could not read JSON: " + json, e); }
    }
}
