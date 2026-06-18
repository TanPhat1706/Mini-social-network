package com.example.backend;

import java.util.HashMap;
import java.util.Map;

import com.example.backend.Storage.StorageException;
import com.example.backend.User.BadRequestException;
import com.example.backend.User.UserProfileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j // 🟢 BẬT LOG: Rất quan trọng cho môi trường AWS
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserProfileNotFound(UserProfileNotFoundException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "User not found");
        errorResponse.put("message", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // ==========================================
    // 🟢 THÊM MỚI: BẮT LỖI VALIDATION TỪ @Valid
    // ==========================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Validation Failed");
        errorResponse.put("message", "Dữ liệu đầu vào không hợp lệ");

        // Gom toàn bộ lỗi của các trường (fields) bị sai vào một Map
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // Trả về chi tiết các field bị lỗi cho Frontend dễ parse
        errorResponse.put("details", fieldErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Payload Too Large");
        errorResponse.put("message", "Kích thước file vượt quá giới hạn cho phép.");
        return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, String>> handleStorageException(StorageException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // ==========================================
    // 🟢 CẬP NHẬT: CHỐT CHẶN CUỐI CÙNG (LỖI 500)
    // ==========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex) {
        // 🟢 MỞ COMMENT BẮT BUỘC: Nếu không ghi log ở đây, khi lên Cloud bạn sẽ "mù rờ"
        // không biết vì sao app sập
        log.error("Lỗi hệ thống nghiêm trọng (500 Internal Server Error): ", ex);

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal Server Error");
        // Giấu chi tiết lỗi thực sự với người dùng để bảo mật (Không leak database/code
        // info)
        errorResponse.put("message", "Hệ thống đang gặp sự cố, vui lòng thử lại sau.");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}