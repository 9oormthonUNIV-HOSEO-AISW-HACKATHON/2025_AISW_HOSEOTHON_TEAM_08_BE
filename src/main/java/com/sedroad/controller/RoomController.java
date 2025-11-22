package com.sedroad.controller;

import com.sedroad.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "방 관리", description = "여행 방 생성 및 관리 API")
public class RoomController {
    
    private final RoomService roomService;
    
    @PostMapping("/create")
    public ResponseEntity<RoomService.RoomResponse> createRoom(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String roomName = request.get("roomName");
            
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("사용자 ID가 필요합니다."));
            }
            
            RoomService.RoomResponse response = roomService.createRoom(userId, roomName);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/join")
    public ResponseEntity<RoomService.RoomResponse> joinRoom(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String inviteCode = request.get("inviteCode");
            
            if (userId == null || inviteCode == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("사용자 ID와 초대 코드가 필요합니다."));
            }
            
            RoomService.RoomResponse response = roomService.joinRoom(userId, inviteCode);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoom(@PathVariable String roomId) {
        try {
            RoomService.RoomDto room = roomService.getRoom(roomId);
            return ResponseEntity.ok(Map.of("room", room));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRooms(@PathVariable String userId) {
        try {
            List<RoomService.RoomDto> rooms = roomService.getUserRooms(userId);
            return ResponseEntity.ok(Map.of("rooms", rooms));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "방 목록 조회 중 오류가 발생했습니다."));
        }
    }
    
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<Map<String, Object>> getRoomParticipants(@PathVariable String roomId) {
        try {
            List<RoomService.ParticipantDto> participants = roomService.getRoomParticipants(roomId);
            return ResponseEntity.ok(Map.of("participants", participants));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    private RoomService.RoomResponse createErrorResponse(String message) {
        RoomService.RoomResponse response = new RoomService.RoomResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}

