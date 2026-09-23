package com.example.vote.dto;
import java.util.List;
import java.util.UUID;
public record ResultsResponse(UUID pollId, long totalVotes, UUID selectedOptionId, List<OptionResult> options) {
    public record OptionResult(UUID id, String label, short position, long votes, int percentage) { }
}
