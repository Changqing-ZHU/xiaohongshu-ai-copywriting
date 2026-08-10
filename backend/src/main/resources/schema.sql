CREATE TABLE IF NOT EXISTS generations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL,
    original_file_name VARCHAR(255) NULL,
    stored_file_name VARCHAR(255) NULL,
    image_path VARCHAR(500) NULL,
    image_content_type VARCHAR(100) NULL,
    image_size BIGINT NULL,
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

-- The creation API precedes image upload, so existing installations must allow
-- image metadata to remain empty until that later feature is implemented.
ALTER TABLE generations
    MODIFY original_file_name VARCHAR(255) NULL,
    MODIFY stored_file_name VARCHAR(255) NULL,
    MODIFY image_path VARCHAR(500) NULL,
    MODIFY image_content_type VARCHAR(100) NULL,
    MODIFY image_size BIGINT NULL;
