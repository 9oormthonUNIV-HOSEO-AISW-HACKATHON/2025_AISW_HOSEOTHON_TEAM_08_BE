package com.sedroad.controller;

import com.sedroad.dto.TravelProfile;
import com.sedroad.entity.SavedTrip;
import com.sedroad.entity.TripRecommendation;
import com.sedroad.entity.User;
import com.sedroad.repository.SavedTripRepository;
import com.sedroad.repository.TripRecommendationRepository;
import com.sedroad.repository.UserRepository;
import com.sedroad.repository.UserProfileRepository;
import com.sedroad.service.AnalysisService;
import com.sedroad.service.RecommendationService;
import com.sedroad.service.TalkingGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "여행 추천", description = "여행 추천 및 분석 관련 API")
public class ApiController {
    
    private final AnalysisService analysisService;
    private final RecommendationService recommendationService;
    private final TalkingGuideService talkingGuideService;
    private final SavedTripRepository savedTripRepository;
    private final TripRecommendationRepository tripRecommendationRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    
    @io.swagger.v3.oas.annotations.Operation(summary = "세대 차이 분석", description = "사용자와 동반자의 여행 가치관 차이를 분석합니다.")
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> userAnswersMap = (List<Map<String, Object>>) request.get("userAnswers");
            String userGeneration = (String) request.get("userGeneration");
            String companionGeneration = (String) request.get("companionGeneration");
            
            if (userAnswersMap == null || userGeneration == null || companionGeneration == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "필수 파라미터가 누락되었습니다."));
            }
            
            List<AnalysisService.UserAnswer> userAnswers = userAnswersMap.stream()
                    .map(m -> {
                        AnalysisService.UserAnswer answer = new AnalysisService.UserAnswer();
                        answer.setQuestionId(((Number) m.get("questionId")).intValue());
                        answer.setValue(((Number) m.get("value")).intValue());
                        return answer;
                    })
                    .toList();
            
            AnalysisService.AnalysisResult result = analysisService.analyzeGenerationDifference(
                    userAnswers, userGeneration, companionGeneration);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "분석 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/recommend")
    public ResponseEntity<?> recommend(@RequestBody Map<String, Object> request) {
        try {
            String userGeneration = (String) request.get("userGeneration");
            String companionGeneration = (String) request.get("companionGeneration");
            @SuppressWarnings("unchecked")
            Map<String, Object> preferences = (Map<String, Object>) request.get("preferences");
            
            if (userGeneration == null || companionGeneration == null || preferences == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "필수 파라미터가 누락되었습니다."));
            }
            
            return ResponseEntity.ok(Map.of(
                    "title", "추천 여행",
                    "course", List.of("장소1", "장소2", "장소3"),
                    "why", "추천 이유"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "추천 생성 중 오류가 발생했습니다."));
        }
    }
    
    @io.swagger.v3.oas.annotations.Operation(summary = "대화 가이드 생성", description = "AI가 생성한 세대 간 대화 가이드를 제공합니다.")
    @PostMapping("/talking-guide")
    public ResponseEntity<?> talkingGuide(@RequestBody Map<String, Object> request) {
        try {
            String userGeneration = (String) request.get("userGeneration");
            String companionGeneration = (String) request.get("companionGeneration");
            @SuppressWarnings("unchecked")
            Map<String, Object> recommendation = (Map<String, Object>) request.get("recommendation");
            
            if (userGeneration == null || companionGeneration == null || recommendation == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "필수 파라미터가 누락되었습니다."));
            }
            
            TravelProfile userProfile = null;
            TravelProfile companionProfile = null;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> userProfileMap = (Map<String, Object>) request.get("userProfile");
            if (userProfileMap != null) {
                userProfile = TravelProfile.builder()
                        .speed(((Number) userProfileMap.getOrDefault("speed", 50)).intValue())
                        .stamina(((Number) userProfileMap.getOrDefault("stamina", 50)).intValue())
                        .budget(((Number) userProfileMap.getOrDefault("budget", 50)).intValue())
                        .photo(((Number) userProfileMap.getOrDefault("photo", 50)).intValue())
                        .tradition(((Number) userProfileMap.getOrDefault("tradition", 50)).intValue())
                        .build();
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> companionProfileMap = (Map<String, Object>) request.get("companionProfile");
            if (companionProfileMap != null) {
                companionProfile = TravelProfile.builder()
                        .speed(((Number) companionProfileMap.getOrDefault("speed", 50)).intValue())
                        .stamina(((Number) companionProfileMap.getOrDefault("stamina", 50)).intValue())
                        .budget(((Number) companionProfileMap.getOrDefault("budget", 50)).intValue())
                        .photo(((Number) companionProfileMap.getOrDefault("photo", 50)).intValue())
                        .tradition(((Number) companionProfileMap.getOrDefault("tradition", 50)).intValue())
                        .build();
            }
            
            TalkingGuideService.TalkingGuideResponse guide = talkingGuideService.getTalkingGuide(
                    userGeneration, companionGeneration, userProfile, companionProfile, recommendation);
            
            return ResponseEntity.ok(guide);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "가이드 생성 중 오류가 발생했습니다.", "details", e.getMessage()));
        }
    }
    
    @io.swagger.v3.oas.annotations.Operation(summary = "개인 추천 조회", description = "사용자별 개인화된 여행 추천을 조회합니다.")
    @GetMapping("/recommendations/personal/{userId}")
    public ResponseEntity<?> getPersonalRecommendations(@PathVariable String userId) {
        try {
            List<RecommendationService.PersonalRecommendationDto> recommendations = 
                    recommendationService.generatePersonalRecommendations(userId);
            return ResponseEntity.ok(recommendations);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @io.swagger.v3.oas.annotations.Operation(summary = "방 추천 조회", description = "방 참여자들을 위한 공동 여행 추천을 조회합니다.")
    @GetMapping("/recommendations/room/{roomId}")
    public ResponseEntity<?> getRoomRecommendations(@PathVariable String roomId) {
        try {
            List<RecommendationService.RoomRecommendationDto> recommendations = 
                    recommendationService.generateRoomRecommendations(roomId);
            return ResponseEntity.ok(recommendations);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/trips/save")
    public ResponseEntity<?> saveTrip(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String tripId = request.get("tripId");
            
            if (userId == null || tripId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "사용자 ID와 여행지 ID가 필요합니다."));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            TripRecommendation recommendation = tripRecommendationRepository.findById(tripId)
                    .orElseThrow(() -> new RuntimeException("추천을 찾을 수 없습니다."));
            
            if (savedTripRepository.existsByUserAndRecommendationId(user, tripId)) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "이미 저장된 여행지입니다."
                ));
            }
            
            SavedTrip savedTrip = SavedTrip.builder()
                    .id(UUID.randomUUID().toString())
                    .user(user)
                    .recommendation(recommendation)
                    .build();
            
            savedTripRepository.save(savedTrip);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "여행지가 저장되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "저장 중 오류가 발생했습니다."));
        }
    }
    
    @PostMapping("/trips/unsave")
    public ResponseEntity<?> unsaveTrip(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String tripId = request.get("tripId");
            
            if (userId == null || tripId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "사용자 ID와 여행지 ID가 필요합니다."));
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            savedTripRepository.findByUserAndRecommendationId(user, tripId)
                    .ifPresent(savedTripRepository::delete);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "저장이 취소되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "저장 취소 중 오류가 발생했습니다."));
        }
    }
    
    @io.swagger.v3.oas.annotations.Operation(summary = "저장된 여행지 조회", description = "사용자가 저장한 여행지 목록을 조회합니다.")
    @GetMapping("/trips/saved/{userId}")
    public ResponseEntity<?> getSavedTrips(@PathVariable String userId) {
        try {
            if (!userRepository.existsById(userId)) {
                throw new RuntimeException("사용자를 찾을 수 없습니다.");
            }
            
            List<SavedTrip> savedTrips = savedTripRepository.findByUserId(userId);
            
            List<Map<String, Object>> trips = savedTrips.stream().map(st -> {
                TripRecommendation rec = st.getRecommendation();
                return Map.of(
                    "id", rec.getId(),
                    "title", rec.getTitle() != null ? rec.getTitle() : "추천 여행",
                    "description", rec.getWhy() != null ? rec.getWhy() : "",
                    "course", rec.getCourse() != null ? rec.getCourse() : List.of(),
                    "satisfaction", rec.getSatisfaction() != null ? rec.getSatisfaction() : Map.of(),
                    "savedAt", st.getCreatedAt() != null ? st.getCreatedAt().toString() : ""
                );
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("trips", trips));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "저장된 여행지 조회 중 오류가 발생했습니다."));
        }
    }
    
    @io.swagger.v3.oas.annotations.Operation(summary = "사용자 프로필 조회", description = "사용자의 여행 가치관 프로필을 조회합니다.")
    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<?> getUserProfile(@PathVariable String userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            com.sedroad.entity.UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElse(null);
            
            if (profile == null) {
                return ResponseEntity.ok(Map.of(
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "generation", user.getGeneration() != null ? user.getGeneration() : "",
                    "profile", Map.of(
                        "speed", 50,
                        "stamina", 50,
                        "budget", 50,
                        "photo", 50,
                        "tradition", 50
                    )
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "generation", user.getGeneration() != null ? user.getGeneration() : "",
                "profile", Map.of(
                    "speed", profile.getSpeed(),
                    "stamina", profile.getStamina(),
                    "budget", profile.getBudget(),
                    "photo", profile.getPhoto(),
                    "tradition", profile.getTradition()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "프로필 조회 중 오류가 발생했습니다."));
        }
    }
}

