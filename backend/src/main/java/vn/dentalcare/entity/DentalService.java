package vn.dentalcare.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "dental_services")
public class DentalService {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 80) private String category;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false) private int durationMinutes;
    @Column(length = 2000) private String description;
    @Column(nullable = false) private boolean active = true;
    protected DentalService() {}
    public DentalService(String name, String category, BigDecimal price, int durationMinutes, String description) {
        this.name = name; this.category = category; this.price = price; this.durationMinutes = durationMinutes; this.description = description;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public void update(String name,String category,BigDecimal price,int durationMinutes,String description) {
        this.name=name;this.category=category;this.price=price;this.durationMinutes=durationMinutes;this.description=description;
    }
}
