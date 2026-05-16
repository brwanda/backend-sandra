package com.earacg.earaconnect.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountryCommitteeMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String phone;
    private String email;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne
    @JoinColumn(name = "committee_id")
    private Committee committee;

    @Column(name = "chair")
    @JsonProperty("isChair")
    private boolean isChair;

    @Column(name = "vice_chair")
    @JsonProperty("isViceChair")
    private boolean isViceChair;

    @Column(name = "committee_secretary")
    @JsonProperty("isCommitteeSecretary")
    private boolean isCommitteeSecretary;

    @Column(name = "committee_member")
    @JsonProperty("isCommitteeMember")
    private boolean isCommitteeMember;

    @Column(name = "delegation_secretary")
    @JsonProperty("isDelegationSecretary")
    private boolean isDelegationSecretary;

    @Column(name = "commissioner_general")
    @JsonProperty("isCommissionerGeneral")
    private boolean isCommissionerGeneral;

    /**
     * Determine the user role based on boolean flags.
     * The Chair of the Commissioner General committee IS the Commissioner General.
     * Other positions map to their respective roles.
     * Default (no boolean set) is COMMITTEE_MEMBER.
     */
    public User.UserRole determineUserRole() {
        // Check for explicit commissioner_general flag first
        if (isCommissionerGeneral) {
            return User.UserRole.COMMISSIONER_GENERAL;
        }
        
        if (isChair) {
            String committeeName = committee != null && committee.getName() != null
                    ? committee.getName().trim().toLowerCase()
                    : "";

            if (committeeName.contains("head of delegation")) {
                return User.UserRole.HOD;
            }
            if (committeeName.contains("commissioner general")) {
                return User.UserRole.COMMISSIONER_GENERAL;
            }
            return User.UserRole.CHAIR;
        } else if (isViceChair) {
            return User.UserRole.VICE_CHAIR;
        } else if (isCommitteeSecretary) {
            return User.UserRole.COMMITTEE_SECRETARY;
        } else if (isDelegationSecretary) {
            return User.UserRole.DELEGATION_SECRETARY;
        } else if (isCommitteeMember) {
            return User.UserRole.COMMITTEE_MEMBER;
        } else {
            return User.UserRole.COMMITTEE_MEMBER;
        }
    }

    /**
     * Set boolean flags from a User.UserRole value.
     * Used for reverse-sync when Admin changes User.role.
     * COMMISSIONER_GENERAL maps to isChair since the CG IS the chair of this committee.
     */
    public void applyRoleBooleans(User.UserRole role) {
        this.isChair = (role == User.UserRole.CHAIR || role == User.UserRole.COMMISSIONER_GENERAL || role == User.UserRole.HOD);
        this.isViceChair = (role == User.UserRole.VICE_CHAIR);
        this.isCommitteeSecretary = (role == User.UserRole.COMMITTEE_SECRETARY);
        this.isDelegationSecretary = (role == User.UserRole.DELEGATION_SECRETARY);
        this.isCommitteeMember = (role == User.UserRole.COMMITTEE_MEMBER);
        this.isCommissionerGeneral = (role == User.UserRole.COMMISSIONER_GENERAL);
    }
}