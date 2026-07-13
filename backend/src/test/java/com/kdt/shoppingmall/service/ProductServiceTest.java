package com.kdt.shoppingmall.service;

import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.dto.product.ProductRequest;
import com.kdt.shoppingmall.dto.product.ProductResponse;
import com.kdt.shoppingmall.exception.ResourceNotFoundException;
import com.kdt.shoppingmall.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void create_성공() {
        ProductRequest request = new ProductRequest("상품A", "설명", 10000, 100, null, null);
        Product product = new Product("상품A", "설명", 10000, 100, null);
        given(productRepository.save(any(Product.class))).willReturn(product);

        ProductResponse response = productService.create(request);

        assertThat(response.name()).isEqualTo("상품A");
        assertThat(response.price()).isEqualTo(10000);
        assertThat(response.stockQuantity()).isEqualTo(100);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void findAll_성공() {
        Product p1 = new Product("상품A", "설명A", 10000, 50, null);
        Product p2 = new Product("상품B", "설명B", 20000, 30, null);
        given(productRepository.findAll()).willReturn(List.of(p1, p2));

        List<ProductResponse> responses = productService.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).name()).isEqualTo("상품A");
        assertThat(responses.get(1).name()).isEqualTo("상품B");
    }

    @Test
    void findById_성공() {
        Product product = new Product("상품A", "설명", 10000, 100, null);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        ProductResponse response = productService.findById(1L);

        assertThat(response.name()).isEqualTo("상품A");
    }

    @Test
    void findById_존재하지않는상품_예외발생() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_성공() {
        Product product = new Product("기존상품", "기존설명", 5000, 10, null);
        ProductRequest request = new ProductRequest("수정상품", "수정설명", 9000, 20, null, null);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        ProductResponse response = productService.update(1L, request);

        assertThat(response.name()).isEqualTo("수정상품");
        assertThat(response.price()).isEqualTo(9000);
        assertThat(response.stockQuantity()).isEqualTo(20);
    }

    @Test
    void delete_성공() {
        Product product = new Product("상품A", "설명", 10000, 100, null);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        productService.delete(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void delete_존재하지않는상품_예외발생() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
