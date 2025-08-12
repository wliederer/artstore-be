package com.art.store.controller;

import com.art.store.dto.ProductDto;
import com.art.store.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class WebController {
    
    private final ProductService productService;
    
    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;
    
    @Value("${app.business.name}")
    private String businessName;
    
    @Autowired
    public WebController(ProductService productService) {
        this.productService = productService;
    }
    
    @GetMapping("/")
    public String index(Model model) {
        List<ProductDto> products = productService.getAllActiveProducts();
        model.addAttribute("products", products);
        model.addAttribute("businessName", businessName);
        model.addAttribute("stripePublishableKey", stripePublishableKey);
        return "index";
    }
    
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam(required = false) String session_id,
                                @RequestParam(required = false) String order_id,
                                Model model) {
        model.addAttribute("sessionId", session_id);
        model.addAttribute("orderId", order_id);
        model.addAttribute("businessName", businessName);
        return "success";
    }
    
    @GetMapping("/cancel")
    public String paymentCancel(Model model) {
        model.addAttribute("businessName", businessName);
        return "cancel";
    }
}