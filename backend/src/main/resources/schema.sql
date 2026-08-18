CREATE TABLE IF NOT EXISTS generations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
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

SET @user_id_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'generations' AND column_name = 'user_id'
);
SET @user_id_ddl = IF(
    @user_id_exists = 0,
    'ALTER TABLE generations ADD COLUMN user_id BIGINT NULL AFTER id',
    'SELECT 1'
);
PREPARE user_id_statement FROM @user_id_ddl;
EXECUTE user_id_statement;
DEALLOCATE PREPARE user_id_statement;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @generation_user_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'generations'
      AND index_name = 'idx_generations_user_created'
);
SET @generation_user_index_ddl = IF(
    @generation_user_index_exists = 0,
    'ALTER TABLE generations ADD INDEX idx_generations_user_created (user_id, created_at)',
    'SELECT 1'
);
PREPARE generation_user_index_statement FROM @generation_user_index_ddl;
EXECUTE generation_user_index_statement;
DEALLOCATE PREPARE generation_user_index_statement;

SET @generation_user_fk_exists = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'generations'
      AND constraint_name = 'fk_generations_user'
      AND constraint_type = 'FOREIGN KEY'
);
SET @generation_user_fk_ddl = IF(
    @generation_user_fk_exists = 0,
    'ALTER TABLE generations ADD CONSTRAINT fk_generations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL',
    'SELECT 1'
);
PREPARE generation_user_fk_statement FROM @generation_user_fk_ddl;
EXECUTE generation_user_fk_statement;
DEALLOCATE PREPARE generation_user_fk_statement;
