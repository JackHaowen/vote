package com.example.vote.repository;
import com.example.vote.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface PollRepository extends JpaRepository<Poll, UUID> { }
