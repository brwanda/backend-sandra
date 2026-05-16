// 2. Enhanced EmailConfig with better properties and debugging

package com.earacg.earaconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Basic configuration
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        // Enhanced properties for Gmail with proper SSL/TLS configuration
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        // STARTTLS configuration (preferred for port 587)
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        
        // SSL Trust and Protocol settings
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        
        // Connection timeouts
        props.put("mail.smtp.connectiontimeout", "30000");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");
        
        // Remove conflicting SSL socket factory settings for STARTTLS
        // These are only needed for SSL (port 465), not STARTTLS (port 587)
        
        // Debug settings (set to false in production)
        props.put("mail.debug", "true");
        props.put("mail.smtp.debug", "true");
        
        // Additional settings for better compatibility
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.ssl.enable", "false"); // Disable SSL for STARTTLS

        System.out.println("📧 Email Configuration:");
        System.out.println("   Host: " + host);
        System.out.println("   Port: " + port);
        System.out.println("   Username: " + username);
        System.out.println("   Password: " + (password != null && !password.isEmpty() ? "[CONFIGURED]" : "[NOT SET]"));

        return mailSender;
    }
}