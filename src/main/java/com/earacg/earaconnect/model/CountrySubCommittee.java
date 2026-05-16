package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountrySubCommittee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sub_committe_id")
    private SubCommittee subCommittee;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;
}
