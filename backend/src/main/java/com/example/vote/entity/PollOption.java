package com.example.vote.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "poll_option")
public class PollOption {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "poll_id") private Poll poll;
    @Column(nullable = false, length = 100) private String label;
    @Column(nullable = false) private short position;

    protected PollOption() { }
    public PollOption(UUID id, Poll poll, String label, short position) { this.id = id; this.poll = poll; this.label = label; this.position = position; }
    public UUID getId() { return id; }
    public Poll getPoll() { return poll; }
    public String getLabel() { return label; }
    public short getPosition() { return position; }
}
