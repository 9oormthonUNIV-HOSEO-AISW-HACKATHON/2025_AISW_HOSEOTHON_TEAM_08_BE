package com.sedroad.controller;

import java.util.HashMap;
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
import com.sedroad.entity.User;
import com.sedroad.repository.RoomParticipantRepository;
import com.sedroad.repository.RoomRepository;
import com.sedroad.repository.UserProfileRepository;
import com.sedroad.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@org.springframework.context.annotation.Import(com.sedroad.config.TestSecurityConfig.class)
class RoomControllerIntegrationTest {

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
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User testUser2;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        roomParticipantRepository.deleteAll();
        roomRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email("roomtest@example.com")
                .password(passwordEncoder.encode("password"))
                .name("Room Test User")
                .generation("30대")
                .build();
        testUser = userRepository.save(testUser);

        testUser2 = User.builder()
                .id(UUID.randomUUID().toString())
                .email("roomtest2@example.com")
                .password(passwordEncoder.encode("password"))
                .name("Room Test User 2")
                .generation("20대")
                .build();
        testUser2 = userRepository.save(testUser2);

        testRoom = Room.builder()
                .id(UUID.randomUUID().toString())
                .name("Test Room")
                .inviteCode("TESTCODE")
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
    }

    @Test
    void testCreateRoom_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());
        request.put("roomName", "New Test Room");

        mockMvc.perform(post("/api/rooms/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.room.name").value("New Test Room"))
                .andExpect(jsonPath("$.room.inviteCode").exists())
                .andExpect(jsonPath("$.room.inviteLink").exists());
    }

    @Test
    void testCreateRoom_WithoutRoomName() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());

        mockMvc.perform(post("/api/rooms/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.room.name").value("새로운 여행 방"));
    }

    @Test
    void testCreateRoom_InvalidUserId() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", "invalid-user-id");
        request.put("roomName", "Test Room");

        mockMvc.perform(post("/api/rooms/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    void testJoinRoom_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser2.getId());
        request.put("inviteCode", testRoom.getInviteCode());

        mockMvc.perform(post("/api/rooms/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("방에 입장했습니다."));
    }

    @Test
    void testJoinRoom_InvalidInviteCode() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser2.getId());
        request.put("inviteCode", "INVALIDCODE");

        mockMvc.perform(post("/api/rooms/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("유효하지 않은 초대 코드입니다."));
    }

    @Test
    void testJoinRoom_AlreadyJoined() throws Exception {
        // 이미 참여한 사용자로 다시 조인 시도
        Map<String, String> request = new HashMap<>();
        request.put("userId", testUser.getId());
        request.put("inviteCode", testRoom.getInviteCode());

        mockMvc.perform(post("/api/rooms/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("이미 참여 중인 방입니다."));
    }

    @Test
    void testGetRoom_Success() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}", testRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.id").value(testRoom.getId()))
                .andExpect(jsonPath("$.room.name").value("Test Room"));
    }

    @Test
    void testGetRoom_NotFound() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}", "non-existent-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("방을 찾을 수 없습니다."));
    }

    @Test
    void testGetUserRooms_Success() throws Exception {
        mockMvc.perform(get("/api/rooms/user/{userId}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms").isArray())
                .andExpect(jsonPath("$.rooms[0].id").exists());
    }

    @Test
    void testGetRoomParticipants_Success() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/participants", testRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants").isArray())
                .andExpect(jsonPath("$.participants[0].id").exists())
                .andExpect(jsonPath("$.participants[0].name").exists());
    }
}

