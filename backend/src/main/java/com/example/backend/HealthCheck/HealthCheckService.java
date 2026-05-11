package com.example.backend.HealthCheck;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import java.lang.management.ManagementFactory;

@Service
public class HealthCheckService {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * Kiểm tra tình trạng chung của API
     * @return Map chứa thông tin về trạng thái API
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("status", "UP");
        response.put("service", "Mini Social Network API");
        response.put("timestamp", System.currentTimeMillis());
        response.put("database", isDatabaseHealthy() ? "Connected" : "Disconnected");
        response.put("uptime", getUptime());
        response.put("version", "1.0.0");
        
        return response;
    }

    /**
     * Kiểm tra kết nối database
     * @return true nếu database khoẻ, false nếu có vấn đề
     */
    public boolean isDatabaseHealthy() {
        try {
            if (jdbcTemplate == null) {
                return false;
            }
            
            // Thực hiện một câu query đơn giản để kiểm tra kết nối
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy thời gian hoạt động của ứng dụng
     * @return Thời gian uptime (milliseconds)
     */
    private long getUptime() {
        return System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime();
    }
}
