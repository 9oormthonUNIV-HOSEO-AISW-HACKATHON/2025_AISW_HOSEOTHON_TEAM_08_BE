package com.sedroad.repository;

import com.sedroad.entity.Room;
import com.sedroad.entity.TripRecommendation;
import com.sedroad.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRecommendationRepository extends JpaRepository<TripRecommendation, String> {
    List<TripRecommendation> findByUser(User user);
    List<TripRecommendation> findByRoom(Room room);
    
    @Query("SELECT tr FROM TripRecommendation tr WHERE tr.room.id = :roomId")
    List<TripRecommendation> findByRoomId(@Param("roomId") String roomId);
}

