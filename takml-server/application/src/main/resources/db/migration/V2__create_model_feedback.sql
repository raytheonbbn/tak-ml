CREATE TABLE model_feedback (
    id                      BIGSERIAL PRIMARY KEY,
    model_hash              VARCHAR(64) NOT NULL,
    callsign                VARCHAR(64) NOT NULL,
    input_type              VARCHAR(16) NOT NULL, -- type of input (text, image, audio), add more types as needed
    input                   TEXT NOT NULL,  -- text input or hash for non-text input stored in tak file server
    output                  TEXT NOT NULL,  -- model output as TEXT, change to json?
    evaluation              VARCHAR(32) NOT NULL, -- change to array?
    evaluation_confidence   SMALLINT CHECK (evaluation_confidence BETWEEN 1 AND 5),
    evaluation_rating       SMALLINT CHECK (evaluation_confidence BETWEEN 1 AND 5),
    comment                 TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_model_feedback_model_hash
    ON model_feedback (model_hash);
