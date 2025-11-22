package com.sedroad.repository;

import com.sedroad.entity.Room;
import com.sedroad.entity.RoomParticipant;
import com.sedroad.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, String> {
    List<RoomParticipant> findByRoom(Room room);
    
    @Query("SELECT rp FROM RoomParticipant rp WHERE rp.room.id = :roomId")
    List<RoomParticipant> findByRoomId(@Param("roomId") String roomId);
    
    Optional<RoomParticipant> findByRoomAndUser(Room room, User user);
    
    boolean existsByRoomAndUser(Room room, User user);
}

