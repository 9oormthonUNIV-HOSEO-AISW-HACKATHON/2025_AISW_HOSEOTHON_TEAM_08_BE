package com.sedroad.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 일관된 에러 응답 형식을 제공하는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String error;
    private Map<String, Object> details;
    
    /**
     * 간단한 에러 응답 생성
     */
    public static ErrorResponse of(String message) {
        return ErrorResponse.builder()
                .message(message)
                .build();
    }
    
    /**
     * 에러 코드와 함께 에러 응답 생성
     */
    public static ErrorResponse of(String message, String error) {
        return ErrorResponse.builder()
                .message(message)
                .error(error)
                .build();
    }
    
    /**
     * 상세 정보와 함께 에러 응답 생성
     */
    public static ErrorResponse of(String message, String error, Map<String, Object> details) {
        return ErrorResponse.builder()
                .message(message)
                .error(error)
                .details(details)
                .build();
    }
    
    /**
     * 진단 미완료 에러 응답
     */
    public static ErrorResponse diagnosisNotCompleted() {
        return ErrorResponse.builder()
                .message("여행 진단을 먼저 완료해주세요.")
                .error("DIAGNOSIS_NOT_COMPLETED")
                .build();
    }
    
    /**
     * 인증 실패 에러 응답
     */
    public static ErrorResponse unauthorized(String message) {
        return ErrorResponse.builder()
                .message(message != null ? message : "인증이 필요합니다.")
                .error("UNAUTHORIZED")
                .build();
    }
    
    /**
     * 권한 없음 에러 응답
     */
    public static ErrorResponse forbidden(String message) {
        return ErrorResponse.builder()
                .message(message != null ? message : "권한이 없습니다.")
                .error("FORBIDDEN")
                .build();
    }
    
    /**
     * 리소스 없음 에러 응답
     */
    public static ErrorResponse notFound(String resource) {
        return ErrorResponse.builder()
                .message(resource + "을(를) 찾을 수 없습니다.")
                .error("NOT_FOUND")
                .build();
    }
    
    /**
     * 잘못된 요청 에러 응답
     */
    public static ErrorResponse badRequest(String message) {
        return ErrorResponse.builder()
                .message(message)
                .error("BAD_REQUEST")
                .build();
    }
    
    /**
     * 서버 내부 오류 응답
     */
    public static ErrorResponse internalServerError(String message) {
        return ErrorResponse.builder()
                .message(message != null ? message : "서버 오류가 발생했습니다.")
                .error("INTERNAL_SERVER_ERROR")
                .build();
    }
}

