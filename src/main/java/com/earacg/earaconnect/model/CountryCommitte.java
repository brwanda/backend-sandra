package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountryCommitte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "Committee_id")
    private Committee committee;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;
}
