package com.duolinfo.ia.proj.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.duolinfo.ia.proj.dto.request.MascotChatRequestDTO;
import com.duolinfo.ia.proj.dto.response.MascotChatResponseDTO;
import com.duolinfo.ia.proj.exception.GlobalExceptionHandler;
import com.duolinfo.ia.proj.service.MascotAiAssistantService;

import tools.jackson.databind.ObjectMapper;

class MascotAssistantControllerTest {

    private MockMvc mockMvc;
    private MascotAiAssistantService mascotAiAssistantService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mascotAiAssistantService = mock(MascotAiAssistantService.class);
        objectMapper = new ObjectMapper();

        MascotAssistantController controller = new MascotAssistantController(mascotAiAssistantService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void chat_returnsServiceResponseForAuthenticatedPrincipal() throws Exception {
        when(mascotAiAssistantService.chat(any(MascotChatRequestDTO.class), eq("student@escola.com")))
            .thenReturn(new MascotChatResponseDTO(
                "Abra a aba Revisão Espaçada (SRS) para praticar seus cards.",
                true,
                "srsReview"
            ));

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
            "student@escola.com",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        Map<String, Object> body = Map.of(
            "message", "Onde encontro meus flashcards?",
            "activeSection", "home",
            "userRole", "student",
            "currentLanguage", "pt",
            "history", List.of(Map.of("role", "user", "content", "Oi Bode"))
        );

        mockMvc.perform(post("/api/ai/mascot/chat")
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reply", is("Abra a aba Revisão Espaçada (SRS) para praticar seus cards.")))
            .andExpect(jsonPath("$.inScope", is(true)))
            .andExpect(jsonPath("$.suggestedAction", is("srsReview")));

        verify(mascotAiAssistantService).chat(any(MascotChatRequestDTO.class), eq("student@escola.com"));
    }

    @Test
    void chat_rejectsBlankMessageWithBadRequest() throws Exception {
        Map<String, Object> body = Map.of(
            "message", "   ",
            "activeSection", "home"
        );

        mockMvc.perform(post("/api/ai/mascot/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }
}
