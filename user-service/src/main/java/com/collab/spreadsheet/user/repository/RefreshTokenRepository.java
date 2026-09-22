package com.collab.spreadsheet.user.repository;

import com.collab.spreadsheet.user.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RefreshToken (Redis)
 */
@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    
    List<RefreshToken> findByUserId(String userId);
    
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    void deleteByUserId(String userId);
}
