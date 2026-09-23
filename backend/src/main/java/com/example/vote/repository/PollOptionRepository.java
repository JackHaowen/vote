package com.example.vote.repository;
import com.example.vote.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {
    List<PollOption> findByPollIdOrderByPositionAsc(UUID pollId);
    Optional<PollOption> findByIdAndPollId(UUID id, UUID pollId);
}
