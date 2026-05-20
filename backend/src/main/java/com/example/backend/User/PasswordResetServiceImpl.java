package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PasswordResetServiceImpl(UserRepository userRepository,
                                    PasswordResetTokenRepository tokenRepository,
                                    EmailService emailService,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Async
    public void asyncInitiateForgotPassword(String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                // Do nothing to avoid user enumeration
                return;
            }

            User user = userOpt.get();

            // Generate high-entropy token
            SecureRandom rnd = SecureRandom.getInstanceStrong();
            byte[] bytes = new byte[32];
            rnd.nextBytes(bytes);
            String clearToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            // Hash with SHA-256
            String hashed = sha256Hex(clearToken);

            PasswordResetToken prt = new PasswordResetToken();
            prt.setUserId(user.getId());
            prt.setHashedToken(hashed);
            prt.setExpiryTime(LocalDateTime.now().plusMinutes(15));

            tokenRepository.save(prt);

            // Send email with link containing clear token via EmailService (handles errors)
            emailService.sendPasswordResetEmail(user.getEmail(), clearToken);

        } catch (Exception e) {
            // swallow to prevent timing differences and leaks
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public boolean resetPassword(PasswordResetRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        try {
            String hashed = sha256Hex(req.getToken());
            Optional<PasswordResetToken> opt = tokenRepository.findByHashedToken(hashed);
            if (opt.isEmpty()) return false;

            PasswordResetToken prt = opt.get();
            if (prt.getExpiryTime().isBefore(LocalDateTime.now())) {
                tokenRepository.delete(prt);
                return false;
            }

            Optional<User> userOpt = userRepository.findById(prt.getUserId());
            if (userOpt.isEmpty()) {
                tokenRepository.delete(prt);
                return false;
            }

            User user = userOpt.get();
            String encoded = passwordEncoder.encode(req.getNewPassword());
            user.setPassword(encoded);
            user.setLastPasswordResetAt(LocalDateTime.now());
            userRepository.save(user);

            tokenRepository.delete(prt);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String sha256Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
