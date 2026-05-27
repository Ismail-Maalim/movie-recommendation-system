package com.recommend.movie.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

public class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeEach
    public void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new EmailService(mailSender);
    }

    @Test
    public void testSendWelcomeEmail_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendWelcomeEmail("test@example.com", "TestUser");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mockMessage);
    }

    @Test
    public void testSendWelcomeEmail_EmptyEmail() {
        emailService.sendWelcomeEmail("", "TestUser");
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    public void testSendOtpEmail_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendOtpEmail("test@example.com", "123456");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mockMessage);
    }

    @Test
    public void testSendOtpEmail_EmptyEmail() {
        emailService.sendOtpEmail("", "123456");
        verify(mailSender, never()).createMimeMessage();
    }
}
