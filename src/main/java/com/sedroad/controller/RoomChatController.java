package com.sedroad.controller;

import com.sedroad.service.RoomChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "방 채팅", description = "방 내 댓글 및 투표 API")
public class RoomChatController {
    
    private final RoomChatService roomChatService;
    
    @GetMapping("/{roomId}/comments")
    public ResponseEntity<Map<String, Object>> getComments(@PathVariable String roomId) {
        try {
            List<RoomChatService.CommentDto> comments = roomChatService.getComments(roomId);
            return ResponseEntity.ok(Map.of("comments", comments));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "댓글 조회 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/{roomId}/comments")
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request
    ) {
        try {
            String userId = request.get("userId");
            String content = request.get("content");
            
            if (userId == null || content == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "사용자 ID와 내용이 필요합니다."));
            }
            
            RoomChatService.CommentDto comment = roomChatService.createComment(roomId, userId, content);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "comment", comment
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{roomId}/votes")
    public ResponseEntity<Map<String, Object>> getVotes(@PathVariable String roomId) {
        try {
            Map<String, Integer> votes = roomChatService.getVotes(roomId);
            return ResponseEntity.ok(Map.of("votes", votes));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "투표 조회 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/{roomId}/votes")
    public ResponseEntity<RoomChatService.VoteResponse> createVote(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request
    ) {
        try {
            String userId = request.get("userId");
            String recommendationId = request.get("recommendationId");
            
            if (userId == null || recommendationId == null) {
                RoomChatService.VoteResponse errorResponse = new RoomChatService.VoteResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("사용자 ID와 여행지 ID가 필요합니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            RoomChatService.VoteResponse response = roomChatService.createVote(roomId, userId, recommendationId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            RoomChatService.VoteResponse errorResponse = new RoomChatService.VoteResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

