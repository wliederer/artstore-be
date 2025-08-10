package com.art.store.config;

import com.art.store.entity.Product;
import com.art.store.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Autowired
    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only initialize data if the database is empty
        if (productRepository.count() == 0) {
            initializeProducts();
        }
    }

    private void initializeProducts() {
        List<Product> sampleProducts = Arrays.asList(
            createProduct("ESSENTIAL TEE", new BigDecimal("90.00"), 
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400&h=500&fit=crop", 
                "APPAREL", "Essential cotton tee for everyday wear", 100),
                
            createProduct("VINTAGE HOODIE", new BigDecimal("200.00"),
                "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=400&h=500&fit=crop",
                "APPAREL", "Comfortable vintage-style hoodie", 50),
                
            createProduct("OVERSIZED SHIRT", new BigDecimal("120.00"),
                "https://images.unsplash.com/photo-1583743814966-8936f37f605b?w=400&h=500&fit=crop",
                "APPAREL", "Relaxed fit oversized shirt", 75),
                
            createProduct("DENIM JACKET", new BigDecimal("350.00"),
                "https://images.unsplash.com/photo-1544022613-e87ca75a784a?w=400&h=500&fit=crop",
                "OUTERWEAR", "Classic denim jacket with modern fit", 30),
                
            createProduct("CARGO PANTS", new BigDecimal("180.00"),
                "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=400&h=500&fit=crop",
                "BOTTOMS", "Functional cargo pants with multiple pockets", 60),
                
            createProduct("CLASSIC TEE", new BigDecimal("75.00"),
                "https://images.unsplash.com/photo-1503341504253-dff4815485f1?w=400&h=500&fit=crop",
                "APPAREL", "Timeless classic tee shirt", 120),
                
            createProduct("UTILITY VEST", new BigDecimal("280.00"),
                "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=400&h=500&fit=crop",
                "OUTERWEAR", "Multi-pocket utility vest", 25),
                
            createProduct("RELAXED FIT TEE", new BigDecimal("95.00"),
                "https://images.unsplash.com/photo-1571945153237-4929e783af4a?w=400&h=500&fit=crop",
                "APPAREL", "Comfortable relaxed fit tee", 80)
        );

        productRepository.saveAll(sampleProducts);
        System.out.println("Sample products initialized successfully!");
    }

    private Product createProduct(String name, BigDecimal price, String image, 
                                String category, String description, Integer stock) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setImage(image);
        product.setCategory(category);
        product.setDescription(description);
        product.setStockQuantity(stock);
        product.setActive(true);
        return product;
    }
}