package com.sedroad.repository;

import com.sedroad.entity.Room;
import com.sedroad.entity.RoomComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomCommentRepository extends JpaRepository<RoomComment, String> {
    List<RoomComment> findByRoom(Room room);
    
    @Query("SELECT rc FROM RoomComment rc WHERE rc.room.id = :roomId AND rc.deletedAt IS NULL ORDER BY rc.createdAt ASC")
    List<RoomComment> findByRoomId(@Param("roomId") String roomId);
}

