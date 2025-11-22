package com.sedroad.service;

import com.sedroad.dto.TravelProfile;
import com.sedroad.service.OpenAIService.TalkingGuideContext;
import com.sedroad.service.OpenAIService.TalkingGuideResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalkingGuideService {
    
    private final OpenAIService openAIService;
    
    public TalkingGuideResponse getTalkingGuide(
            String userGeneration,
            String companionGeneration,
            Map<String, Object> recommendation
    ) {
        return getTalkingGuide(userGeneration, companionGeneration, null, null, recommendation);
    }
    
    public TalkingGuideResponse getTalkingGuide(
            String userGeneration,
            String companionGeneration,
            TravelProfile userProfile,
            TravelProfile companionProfile,
            Map<String, Object> recommendation
    ) {
        try {
            // OpenAI를 사용하여 개인화된 대화 가이드 생성
            TalkingGuideContext context = new TalkingGuideContext();
            context.setUserGeneration(userGeneration);
            context.setCompanionGeneration(companionGeneration);
            context.setUserProfile(userProfile);
            context.setCompanionProfile(companionProfile);
            context.setRecommendation(recommendation);
            
            TalkingGuideResult result = openAIService.generateTalkingGuide(context);
            
            TalkingGuideResponse response = new TalkingGuideResponse();
            response.setSuggestions(result.getSuggestions());
            response.setTips(result.getTips());
            response.setTopics(result.getTopics());
            
            return response;
        } catch (Exception e) {
            log.error("대화 가이드 생성 오류", e);
            // 오류 발생 시 기본 가이드 반환
            return getDefaultGuide(userGeneration, companionGeneration, recommendation);
        }
    }
    
    private TalkingGuideResponse getDefaultGuide(
            String userGeneration,
            String companionGeneration,
            Map<String, Object> recommendation
    ) {
        boolean isParentCompanion = companionGeneration != null && 
                (companionGeneration.contains("50") || companionGeneration.contains("40"));
        
        TalkingGuideResponse response = new TalkingGuideResponse();
        
        if (isParentCompanion) {
            List<String> suggestions = new ArrayList<>();
            Object talkingTip = recommendation != null ? recommendation.get("talking_tip") : null;
            if (talkingTip != null) {
                suggestions.add(talkingTip.toString());
            } else {
                suggestions.add("엄마, 요즘 감성 여행도 전통 분위기랑 섞어서 즐기면 더 특별하대!");
            }
            suggestions.add("이런 곳이 요즘 젊은 사람들 사이에서 인기래요. 부모님도 좋아하실 것 같아서 데려왔어요.");
            suggestions.add("전통과 현대가 만나는 느낌이 신기하지 않으세요?");
            response.setSuggestions(suggestions);
            
            response.setTips(List.of(
                    "부모님의 반응을 살펴보며 속도를 조절하세요",
                    "전통적인 요소를 강조하면 더 공감하기 쉬워요",
                    "사진을 찍을 때는 함께 찍는 것을 제안해보세요"
            ));
            
            response.setTopics(List.of(
                    "이 장소의 역사 이야기",
                    "요즘 젊은 사람들의 여행 트렌드",
                    "전통과 현대의 조화"
            ));
        } else {
            response.setSuggestions(List.of(
                    "이런 여행은 세대를 넘어서 함께 즐길 수 있어요!",
                    "서로 다른 관점에서 보면 더 재미있을 것 같아요",
                    "함께 경험하면 더 특별한 추억이 될 거예요"
            ));
            
            response.setTips(List.of(
                    "서로의 취향을 존중하며 즐기세요",
                    "각자의 옵션을 선택한 후 함께 모이는 시간을 가져보세요",
                    "서로 느낀 점을 공유하면 더 좋아요"
            ));
            
            response.setTopics(List.of(
                    "각자의 여행 스타일",
                    "이번 여행에서 새롭게 발견한 점",
                    "다음에 함께 가고 싶은 곳"
            ));
        }
        
        return response;
    }
    
    @lombok.Data
    public static class TalkingGuideResponse {
        private List<String> suggestions;
        private List<String> tips;
        private List<String> topics;
    }
}

