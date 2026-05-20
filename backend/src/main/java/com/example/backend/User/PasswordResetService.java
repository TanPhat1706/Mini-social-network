package com.example.backend.User;

public interface PasswordResetService {
    void asyncInitiateForgotPassword(String email);

    boolean resetPassword(PasswordResetRequest req);
}
