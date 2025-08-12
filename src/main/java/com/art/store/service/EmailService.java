//package com.art.store.service;
//
//import com.art.store.entity.Order;
//import com.art.store.entity.OrderItem;
//import jakarta.mail.MessagingException;
//import jakarta.mail.internet.MimeMessage;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.time.format.DateTimeFormatter;
//
//@Service
//public class EmailService {
//
//    private final JavaMailSender mailSender;
//
//    @Value("${app.business.name:Will Dawgs art store}")
//    private String businessName;
//
//    @Value("${app.business.email:freestickerdotorg@gmail.com}")
//    private String businessEmail;
//
//    @Value("${app.business.support-email:freestickerdotorg@gmail.com}")
//    private String supportEmail;
//
//    @Value("${app.frontend.url:http://localhost:3000}")
//    private String frontendUrl;
//
//    @Autowired
//    public EmailService(JavaMailSender mailSender) {
//        this.mailSender = mailSender;
//    }
//
//    public void sendOrderConfirmationToCustomer(Order order) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//            helper.setFrom(businessEmail);
//            helper.setTo(order.getEmail());
//            helper.setSubject("Order Confirmation - " + businessName + " #" + order.getId());
//
//            String htmlContent = generateCustomerEmailTemplate(order);
//            helper.setText(htmlContent, true);
//
//            mailSender.send(message);
//            System.out.println("Order confirmation email sent to customer: " + order.getEmail());
//
//        } catch (MessagingException e) {
//            System.err.println("Failed to send order confirmation email to customer: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    public void sendOrderNotificationToAdmin(Order order) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//            helper.setFrom(businessEmail);
//            helper.setTo(businessEmail);
//            helper.setSubject("New Order Received - #" + order.getId());
//
//            String htmlContent = generateAdminEmailTemplate(order);
//            helper.setText(htmlContent, true);
//
//            mailSender.send(message);
//            System.out.println("Order notification email sent to admin: " + businessEmail);
//
//        } catch (MessagingException e) {
//            System.err.println("Failed to send order notification email to admin: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private String generateCustomerEmailTemplate(Order order) {
//        StringBuilder sb = new StringBuilder();
//
//        sb.append("<!DOCTYPE html>");
//        sb.append("<html><head><meta charset='UTF-8'>");
//        sb.append("<style>");
//        sb.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
//        sb.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
//        sb.append(".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }");
//        sb.append(".content { padding: 20px; }");
//        sb.append(".order-details { border: 1px solid #ddd; border-radius: 5px; padding: 15px; margin: 20px 0; }");
//        sb.append(".item { border-bottom: 1px solid #eee; padding: 10px 0; }");
//        sb.append(".item:last-child { border-bottom: none; }");
//        sb.append(".total { font-weight: bold; font-size: 1.2em; color: #4CAF50; }");
//        sb.append(".footer { background-color: #f8f9fa; padding: 20px; text-align: center; font-size: 0.9em; }");
//        sb.append("</style></head><body>");
//
//        sb.append("<div class='container'>");
//        sb.append("<div class='header'>");
//        sb.append("<h1>Order Confirmation</h1>");
//        sb.append("<p>Thank you for your purchase!</p>");
//        sb.append("</div>");
//
//        sb.append("<div class='content'>");
//        sb.append("<p>Dear ").append(order.getFirstName()).append(" ").append(order.getLastName()).append(",</p>");
//        sb.append("<p>Thank you for your order! We're excited to let you know that your payment has been successfully processed.</p>");
//
//        sb.append("<div class='order-details'>");
//        sb.append("<h3>Order Details</h3>");
//        sb.append("<p><strong>Order Number:</strong> #").append(order.getId()).append("</p>");
//        sb.append("<p><strong>Order Date:</strong> ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"))).append("</p>");
//        sb.append("<p><strong>Email:</strong> ").append(order.getEmail()).append("</p>");
//        if (order.getPhone() != null && !order.getPhone().trim().isEmpty()) {
//            sb.append("<p><strong>Phone:</strong> ").append(order.getPhone()).append("</p>");
//        }
//
//        sb.append("<h4>Items Ordered:</h4>");
//        for (OrderItem item : order.getOrderItems()) {
//            sb.append("<div class='item'>");
//            sb.append("<strong>").append(item.getProduct().getName()).append("</strong><br>");
//            sb.append("Quantity: ").append(item.getQuantity()).append("<br>");
//            sb.append("Price: $").append(item.getUnitPrice()).append(" each<br>");
//            sb.append("Subtotal: $").append(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
//            sb.append("</div>");
//        }
//
//        sb.append("<div class='total'>");
//        sb.append("<p>Total Amount: $").append(order.getTotalAmount()).append("</p>");
//        sb.append("</div>");
//        sb.append("</div>");
//
//        sb.append("<h3>Shipping Information</h3>");
//        sb.append("<p>").append(order.getFirstName()).append(" ").append(order.getLastName()).append("<br>");
//        sb.append(order.getAddress()).append("<br>");
//        sb.append(order.getCity()).append(", ").append(order.getState()).append(" ").append(order.getZipCode()).append("</p>");
//
//        sb.append("<p>We'll send you another email once your order has been shipped. If you have any questions, please don't hesitate to contact us at ").append(supportEmail).append(".</p>");
//        sb.append("</div>");
//
//        sb.append("<div class='footer'>");
//        sb.append("<p>Thank you for choosing ").append(businessName).append("!</p>");
//        sb.append("<p>Visit us at <a href='").append(frontendUrl).append("'>").append(frontendUrl).append("</a></p>");
//        sb.append("</div>");
//        sb.append("</div>");
//
//        sb.append("</body></html>");
//
//        return sb.toString();
//    }
//
//    private String generateAdminEmailTemplate(Order order) {
//        StringBuilder sb = new StringBuilder();
//
//        sb.append("<!DOCTYPE html>");
//        sb.append("<html><head><meta charset='UTF-8'>");
//        sb.append("<style>");
//        sb.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
//        sb.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
//        sb.append(".header { background-color: #007bff; color: white; padding: 20px; text-align: center; }");
//        sb.append(".content { padding: 20px; }");
//        sb.append(".order-details { border: 1px solid #ddd; border-radius: 5px; padding: 15px; margin: 20px 0; }");
//        sb.append(".item { border-bottom: 1px solid #eee; padding: 10px 0; }");
//        sb.append(".item:last-child { border-bottom: none; }");
//        sb.append(".total { font-weight: bold; font-size: 1.2em; color: #007bff; }");
//        sb.append("</style></head><body>");
//
//        sb.append("<div class='container'>");
//        sb.append("<div class='header'>");
//        sb.append("<h1>New Order Received</h1>");
//        sb.append("<p>Order #").append(order.getId()).append("</p>");
//        sb.append("</div>");
//
//        sb.append("<div class='content'>");
//        sb.append("<p>A new order has been placed and payment has been successfully processed.</p>");
//
//        sb.append("<div class='order-details'>");
//        sb.append("<h3>Order Information</h3>");
//        sb.append("<p><strong>Order Number:</strong> #").append(order.getId()).append("</p>");
//        sb.append("<p><strong>Order Date:</strong> ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"))).append("</p>");
//        sb.append("<p><strong>Payment Status:</strong> ").append(order.getPaymentStatus()).append("</p>");
//        sb.append("<p><strong>Order Status:</strong> ").append(order.getStatus()).append("</p>");
//
//        sb.append("<h4>Customer Information</h4>");
//        sb.append("<p><strong>Name:</strong> ").append(order.getFirstName()).append(" ").append(order.getLastName()).append("</p>");
//        sb.append("<p><strong>Email:</strong> ").append(order.getEmail()).append("</p>");
//        sb.append("<p><strong>Phone:</strong> ").append(order.getPhone() != null ? order.getPhone() : "Not provided").append("</p>");
//
//        sb.append("<h4>Shipping Address</h4>");
//        sb.append("<p>").append(order.getAddress()).append("<br>");
//        sb.append(order.getCity()).append(", ").append(order.getState()).append(" ").append(order.getZipCode()).append("</p>");
//
//        sb.append("<h4>Items Ordered</h4>");
//        for (OrderItem item : order.getOrderItems()) {
//            sb.append("<div class='item'>");
//            sb.append("<strong>").append(item.getProduct().getName()).append("</strong><br>");
//            sb.append("SKU: ").append(item.getProduct().getId() != null ? item.getProduct().getId() : "N/A").append("<br>");
//            sb.append("Quantity: ").append(item.getQuantity()).append("<br>");
//            sb.append("Price: $").append(item.getUnitPrice()).append(" each<br>");
//            sb.append("Subtotal: $").append(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
//            sb.append("</div>");
//        }
//
//        sb.append("<div class='total'>");
//        sb.append("<p>Total Amount: $").append(order.getTotalAmount()).append("</p>");
//        sb.append("</div>");
//        sb.append("</div>");
//
//        sb.append("<p>Please process this order and update the shipping status accordingly.</p>");
//        sb.append("</div>");
//        sb.append("</div>");
//
//        sb.append("</body></html>");
//
//        return sb.toString();
//    }
//}