package com.example.chat_agent.service;

import com.example.chat_agent.entity.Conversation;
import com.example.chat_agent.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ConversationRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    public ChatService(ConversationRepository repository) {
        this.repository = repository;
    }

    public String sendMessage(String userId, String message) {

        if (apiKey == null || apiKey.isBlank()) {
            return "Gemini no está configurado (falta API key).";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", message)
                                )
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            GeminiResponse response = restTemplate.postForObject(
                    url,
                    requestEntity,
                    GeminiResponse.class
            );

            String answer = extractTextFromResponse(response);

            try {
                Conversation conv = new Conversation(userId, message, answer);
                repository.save(conv);
            } catch (Exception dbEx) {
                dbEx.printStackTrace();
                answer += "\n\n(Advertencia: hubo un problema al guardar en la base de datos)";
            }

            return answer;

        } catch (HttpClientErrorException e) {
            System.err.println("❌ Error HTTP al llamar a Gemini: " + e.getStatusCode());
            System.err.println("❌ Body: " + e.getResponseBodyAsString());
            return "Error al llamar a Gemini (" + e.getStatusCode() + "): "
                    + e.getResponseBodyAsString();

        } catch (RestClientException e) {
            e.printStackTrace();
            return "No se pudo conectar con Gemini: " + e.getMessage();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error interno del servidor: " + e.getMessage();
        }
    }

    public List<Conversation> getHistory(String userId) {
        try {
            return repository.findByUserIdOrderByTimestampAsc(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private String extractTextFromResponse(GeminiResponse response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            return "No se recibió respuesta del modelo Gemini.";
        }

        GeminiCandidate candidate = response.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
            return "Gemini devolvió contenido vacío.";
        }

        StringBuilder sb = new StringBuilder();
        for (GeminiPart part : candidate.getContent().getParts()) {
            if (part.getText() != null) {
                sb.append(part.getText());
            }
        }

        String result = sb.toString().trim();
        if (result.isEmpty()) {
            return "Gemini no generó texto.";
        }
        return result;
    }

    public static class GeminiResponse {
        private List<GeminiCandidate> candidates;

        public List<GeminiCandidate> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<GeminiCandidate> candidates) {
            this.candidates = candidates;
        }
    }

    public static class GeminiCandidate {
        private GeminiContent content;

        public GeminiContent getContent() {
            return content;
        }

        public void setContent(GeminiContent content) {
            this.content = content;
        }
    }

    public static class GeminiContent {
        private List<GeminiPart> parts;

        public List<GeminiPart> getParts() {
            return parts;
        }

        public void setParts(List<GeminiPart> parts) {
            this.parts = parts;
        }
    }

    public static class GeminiPart {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
