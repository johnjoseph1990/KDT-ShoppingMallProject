package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.product.Product;
import com.kdt.shoppingmall.domain.product.ProductTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositorySearchTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        Product shoes = new Product("나이키 운동화", "편안한 운동화", 89000, 50, null);
        shoes.addTag(new ProductTag("신발"));
        shoes.addTag(new ProductTag("스포츠"));
        em.persist(shoes);

        Product shirt = new Product("폴로 티셔츠", "여름 티셔츠", 45000, 100, null);
        shirt.addTag(new ProductTag("의류"));
        em.persist(shirt);

        Product bag = new Product("나이키 가방", "스포츠 백팩", 120000, 20, null);
        bag.addTag(new ProductTag("가방"));
        bag.addTag(new ProductTag("스포츠"));
        em.persist(bag);

        em.flush();
        em.clear();
    }

    @Test
    void 키워드없이_전체조회() {
        Page<Product> result = productRepository.searchProducts(null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void 상품명_키워드검색() {
        Page<Product> result = productRepository.searchProducts("나이키", null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting("name")
                .containsExactlyInAnyOrder("나이키 운동화", "나이키 가방");
    }

    @Test
    void 설명_키워드검색() {
        Page<Product> result = productRepository.searchProducts("티셔츠", null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("폴로 티셔츠");
    }

    @Test
    void 태그_필터링() {
        Page<Product> result = productRepository.searchProducts(null, "스포츠", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting("name")
                .containsExactlyInAnyOrder("나이키 운동화", "나이키 가방");
    }

    @Test
    void 키워드_태그_복합검색() {
        Page<Product> result = productRepository.searchProducts("나이키", "스포츠", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void 일치결과없음_빈페이지반환() {
        Page<Product> result = productRepository.searchProducts("존재하지않는상품", null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void 페이징_동작확인() {
        Page<Product> page0 = productRepository.searchProducts(null, null, PageRequest.of(0, 2));
        Page<Product> page1 = productRepository.searchProducts(null, null, PageRequest.of(1, 2));

        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getContent()).hasSize(2);
        assertThat(page1.getContent()).hasSize(1);
    }
}
