package com.example.vote.service;

import com.example.vote.dto.*;
import com.example.vote.entity.Poll;
import com.example.vote.entity.PollOption;
import com.example.vote.exception.ApiException;
import com.example.vote.repository.PollOptionRepository;
import com.example.vote.repository.PollRepository;
import com.example.vote.repository.VoteRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PollService {
    private final PollRepository polls;
    private final PollOptionRepository options;
    private final VoteRepository votes;
    private final VisitorKeyService visitorKeys;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public PollService(PollRepository polls, PollOptionRepository options, VoteRepository votes, VisitorKeyService visitorKeys, JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        this.polls = polls; this.options = options; this.votes = votes; this.visitorKeys = visitorKeys; this.jdbcTemplate = jdbcTemplate; this.entityManager = entityManager;
    }

    @Transactional
    public PollResponse create(CreatePollRequest request) {
        String title = normalized(request.title());
        List<String> labels = request.options().stream().map(this::normalized).toList();
        if (labels.stream().anyMatch(String::isEmpty) || new HashSet<>(labels).size() != 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OPTIONS", "三个选项不能为空且不能重复");
        }
        if (title.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TITLE", "标题不能为空");
        Poll poll = polls.save(new Poll(UUID.randomUUID(), title));
        List<PollOption> created = new ArrayList<>();
        for (short i = 0; i < 3; i++) created.add(new PollOption(UUID.randomUUID(), poll, labels.get(i), (short) (i + 1)));
        options.saveAll(created);
        return toPollResponse(poll, created);
    }

    @Transactional(readOnly = true)
    public PollResponse getPoll(UUID pollId) { return toPollResponse(requiredPoll(pollId), options.findByPollIdOrderByPositionAsc(pollId)); }

    @Transactional(readOnly = true)
    public ResultsResponse results(UUID pollId, UUID visitorId) {
        requiredPoll(pollId);
        return buildResults(pollId, visitorId == null ? null : visitorKeys.from(visitorId));
    }

    @Transactional
    public VoteSubmission vote(UUID pollId, VoteRequest request) {
        Poll poll = requiredPoll(pollId);
        PollOption option = options.findByIdAndPollId(request.optionId(), pollId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "OPTION_NOT_FOUND", "选项不存在"));
        String visitorKey = visitorKeys.from(request.visitorId());
        Optional<com.example.vote.entity.Vote> existing = votes.findByPollIdAndVisitorKey(pollId, visitorKey);
        if (existing.isPresent()) return retryOrUpdate(pollId, request.optionId(), existing.get().getOptionId(), visitorKey);
        int inserted = jdbcTemplate.update("INSERT INTO vote (id, poll_id, option_id, visitor_key, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) ON CONFLICT (poll_id, visitor_key) DO NOTHING", UUID.randomUUID(), poll.getId(), option.getId(), visitorKey);
        if (inserted == 0) {
            UUID existingOption = votes.findByPollIdAndVisitorKey(pollId, visitorKey)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "VOTE_CONFLICT", "投票状态发生冲突，请重试"))
                    .getOptionId();
            return retryOrUpdate(pollId, request.optionId(), existingOption, visitorKey);
        }
        return new VoteSubmission(true, true, buildResults(pollId, visitorKey));
    }

    private VoteSubmission retryOrUpdate(UUID pollId, UUID requestedOptionId, UUID existingOptionId, String visitorKey) {
        boolean changed = !existingOptionId.equals(requestedOptionId);
        if (changed) {
            jdbcTemplate.update("UPDATE vote SET option_id = ?, updated_at = CURRENT_TIMESTAMP WHERE poll_id = ? AND visitor_key = ?", requestedOptionId, pollId, visitorKey);
            entityManager.clear();
        }
        return new VoteSubmission(false, changed, buildResults(pollId, visitorKey));
    }
    private ResultsResponse buildResults(UUID pollId, String visitorKey) {
        List<PollOption> pollOptions = options.findByPollIdOrderByPositionAsc(pollId);
        Map<UUID, Long> counts = new HashMap<>();
        for (VoteRepository.VoteCount count : votes.countByOptionForPoll(pollId)) counts.put(count.getOptionId(), count.getTotal());
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        UUID selected = visitorKey == null ? null : votes.findByPollIdAndVisitorKey(pollId, visitorKey).map(com.example.vote.entity.Vote::getOptionId).orElse(null);
        List<ResultsResponse.OptionResult> result = pollOptions.stream().map(option -> {
            long count = counts.getOrDefault(option.getId(), 0L);
            int percentage = total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
            return new ResultsResponse.OptionResult(option.getId(), option.getLabel(), option.getPosition(), count, percentage);
        }).toList();
        return new ResultsResponse(pollId, total, selected, result);
    }
    private Poll requiredPoll(UUID pollId) { return polls.findById(pollId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POLL_NOT_FOUND", "投票不存在")); }
    private PollResponse toPollResponse(Poll poll, List<PollOption> pollOptions) { return new PollResponse(poll.getId(), poll.getTitle(), poll.getCreatedAt(), pollOptions.stream().map(o -> new PollResponse.OptionResponse(o.getId(), o.getLabel(), o.getPosition())).toList()); }
    private String normalized(String value) { return value == null ? "" : value.trim(); }
    public record VoteSubmission(boolean created, boolean changed, ResultsResponse results) { }
}
