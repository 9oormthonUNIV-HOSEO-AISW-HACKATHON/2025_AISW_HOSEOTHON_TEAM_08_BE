package com.sedroad.service;

import com.sedroad.entity.Room;
import com.sedroad.entity.RoomParticipant;
import com.sedroad.entity.User;
import com.sedroad.entity.UserProfile;
import com.sedroad.repository.RoomParticipantRepository;
import com.sedroad.repository.RoomRepository;
import com.sedroad.repository.UserProfileRepository;
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
public class RoomService {
    
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    
    @Transactional
    public RoomResponse createRoom(String userId, String roomName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Room room = Room.builder()
                .id(UUID.randomUUID().toString())
                .name(roomName != null && !roomName.isEmpty() ? roomName : "새로운 여행 방")
                .inviteCode(generateInviteCode())
                .createdBy(user)
                .isActive(true)
                .build();
        
        room = roomRepository.save(room);
        
        // 생성자를 참여자로 추가
        RoomParticipant participant = RoomParticipant.builder()
                .id(UUID.randomUUID().toString())
                .room(room)
                .user(user)
                .role(RoomParticipant.Role.owner)
                .build();
        roomParticipantRepository.save(participant);
        
        RoomResponse response = new RoomResponse();
        response.setSuccess(true);
        RoomDto roomDto = new RoomDto();
        roomDto.setId(room.getId());
        roomDto.setName(room.getName());
        roomDto.setInviteCode(room.getInviteCode());
        roomDto.setInviteLink("https://sedroad.app/join/" + room.getInviteCode());
        roomDto.setParticipants(List.of(userId));
        roomDto.setCreatedAt(room.getCreatedAt().toString());
        response.setRoom(roomDto);
        
        return response;
    }
    
    @Transactional
    public RoomResponse joinRoom(String userId, String inviteCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Room room = roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 초대 코드입니다."));
        
        if (roomParticipantRepository.existsByRoomAndUser(room, user)) {
            RoomResponse response = new RoomResponse();
            response.setSuccess(true);
            response.setMessage("이미 참여 중인 방입니다.");
            RoomDto roomDto = new RoomDto();
            roomDto.setId(room.getId());
            roomDto.setName(room.getName());
            roomDto.setParticipants(List.of());
            response.setRoom(roomDto);
            return response;
        }
        
        RoomParticipant participant = RoomParticipant.builder()
                .id(UUID.randomUUID().toString())
                .room(room)
                .user(user)
                .role(RoomParticipant.Role.member)
                .build();
        roomParticipantRepository.save(participant);
        
        RoomResponse response = new RoomResponse();
        response.setSuccess(true);
        response.setMessage("방에 입장했습니다.");
        RoomDto roomDto = new RoomDto();
        roomDto.setId(room.getId());
        roomDto.setName(room.getName());
        roomDto.setParticipants(List.of());
        response.setRoom(roomDto);
        
        return response;
    }
    
    public RoomDto getRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setCreatedBy(room.getCreatedBy().getId());
        dto.setCreatedAt(room.getCreatedAt().toString());
        return dto;
    }
    
    public List<RoomDto> getUserRooms(String userId) {
        List<Room> rooms = roomRepository.findByUserId(userId);
        
        return rooms.stream().map(room -> {
            RoomDto dto = new RoomDto();
            dto.setId(room.getId());
            dto.setName(room.getName());
            dto.setInviteCode(room.getInviteCode());
            dto.setCreatedAt(room.getCreatedAt().toString());
            
            long participantCount = roomParticipantRepository.findByRoomId(room.getId()).size();
            dto.setParticipantsCount((int) participantCount);
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    public List<ParticipantDto> getRoomParticipants(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        
        List<RoomParticipant> participants = roomParticipantRepository.findByRoomId(roomId);
        
        return participants.stream().map(rp -> {
            User user = rp.getUser();
            Optional<UserProfile> profileOpt = userProfileRepository.findByUser(user);
            
            TravelProfileDto profile = profileOpt.map(up -> TravelProfileDto.builder()
                    .speed(up.getSpeed())
                    .stamina(up.getStamina())
                    .budget(up.getBudget())
                    .photo(up.getPhoto())
                    .tradition(up.getTradition())
                    .build()).orElse(TravelProfileDto.builder()
                    .speed(50).stamina(50).budget(50).photo(50).tradition(50)
                    .build());
            
            String tag = determineTag(profile);
            
            ParticipantDto dto = new ParticipantDto();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setGeneration(user.getGeneration() != null ? user.getGeneration() : "30대");
            dto.setProfile(profile);
            dto.setTag(tag);
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    private String generateInviteCode() {
        return UUID.randomUUID().toString()
                .substring(0, 8)
                .toUpperCase()
                .replace("-", "");
    }
    
    private String determineTag(TravelProfileDto profile) {
        if (profile.getPhoto() > 70 && profile.getTradition() < 40) {
            return "감성형";
        } else if (profile.getTradition() > 70 && profile.getSpeed() < 50) {
            return "여유형";
        } else if (profile.getSpeed() > 70) {
            return "체험형";
        }
        return "균형형";
    }
    
    @lombok.Data
    public static class RoomResponse {
        private Boolean success;
        private String message;
        private RoomDto room;
    }
    
    @lombok.Data
    public static class RoomDto {
        private String id;
        private String name;
        private String inviteCode;
        private String inviteLink;
        private List<String> participants;
        private Integer participantsCount;
        private String createdBy;
        private String createdAt;
    }
    
    @lombok.Data
    public static class ParticipantDto {
        private String id;
        private String name;
        private String generation;
        private TravelProfileDto profile;
        private String tag;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class TravelProfileDto {
        private Integer speed;
        private Integer stamina;
        private Integer budget;
        private Integer photo;
        private Integer tradition;
    }
}

