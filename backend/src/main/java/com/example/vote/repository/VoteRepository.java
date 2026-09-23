package com.example.vote.repository;

import com.example.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {
    Optional<Vote> findByPollIdAndVisitorKey(UUID pollId, String visitorKey);
    @Query("select v.option.id as optionId, count(v) as total from Vote v where v.poll.id = :pollId group by v.option.id")
    List<VoteCount> countByOptionForPoll(UUID pollId);
    interface VoteCount { UUID getOptionId(); long getTotal(); }
}
