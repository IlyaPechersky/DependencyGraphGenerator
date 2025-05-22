package com.parser.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.File;
import java.io.IOException;

public class AntiPatternConfigLoader {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static AntiPatternConfig load(String configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(new File(configPath), AntiPatternConfig.class);
    }
}
