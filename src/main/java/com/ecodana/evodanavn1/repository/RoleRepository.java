package com.ecodana.evodanavn1.repository;

import java.util.Optional;

import com.ecodana.evodanavn1.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    
    /**
     * Find role by role name
     * @param roleName the role name to search for
     * @return Optional containing the role if found
     */
    Optional<Role> findByRoleName(String roleName);
    
    /**
     * Find role by normalized name
     * @param normalizedName the normalized name to search for
     * @return Optional containing the role if found
     */
    Optional<Role> findByNormalizedName(String normalizedName);
    
    /**
     * Check if role exists by role name
     * @param roleName the role name to check
     * @return true if role exists, false otherwise
     */
    boolean existsByRoleName(String roleName);
    
    /**
     * Check if role exists by normalized name
     * @param normalizedName the normalized name to check
     * @return true if role exists, false otherwise
     */
    boolean existsByNormalizedName(String normalizedName);
}
