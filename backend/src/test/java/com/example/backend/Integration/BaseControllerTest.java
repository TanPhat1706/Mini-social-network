package com.example.backend.Integration;

import com.example.backend.User.CustomUserDetailsService;
import com.example.backend.User.JwtUtil;
import com.example.backend.User.SecurityHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false) 
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
    
    // 🟢 MOCK CHO JWTFILTER CẦN
    @MockBean
    protected JwtUtil jwtUtil; 

    @MockBean
    protected SecurityHistoryRepository securityHistoryRepository;

    @MockBean
    protected CustomUserDetailsService customUserDetailsService;

    // 🟢 MOCK CHO @ENABLEJPAUDITING
    @MockBean
    protected JpaMetamodelMappingContext jpaMappingContext;

    // 🟢 MOCK CHO EMAIL FUNCTIONALITY
    @MockBean
    protected JavaMailSender javaMailSender;
}