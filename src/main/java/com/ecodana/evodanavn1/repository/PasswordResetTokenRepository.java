package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.PasswordResetToken;
import com.ecodana.evodanavn1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    PasswordResetToken findByToken(String token);

    Optional<PasswordResetToken> findByUser(User user);
}
