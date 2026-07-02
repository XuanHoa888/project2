package com.xhstore.cosmetic.controller;

import com.xhstore.cosmetic.model.BoothConfig;
import com.xhstore.cosmetic.model.Product;
import com.xhstore.cosmetic.repository.BoothConfigRepository;
import com.xhstore.cosmetic.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired ProductService productService;
    @Autowired BoothConfigRepository boothConfigRepository;

    /** Lấy danh sách sản phẩm lọc theo danh mục hoặc tìm kiếm. */
    @GetMapping
    public ResponseEntity<List<Product>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {

        if (categoryId != null)
            return ResponseEntity.ok(productService.getByCategory(categoryId));
        if (search != null && !search.trim().isEmpty())
            return ResponseEntity.ok(productService.search(search));
        return ResponseEntity.ok(productService.getActive());
    }

    /** Chi tiết sản phẩm kèm sản phẩm liên quan. */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Map.of(
                "product", productService.getById(id),
                "related", productService.getRelated(id)
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/booths")
    public ResponseEntity<List<BoothConfig>> getPublicBooths() {
        return ResponseEntity.ok(boothConfigRepository.findAll());
    }
}
