package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Entity
@Table(name = "committee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Committee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "committee_name")
    private String name;

    @OneToMany(mappedBy = "parentCommittee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SubCommittee> subCommittees;
}
