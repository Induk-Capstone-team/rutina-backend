package com.rutina.rutinabackend.domain.email.service;

import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.auth.email.EmailVerificationStore;
import com.rutina.rutinabackend.global.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationStore store;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${email.from}")
    private String from;

    @Value("${email.verification-ttl-seconds}")
    private long verificationTtlSeconds;

    @Value("${email.verified-ttl-seconds}")
    private long verifiedTtlSeconds;

    public void sendVerificationCode(String email) {
        String code = generateCode();
        store.saveCode(email, code, verificationTtlSeconds);
        sendEmail(email, "[Rutina] 이메일 인증번호", "email/verification-code", code);
    }

    public void verifyCode(String email, String code) {
        String stored = store.getCode(email)
                .orElseThrow(ErrorCode.INVALID_VERIFICATION_CODE::toException);
        if (!stored.equals(code)) {
            throw ErrorCode.INVALID_VERIFICATION_CODE.toException();
        }
        store.deleteCode(email);
        // 인증 완료 상태는 회원가입 완료 전까지 유지되어야 하므로 verificationTtl보다 긴 verifiedTtl을 적용한 별도 키로 전환
        store.saveVerified(email, verifiedTtlSeconds);
    }

    public void checkVerified(String email) {
        if (!store.isVerified(email)) {
            throw ErrorCode.EMAIL_VERIFICATION_REQUIRED.toException();
        }
    }

    public void sendPasswordResetCode(String email) {
        // 가입되지 않은 이메일로의 불필요한 발송을 방지
        if (!userRepository.existsByEmail(email)) {
            throw ErrorCode.USER_NOT_FOUND.toException();
        }
        String code = generateCode();
        store.savePwResetCode(email, code, verificationTtlSeconds);
        sendEmail(email, "[Rutina] 비밀번호 재설정 인증번호", "email/password-reset-code", code);
    }

    public void verifyPasswordResetCode(String email, String code) {
        String stored = store.getPwResetCode(email)
                .orElseThrow(ErrorCode.INVALID_VERIFICATION_CODE::toException);
        if (!stored.equals(code)) {
            throw ErrorCode.INVALID_VERIFICATION_CODE.toException();
        }
        store.deletePwResetCode(email);
        // 비밀번호 변경 API 호출 시까지 인증 상태가 유지되어야 하므로 verifiedTtl 적용
        store.savePwResetVerified(email, verifiedTtlSeconds);
    }

    public void checkPasswordResetVerified(String email) {
        if (!store.isPwResetVerified(email)) {
            throw ErrorCode.EMAIL_VERIFICATION_REQUIRED.toException();
        }
    }

    public void clearVerified(String email) {
        store.deleteVerified(email);
    }

    public void clearPasswordResetVerified(String email) {
        store.deletePwResetVerified(email);
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private void sendEmail(String to, String subject, String templateName, String code) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("minutes", verificationTtlSeconds / 60);
        String htmlContent = templateEngine.process(templateName, context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw ErrorCode.EMAIL_SEND_FAILED.toException();
        }
    }
}
