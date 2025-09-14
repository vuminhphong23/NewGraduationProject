-- Create group_topics table for many-to-many relationship between groups and topics
CREATE TABLE IF NOT EXISTS group_topics (
    group_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, topic_id),
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

-- Create index for better performance
CREATE INDEX IF NOT EXISTS idx_group_topics_group_id ON group_topics(group_id);
CREATE INDEX IF NOT EXISTS idx_group_topics_topic_id ON group_topics(topic_id);
