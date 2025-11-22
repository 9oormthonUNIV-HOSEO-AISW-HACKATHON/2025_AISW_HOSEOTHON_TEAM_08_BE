package com.sedroad.controller;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sedroad.dto.TravelProfile;
import com.sedroad.entity.User;
import com.sedroad.entity.UserProfile;
import com.sedroad.repository.UserProfileRepository;
import com.sedroad.repository.UserRepository;
import com.sedroad.service.OpenAIService;
import com.sedroad.service.RecommendationService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OpenAIApiVerificationTest {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email("openaitest@example.com")
                .password(passwordEncoder.encode("password"))
                .name("OpenAI Test User")
                .generation("30대")
                .build();
        testUser = userRepository.save(testUser);

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
    }

    @Test
    void testOpenAIServiceDirectCall() {
        OpenAIService.RecommendationContext context = new OpenAIService.RecommendationContext();
        context.setUserGeneration("30대");
        
        TravelProfile profile = TravelProfile.builder()
                .speed(70)
                .stamina(80)
                .budget(60)
                .photo(70)
                .tradition(50)
                .build();
        context.setUserProfile(profile);
        
        OpenAIService.Preferences prefs = new OpenAIService.Preferences();
        prefs.setPurposes(List.of("감성", "사진"));
        prefs.setBudget("5~10만원");
        context.setPreferences(prefs);

        try {
            OpenAIService.RecommendationResult result = openAIService.generateTripRecommendation(context);
            
            assertNotNull(result.getTitle(), "제목이 null이면 안 됩니다");
            assertNotNull(result.getCourse(), "코스가 null이면 안 됩니다");
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("OpenAI API 키가 설정되지 않았습니다")) {
                fail("OpenAI API 키를 설정해주세요: " + e.getMessage());
            } else if (e.getMessage().contains("AI 추천 생성 실패")) {
                fail("OpenAI API 호출 실패: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Test
    void testRecommendationServiceWithOpenAI() {
        try {
            List<RecommendationService.PersonalRecommendationDto> recommendations = 
                    recommendationService.generatePersonalRecommendations(testUser.getId());
            
            assertFalse(recommendations.isEmpty(), "추천 목록이 비어있으면 안 됩니다");
            assertNotNull(recommendations.get(0).getTitle(), "첫 번째 추천의 제목이 null이면 안 됩니다");
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("OpenAI API 키가 설정되지 않았습니다")) {
                fail("OpenAI API 키를 설정해주세요: " + e.getMessage());
            } else if (e.getMessage().contains("AI 추천 생성 실패")) {
                fail("OpenAI API 호출 실패: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }
}

