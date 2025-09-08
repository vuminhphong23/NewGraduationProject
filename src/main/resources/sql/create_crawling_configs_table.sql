-- Tạo bảng crawling_configs
CREATE TABLE IF NOT EXISTS crawling_configs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    base_url VARCHAR(500) NOT NULL,
    title_selector VARCHAR(200) NOT NULL,
    content_selector VARCHAR(200),
    link_selector VARCHAR(200),
    image_selector VARCHAR(200),
    author_selector VARCHAR(200),
    date_selector VARCHAR(200),
    topic_name VARCHAR(100) NOT NULL,
    topic_selector VARCHAR(200),
    max_posts INTEGER NOT NULL DEFAULT 10,
    interval_minutes INTEGER NOT NULL DEFAULT 60,
    enabled BOOLEAN NOT NULL DEFAULT true,
    user_agent VARCHAR(500) DEFAULT 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    timeout INTEGER DEFAULT 10000,
    additional_headers TEXT,
    post_processing_rules TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    last_error TEXT,
    last_crawled_at TIMESTAMP,
    total_crawled INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Tạo index cho performance
CREATE INDEX IF NOT EXISTS idx_crawling_configs_enabled ON crawling_configs(enabled);
CREATE INDEX IF NOT EXISTS idx_crawling_configs_status ON crawling_configs(status);
CREATE INDEX IF NOT EXISTS idx_crawling_configs_last_crawled ON crawling_configs(last_crawled_at);
CREATE INDEX IF NOT EXISTS idx_crawling_configs_created_by ON crawling_configs(created_by);

-- Tạo trigger để cập nhật updated_at
CREATE OR REPLACE FUNCTION update_crawling_configs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_crawling_configs_updated_at
    BEFORE UPDATE ON crawling_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_crawling_configs_updated_at();


