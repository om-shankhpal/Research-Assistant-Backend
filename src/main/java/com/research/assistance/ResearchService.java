package com.research.assistance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class ResearchService {
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    public ResearchService(WebClient.Builder webClientBuilder,ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String processContent(ResearchRequest request) {
        // Build the prompt based on the request
        String prompt = buildPrompt(request);
        // Query the AI model API
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );
        String response = webClient.post().uri(geminiApiUrl + geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();


        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(String response) {
        try{
            GeminiResponse geminiResponse = objectMapper.readValue(response,GeminiResponse.class);
            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()){
                GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);
                if (firstCandidate.getContent().getParts() != null &&
                        !firstCandidate.getContent().getParts().isEmpty()){
                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
            return "No content found in response.";
        }catch (Exception e){
            return "Error processing response: " + e.getMessage();
        }
    }

    private String buildPrompt(ResearchRequest request){
        StringBuilder prompt = new StringBuilder();
        switch (request.getOperation()){
            case "summarize":
                prompt.append("Summarize the following content in few sentences:\n");
                break;
            case "suggest":
                prompt.append("Based on the following content, suggest related research topics or papers:\n");
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + request.getOperation());
        }
        prompt.append(request.getContent());
        return prompt.toString();
    }
}
