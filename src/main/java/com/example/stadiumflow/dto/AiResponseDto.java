package com.example.stadiumflow.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for Gemini AI responses.
 * Encapsulates the conversational feedback and provider metadata.
 */
public class AiResponseDto implements Serializable {
    private String response;
    private String provider;

    public AiResponseDto() {}

    public AiResponseDto(String response, String provider) {
        this.response = response;
        this.provider = provider;
    }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
