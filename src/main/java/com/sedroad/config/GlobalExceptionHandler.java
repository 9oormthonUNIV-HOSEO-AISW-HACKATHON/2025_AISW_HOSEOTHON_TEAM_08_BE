package com.sedroad.config;

import com.sedroad.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException 발생", e);
        
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = "요청 처리 중 오류가 발생했습니다.";
        }
        
        // 진단 미완료 관련 메시지 확인
        if (message.contains("진단") || message.contains("프로필") || 
            message.contains("여행 진단을 먼저 완료")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.diagnosisNotCompleted());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.badRequest(message));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.internalServerError("서버 오류가 발생했습니다."));
    }
}

