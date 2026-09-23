package com.example.vote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePollRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Size(min = 3, max = 3) List<@NotBlank @Size(max = 100) String> options) { }
