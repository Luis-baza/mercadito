package com.uniremington.marketplace.service;

import com.uniremington.marketplace.config.GeminiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiChatServiceTests {

    @Test
    void sendsPreviousInteractionAndExtractsFinalText() {
        GeminiProperties properties = new GeminiProperties(
                "test-api-key",
                "gemini-4-hydrogen",
                "https://generativelanguage.googleapis.com/v1beta/interactions",
                "2026-05-20",
                30,
                1000,
                "Responde en español."
        );
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        server.expect(requestTo(properties.baseUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andExpect(header("Api-Revision", "2026-05-20"))
                .andExpect(content().json("""
                        {
                          "model": "gemini-4-hydrogen",
                          "input": "¿Cómo publico?",
                          "system_instruction": "Responde en español.",
                          "previous_interaction_id": "interaction-anterior"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id": "interaction-nueva",
                          "steps": [
                            {"type": "thought", "content": []},
                            {"type": "model_output", "content": [
                              {"type": "text", "text": "Ve a publicar "},
                              {"type": "text", "text": "un producto."}
                            ]}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        GeminiChatResult result = new GeminiChatService(restClient, properties)
                .responder("¿Cómo publico?", "interaction-anterior");

        assertEquals("Ve a publicar un producto.", result.respuesta());
        assertEquals("interaction-nueva", result.interactionId());
        server.verify();
    }
}
