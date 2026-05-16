// // Alternative EmailConfig using SSL (port 465) instead of STARTTLS (port 587)
// // Try this if the STARTTLS version doesn't work

// package com.earacg.earaconnect.config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.mail.javamail.JavaMailSender;
// import org.springframework.mail.javamail.JavaMailSenderImpl;

// import java.util.Properties;

// @Configuration
// public class EmailConfigSSL {

//     @Value("${spring.mail.host:smtp.gmail.com}")
//     private String host;

//     @Value("${spring.mail.port:465}") // Use SSL port
//     private int port;

//     @Value("${spring.mail.username}")
//     private String username;

//     @Value("${spring.mail.password}")
//     private String password;

//     @Bean
//     public JavaMailSender javaMailSender() {
//         JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
//         // Basic configuration for SSL
//         mailSender.setHost(host);
//         mailSender.setPort(port); // Port 465 for SSL
//         mailSender.setUsername(username);
//         mailSender.setPassword(password);
//         mailSender.setDefaultEncoding("UTF-8");

//         // SSL properties for Gmail (port 465)
//         Properties props = mailSender.getJavaMailProperties();
//         props.put("mail.transport.protocol", "smtp");
//         props.put("mail.smtp.auth", "true");
        
//         // SSL configuration (for port 465)
//         props.put("mail.smtp.ssl.enable", "true");
//         props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
//         props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        
//         // Disable STARTTLS for SSL
//         props.put("mail.smtp.starttls.enable", "false");
        
//         // SSL Socket Factory
//         props.put("mail.smtp.socketFactory.port", "465");
//         props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//         props.put("mail.smtp.socketFactory.fallback", "false");
        
//         // Connection timeouts
//         props.put("mail.smtp.connectiontimeout", "30000");
//         props.put("mail.smtp.timeout", "30000");
//         props.put("mail.smtp.writetimeout", "30000");
        
//         // Debug settings
//         props.put("mail.debug", "true");
//         props.put("mail.smtp.debug", "true");
        
//         // Additional settings
//         props.put("mail.smtp.ssl.checkserveridentity", "true");

//         System.out.println("📧 Email Configuration (SSL):");
//         System.out.println("   Host: " + host);
//         System.out.println("   Port: " + port + " (SSL)");
//         System.out.println("   Username: " + username);
//         System.out.println("   Password: " + (password != null && !password.isEmpty() ? "[CONFIGURED]" : "[NOT SET]"));

//         return mailSender;
//     }
// }