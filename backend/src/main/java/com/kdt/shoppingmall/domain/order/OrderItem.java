package com.kdt.shoppingmall.domain.order;

import com.kdt.shoppingmall.domain.product.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "orders_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @jakarta.persistence.Column(nullable = false)
    private int orderPrice;

    @jakarta.persistence.Column(nullable = false)
    private int quantity;

    public OrderItem(Product product, int orderPrice, int quantity) {
        this.product = product;
        this.orderPrice = orderPrice;
        this.quantity = quantity;
    }

    void assignOrder(Order order) {
        this.order = order;
    }
}
