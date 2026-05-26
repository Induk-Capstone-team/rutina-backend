package com.rutina.rutinabackend.global.auth.oauth2;

import java.util.Optional;

public interface OAuth2LoginCodeStore {
    void save(String code, OAuth2LoginCode loginCode, long ttlSeconds);
    Optional<OAuth2LoginCode> consume(String code);
}
