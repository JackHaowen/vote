package com.example.vote.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record VoteRequest(@NotNull UUID optionId, @NotNull UUID visitorId) { }
