package com.sedroad.repository;

import com.sedroad.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findByInviteCode(String inviteCode);
    
    @Query("SELECT r FROM Room r WHERE r.createdBy.id = :userId OR r.id IN " +
           "(SELECT rp.room.id FROM RoomParticipant rp WHERE rp.user.id = :userId)")
    List<Room> findByUserId(@Param("userId") String userId);
}

