package com.sedroad.repository;

import com.sedroad.entity.Room;
import com.sedroad.entity.RoomVote;
import com.sedroad.entity.TripRecommendation;
import com.sedroad.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface RoomVoteRepository extends JpaRepository<RoomVote, String> {
    List<RoomVote> findByRoom(Room room);
    
    @Query("SELECT rv.recommendation.id, COUNT(rv) FROM RoomVote rv WHERE rv.room.id = :roomId GROUP BY rv.recommendation.id")
    List<Object[]> countVotesByRoomId(@Param("roomId") String roomId);
    
    Optional<RoomVote> findByRoomAndUserAndRecommendation(Room room, User user, TripRecommendation recommendation);
    
    boolean existsByRoomAndUserAndRecommendation(Room room, User user, TripRecommendation recommendation);
}

