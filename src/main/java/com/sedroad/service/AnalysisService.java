package com.sedroad.service;

import com.sedroad.dto.TravelProfile;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {
    
    private static final Map<String, TravelProfile> GENERATION_PROFILES = new HashMap<>();
    
    static {
        GENERATION_PROFILES.put("10대", TravelProfile.builder().speed(90).stamina(95).budget(40).photo(95).tradition(20).build());
        GENERATION_PROFILES.put("20대", TravelProfile.builder().speed(80).stamina(90).budget(50).photo(90).tradition(30).build());
        GENERATION_PROFILES.put("30대", TravelProfile.builder().speed(70).stamina(80).budget(60).photo(70).tradition(50).build());
        GENERATION_PROFILES.put("40대", TravelProfile.builder().speed(60).stamina(70).budget(70).photo(50).tradition(70).build());
        GENERATION_PROFILES.put("50대+", TravelProfile.builder().speed(50).stamina(60).budget(80).photo(30).tradition(90).build());
    }
    
    public AnalysisResult analyzeGenerationDifference(
            List<UserAnswer> userAnswers,
            String userGeneration,
            String companionGeneration
    ) {
        TravelProfile userProfile = calculateUserProfile(userAnswers);
        TravelProfile companionProfile = GENERATION_PROFILES.getOrDefault(
                companionGeneration, 
                GENERATION_PROFILES.get("50대+")
        );
        
        Differences differences = new Differences();
        differences.setSpeed(Math.abs(userProfile.getSpeed() - companionProfile.getSpeed()));
        differences.setStamina(Math.abs(userProfile.getStamina() - companionProfile.getStamina()));
        differences.setBudget(Math.abs(userProfile.getBudget() - companionProfile.getBudget()));
        differences.setPhoto(Math.abs(userProfile.getPhoto() - companionProfile.getPhoto()));
        differences.setTradition(Math.abs(userProfile.getTradition() - companionProfile.getTradition()));
        
        int speedDiff = userProfile.getSpeed() - companionProfile.getSpeed();
        double scheduleSpeedAdjustment = speedDiff > 0 
                ? Math.max(-30, Math.min(0, -speedDiff * 0.5))
                : 0;
        
        double traditionalRatio = Math.max(0.3, Math.min(0.7, 0.5 + (companionProfile.getTradition() - userProfile.getTradition()) / 200.0));
        
        Adjustments adjustments = new Adjustments();
        adjustments.setScheduleSpeed(Math.abs((int) scheduleSpeedAdjustment));
        FoodBalance foodBalance = new FoodBalance();
        foodBalance.setTraditional((int) Math.round(traditionalRatio * 100));
        foodBalance.setTrendy((int) Math.round((1 - traditionalRatio) * 100));
        adjustments.setFoodBalance(foodBalance);
        adjustments.setPhotoZoneOptimization(true);
        
        String summary = generateSummary(userProfile, companionProfile, userGeneration, companionGeneration);
        
        AnalysisResult result = new AnalysisResult();
        result.setDifferences(differences);
        result.setUserProfile(userProfile);
        result.setCompanionProfile(companionProfile);
        result.setAdjustments(adjustments);
        result.setSummary(summary);
        
        return result;
    }
    
    private TravelProfile calculateUserProfile(List<UserAnswer> answers) {
        TravelProfile profile = TravelProfile.builder()
                .speed(50).stamina(50).budget(50).photo(50).tradition(50)
                .build();
        
        for (int i = 0; i < answers.size(); i++) {
            UserAnswer answer = answers.get(i);
            int value = answer.getValue();
            
            switch (i) {
                case 0 -> profile.setSpeed(value);
                case 1 -> profile.setPhoto(value);
                case 2 -> profile.setTradition(value < 50 ? 30 : 70);
                case 3 -> profile.setBudget(value);
                case 4 -> profile.setStamina(value);
                case 5 -> profile.setSpeed((profile.getSpeed() + value) / 2);
                case 6 -> profile.setTradition((profile.getTradition() + value) / 2);
            }
        }
        
        return profile;
    }
    
    private String generateSummary(
            TravelProfile userProfile,
            TravelProfile companionProfile,
            String userGen,
            String companionGen
    ) {
        List<String> userTraits = new ArrayList<>();
        List<String> companionTraits = new ArrayList<>();
        
        if (userProfile.getSpeed() > 70) userTraits.add("빠른 일정");
        if (userProfile.getPhoto() > 70) userTraits.add("사진 중심");
        if (userProfile.getTradition() < 40) userTraits.add("트렌드 선호");
        if (userProfile.getBudget() < 50) userTraits.add("경험 위주");
        
        if (companionProfile.getSpeed() < 60) companionTraits.add("편안함");
        if (companionProfile.getTradition() > 60) companionTraits.add("익숙함");
        if (companionProfile.getPhoto() < 50) companionTraits.add("기억 중심");
        
        return companionGen + "은 **" + String.join(" + ", companionTraits) + 
               "**을 선호하고, 당신은 **" + String.join(" + ", userTraits) + "** 비중이 높아요.";
    }
    
    @Data
    public static class UserAnswer {
        private Integer questionId;
        private Integer value;
    }
    
    @Data
    public static class AnalysisResult {
        private Differences differences;
        private TravelProfile userProfile;
        private TravelProfile companionProfile;
        private Adjustments adjustments;
        private String summary;
    }
    
    @Data
    public static class Differences {
        private Integer speed;
        private Integer stamina;
        private Integer budget;
        private Integer photo;
        private Integer tradition;
    }
    
    @Data
    public static class Adjustments {
        private Integer scheduleSpeed;
        private FoodBalance foodBalance;
        private Boolean photoZoneOptimization;
    }
    
    @Data
    public static class FoodBalance {
        private Integer traditional;
        private Integer trendy;
    }
}

