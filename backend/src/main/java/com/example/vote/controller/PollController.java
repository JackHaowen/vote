package com.example.vote.controller;

import com.example.vote.dto.*;
import com.example.vote.service.PollService;
import com.example.vote.service.PollUpdateNotifier;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/polls")
public class PollController {
    private final PollService pollService;
    private final PollUpdateNotifier updateNotifier;
    public PollController(PollService pollService, PollUpdateNotifier updateNotifier) { this.pollService = pollService; this.updateNotifier = updateNotifier; }

    @PostMapping
    public ResponseEntity<CreatedPollResponse> create(@Valid @RequestBody CreatePollRequest request) {
        PollResponse poll = pollService.create(request);
        return ResponseEntity.status(201).body(new CreatedPollResponse(poll.id(), "/polls/" + poll.id()));
    }

    @GetMapping("/{pollId}")
    public PollResponse get(@PathVariable UUID pollId) { return pollService.getPoll(pollId); }

    @GetMapping("/{pollId}/results")
    public ResultsResponse results(@PathVariable UUID pollId, @RequestHeader(value = "X-Visitor-Id", required = false) UUID visitorId) {
        return pollService.results(pollId, visitorId);
    }

    @GetMapping(value = "/{pollId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable UUID pollId) {
        pollService.getPoll(pollId);
        return updateNotifier.subscribe(pollId);
    }

    @PostMapping("/{pollId}/votes")
    public ResponseEntity<ResultsResponse> vote(@PathVariable UUID pollId, @Valid @RequestBody VoteRequest request) {
        PollService.VoteSubmission submission = pollService.vote(pollId, request);
        if (submission.changed()) updateNotifier.publishResultChanged(pollId);
        return ResponseEntity.status(submission.created() ? 201 : 200).body(submission.results());
    }
}
