package com.recommend.movie.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api.key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${sendgrid.api.key:${SENDGRID_API_KEY:}}")
    private String sendgridApiKey;

    @Value("${email.from.address:${EMAIL_FROM_ADDRESS:}}")
    private String emailFromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private boolean sendRestEmail(String toEmail, String subject, String htmlContent) {
        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            try {
                log.info("Attempting to send email via Resend API to: {}", toEmail);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + resendApiKey.trim());

                String from = (emailFromAddress != null && !emailFromAddress.trim().isEmpty()) 
                        ? emailFromAddress 
                        : "CineMatch <onboarding@resend.dev>";

                Map<String, Object> payload = new HashMap<>();
                payload.put("from", from);
                payload.put("to", Collections.singletonList(toEmail));
                payload.put("subject", subject);
                payload.put("html", htmlContent);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.postForEntity("https://api.resend.com/emails", entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Email successfully sent via Resend API to: {}", toEmail);
                    return true;
                } else {
                    log.error("Failed to send email via Resend API. Response code: {}, Body: {}", response.getStatusCode(), response.getBody());
                }
            } catch (Exception e) {
                log.error("Exception occurred while sending email via Resend API to {}: {}", toEmail, e.getMessage());
            }
        }

        if (sendgridApiKey != null && !sendgridApiKey.trim().isEmpty()) {
            try {
                log.info("Attempting to send email via SendGrid API to: {}", toEmail);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + sendgridApiKey.trim());

                String fromEmail = (emailFromAddress != null && !emailFromAddress.trim().isEmpty()) 
                        ? emailFromAddress 
                        : "noreply@cinematch.com";

                Map<String, Object> payload = new HashMap<>();
                
                Map<String, Object> fromMap = new HashMap<>();
                fromMap.put("email", fromEmail);
                fromMap.put("name", "CineMatch");
                payload.put("from", fromMap);
                
                payload.put("subject", subject);
                
                Map<String, Object> personalization = new HashMap<>();
                Map<String, Object> toMap = new HashMap<>();
                toMap.put("email", toEmail);
                personalization.put("to", Collections.singletonList(toMap));
                payload.put("personalizations", Collections.singletonList(personalization));
                
                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("type", "text/html");
                contentMap.put("value", htmlContent);
                payload.put("content", Collections.singletonList(contentMap));

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = restTemplate.postForEntity("https://api.sendgrid.com/v3/mail/send", entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Email successfully sent via SendGrid API to: {}", toEmail);
                    return true;
                } else {
                    log.error("Failed to send email via SendGrid API. Response code: {}, Body: {}", response.getStatusCode(), response.getBody());
                }
            } catch (Exception e) {
                log.error("Exception occurred while sending email via SendGrid API to {}: {}", toEmail, e.getMessage());
            }
        }

        return false;
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Cannot send welcome email: email address is empty");
            return;
        }

        log.info("Preparing welcome email for: {}", toEmail);

        // ClickUp-style premium HTML newsletter
        String htmlContent = "<!DOCTYPE html>"
            + "<html>"
            + "<head>"
            + "  <meta charset='utf-8'>"
            + "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "  <style>"
            + "    body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f6f8fa; margin: 0; padding: 0; color: #2d3748; -webkit-font-smoothing: antialiased; }"
            + "    .email-container { max-width: 580px; margin: 40px auto; background: #ffffff; border: 1px solid #e1e4e6; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03); }"
            + "    .email-header { background-color: #8b5cf6; padding: 32px 24px; text-align: center; color: #ffffff; }"
            + "    .logo-container { display: inline-flex; align-items: center; justify-content: center; width: 48px; height: 48px; background: rgba(255, 255, 255, 0.2); border-radius: 12px; margin-bottom: 12px; }"
            + "    .logo-svg { font-size: 24px; font-weight: 800; }"
            + "    .email-header h1 { margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.5px; }"
            + "    .email-content { padding: 40px 32px; line-height: 1.6; }"
            + "    .greeting { font-size: 20px; font-weight: 700; color: #1a202c; margin-top: 0; margin-bottom: 16px; }"
            + "    .intro { font-size: 15px; color: #4a5568; margin-bottom: 24px; }"
            + "    .features-card { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; margin-bottom: 32px; }"
            + "    .feature-item { display: flex; align-items: flex-start; margin-bottom: 16px; }"
            + "    .feature-item:last-child { margin-bottom: 0; }"
            + "    .feature-icon { font-size: 20px; margin-right: 12px; line-height: 1; }"
            + "    .feature-details h4 { margin: 0 0 4px 0; font-size: 14px; font-weight: 700; color: #1a202c; }"
            + "    .feature-details p { margin: 0; font-size: 13.5px; color: #718096; }"
            + "    .cta-container { text-align: center; margin: 32px 0; }"
            + "    .cta-btn { display: inline-block; background-color: #8b5cf6; color: #ffffff !important; padding: 14px 32px; font-size: 14.5px; font-weight: 700; text-decoration: none; border-radius: 24px; box-shadow: 0 4px 10px rgba(139, 92, 246, 0.3); transition: transform 0.2s, background-color 0.2s; }"
            + "    .cta-btn:hover { background-color: #7c3aed; }"
            + "    .outro { font-size: 15px; color: #4a5568; border-top: 1px solid #edf2f7; padding-top: 24px; margin-top: 32px; }"
            + "    .outro p { margin: 4px 0; }"
            + "    .email-footer { background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 24px; text-align: center; font-size: 12px; color: #718096; }"
            + "    .email-footer a { color: #8b5cf6; text-decoration: none; font-weight: 500; }"
            + "    .email-footer a:hover { text-decoration: underline; }"
            + "    .social-links { margin-bottom: 12px; }"
            + "    .social-links a { margin: 0 8px; color: #a0aec0; font-size: 16px; }"
            + "  </style>"
            + "</head>"
            + "<body>"
            + "  <div class='email-container'>"
            + "    <div class='email-header'>"
            + "      <div class='logo-container'>"
            + "        <span class='logo-svg'>🍿</span>"
            + "      </div>"
            + "      <h1>Welcome to CineMatch!</h1>"
            + "    </div>"
            + "    <div class='email-content'>"
            + "      <h2 class='greeting'>Settle in, grab some popcorn, " + username + "! 🍿</h2>"
            + "      <p class='intro'>You're officially part of CineMatch—where we align your unique taste coordinates with advanced algorithms to deliver the ultimate cinematic recommendations.</p>"
            + "      "
            + "      <div class='features-card'>"
            + "        <div class='feature-item'>"
            + "          <div class='feature-icon'>🎬</div>"
            + "          <div class='feature-details'>"
            + "            <h4>Personalized Recommendations</h4>"
            + "            <p>Our hybrid AI engine blends content-based overlap with collaborative community filters tailored for you.</p>"
            + "          </div>"
            + "        </div>"
            + "        <div class='feature-item'>"
            + "          <div class='feature-icon'>📈</div>"
            + "          <div class='feature-details'>"
            + "            <h4>AI-Powered Rating Predictor</h4>"
            + "            <p>Know how likely you are to enjoy any movie before you click play, with stars-based matching percentages.</p>"
            + "          </div>"
            + "        </div>"
            + "        <div class='feature-item'>"
            + "          <div class='feature-icon'>🔖</div>"
            + "          <div class='feature-details'>"
            + "            <h4>Smart Watchlist</h4>"
            + "            <p>Build and curate your watchlist, kept in perfect sync between H2 cache and your authority database.</p>"
            + "          </div>"
            + "        </div>"
            + "      </div>"
            + "      "
            + "      <div class='cta-container'>"
            + "        <a href='http://localhost:8080' class='cta-btn' target='_blank'>Explore Your Recommendations</a>"
            + "      </div>"
            + "      "
            + "      <div class='outro'>"
            + "        <p>Happy watching,</p>"
            + "        <p><strong>The CineMatch Team</strong></p>"
            + "      </div>"
            + "    </div>"
            + "    <div class='email-footer'>"
            + "      <div class='social-links'>"
            + "        <a href='#'>🌐</a> | <a href='#'>🐦</a> | <a href='#'>📺</a>"
            + "      </div>"
            + "      <p>You received this email because you registered on CineMatch.</p>"
            + "      <p>© 2026 CineMatch, Inc. All rights reserved.</p>"
            + "    </div>"
            + "  </div>"
            + "</body>"
            + "</html>";

        // Try REST API dispatch first
        if (sendRestEmail(toEmail, "Welcome to CineMatch! 🍿", htmlContent)) {
            return;
        }

        // Fallback to standard SMTP
        log.info("No REST API key found or REST call failed. Falling back to SMTP for welcome email to: {}", toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Welcome to CineMatch! 🍿");
            helper.setFrom(resolveFromAddress());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully via SMTP to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email via SMTP to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Cannot send welcoming OTP email: email address is empty");
            return;
        }

        log.info("Preparing welcoming OTP email for: {}", toEmail);

        // ClickUp-style premium HTML newsletter with OTP
        String htmlContent = "<!DOCTYPE html>"
            + "<html>"
            + "<head>"
            + "  <meta charset='utf-8'>"
            + "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "  <style>"
            + "    body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f6f8fa; margin: 0; padding: 0; color: #2d3748; -webkit-font-smoothing: antialiased; }"
            + "    .email-container { max-width: 580px; margin: 40px auto; background: #ffffff; border: 1px solid #e1e4e6; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03); }"
            + "    .email-header { background-color: #8b5cf6; padding: 32px 24px; text-align: center; color: #ffffff; }"
            + "    .logo-container { display: inline-flex; align-items: center; justify-content: center; width: 48px; height: 48px; background: rgba(255, 255, 255, 0.2); border-radius: 12px; margin-bottom: 12px; }"
            + "    .logo-svg { font-size: 24px; font-weight: 800; }"
            + "    .email-header h1 { margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.5px; }"
            + "    .email-content { padding: 40px 32px; line-height: 1.6; text-align: center; }"
            + "    .greeting { font-size: 20px; font-weight: 700; color: #1a202c; margin-top: 0; margin-bottom: 16px; }"
            + "    .intro { font-size: 15px; color: #4a5568; margin-bottom: 24px; text-align: left; }"
            + "    .otp-card { background-color: #f8fafc; border: 2px dashed #8b5cf6; border-radius: 12px; padding: 24px; margin: 32px 0; display: inline-block; min-width: 200px; }"
            + "    .otp-code { font-size: 36px; font-weight: 800; color: #8b5cf6; letter-spacing: 6px; margin: 0; }"
            + "    .otp-expiry { font-size: 12px; color: #718096; margin-top: 8px; }"
            + "    .features-card { background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 24px; margin-bottom: 32px; text-align: left; }"
            + "    .feature-item { display: flex; align-items: flex-start; margin-bottom: 16px; }"
            + "    .feature-item:last-child { margin-bottom: 0; }"
            + "    .feature-icon { font-size: 20px; margin-right: 12px; line-height: 1; }"
            + "    .feature-details h4 { margin: 0 0 4px 0; font-size: 14px; font-weight: 700; color: #1a202c; }"
            + "    .feature-details p { margin: 0; font-size: 13.5px; color: #718096; }"
            + "    .outro { font-size: 15px; color: #4a5568; border-top: 1px solid #edf2f7; padding-top: 24px; margin-top: 32px; text-align: left; }"
            + "    .outro p { margin: 4px 0; }"
            + "    .email-footer { background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 24px; text-align: center; font-size: 12px; color: #718096; }"
            + "  </style>"
            + "</head>"
            + "<body>"
            + "  <div class='email-container'>"
            + "    <div class='email-header'>"
            + "      <div class='logo-container'>"
            + "        <span class='logo-svg'>🍿</span>"
            + "      </div>"
            + "      <h1>Welcome to CineMatch!</h1>"
            + "    </div>"
            + "    <div class='email-content'>"
            + "      <h2 class='greeting'>Welcome! 🍿</h2>"
            + "      <p class='intro'>You're officially starting your journey with CineMatch! To complete your registration and verify your email address, please use the secure verification code below:</p>"
            + "      "
            + "      <div class='otp-card'>"
            + "        <div class='otp-code'>" + otpCode + "</div>"
            + "        <div class='otp-expiry'>This code is valid for 10 minutes.</div>"
            + "      </div>"
            + "      "
            + "      <p class='intro' style='margin-bottom: 8px;'>Here is what awaits you once you verify your account:</p>"
            + "      <div class='features-card'>"
            + "        <div class='feature-item'>"
            + "          <div class='feature-icon'>🎬</div>"
            + "          <div class='feature-details'>"
            + "            <h4>Personalized Recommendations</h4>"
            + "            <p>Our hybrid AI engine blends content-based overlap with collaborative community filters tailored for you.</p>"
            + "          </div>"
            + "        </div>"
            + "        <div class='feature-item'>"
            + "          <div class='feature-icon'>📈</div>"
            + "          <div class='feature-details'>"
            + "            <h4>AI-Powered Rating Predictor</h4>"
            + "            <p>Know how likely you are to enjoy any movie before you click play, with stars-based matching percentages.</p>"
            + "          </div>"
            + "        </div>"
            + "      </div>"
            + "      "
            + "      <div class='outro'>"
            + "        <p>Happy watching,</p>"
            + "        <p><strong>The CineMatch Team</strong></p>"
            + "      </div>"
            + "    </div>"
            + "    <div class='email-footer'>"
            + "      <p>You received this email because you initiated registration on CineMatch.</p>"
            + "      <p>© 2026 CineMatch, Inc. All rights reserved.</p>"
            + "    </div>"
            + "  </div>"
            + "</body>"
            + "</html>";

        // Try REST API dispatch first
        if (sendRestEmail(toEmail, "Welcome to CineMatch! Verify Your Account 🍿", htmlContent)) {
            return;
        }

        // Fallback to standard SMTP
        log.info("No REST API key found or REST call failed. Falling back to SMTP for welcoming OTP email to: {}", toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Welcome to CineMatch! Verify Your Account 🍿");
            helper.setFrom(resolveFromAddress());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcoming OTP email sent successfully via SMTP to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcoming OTP email via SMTP to {}: {}", toEmail, e.getMessage());
        }
    }

    private String resolveFromAddress() {
        String fromAddress = "CineMatch Team <noreply@cinematch.com>";
        if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl) {
            org.springframework.mail.javamail.JavaMailSenderImpl impl = 
                (org.springframework.mail.javamail.JavaMailSenderImpl) mailSender;
            String configuredUser = impl.getUsername();
            if (configuredUser == null || configuredUser.contains("your-email")) {
                log.warn("ATTENTION: Spring Mail is using default placeholder 'your-email@gmail.com'. " +
                         "To send real emails, you must configure the following environment variables on Railway: " +
                         "SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD");
            } else if (!configuredUser.trim().isEmpty()) {
                fromAddress = configuredUser;
            }
        }
        return fromAddress;
    }
}
