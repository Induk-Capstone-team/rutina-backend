package com.rutina.rutinabackend.global.auth.oauth2;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Profile("local")
@Component
public class InMemoryOAuth2LoginCodeStore implements OAuth2LoginCodeStore {

    private final Map<String, OAuth2LoginCode> values = new ConcurrentHashMap<>();
    private final Map<String, Long> expiries = new ConcurrentHashMap<>();

    @Override
    public void save(String code, OAuth2LoginCode loginCode, long ttlSeconds) {
        values.put(code, loginCode);
        expiries.put(code, System.currentTimeMillis() + ttlSeconds * 1000);
    }

    @Override
    public Optional<OAuth2LoginCode> consume(String code) {
        Long expiry = expiries.remove(code);
        OAuth2LoginCode value = values.remove(code);
        if (expiry == null || value == null || System.currentTimeMillis() > expiry) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
