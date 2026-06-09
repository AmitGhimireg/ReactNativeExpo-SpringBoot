package com.yashitech.html_to_figma_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * EmailService.java — NEW FILE. Sends transactional emails via SMTP.
 *
 * Currently used for:
 *   • Email verification link sent after registration
 *
 * HOW IT WORKS:
 *   Spring Boot's JavaMailSender (configured in application.properties via
 *   spring.mail.*) handles the actual SMTP connection. This service just
 *   builds and sends SimpleMailMessage objects.
 *
 * REUSE: copy to any Spring Boot project. Add spring-boot-starter-mail to
 * pom.xml and fill in spring.mail.* properties.
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;  // auto-configured from spring.mail.* properties

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * ADDED: Sends an email containing the verification link to the newly registered user.
     * The link points to GET /api/auth/verify-email?token=<uuid>
     *
     * @param toEmail   recipient's email address
     * @param token     UUID stored in User.emailVerifyToken
     */
    public void sendVerificationEmail(String toEmail, String token) {
        String verifyUrl = baseUrl + "/api/auth/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Verify your email address");
        message.setText(
            "Hello!\n\n" +
            "Thank you for registering. Please verify your email by clicking the link below:\n\n" +
            verifyUrl + "\n\n" +
            "This link expires in 24 hours.\n\n" +
            "If you did not register, please ignore this email."
        );

        mailSender.send(message);  // throws MailException if SMTP fails
    }
}
