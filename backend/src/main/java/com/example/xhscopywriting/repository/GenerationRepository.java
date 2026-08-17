package com.example.xhscopywriting.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.xhscopywriting.model.Generation;

@Repository
public class GenerationRepository {

    private static final String INSERT_SQL = """
            INSERT INTO generations (
                status,
                source_url,
                url_title,
                url_description,
                original_file_name,
                stored_file_name,
                image_path,
                image_content_type,
                image_size,
                image_analysis,
                title,
                content,
                tags,
                error_message,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT
                id,
                status,
                source_url,
                url_title,
                url_description,
                original_file_name,
                stored_file_name,
                image_path,
                image_content_type,
                image_size,
                image_analysis,
                title,
                content,
                tags,
                error_message,
                created_at,
                updated_at
            FROM generations
            WHERE id = ?
            """;

    private static final String UPDATE_URL_CONTENT_SQL = """
            UPDATE generations
            SET url_title = ?,
                url_description = ?,
                updated_at = ?
            WHERE id = ?
            """;

    private static final String UPDATE_IMAGE_INFO_SQL = """
            UPDATE generations
            SET original_file_name = ?,
                stored_file_name = ?,
                image_path = ?,
                image_content_type = ?,
                image_size = ?,
                updated_at = ?
            WHERE id = ?
            """;

    private static final String UPDATE_GENERATION_RESULT_SQL = """
            UPDATE generations
            SET image_analysis = ?,
                title = ?,
                content = ?,
                tags = ?,
                error_message = NULL,
                status = ?,
                updated_at = ?
            WHERE id = ?
            """;

    private static final String MARK_FAILED_SQL = """
            UPDATE generations
            SET status = 'FAILED',
                error_message = ?,
                updated_at = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public GenerationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Generation generation) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    INSERT_SQL,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, generation.getStatus());
            statement.setString(2, generation.getSourceUrl());
            statement.setString(3, generation.getUrlTitle());
            statement.setString(4, generation.getUrlDescription());
            statement.setString(5, generation.getOriginalFileName());
            statement.setString(6, generation.getStoredFileName());
            statement.setString(7, generation.getImagePath());
            statement.setString(8, generation.getImageContentType());
            statement.setObject(9, generation.getImageSize(), Types.BIGINT);
            statement.setString(10, generation.getImageAnalysis());
            statement.setString(11, generation.getTitle());
            statement.setString(12, generation.getContent());
            statement.setString(13, generation.getTags());
            statement.setString(14, generation.getErrorMessage());
            statement.setTimestamp(15, Timestamp.valueOf(generation.getCreatedAt()));
            statement.setTimestamp(16, Timestamp.valueOf(generation.getUpdatedAt()));
            return statement;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new IllegalStateException("MySQL did not return a generated generation id");
        }

        Long id = generatedKey.longValue();
        generation.setId(id);
        return id;
    }

    public Optional<Generation> findById(Long id) {
        return jdbcTemplate.query(FIND_BY_ID_SQL, (resultSet, rowNumber) -> {
            Generation generation = new Generation();
            generation.setId(resultSet.getLong("id"));
            generation.setStatus(resultSet.getString("status"));
            generation.setSourceUrl(resultSet.getString("source_url"));
            generation.setUrlTitle(resultSet.getString("url_title"));
            generation.setUrlDescription(resultSet.getString("url_description"));
            generation.setOriginalFileName(resultSet.getString("original_file_name"));
            generation.setStoredFileName(resultSet.getString("stored_file_name"));
            generation.setImagePath(resultSet.getString("image_path"));
            generation.setImageContentType(resultSet.getString("image_content_type"));
            generation.setImageSize(resultSet.getObject("image_size", Long.class));
            generation.setImageAnalysis(resultSet.getString("image_analysis"));
            generation.setTitle(resultSet.getString("title"));
            generation.setContent(resultSet.getString("content"));
            generation.setTags(resultSet.getString("tags"));
            generation.setErrorMessage(resultSet.getString("error_message"));
            generation.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
            generation.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
            return generation;
        }, id).stream().findFirst();
    }

    public int updateUrlContent(
            Long id,
            String urlTitle,
            String urlDescription,
            LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                UPDATE_URL_CONTENT_SQL,
                urlTitle,
                urlDescription,
                Timestamp.valueOf(updatedAt),
                id);
    }

    public int updateImageInfo(
            Long id,
            String originalFileName,
            String storedFileName,
            String imagePath,
            String imageContentType,
            long imageSize,
            LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                UPDATE_IMAGE_INFO_SQL,
                originalFileName,
                storedFileName,
                imagePath,
                imageContentType,
                imageSize,
                Timestamp.valueOf(updatedAt),
                id);
    }

    public int updateGenerationResult(
            Long id,
            String imageAnalysis,
            String title,
            String content,
            String tags,
            String status,
            LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                UPDATE_GENERATION_RESULT_SQL,
                imageAnalysis,
                title,
                content,
                tags,
                status,
                Timestamp.valueOf(updatedAt),
                id);
    }

    public int markFailed(Long id, String errorMessage, LocalDateTime updatedAt) {
        return jdbcTemplate.update(
                MARK_FAILED_SQL,
                errorMessage,
                Timestamp.valueOf(updatedAt),
                id);
    }
}
