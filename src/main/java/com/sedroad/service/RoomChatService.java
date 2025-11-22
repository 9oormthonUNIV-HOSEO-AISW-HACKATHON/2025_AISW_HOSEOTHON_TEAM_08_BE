package com.sedroad.service;

import com.sedroad.entity.Room;
import com.sedroad.entity.RoomComment;
import com.sedroad.entity.RoomVote;
import com.sedroad.entity.TripRecommendation;
import com.sedroad.entity.User;
import com.sedroad.repository.RoomCommentRepository;
import com.sedroad.repository.RoomRepository;
import com.sedroad.repository.RoomVoteRepository;
import com.sedroad.repository.TripRecommendationRepository;
import com.sedroad.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomChatService {
    
    private final RoomCommentRepository roomCommentRepository;
    private final RoomVoteRepository roomVoteRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final TripRecommendationRepository tripRecommendationRepository;
    
    public List<CommentDto> getComments(String roomId) {
        List<RoomComment> comments = roomCommentRepository.findByRoomId(roomId);
        
        return comments.stream().map(comment -> {
            CommentDto dto = new CommentDto();
            dto.setId(comment.getId());
            dto.setUserId(comment.getUser().getId());
            dto.setUserName(comment.getUser().getName());
            dto.setContent(comment.getContent());
            dto.setCreatedAt(comment.getCreatedAt().toString());
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Transactional
    public CommentDto createComment(String roomId, String userId, String content) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        RoomComment comment = RoomComment.builder()
                .id(UUID.randomUUID().toString())
                .room(room)
                .user(user)
                .content(content)
                .build();
        
        comment = roomCommentRepository.save(comment);
        
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setUserId(comment.getUser().getId());
        dto.setUserName(comment.getUser().getName());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt().toString());
        
        return dto;
    }
    
    public Map<String, Integer> getVotes(String roomId) {
        List<Object[]> voteCounts = roomVoteRepository.countVotesByRoomId(roomId);
        
        Map<String, Integer> votes = new HashMap<>();
        for (Object[] row : voteCounts) {
            String recommendationId = (String) row[0];
            Long count = (Long) row[1];
            votes.put(recommendationId, count.intValue());
        }
        
        return votes;
    }
    
    @Transactional
    public VoteResponse createVote(String roomId, String userId, String recommendationId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        TripRecommendation recommendation = tripRecommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("추천을 찾을 수 없습니다."));
        
        if (roomVoteRepository.existsByRoomAndUserAndRecommendation(room, user, recommendation)) {
            VoteResponse response = new VoteResponse();
            response.setSuccess(true);
            response.setMessage("이미 투표하셨습니다.");
            return response;
        }
        
        RoomVote vote = RoomVote.builder()
                .id(UUID.randomUUID().toString())
                .room(room)
                .user(user)
                .recommendation(recommendation)
                .build();
        
        roomVoteRepository.save(vote);
        
        VoteResponse response = new VoteResponse();
        response.setSuccess(true);
        response.setMessage("투표가 완료되었습니다.");
        return response;
    }
    
    @lombok.Data
    public static class CommentDto {
        private String id;
        private String userId;
        private String userName;
        private String content;
        private String createdAt;
    }
    
    @lombok.Data
    public static class VoteResponse {
        private Boolean success;
        private String message;
    }
}

