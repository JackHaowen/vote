package com.example.vote.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vote", uniqueConstraints = @UniqueConstraint(name = "uk_vote_poll_visitor", columnNames = {"poll_id", "visitor_key"}))
public class Vote {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "poll_id") private Poll poll;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "option_id") private PollOption option;
    @Column(name = "visitor_key", nullable = false, length = 64) private String visitorKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Vote() { }
    public Vote(UUID id, Poll poll, PollOption option, String visitorKey) { this.id = id; this.poll = poll; this.option = option; this.visitorKey = visitorKey; this.createdAt = Instant.now(); this.updatedAt = this.createdAt; }
    public UUID getOptionId() { return option.getId(); }
}
