package com.rutina.rutinabackend.global.auth.email;

import java.util.Optional;

public interface EmailVerificationStore {
    void saveCode(String email, String code, long ttlSeconds);
    Optional<String> getCode(String email);
    void deleteCode(String email);
    void saveVerified(String email, long ttlSeconds);
    boolean isVerified(String email);
    void deleteVerified(String email);

    void savePwResetCode(String email, String code, long ttlSeconds);
    Optional<String> getPwResetCode(String email);
    void deletePwResetCode(String email);
    void savePwResetVerified(String email, long ttlSeconds);
    boolean isPwResetVerified(String email);
    void deletePwResetVerified(String email);
}
