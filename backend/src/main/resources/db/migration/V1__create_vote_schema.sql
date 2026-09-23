CREATE TABLE poll (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT poll_title_not_blank CHECK (length(btrim(title)) > 0)
);

CREATE TABLE poll_option (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
    label VARCHAR(100) NOT NULL,
    position SMALLINT NOT NULL,
    CONSTRAINT poll_option_label_not_blank CHECK (length(btrim(label)) > 0),
    CONSTRAINT poll_option_position_range CHECK (position BETWEEN 1 AND 3),
    CONSTRAINT uk_poll_option_position UNIQUE (poll_id, position)
);

CREATE TABLE vote (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES poll_option(id) ON DELETE RESTRICT,
    visitor_key VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vote_poll_visitor UNIQUE (poll_id, visitor_key)
);

CREATE INDEX idx_poll_option_poll_id ON poll_option (poll_id);
CREATE INDEX idx_vote_poll_option_id ON vote (poll_id, option_id);
