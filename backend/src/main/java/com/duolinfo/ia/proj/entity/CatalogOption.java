package com.duolinfo.ia.proj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "catalog_options",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"catalog_type", "option_value"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class CatalogOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "catalog_type", nullable = false, length = 40)
    private String catalogType;

    @Column(name = "option_value", nullable = false, length = 100)
    private String value;

    @Column(nullable = false)
    private boolean active = true;

    public CatalogOption(String catalogType, String value) {
        this.catalogType = catalogType;
        this.value = value;
        this.active = true;
    }
}
