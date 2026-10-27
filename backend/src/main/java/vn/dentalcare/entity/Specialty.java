package vn.dentalcare.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "specialties")
public class Specialty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String name;
    @Column(length = 500) private String description;
    protected Specialty() {}
    public Specialty(String name, String description) { this.name = name; this.description = description; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
