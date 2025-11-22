package com.sedroad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedroad.entity.Room;
import com.sedroad.entity.RoomParticipant;
import com.sedroad.entity.TripRecommendation;
import com.sedroad.entity.User;
import com.sedroad.repository.RoomParticipantRepository;
import com.sedroad.repository.RoomRepository;
import com.sedroad.repository.TripRecommendationRepository;
import com.sedroad.repository.UserProfileRepository;
import com.sedroad.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@org.springframework.context.annotation.Import(com.sedroad.config.TestSecurityConfig.class)
class RoomChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private TripRecommendationRepository tripRecommendationRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Room testRoom;
    private TripRecommendation testRecommendation;

    @BeforeEach
    void setUp() {
        roomParticipantRepository.deleteAll();
        tripRecommendationRepository.deleteAll();
        roomRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email("chattest@example.com")
                .password(passwordEncoder.encode("password"))
                .name("Chat Test User")
                .generation("30대")
                .build();
        testUser = userRepository.save(testUser);

        testRoom = Room.builder()
                .id(UUID.randomUUID().toString())
                .name("Chat Test Room")
                .inviteCode("CHATCODE")
                .createdBy(testUser)
                .isActive(true)
                .build();
        testRoom = roomRepository.save(testRoom);

        RoomParticipant participant = RoomParticipant.builder()
                .id(UUID.randomUUID().toString())
                .room(testRoom)
                .user(testUser)
                .role(RoomParticipant.Role.owner)
                .build();
        roomParticipantRepository.save(participant);

        testRecommendation = TripRecommendation.builder()
                .id(UUID.randomUUID().toString())
                .title("Test Recommendation")
                .room(testRoom)
                .user(testUser)
                .type(TripRecommendation.Type.personal)
                .course(Arrays.asList("장소1", "장소2"))
                .why("테스트 추천")
                .build();
        testRecommendation = tripRecommendationRepository.save(testRecommendation);
    }

    @Test
    void testGetComments_Success() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/comments", testRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray());
    }

    @Test
    void testGetComments_RoomNotFound() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/comments", "non-existent-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists()); // 에러 메시지 확인
    }

    @Test
    void testCreateComment_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());
        request.put("content", "Test comment content");

        mockMvc.perform(post("/api/rooms/{roomId}/comments", testRoom.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.comment.content").value("Test comment content"))
                .andExpect(jsonPath("$.comment.userId").value(testUser.getId()));
    }

    @Test
    void testCreateComment_MissingFields() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());
        // content 누락

        mockMvc.perform(post("/api/rooms/{roomId}/comments", testRoom.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("사용자 ID와 내용이 필요합니다."));
    }

    @Test
    void testGetVotes_Success() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/votes", testRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes").exists());
    }

    @Test
    void testCreateVote_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());
        request.put("recommendationId", testRecommendation.getId());

        mockMvc.perform(post("/api/rooms/{roomId}/votes", testRoom.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("투표가 완료되었습니다."));
    }

    @Test
    void testCreateVote_Duplicate() throws Exception {
        // 첫 번째 투표
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());
        request.put("recommendationId", testRecommendation.getId());

        mockMvc.perform(post("/api/rooms/{roomId}/votes", testRoom.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 두 번째 투표 (중복)
        mockMvc.perform(post("/api/rooms/{roomId}/votes", testRoom.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("이미 투표하셨습니다."));
    }

    @Test
    void testCreateVote_MissingFields() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());
        // recommendationId 누락

        mockMvc.perform(post("/api/rooms/{roomId}/votes", testRoom.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자 ID와 여행지 ID가 필요합니다."));
    }
}

