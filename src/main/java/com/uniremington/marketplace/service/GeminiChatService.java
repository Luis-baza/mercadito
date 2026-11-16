package com.uniremington.marketplace.service;

import com.uniremington.marketplace.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GeminiChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiChatService.class);
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final String API_REVISION_HEADER = "Api-Revision";

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiChatService(RestClient geminiRestClient, GeminiProperties properties) {
        this.restClient = geminiRestClient;
        this.properties = properties;
    }

    public GeminiChatResult responder(String mensaje, String previousInteractionId) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new GeminiChatException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El chat no está configurado. Falta definir GEMINI_API_KEY."
            );
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.model());
        request.put("input", mensaje);
        request.put("system_instruction", properties.systemInstruction());
        if (previousInteractionId != null && !previousInteractionId.isBlank()) {
            request.put("previous_interaction_id", previousInteractionId);
        }

        try {
            JsonNode response = restClient.post()
                    .uri(properties.baseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, properties.apiKey())
                    .header(API_REVISION_HEADER, properties.apiRevision())
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw unavailable();
            }
            String id = response.path("id").asString("");
            String text = extractOutputText(response);
            if (id.isBlank() || text.isBlank()) {
                LOGGER.warn("Gemini devolvió una interacción sin id o texto final.");
                throw unavailable();
            }
            return new GeminiChatResult(text, id);
        } catch (RestClientResponseException exception) {
            throw translateResponseError(exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            LOGGER.warn("La llamada a Gemini agotó el tiempo de espera o no pudo conectarse.");
            throw new GeminiChatException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No pudimos conectar con el asistente. Inténtalo de nuevo en unos momentos."
            );
        }
    }

    private String extractOutputText(JsonNode response) {
        StringBuilder text = new StringBuilder();
        JsonNode steps = response.path("steps");
        if (steps.isArray()) {
            for (JsonNode step : steps) {
                if (!"model_output".equals(step.path("type").asString())) {
                    continue;
                }
                JsonNode content = step.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode item : content) {
                    if ("text".equals(item.path("type").asString())) {
                        text.append(item.path("text").asString(""));
                    }
                }
            }
        }
        return text.toString().trim();
    }

    private GeminiChatException translateResponseError(int status) {
        if (status == 401 || status == 403) {
            LOGGER.warn("Gemini rechazó la clave de API (HTTP {}).", status);
            return new GeminiChatException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La clave de Gemini no es válida o no tiene permiso. Verifica GEMINI_API_KEY."
            );
        }
        if (status == 429) {
            LOGGER.warn("Gemini indicó que se alcanzó el límite de cuota.");
            return new GeminiChatException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "El asistente alcanzó su límite de uso. Inténtalo más tarde."
            );
        }
        if (status >= 500) {
            LOGGER.warn("Gemini no está disponible (HTTP {}).", status);
        } else {
            LOGGER.warn("Gemini rechazó la solicitud (HTTP {}).", status);
        }
        return unavailable();
    }

    private GeminiChatException unavailable() {
        return new GeminiChatException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "El asistente no pudo responder en este momento. Inténtalo de nuevo."
        );
    }
}
