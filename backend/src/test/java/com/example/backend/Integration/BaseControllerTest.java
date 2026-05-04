package com.example.backend.Integration;

import com.example.backend.User.CustomUserDetailsService;
import com.example.backend.User.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext; // 🟢 NHỚ IMPORT CLASS NÀY
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false) 
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
    
    // Trấn an JwtFilter
    @MockBean
    protected JwtUtil jwtUtil; 

    @MockBean
    protected CustomUserDetailsService customUserDetailsService;

    // 🟢 CÚ CHỐT HIỆP ĐỒNG: Trấn an @EnableJpaAuditing
    @MockBean
    protected JpaMetamodelMappingContext jpaMappingContext; 
}