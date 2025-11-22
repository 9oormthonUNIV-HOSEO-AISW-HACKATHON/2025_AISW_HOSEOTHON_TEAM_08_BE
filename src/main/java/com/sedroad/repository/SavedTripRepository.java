package com.sedroad.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sedroad.entity.SavedTrip;
import com.sedroad.entity.User;

@Repository
public interface SavedTripRepository extends JpaRepository<SavedTrip, String> {
    List<SavedTrip> findByUser(User user);
    
    @Query("SELECT st FROM SavedTrip st WHERE st.user.id = :userId")
    List<SavedTrip> findByUserId(@Param("userId") String userId);
    
    @Query("SELECT st FROM SavedTrip st WHERE st.user = :user AND st.recommendation.id = :recommendationId")
    Optional<SavedTrip> findByUserAndRecommendationId(@Param("user") User user, @Param("recommendationId") String recommendationId);
    
    @Query("SELECT COUNT(st) > 0 FROM SavedTrip st WHERE st.user = :user AND st.recommendation.id = :recommendationId")
    boolean existsByUserAndRecommendationId(@Param("user") User user, @Param("recommendationId") String recommendationId);
}

