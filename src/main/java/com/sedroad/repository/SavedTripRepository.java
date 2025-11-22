package com.sedroad.repository;

import com.sedroad.entity.SavedTrip;
import com.sedroad.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedTripRepository extends JpaRepository<SavedTrip, String> {
    List<SavedTrip> findByUser(User user);
    
    @Query("SELECT st FROM SavedTrip st WHERE st.user.id = :userId")
    List<SavedTrip> findByUserId(@Param("userId") String userId);
    
    Optional<SavedTrip> findByUserAndRecommendationId(User user, String recommendationId);
    
    boolean existsByUserAndRecommendationId(User user, String recommendationId);
}

