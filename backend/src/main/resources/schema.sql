CREATE TABLE IF NOT EXISTS generations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    image_path VARCHAR(500) NOT NULL,
    image_content_type VARCHAR(100) NOT NULL,
    image_size BIGINT NOT NULL,
    image_analysis TEXT NULL,
    title VARCHAR(255) NULL,
    content TEXT NULL,
    tags TEXT NULL,
    error_message TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_generations_status
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
