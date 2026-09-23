package com.example.vote;

import com.example.vote.dto.CreatePollRequest;
import com.example.vote.dto.PollResponse;
import com.example.vote.dto.VoteRequest;
import com.example.vote.service.PollService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PollServiceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired PollService pollService;

    @Test
    void visitorCanChangeVoteWithoutIncreasingTotal() {
        PollResponse poll = pollService.create(new CreatePollRequest("午餐", List.of("面条", "米饭", "沙拉")));
        UUID visitorId = UUID.randomUUID();

        PollService.VoteSubmission first = pollService.vote(poll.id(), new VoteRequest(poll.options().get(0).id(), visitorId));
        PollService.VoteSubmission retry = pollService.vote(poll.id(), new VoteRequest(poll.options().get(0).id(), visitorId));
        PollService.VoteSubmission changed = pollService.vote(poll.id(), new VoteRequest(poll.options().get(1).id(), visitorId));

        assertThat(first.created()).isTrue();
        assertThat(retry.created()).isFalse();
        assertThat(retry.results().totalVotes()).isEqualTo(1);
        assertThat(changed.created()).isFalse();
        assertThat(changed.results().totalVotes()).isEqualTo(1);
        assertThat(changed.results().selectedOptionId()).isEqualTo(poll.options().get(1).id());
        assertThat(changed.results().options()).extracting(result -> result.votes()).containsExactly(0L, 1L, 0L);
    }
}
