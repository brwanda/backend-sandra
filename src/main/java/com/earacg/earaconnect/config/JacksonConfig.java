package com.earacg.earaconnect.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure Jackson to handle LocalDateTime properly
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Exclude null values from JSON
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // Prevent infinite recursion by limiting nesting depth
        StreamWriteConstraints constraints = StreamWriteConstraints.builder()
                .maxNestingDepth(50) // Reduced from default 1000 to prevent deep nesting issues
                .build();
        mapper.getFactory().setStreamWriteConstraints(constraints);
        
        // Fail on unknown properties to catch issues early
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return mapper;
    }
}