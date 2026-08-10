package com.example.xhscopywriting.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
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
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT
                id,
                status,
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
            statement.setString(2, generation.getOriginalFileName());
            statement.setString(3, generation.getStoredFileName());
            statement.setString(4, generation.getImagePath());
            statement.setString(5, generation.getImageContentType());
            statement.setObject(6, generation.getImageSize(), Types.BIGINT);
            statement.setString(7, generation.getImageAnalysis());
            statement.setString(8, generation.getTitle());
            statement.setString(9, generation.getContent());
            statement.setString(10, generation.getTags());
            statement.setString(11, generation.getErrorMessage());
            statement.setTimestamp(12, Timestamp.valueOf(generation.getCreatedAt()));
            statement.setTimestamp(13, Timestamp.valueOf(generation.getUpdatedAt()));
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
}
