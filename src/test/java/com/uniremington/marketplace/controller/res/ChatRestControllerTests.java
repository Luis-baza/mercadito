package com.uniremington.marketplace.controller.res;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import com.uniremington.marketplace.config.GeminiProperties;
import com.uniremington.marketplace.service.ChatRateLimiter;
import com.uniremington.marketplace.service.GeminiChatResult;
import com.uniremington.marketplace.service.GeminiChatService;

class ChatRestControllerTests {

        private static final String URL = "/chat/mensaje";

        private final GeminiChatService chatService = mock(GeminiChatService.class);
        private final GeminiProperties properties = new GeminiProperties(
                        "", "gemini-4-hydrogen", "https://example.test", "2026-05-20", 30, 8, "Ayuda.");
        private final StaticApplicationContext applicationContext = new StaticApplicationContext();
        private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                applicationContext.getDefaultListableBeanFactory()
                                .registerSingleton("geminiProperties", properties);
                applicationContext.refresh();
                validator.setConstraintValidatorFactory(
                                new SpringConstraintValidatorFactory(applicationContext.getBeanFactory()));
                validator.afterPropertiesSet();
                mockMvc = MockMvcBuilders.standaloneSetup(
                                new ChatRestController(chatService, new ChatRateLimiter()))
                                .setControllerAdvice(new ChatControllerAdvice())
                                .setValidator(validator)
                                .build();
        }

        @AfterEach
        void tearDown() {
                validator.close();
                applicationContext.close();
        }

        @Test
        void rejectsBlankMessage() throws Exception {
                mockMvc.perform(post(URL)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {"mensaje":"   "}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().json("""
                                                {"error":"Escribe un mensaje antes de enviarlo."}
                                                """));
                verifyNoInteractions(chatService);
        }

        @Test
        void rejectsMessageLongerThanConfiguredLimit() throws Exception {
                mockMvc.perform(post(URL)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {"mensaje":"123456789"}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().json("""
                                                {"error":"El mensaje supera el máximo de caracteres permitido."}
                                                """));
                verifyNoInteractions(chatService);
        }

        @Test
        void returnsChatResponse() throws Exception {
                when(chatService.responder(eq("Hola"), eq(null)))
                                .thenReturn(new GeminiChatResult("¡Hola! ¿En qué te ayudo?", "interaction-1"));

                mockMvc.perform(post(URL)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {"mensaje":"Hola"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(content().json("""
                                                {"respuesta":"¡Hola! ¿En qué te ayudo?"}
                                                """));

                verify(chatService).responder("Hola", null);
        }

        @Test
        void continuesConversationUsingPreviousInteractionId() throws Exception {
                MockHttpSession session = new MockHttpSession();
                when(chatService.responder("Hola", null))
                                .thenReturn(new GeminiChatResult("¡Hola!", "interaction-1"));
                when(chatService.responder("Sigue", "interaction-1"))
                                .thenReturn(new GeminiChatResult("Publica tu producto.", "interaction-2"));

                mockMvc.perform(post(URL)
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {"mensaje":"Hola"}
                                                """))
                                .andExpect(status().isOk());
                mockMvc.perform(post(URL)
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {"mensaje":"Sigue"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(content().json("""
                                                {"respuesta":"Publica tu producto."}
                                                """));

                verify(chatService).responder("Sigue", "interaction-1");
        }

}
