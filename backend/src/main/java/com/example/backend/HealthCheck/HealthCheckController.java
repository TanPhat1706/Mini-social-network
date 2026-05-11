package com.example.backend.HealthCheck;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @Autowired
    private HealthCheckService healthCheckService;

    /**
     * Endpoint để kiểm tra tình trạng cơ bản của API
     * @return Trạng thái API và các thông tin liên quan
     */
    @GetMapping("/status")
    public ResponseEntity<?> getHealthStatus() {
        try {
            Map<String, Object> response = healthCheckService.checkHealth();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "DOWN",
                            "message", "Health check failed: " + e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    /**
     * Endpoint để kiểm tra kết nối cơ sở dữ liệu
     * @return Trạng thái kết nối database
     */
    @GetMapping("/db")
    public ResponseEntity<?> checkDatabase() {
        try {
            boolean dbHealthy = healthCheckService.isDatabaseHealthy();
            if (dbHealthy) {
                return ResponseEntity.ok(Map.of(
                        "status", "UP",
                        "database", "Connected",
                        "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(503)
                        .body(Map.of(
                                "status", "DOWN",
                                "database", "Disconnected",
                                "timestamp", System.currentTimeMillis()
                        ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "status", "DOWN",
                            "database", "Connection failed",
                            "message", e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    /**
     * Endpoint đơn giản để kiểm tra API đang chạy hay không
     * @return Status "UP"
     */
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "API is running",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
