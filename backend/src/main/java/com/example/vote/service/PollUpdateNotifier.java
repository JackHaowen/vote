package com.example.vote.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class PollUpdateNotifier {
    private final ConcurrentHashMap<UUID, CopyOnWriteArraySet<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID pollId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        CopyOnWriteArraySet<SseEmitter> emitters = subscribers.computeIfAbsent(pollId, ignored -> new CopyOnWriteArraySet<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> remove(pollId, emitter));
        emitter.onTimeout(() -> { emitter.complete(); remove(pollId, emitter); });
        emitter.onError(error -> remove(pollId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException exception) {
            remove(pollId, emitter);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void publishResultChanged(UUID pollId) {
        CopyOnWriteArraySet<SseEmitter> emitters = subscribers.get(pollId);
        if (emitters == null) return;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("results-updated").data(pollId.toString()));
            } catch (IOException exception) {
                remove(pollId, emitter);
                emitter.completeWithError(exception);
            }
        }
    }

    private void remove(UUID pollId, SseEmitter emitter) {
        subscribers.computeIfPresent(pollId, (ignored, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
