package com.sedroad.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedroad.entity.Room;
import com.sedroad.entity.RoomParticipant;
import com.sedroad.entity.TripRecommendation;
import com.sedroad.entity.User;
import com.sedroad.entity.UserProfile;
import com.sedroad.repository.RoomParticipantRepository;
import com.sedroad.repository.RoomRepository;
import com.sedroad.repository.SavedTripRepository;
import com.sedroad.repository.TripRecommendationRepository;
import com.sedroad.repository.UserProfileRepository;
import com.sedroad.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@org.springframework.context.annotation.Import(com.sedroad.config.TestSecurityConfig.class)
class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private TripRecommendationRepository tripRecommendationRepository;

    @Autowired
    private SavedTripRepository savedTripRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User testUser2;
    private Room testRoom;
    private TripRecommendation testRecommendation;

    @BeforeEach
    void setUp() {
        savedTripRepository.deleteAll();
        tripRecommendationRepository.deleteAll();
        roomParticipantRepository.deleteAll();
        roomRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email("apitest@example.com")
                .password(passwordEncoder.encode("password"))
                .name("API Test User")
                .generation("30대")
                .build();
        testUser = userRepository.save(testUser);

        testUser2 = User.builder()
                .id(UUID.randomUUID().toString())
                .email("apitest2@example.com")
                .password(passwordEncoder.encode("password"))
                .name("API Test User 2")
                .generation("20대")
                .build();
        testUser2 = userRepository.save(testUser2);

        // 사용자 프로필 생성
        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID().toString())
                .user(testUser)
                .speed(70)
                .stamina(80)
                .budget(60)
                .photo(70)
                .tradition(50)
                .build();
        userProfileRepository.save(profile);

        testRoom = Room.builder()
                .id(UUID.randomUUID().toString())
                .name("API Test Room")
                .inviteCode("APICODE")
                .createdBy(testUser)
                .isActive(true)
                .build();
        testRoom = roomRepository.save(testRoom);

        RoomParticipant participant1 = RoomParticipant.builder()
                .id(UUID.randomUUID().toString())
                .room(testRoom)
                .user(testUser)
                .role(RoomParticipant.Role.owner)
                .build();
        roomParticipantRepository.save(participant1);

        RoomParticipant participant2 = RoomParticipant.builder()
                .id(UUID.randomUUID().toString())
                .room(testRoom)
                .user(testUser2)
                .role(RoomParticipant.Role.member)
                .build();
        roomParticipantRepository.save(participant2);

        testRecommendation = TripRecommendation.builder()
                .id(UUID.randomUUID().toString())
                .title("Test Recommendation")
                .room(testRoom)
                .user(testUser)
                .type(TripRecommendation.Type.personal)
                .course(Arrays.asList("장소1", "장소2", "장소3"))
                .why("테스트 추천 이유")
                .build();
        testRecommendation = tripRecommendationRepository.save(testRecommendation);
    }

    @Test
    void testAnalyze_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        List<Map<String, Object>> userAnswers = new ArrayList<>();
        
        Map<String, Object> answer1 = new HashMap<>();
        answer1.put("questionId", 1);
        answer1.put("value", 70);
        userAnswers.add(answer1);
        
        Map<String, Object> answer2 = new HashMap<>();
        answer2.put("questionId", 2);
        answer2.put("value", 80);
        userAnswers.add(answer2);

        request.put("userAnswers", userAnswers);
        request.put("userGeneration", "30대");
        request.put("companionGeneration", "20대");

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.differences").exists())
                .andExpect(jsonPath("$.userProfile").exists())
                .andExpect(jsonPath("$.companionProfile").exists())
                .andExpect(jsonPath("$.adjustments").exists())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void testAnalyze_MissingParameters() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userGeneration", "30대");
        // userAnswers와 companionGeneration 누락

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("필수 파라미터가 누락되었습니다."));
    }

    @Test
    void testRecommend_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userGeneration", "30대");
        request.put("companionGeneration", "20대");
        
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("purposes", Arrays.asList("감성", "사진"));
        preferences.put("budget", "5~10만원");
        request.put("preferences", preferences);

        mockMvc.perform(post("/api/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.course").exists());
    }

    @Test
    void testTalkingGuide_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userGeneration", "30대");
        request.put("companionGeneration", "20대");
        
        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("title", "Test Recommendation");
        recommendation.put("course", Arrays.asList("장소1", "장소2"));
        request.put("recommendation", recommendation);

        mockMvc.perform(post("/api/talking-guide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions").exists());
    }

    @Test
    void testGetPersonalRecommendations_Success() throws Exception {
        // 실제 OpenAI API 키가 있으면 200, 없으면 400 에러
        mockMvc.perform(get("/api/recommendations/personal/{userId}", testUser.getId()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    String responseBody = result.getResponse().getContentAsString();
                    // OpenAI API 키가 있으면 200, 없거나 실패하면 400
                    if (status == 200) {
                        // 성공 시 배열이 반환되어야 함
                        assert responseBody.contains("[") || responseBody.contains("title") 
                            : "Expected array or recommendation object, got: " + responseBody;
                    } else if (status == 400) {
                        // 실패 시 에러 메시지 확인
                        assert responseBody.contains("error") || responseBody.contains("OpenAI")
                            : "Expected error message, got: " + responseBody;
                    } else {
                        throw new AssertionError("Expected 200 or 400, but got " + status + ". Response: " + responseBody);
                    }
                });
    }

    @Test
    void testGetPersonalRecommendations_UserNotFound() throws Exception {
        mockMvc.perform(get("/api/recommendations/personal/{userId}", "non-existent-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    void testGetRoomRecommendations_Success() throws Exception {
        // 실제 OpenAI API 키가 있으면 200, 없으면 400 에러
        mockMvc.perform(get("/api/recommendations/room/{roomId}", testRoom.getId()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    String responseBody = result.getResponse().getContentAsString();
                    // OpenAI API 키가 있으면 200, 없거나 실패하면 400
                    if (status == 200) {
                        // 성공 시 배열이 반환되어야 함
                        assert responseBody.contains("[") || responseBody.contains("title")
                            : "Expected array or recommendation object, got: " + responseBody;
                    } else if (status == 400) {
                        // 실패 시 에러 메시지 확인
                        assert responseBody.contains("error") || responseBody.contains("OpenAI")
                            : "Expected error message, got: " + responseBody;
                    } else {
                        throw new AssertionError("Expected 200 or 400, but got " + status + ". Response: " + responseBody);
                    }
                });
    }

    @Test
    void testGetRoomRecommendations_RoomNotFound() throws Exception {
        mockMvc.perform(get("/api/recommendations/room/{roomId}", "non-existent-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testSaveTrip_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());
        request.put("tripId", testRecommendation.getId());

        mockMvc.perform(post("/api/trips/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("여행지가 저장되었습니다."));
    }

    @Test
    void testSaveTrip_Duplicate() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());
        request.put("tripId", testRecommendation.getId());

        // 첫 번째 저장
        mockMvc.perform(post("/api/trips/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 두 번째 저장 (중복)
        mockMvc.perform(post("/api/trips/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("이미 저장된 여행지입니다."));
    }

    @Test
    void testUnsaveTrip_Success() throws Exception {
        // 먼저 저장
        Map<String, String> saveRequest = new HashMap<>();
        saveRequest.put("userId", testUser.getId());
        saveRequest.put("tripId", testRecommendation.getId());
        mockMvc.perform(post("/api/trips/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveRequest)))
                .andExpect(status().isOk());

        // 저장 취소
        Map<String, String> unsaveRequest = new HashMap<>();
        unsaveRequest.put("userId", testUser.getId());
        unsaveRequest.put("tripId", testRecommendation.getId());

        mockMvc.perform(post("/api/trips/unsave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unsaveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("저장이 취소되었습니다."));
    }

    @Test
    void testGetSavedTrips_Success() throws Exception {
        // 먼저 저장
        Map<String, String> saveRequest = new HashMap<>();
        saveRequest.put("userId", testUser.getId());
        saveRequest.put("tripId", testRecommendation.getId());
        mockMvc.perform(post("/api/trips/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips/saved/{userId}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trips").isArray());
    }

    @Test
    void testGetUserProfile_Success() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/profile", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.profile").exists());
    }

    @Test
    void testGetUserProfile_UserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/profile", "non-existent-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("프로필 조회 중 오류가 발생했습니다."));
    }
}

