package com.sedroad.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sedroad.dto.TravelProfile;
import com.sedroad.entity.Room;
import com.sedroad.entity.TripRecommendation;
import com.sedroad.entity.User;
import com.sedroad.entity.UserProfile;
import com.sedroad.repository.RoomRepository;
import com.sedroad.repository.TripRecommendationRepository;
import com.sedroad.repository.UserProfileRepository;
import com.sedroad.repository.UserRepository;
import com.sedroad.service.OpenAIService.ParticipantInfo;
import com.sedroad.service.OpenAIService.Preferences;
import com.sedroad.service.OpenAIService.RecommendationContext;
import com.sedroad.service.OpenAIService.RecommendationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    
    private final OpenAIService openAIService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoomRepository roomRepository;
    private final TripRecommendationRepository tripRecommendationRepository;
    private final RoomService roomService;
    
    public List<PersonalRecommendationDto> generatePersonalRecommendations(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("여행 진단을 먼저 완료해주세요."));
        
        TravelProfile travelProfile = TravelProfile.builder()
                .speed(profile.getSpeed())
                .stamina(profile.getStamina())
                .budget(profile.getBudget())
                .photo(profile.getPhoto())
                .tradition(profile.getTradition())
                .build();
        
        String userGeneration = user.getGeneration() != null ? user.getGeneration() : "30대";
        
        List<PersonalRecommendationDto> recommendations = new ArrayList<>();
        
        RecommendationContext context1 = new RecommendationContext();
        context1.setUserGeneration(userGeneration);
        context1.setUserProfile(travelProfile);
        Preferences prefs1 = new Preferences();
        prefs1.setPurposes(List.of("감성", "사진"));
        prefs1.setBudget("5~10만원");
        context1.setPreferences(prefs1);
        
        RecommendationResult personalRec = openAIService.generateTripRecommendation(context1);
        
        PersonalRecommendationDto dto1 = new PersonalRecommendationDto();
        dto1.setId("personal_" + System.currentTimeMillis());
        dto1.setTitle(personalRec.getTitle() != null ? personalRec.getTitle() : "당신과 비슷한 감성 여행 추천");
        dto1.setDescription("당신의 여행 감각에 맞는 TOP 여행지");
        dto1.setCourse(personalRec.getCourse());
        dto1.setWhy(personalRec.getWhy());
        dto1.setSatisfaction(personalRec.getSatisfaction().getOrDefault(userGeneration, 88));
        dto1.setType("personal");
        dto1.setForGeneration(userGeneration);
        dto1.setOptions(personalRec.getOptions());
        dto1.setEstimatedTime(personalRec.getEstimatedTime());
        dto1.setEstimatedCost(personalRec.getEstimatedCost());
        dto1.setTalkingTip(personalRec.getTalkingTip());
        recommendations.add(dto1);
        
        List<String> generations = List.of("30대", "40대", "50대+");
        for (String gen : generations.subList(0, Math.min(2, generations.size()))) {
            if (!gen.equals(userGeneration)) {
                TravelProfile genProfile = getGenerationProfile(gen);
                
                RecommendationContext context2 = new RecommendationContext();
                context2.setUserGeneration(userGeneration);
                context2.setCompanionGeneration(gen);
                context2.setUserProfile(travelProfile);
                context2.setCompanionProfile(genProfile);
                Preferences prefs2 = new Preferences();
                prefs2.setPurposes(List.of("감성", "사진"));
                prefs2.setBudget("5~10만원");
                context2.setPreferences(prefs2);
                
                RecommendationResult genRec = openAIService.generateTripRecommendation(context2);
                
                PersonalRecommendationDto dto2 = new PersonalRecommendationDto();
                dto2.setId("gen_" + gen + "_" + System.currentTimeMillis());
                dto2.setTitle(genRec.getTitle() != null ? genRec.getTitle() : gen + "와 함께 가면 좋은 여행지");
                dto2.setDescription("세대로드가 분석한 만족도 높은 코스");
                dto2.setCourse(genRec.getCourse());
                dto2.setWhy(genRec.getWhy());
                dto2.setSatisfaction(genRec.getSatisfaction().getOrDefault(gen, 85));
                dto2.setType("generation");
                dto2.setForGeneration(genRec.getForGeneration() != null ? genRec.getForGeneration() : gen);
                dto2.setOptions(genRec.getOptions());
                dto2.setEstimatedTime(genRec.getEstimatedTime());
                dto2.setEstimatedCost(genRec.getEstimatedCost());
                dto2.setTalkingTip(genRec.getTalkingTip());
                recommendations.add(dto2);
            }
        }
        
        for (PersonalRecommendationDto rec : recommendations) {
            saveRecommendation(userId, null, rec, "personal".equals(rec.getType()) ? "personal" : "generation");
        }
        
        return recommendations;
    }
    
    public List<RoomRecommendationDto> generateRoomRecommendations(String roomId) {
        List<RoomService.ParticipantDto> participants = 
                roomService.getRoomParticipants(roomId);
        
        if (participants.isEmpty()) {
            throw new RuntimeException("방에 참여자가 없습니다.");
        }
        
        // 모든 참여자가 진단을 완료했는지 확인
        List<String> usersWithoutProfile = new ArrayList<>();
        for (RoomService.ParticipantDto participant : participants) {
            UserProfile profile = userProfileRepository.findByUserId(participant.getId())
                    .orElse(null);
            if (profile == null) {
                usersWithoutProfile.add(participant.getName() != null ? participant.getName() : participant.getId());
            }
        }
        
        if (!usersWithoutProfile.isEmpty()) {
            String message = String.format("모든 참여자가 여행 진단을 완료해야 합니다. 다음 참여자가 아직 진단을 완료하지 않았습니다: %s", 
                    String.join(", ", usersWithoutProfile));
            throw new RuntimeException(message);
        }
        
        List<ParticipantInfo> participantInfos = participants.stream().map(p -> {
            ParticipantInfo info = new ParticipantInfo();
            info.setGeneration(p.getGeneration());
            TravelProfile profile = TravelProfile.builder()
                    .speed(p.getProfile().getSpeed())
                    .stamina(p.getProfile().getStamina())
                    .budget(p.getProfile().getBudget())
                    .photo(p.getProfile().getPhoto())
                    .tradition(p.getProfile().getTradition())
                    .build();
            info.setProfile(profile);
            info.setName(p.getName());
            return info;
        }).collect(Collectors.toList());
        
        RecommendationContext context = new RecommendationContext();
        context.setUserGeneration(participants.get(0).getGeneration());
        TravelProfile firstProfile = TravelProfile.builder()
                .speed(participants.get(0).getProfile().getSpeed())
                .stamina(participants.get(0).getProfile().getStamina())
                .budget(participants.get(0).getProfile().getBudget())
                .photo(participants.get(0).getProfile().getPhoto())
                .tradition(participants.get(0).getProfile().getTradition())
                .build();
        context.setUserProfile(firstProfile);
        Preferences prefs = new Preferences();
        prefs.setPurposes(List.of("감성", "사진"));
        prefs.setBudget("5~10만원");
        context.setPreferences(prefs);
        context.setParticipants(participantInfos);
        
        RecommendationResult result = openAIService.generateTripRecommendation(context);
        
        Map<String, Integer> satisfaction = new HashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            String key = "participant_" + (i + 1) + "_id";
            Integer sat = result.getSatisfaction().get(key);
            if (sat == null) {
                sat = result.getSatisfaction().get(participants.get(i).getGeneration());
            }
            if (sat == null) {
                sat = 85;
            }
            satisfaction.put(participants.get(i).getId(), sat);
        }
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("방을 찾을 수 없습니다."));
        
        RoomRecommendationDto dto = new RoomRecommendationDto();
        dto.setId("room_" + roomId + "_" + System.currentTimeMillis());
        dto.setTitle(result.getTitle() != null ? result.getTitle() : room.getName() + "의 공감 여행 추천");
        dto.setDescription("모든 참여자가 만족할 수 있는 여행 코스");
        dto.setCourse(result.getCourse());
        dto.setWhy(result.getWhy());
        dto.setSatisfaction(satisfaction);
        dto.setType("room");
        dto.setRoomName(room.getName());
        dto.setForGeneration(result.getForGeneration() != null ? result.getForGeneration() : participants.get(0).getGeneration());
        dto.setOptions(result.getOptions());
        dto.setEstimatedTime(result.getEstimatedTime());
        dto.setEstimatedCost(result.getEstimatedCost());
        dto.setTalkingTip(result.getTalkingTip());
        
        saveRecommendation(null, roomId, dto, "room");
        
        return List.of(dto);
    }
    
    private void saveRecommendation(String userId, String roomId, Object recommendation, String type) {
        TripRecommendation tripRec = new TripRecommendation();
        tripRec.setId(UUID.randomUUID().toString());
        
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            tripRec.setUser(user);
        }
        
        if (roomId != null) {
            Room room = roomRepository.findById(roomId).orElse(null);
            tripRec.setRoom(room);
        }
        
        if (recommendation instanceof PersonalRecommendationDto) {
            PersonalRecommendationDto dto = (PersonalRecommendationDto) recommendation;
            tripRec.setTitle(dto.getTitle());
            tripRec.setDescription(dto.getDescription());
            tripRec.setCourse(dto.getCourse());
            tripRec.setWhy(dto.getWhy());
            tripRec.setType(TripRecommendation.Type.valueOf(type));
            tripRec.setForGeneration(dto.getForGeneration());
            tripRec.setOptions(dto.getOptions());
            tripRec.setEstimatedTime(dto.getEstimatedTime());
            tripRec.setEstimatedCost(dto.getEstimatedCost());
            tripRec.setTalkingTip(dto.getTalkingTip());
            tripRec.setAnalysisSummary(dto.getAnalysisSummary());
            if (dto.getAiAdjustment() != null) {
                tripRec.setAiAdjustment(dto.getAiAdjustment());
            }
            // 개인 추천의 경우 satisfaction을 단일 값으로 저장
            if (dto.getSatisfaction() != null) {
                Map<String, Integer> satMap = new HashMap<>();
                satMap.put("default", dto.getSatisfaction());
                tripRec.setSatisfaction(satMap);
            }
        } else if (recommendation instanceof RoomRecommendationDto) {
            RoomRecommendationDto dto = (RoomRecommendationDto) recommendation;
            tripRec.setTitle(dto.getTitle());
            tripRec.setDescription(dto.getDescription());
            tripRec.setCourse(dto.getCourse());
            tripRec.setWhy(dto.getWhy());
            tripRec.setSatisfaction(dto.getSatisfaction());
            tripRec.setType(TripRecommendation.Type.room);
            tripRec.setRoomName(dto.getRoomName());
            tripRec.setForGeneration(dto.getForGeneration());
            tripRec.setOptions(dto.getOptions());
            tripRec.setEstimatedTime(dto.getEstimatedTime());
            tripRec.setEstimatedCost(dto.getEstimatedCost());
            tripRec.setTalkingTip(dto.getTalkingTip());
            tripRec.setAnalysisSummary(dto.getAnalysisSummary());
            if (dto.getAiAdjustment() != null) {
                tripRec.setAiAdjustment(dto.getAiAdjustment());
            }
        }
        
        tripRecommendationRepository.save(tripRec);
    }
    
    private TravelProfile getGenerationProfile(String generation) {
        Map<String, TravelProfile> profiles = Map.of(
                "10대", TravelProfile.builder().speed(90).stamina(95).budget(40).photo(95).tradition(20).build(),
                "20대", TravelProfile.builder().speed(80).stamina(90).budget(50).photo(90).tradition(30).build(),
                "30대", TravelProfile.builder().speed(70).stamina(80).budget(60).photo(70).tradition(50).build(),
                "40대", TravelProfile.builder().speed(60).stamina(70).budget(70).photo(50).tradition(70).build(),
                "50대+", TravelProfile.builder().speed(50).stamina(60).budget(80).photo(30).tradition(90).build()
        );
        return profiles.getOrDefault(generation, profiles.get("30대"));
    }
    
    @lombok.Data
    public static class PersonalRecommendationDto {
        private String id;
        private String title;
        private String description;
        private List<String> course;
        private String why;
        private Integer satisfaction;
        private String type;
        private String forGeneration;
        private Map<String, String> options;
        private String estimatedTime;
        private String estimatedCost;
        private String talkingTip;
        private String analysisSummary;
        private Map<String, Object> aiAdjustment;
    }
    
    @lombok.Data
    public static class RoomRecommendationDto {
        private String id;
        private String title;
        private String description;
        private List<String> course;
        private String why;
        private Map<String, Integer> satisfaction;
        private String type;
        private String roomName;
        private String forGeneration;
        private Map<String, String> options;
        private String estimatedTime;
        private String estimatedCost;
        private String talkingTip;
        private String analysisSummary;
        private Map<String, Object> aiAdjustment;
    }
}

