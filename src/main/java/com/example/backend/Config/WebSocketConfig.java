package com.example.backend.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import com.example.backend.User.JwtUtil;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Chỉ kiểm tra khi Client cố gắng CONNECT
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    
                    // 1. Lấy token từ header "Authorization"
                    String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
                    
                    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                        String token = authorizationHeader.substring(7);
                        
                        // 2. Validate Token & Lấy Username (studentCode)
                        try {
                            String studentCode = jwtUtil.extractUsername(token); // Hoặc hàm tương ứng của em
                            
                            if (studentCode != null) {
                                // 3. Load UserDetails (để lấy quyền authorities nếu cần)
                                UserDetails userDetails = userDetailsService.loadUserByUsername(studentCode);
                                
                                // 4. Tạo Authentication Token chuẩn của Spring Security
                                UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                                
                                // 5. QUAN TRỌNG NHẤT: Gán User đã xác thực vào Session của WebSocket
                                accessor.setUser(authentication);
                                
                                System.out.println(">>> [WS AUTH] Successfully set User Authentication for: " + studentCode);
                            }
                        } catch (Exception e) {
                            System.out.println(">>> [WS AUTH] Token validation failed: " + e.getMessage());
                        }
                    } else {
                        System.out.println(">>> [WS AUTH] No Authorization header found in Connect frame.");
                    }
                }
                return message;
            }
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); 
        config.setApplicationDestinationPrefixes("/app"); 
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:5173")
                .withSockJS();
    }
}
