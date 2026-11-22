package org.example.autoapitesting.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stock;

    @Column
    private String status;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    // Додатковий конструктор під порядок: (id, name, price, category, stock, status)
    public Part(Long id, String name, Double price, String category, Integer stock, String status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = stock != null ? stock : 10;
        this.status = status != null ? status : "ACTIVE";
    }

    // Конструктор під порядок: (id, name, price, category)
    public Part(Long id, String name, Double price, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = 10;
        this.status = "ACTIVE";
    }
}