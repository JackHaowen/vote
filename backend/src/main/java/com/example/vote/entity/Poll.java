package com.example.vote.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "poll")
public class Poll {
    @Id private UUID id;
    @Column(nullable = false, length = 200) private String title;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected Poll() { }
    public Poll(UUID id, String title) { this.id = id; this.title = title; this.createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
}
