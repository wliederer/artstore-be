package com.art.store.service;

import com.art.store.dto.ProductDto;
import com.art.store.entity.Product;
import com.art.store.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @Transactional(readOnly = true)
    public List<ProductDto> getAllActiveProducts() {
        return productRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(ProductDto::new)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Optional<ProductDto> getProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(ProductDto::new);
    }
    
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByActiveTrueAndCategoryIgnoreCase(category)
                .stream()
                .map(ProductDto::new)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ProductDto> searchProducts(String searchTerm) {
        return productRepository.findByActiveTrueAndSearchTerm(searchTerm)
                .stream()
                .map(ProductDto::new)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findDistinctCategoriesByActiveTrue();
    }
    
    @Transactional(readOnly = true)
    public List<ProductDto> getAvailableProducts() {
        return productRepository.findAvailableProducts()
                .stream()
                .map(ProductDto::new)
                .collect(Collectors.toList());
    }
    
    public ProductDto createProduct(ProductDto productDto) {
        Product product = productDto.toEntity();
        Product savedProduct = productRepository.save(product);
        return new ProductDto(savedProduct);
    }
    
    public Optional<ProductDto> updateProduct(Long id, ProductDto productDto) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setName(productDto.getName());
                    existingProduct.setPrice(productDto.getPrice());
                    existingProduct.setImage(productDto.getImage());
                    existingProduct.setCategory(productDto.getCategory());
                    existingProduct.setDescription(productDto.getDescription());
                    existingProduct.setStockQuantity(productDto.getStockQuantity());
                    existingProduct.setActive(productDto.getActive());
                    
                    Product updatedProduct = productRepository.save(existingProduct);
                    return new ProductDto(updatedProduct);
                });
    }
    
    public boolean deleteProduct(Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setActive(false);
                    productRepository.save(product);
                    return true;
                })
                .orElse(false);
    }
    
    public boolean updateStock(Long productId, Integer newStock) {
        return productRepository.findById(productId)
                .map(product -> {
                    product.setStockQuantity(newStock);
                    productRepository.save(product);
                    return true;
                })
                .orElse(false);
    }
    
    public boolean reduceStock(Long productId, Integer quantity) {
        return productRepository.findById(productId)
                .map(product -> {
                    if (product.getStockQuantity() >= quantity) {
                        product.setStockQuantity(product.getStockQuantity() - quantity);
                        productRepository.save(product);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}