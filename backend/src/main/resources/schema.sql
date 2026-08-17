CREATE TABLE IF NOT EXISTS generations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL,
    source_url VARCHAR(2048) NULL,
    url_title VARCHAR(500) NULL,
    url_description TEXT NULL,
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

-- Keep existing installations compatible without introducing a migration tool.
SET @source_url_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'generations' AND column_name = 'source_url'
);
SET @source_url_ddl = IF(
    @source_url_exists = 0,
    'ALTER TABLE generations ADD COLUMN source_url VARCHAR(2048) NULL AFTER status',
    'SELECT 1'
);
PREPARE source_url_statement FROM @source_url_ddl;
EXECUTE source_url_statement;
DEALLOCATE PREPARE source_url_statement;

SET @url_title_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'generations' AND column_name = 'url_title'
);
SET @url_title_ddl = IF(
    @url_title_exists = 0,
    'ALTER TABLE generations ADD COLUMN url_title VARCHAR(500) NULL AFTER source_url',
    'SELECT 1'
);
PREPARE url_title_statement FROM @url_title_ddl;
EXECUTE url_title_statement;
DEALLOCATE PREPARE url_title_statement;

SET @url_description_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'generations' AND column_name = 'url_description'
);
SET @url_description_ddl = IF(
    @url_description_exists = 0,
    'ALTER TABLE generations ADD COLUMN url_description TEXT NULL AFTER url_title',
    'SELECT 1'
);
PREPARE url_description_statement FROM @url_description_ddl;
EXECUTE url_description_statement;
DEALLOCATE PREPARE url_description_statement;
