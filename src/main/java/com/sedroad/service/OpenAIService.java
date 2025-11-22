package com.sedroad.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedroad.dto.TravelProfile;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    private static final String SYSTEM_PROMPT = """
        당신은 '세대로드(Sedroad)'의 전문 여행 추천 AI 어시스턴트입니다.
        세대 간 여행 취향 차이를 이해하고, 모든 참여자가 만족할 수 있는 여행 코스를 추천하는 것이 당신의 역할입니다.
        
        **핵심 원칙:**
        1. 반드시 실제 존재하는 전국 어디든 장소를 추천하세요
        2. 세대 간 공감대를 형성할 수 있는 장소를 우선 선택하세요
        3. 각 세대의 특성을 고려하되, 균형잡힌 추천을 제공하세요
        4. 구체적이고 실행 가능한 여행 코스를 제시하세요
        5. 만족도 예측은 현실적이고 신뢰할 수 있는 수치로 제공하세요
        
        **응답 형식:**
        반드시 유효한 JSON 형식으로만 응답하세요. 추가 설명이나 마크다운 없이 순수 JSON만 반환하세요.
        """;
    
    public RecommendationResult generateTripRecommendation(RecommendationContext context) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API 키가 설정되지 않았습니다.");
            throw new RuntimeException("OpenAI API 키가 설정되지 않았습니다.");
        }
        
        OpenAiService service = new OpenAiService(apiKey);
        
        String userPrompt = buildUserPrompt(context);
        
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), SYSTEM_PROMPT));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), userPrompt));
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(messages)
                .temperature(0.7)
                .maxTokens(2000)
                .build();
        
        try {
            String content = service.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
            
            RecommendationResult result = parseRecommendationResult(content);
            result.setForGeneration(context.getCompanionGeneration() != null 
                    ? context.getCompanionGeneration() 
                    : context.getUserGeneration());
            
            return result;
        } catch (Exception e) {
            log.error("OpenAI API 오류", e);
            throw new RuntimeException("AI 추천 생성 실패: " + e.getMessage());
        }
    }
    
    private String buildUserPrompt(RecommendationContext context) {
        if (context.getParticipants() != null && context.getParticipants().size() > 1) {
            StringBuilder participantsInfo = new StringBuilder();
            for (ParticipantInfo p : context.getParticipants()) {
                participantsInfo.append(String.format("- %s (%s): 속도 %d%%, 체력 %d%%, 예산 %d%%, 사진 %d%%, 전통 %d%%\n",
                        p.getName(), p.getGeneration(), p.getProfile().getSpeed(), p.getProfile().getStamina(),
                        p.getProfile().getBudget(), p.getProfile().getPhoto(), p.getProfile().getTradition()));
            }
            
            return String.format("""
                다음 참여자들의 여행 취향을 분석하여 모두가 만족할 수 있는 1일 여행 코스를 추천해주세요:
                
                **참여자 정보:**
                %s
                
                **선호사항:**
                - 목적: %s
                - 예산: %s
                - 선호 지역: %s
                
                **요구사항:**
                1. 실제 존재하는 전국 어디든 구체적인 장소명을 3-5개 제시하세요
                2. 각 장소는 참여자들의 평균 취향에 맞춰 선택하되, 세대 간 균형을 고려하세요
                3. 이동 경로가 논리적이고 효율적이어야 합니다
                4. 각 참여자별 예상 만족도를 제공하세요
                
                다음 JSON 형식으로 응답하세요:
                {
                  "title": "여행 제목",
                  "course": ["장소1", "장소2", "장소3", "장소4"],
                  "why": "이 코스를 추천하는 이유",
                  "options": {
                    "common": "공통 활동",
                    "generation_specific": "세대별 옵션"
                  },
                  "talking_tip": "대화 주제",
                  "satisfaction": {
                    "participant_1_id": 90,
                    "participant_2_id": 88
                  },
                  "estimated_time": "6-8시간",
                  "estimated_cost": "인당 5-8만원"
                }
                """,
                participantsInfo.toString(),
                context.getPreferences().getPurposes() != null 
                        ? String.join(", ", context.getPreferences().getPurposes()) 
                        : "감성 여행",
                context.getPreferences().getBudget() != null ? context.getPreferences().getBudget() : "5~10만원",
                context.getPreferences().getPreferredPlaces() != null 
                        ? context.getPreferences().getPreferredPlaces() 
                        : "전국");
        } else {
            String companionInfo = context.getCompanionGeneration() != null
                    ? String.format("\n**동반자:** %s (속도 %d%%, 체력 %d%%, 예산 %d%%, 사진 %d%%, 전통 %d%%)",
                            context.getCompanionGeneration(),
                            context.getCompanionProfile().getSpeed(),
                            context.getCompanionProfile().getStamina(),
                            context.getCompanionProfile().getBudget(),
                            context.getCompanionProfile().getPhoto(),
                            context.getCompanionProfile().getTradition())
                    : "";
            
            return String.format("""
                %s 사용자와 %s의 여행 취향을 분석하여 최적의 1일 여행 코스를 추천해주세요:
                
                **사용자 정보:**
                - 세대: %s
                - 여행 스타일: 속도 %d%%, 체력 %d%%, 예산 %d%%, 사진 %d%%, 전통 %d%%%s
                
                **선호사항:**
                - 목적: %s
                - 예산: %s
                - 선호 지역: %s
                
                **요구사항:**
                1. 실제 존재하는 전국 어디든 구체적인 장소명을 3-5개 제시하세요
                2. 두 세대 모두가 만족할 수 있는 균형잡힌 코스로 구성하세요
                3. 이동 경로가 논리적이고 효율적이어야 합니다
                4. 각 세대별 예상 만족도를 제공하세요
                
                다음 JSON 형식으로 응답하세요:
                {
                  "title": "여행 제목",
                  "course": ["장소1", "장소2", "장소3", "장소4"],
                  "why": "이 코스를 추천하는 이유",
                  "options": {
                    "%s": "사용자 세대를 위한 특별 옵션",
                    "%s": "동반자 세대를 위한 특별 옵션"
                  },
                  "talking_tip": "대화 주제",
                  "satisfaction": {
                    "%s": 90,
                    "%s": 88
                  },
                  "estimated_time": "6-8시간",
                  "estimated_cost": "인당 5-8만원"
                }
                """,
                context.getUserGeneration(),
                context.getCompanionGeneration() != null ? context.getCompanionGeneration() : "동반자",
                context.getUserGeneration(),
                context.getUserProfile().getSpeed(),
                context.getUserProfile().getStamina(),
                context.getUserProfile().getBudget(),
                context.getUserProfile().getPhoto(),
                context.getUserProfile().getTradition(),
                companionInfo,
                context.getPreferences().getPurposes() != null 
                        ? String.join(", ", context.getPreferences().getPurposes()) 
                        : "감성 여행",
                context.getPreferences().getBudget() != null ? context.getPreferences().getBudget() : "5~10만원",
                context.getPreferences().getPreferredPlaces() != null 
                        ? context.getPreferences().getPreferredPlaces() 
                        : "전국",
                context.getUserGeneration(),
                context.getCompanionGeneration() != null ? context.getCompanionGeneration() : "companion",
                context.getUserGeneration(),
                context.getCompanionGeneration() != null ? context.getCompanionGeneration() : "companion");
        }
    }
    
    private RecommendationResult parseRecommendationResult(String jsonContent) {
        try {
            String cleanedContent = jsonContent.trim();
            if (cleanedContent.startsWith("```json")) {
                cleanedContent = cleanedContent.substring(7);
            } else if (cleanedContent.startsWith("```")) {
                cleanedContent = cleanedContent.substring(3);
            }
            if (cleanedContent.endsWith("```")) {
                cleanedContent = cleanedContent.substring(0, cleanedContent.length() - 3);
            }
            cleanedContent = cleanedContent.trim();
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(cleanedContent, Map.class);
            
            RecommendationResult result = new RecommendationResult();
            result.setTitle((String) jsonMap.getOrDefault("title", "추천 여행"));
            result.setWhy((String) jsonMap.getOrDefault("why", ""));
            result.setTalkingTip((String) jsonMap.getOrDefault("talking_tip", 
                    jsonMap.getOrDefault("talkingTip", "")));
            result.setEstimatedTime((String) jsonMap.getOrDefault("estimated_time",
                    jsonMap.getOrDefault("estimatedTime", "")));
            result.setEstimatedCost((String) jsonMap.getOrDefault("estimated_cost",
                    jsonMap.getOrDefault("estimatedCost", "")));
            
            @SuppressWarnings("unchecked")
            List<String> course = (List<String>) jsonMap.getOrDefault("course", new ArrayList<>());
            result.setCourse(course);
            
            @SuppressWarnings("unchecked")
            Map<String, String> options = (Map<String, String>) jsonMap.getOrDefault("options", new HashMap<>());
            result.setOptions(options);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> satisfactionObj = (Map<String, Object>) jsonMap.getOrDefault("satisfaction", new HashMap<>());
            Map<String, Integer> satisfaction = new HashMap<>();
            for (Map.Entry<String, Object> entry : satisfactionObj.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    satisfaction.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                }
            }
            result.setSatisfaction(satisfaction);
            
            return result;
        } catch (Exception e) {
            log.error("JSON 파싱 오류", e);
            RecommendationResult result = new RecommendationResult();
            result.setTitle("추천 여행");
            result.setCourse(new ArrayList<>());
            result.setOptions(new HashMap<>());
            result.setSatisfaction(new HashMap<>());
            return result;
        }
    }
    
    @lombok.Data
    public static class RecommendationContext {
        private String userGeneration;
        private String companionGeneration;
        private TravelProfile userProfile;
        private TravelProfile companionProfile;
        private Preferences preferences;
        private List<ParticipantInfo> participants;
    }
    
    @lombok.Data
    public static class Preferences {
        private List<String> purposes;
        private String budget;
        private String preferredPlaces;
        private String companionStyle;
    }
    
    @lombok.Data
    public static class ParticipantInfo {
        private String generation;
        private TravelProfile profile;
        private String name;
    }
    
    public TalkingGuideResult generateTalkingGuide(TalkingGuideContext context) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API 키가 설정되지 않았습니다.");
            throw new RuntimeException("OpenAI API 키가 설정되지 않았습니다.");
        }
        
        OpenAiService service = new OpenAiService(apiKey);
        
        String systemPrompt = """
            당신은 '세대로드(Sedroad)'의 대화 가이드 전문가입니다.
            세대 간 여행에서 자연스럽고 따뜻한 대화를 이끌어낼 수 있는 구체적인 가이드를 제공하는 것이 당신의 역할입니다.
            
            **핵심 원칙:**
            1. 사용자의 세대와 동반자의 세대를 고려하여 적절한 말투와 주제를 제안하세요
            2. 추천된 여행지의 특성을 반영하여 구체적인 대화 주제를 제시하세요
            3. 세대 간 공감대를 형성할 수 있는 자연스러운 대화 문구를 제공하세요
            4. 실용적이고 바로 사용할 수 있는 문구를 제안하세요
            
            **응답 형식:**
            반드시 유효한 JSON 형식으로만 응답하세요. 추가 설명이나 마크다운 없이 순수 JSON만 반환하세요.
            """;
        
        String userPrompt = buildTalkingGuidePrompt(context);
        
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), userPrompt));
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(messages)
                .temperature(0.8)
                .maxTokens(1500)
                .build();
        
        try {
            String content = service.createChatCompletion(request)
                    .getChoices().get(0).getMessage().getContent();
            
            return parseTalkingGuideResult(content);
        } catch (Exception e) {
            log.error("OpenAI 대화 가이드 생성 오류", e);
            return getDefaultTalkingGuide(context);
        }
    }
    
    private String buildTalkingGuidePrompt(TalkingGuideContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 정보를 바탕으로 세대 간 여행에서 사용할 수 있는 대화 가이드를 생성해주세요:\n\n");
        
        prompt.append("**사용자 정보:**\n");
        prompt.append("- 세대: ").append(context.getUserGeneration()).append("\n");
        if (context.getUserProfile() != null) {
            prompt.append("- 여행 스타일: 속도 ").append(context.getUserProfile().getSpeed())
                  .append("%, 체력 ").append(context.getUserProfile().getStamina())
                  .append("%, 예산 ").append(context.getUserProfile().getBudget())
                  .append("%, 사진 ").append(context.getUserProfile().getPhoto())
                  .append("%, 전통 ").append(context.getUserProfile().getTradition()).append("%\n");
        }
        
        prompt.append("\n**동반자 정보:**\n");
        prompt.append("- 세대: ").append(context.getCompanionGeneration()).append("\n");
        if (context.getCompanionProfile() != null) {
            prompt.append("- 여행 스타일: 속도 ").append(context.getCompanionProfile().getSpeed())
                  .append("%, 체력 ").append(context.getCompanionProfile().getStamina())
                  .append("%, 예산 ").append(context.getCompanionProfile().getBudget())
                  .append("%, 사진 ").append(context.getCompanionProfile().getPhoto())
                  .append("%, 전통 ").append(context.getCompanionProfile().getTradition()).append("%\n");
        }
        
        if (context.getRecommendation() != null) {
            prompt.append("\n**추천 여행지:**\n");
            if (context.getRecommendation().get("title") != null) {
                prompt.append("- 제목: ").append(context.getRecommendation().get("title")).append("\n");
            }
            if (context.getRecommendation().get("course") != null) {
                @SuppressWarnings("unchecked")
                List<String> course = (List<String>) context.getRecommendation().get("course");
                prompt.append("- 여행 코스: ").append(String.join(" → ", course)).append("\n");
            }
            if (context.getRecommendation().get("why") != null) {
                prompt.append("- 추천 이유: ").append(context.getRecommendation().get("why")).append("\n");
            }
        }
        
        prompt.append("\n**요구사항:**\n");
        prompt.append("1. 사용자가 동반자에게 자연스럽게 말할 수 있는 구체적인 문구 3개를 제시하세요\n");
        prompt.append("2. 여행 중 주의할 점이나 팁 3개를 제시하세요\n");
        prompt.append("3. 대화 주제 3개를 제시하세요\n");
        prompt.append("4. 말투는 친근하고 자연스럽게, 세대 차이를 고려하여 작성하세요\n");
        
        prompt.append("\n다음 JSON 형식으로 응답하세요:\n");
        prompt.append("{\n");
        prompt.append("  \"suggestions\": [\"문구1\", \"문구2\", \"문구3\"],\n");
        prompt.append("  \"tips\": [\"팁1\", \"팁2\", \"팁3\"],\n");
        prompt.append("  \"topics\": [\"주제1\", \"주제2\", \"주제3\"]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    private TalkingGuideResult parseTalkingGuideResult(String jsonContent) {
        try {
            String cleanedContent = jsonContent.trim();
            if (cleanedContent.startsWith("```json")) {
                cleanedContent = cleanedContent.substring(7);
            } else if (cleanedContent.startsWith("```")) {
                cleanedContent = cleanedContent.substring(3);
            }
            if (cleanedContent.endsWith("```")) {
                cleanedContent = cleanedContent.substring(0, cleanedContent.length() - 3);
            }
            cleanedContent = cleanedContent.trim();
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonMap = mapper.readValue(cleanedContent, Map.class);
            
            TalkingGuideResult result = new TalkingGuideResult();
            
            @SuppressWarnings("unchecked")
            List<String> suggestions = (List<String>) jsonMap.getOrDefault("suggestions", new ArrayList<>());
            result.setSuggestions(suggestions);
            
            @SuppressWarnings("unchecked")
            List<String> tips = (List<String>) jsonMap.getOrDefault("tips", new ArrayList<>());
            result.setTips(tips);
            
            @SuppressWarnings("unchecked")
            List<String> topics = (List<String>) jsonMap.getOrDefault("topics", new ArrayList<>());
            result.setTopics(topics);
            
            return result;
        } catch (Exception e) {
            log.error("JSON 파싱 오류", e);
            TalkingGuideResult result = new TalkingGuideResult();
            result.setSuggestions(new ArrayList<>());
            result.setTips(new ArrayList<>());
            result.setTopics(new ArrayList<>());
            return result;
        }
    }
    
    private TalkingGuideResult getDefaultTalkingGuide(TalkingGuideContext context) {
        TalkingGuideResult result = new TalkingGuideResult();
        boolean isParentCompanion = context.getCompanionGeneration() != null && 
                (context.getCompanionGeneration().contains("50") || context.getCompanionGeneration().contains("40"));
        
        if (isParentCompanion) {
            result.setSuggestions(List.of(
                    "엄마, 요즘 감성 여행도 전통 분위기랑 섞어서 즐기면 더 특별하대!",
                    "이런 곳이 요즘 젊은 사람들 사이에서 인기래요. 부모님도 좋아하실 것 같아서 데려왔어요.",
                    "전통과 현대가 만나는 느낌이 신기하지 않으세요?"
            ));
            result.setTips(List.of(
                    "부모님의 반응을 살펴보며 속도를 조절하세요",
                    "전통적인 요소를 강조하면 더 공감하기 쉬워요",
                    "사진을 찍을 때는 함께 찍는 것을 제안해보세요"
            ));
            result.setTopics(List.of(
                    "이 장소의 역사 이야기",
                    "요즘 젊은 사람들의 여행 트렌드",
                    "전통과 현대의 조화"
            ));
        } else {
            result.setSuggestions(List.of(
                    "이런 여행은 세대를 넘어서 함께 즐길 수 있어요!",
                    "서로 다른 관점에서 보면 더 재미있을 것 같아요",
                    "함께 경험하면 더 특별한 추억이 될 거예요"
            ));
            result.setTips(List.of(
                    "서로의 취향을 존중하며 즐기세요",
                    "각자의 옵션을 선택한 후 함께 모이는 시간을 가져보세요",
                    "서로 느낀 점을 공유하면 더 좋아요"
            ));
            result.setTopics(List.of(
                    "각자의 여행 스타일",
                    "이번 여행에서 새롭게 발견한 점",
                    "다음에 함께 가고 싶은 곳"
            ));
        }
        
        return result;
    }
    
    @lombok.Data
    public static class TalkingGuideContext {
        private String userGeneration;
        private String companionGeneration;
        private TravelProfile userProfile;
        private TravelProfile companionProfile;
        private Map<String, Object> recommendation;
    }
    
    @lombok.Data
    public static class TalkingGuideResult {
        private List<String> suggestions;
        private List<String> tips;
        private List<String> topics;
    }
    
    @lombok.Data
    public static class RecommendationResult {
        private String forGeneration;
        private List<String> course = new ArrayList<>();
        private String why;
        private Map<String, String> options = new HashMap<>();
        private String talkingTip;
        private Map<String, Integer> satisfaction = new HashMap<>();
        private String title;
        private String estimatedTime;
        private String estimatedCost;
    }
}

