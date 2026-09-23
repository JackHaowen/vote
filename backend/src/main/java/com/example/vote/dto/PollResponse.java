package com.example.vote.dto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record PollResponse(UUID id, String title, Instant createdAt, List<OptionResponse> options) {
    public record OptionResponse(UUID id, String label, short position) { }
}
