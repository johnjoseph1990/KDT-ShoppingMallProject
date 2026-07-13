package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.product.ProductTag;
import com.kdt.shoppingmall.dto.product.ProductRequest;
import com.kdt.shoppingmall.dto.product.ProductResponse;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.ProductRepository;
import com.kdt.shoppingmall.repository.ReviewRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

  private final ProductRepository productRepository;
  private final ReviewRepository reviewRepository;

  public ProductService(ProductRepository productRepository, ReviewRepository reviewRepository) {
    this.productRepository = productRepository;
    this.reviewRepository = reviewRepository;
  }

  @Transactional
  public ProductResponse create(ProductRequest request) {
    Product product =
        new Product(
            request.name(),
            request.description(),
            request.price(),
            request.stockQuantity(),
            request.imageUrl());
    addTags(product, request.tags());
    return toResponse(productRepository.save(product));
  }

  public List<ProductResponse> findAll() {
    return productRepository.findAll().stream().map(this::toResponse).toList();
  }

  public Page<ProductResponse> search(String keyword, String tag, Pageable pageable) {
    return productRepository.searchProducts(keyword, tag, pageable).map(this::toResponse);
  }

  public Page<ProductResponse> findBestProducts(Pageable pageable) {
    return productRepository.findAllOrderByAverageRatingDesc(pageable).map(this::toResponse);
  }

  public ProductResponse findById(Long id) {
    return toResponse(getProductOrThrow(id));
  }

  @Transactional
  public ProductResponse update(Long id, ProductRequest request) {
    Product product = getProductOrThrow(id);
    product.update(
        request.name(),
        request.description(),
        request.price(),
        request.stockQuantity(),
        request.imageUrl());
    product.clearTags();
    addTags(product, request.tags());
    return toResponse(product);
  }

  @Transactional
  public void delete(Long id) {
    Product product = getProductOrThrow(id);
    productRepository.delete(product);
  }

  private Product getProductOrThrow(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다. id=" + id));
  }

  private ProductResponse toResponse(Product product) {
    Double averageRating = reviewRepository.findAverageRatingByProductId(product.getId());
    return ProductResponse.from(product, averageRating);
  }

  private void addTags(Product product, List<String> tags) {
    if (tags == null) {
      return;
    }
    tags.forEach(name -> product.addTag(new ProductTag(name)));
  }
}
