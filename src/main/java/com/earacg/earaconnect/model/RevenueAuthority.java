package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "revenue_authority")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "revenue_name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;
}
