CREATE TABLE IF NOT EXISTS articles (
    id VARCHAR(255) PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    normalized_title VARCHAR(500),
    summary TEXT,
    source VARCHAR(100),
    date DATE,
    image_url VARCHAR(1000),
    url VARCHAR(1000) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);