package com.earacg.earaconnect.repository;

import com.earacg.earaconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByRole(User.UserRole role);

    List<User> findByCountryId(Long countryId);

    Optional<User> findByResetToken(String resetToken);

    List<User> findBySubcommitteeId(Long subcommitteeId);

    boolean existsByEmail(String email);

    List<User> findByActive(boolean active);

    List<User> findByRoleAndSubcommitteeId(User.UserRole role, Long subcommitteeId);

    // Add this method to your existing UserRepo.java

    /**
     * Find users by role and country
     */
    List<User> findByRoleAndCountryId(User.UserRole role, Long countryId);

    /**
     * Find users by role and subcommittee (for notifications)
     */

    /**
     * Find users by committee ID - Custom query needed since User is not directly
     * linked to Committee
     * For now, we'll implement this through a service method that uses
     * CountryCommitteeMember
     */
    // List<User> findByCommitteeId(Long committeeId); // Commented out - no direct
    // relationship

}