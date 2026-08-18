package com.example.xhscopywriting.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.xhscopywriting.model.AdminGenerationSummary;
import com.example.xhscopywriting.model.User;

@Repository
public class AdminRepository {

    private static final String TODAY_RANGE = """
            created_at >= CURRENT_DATE
            AND created_at < CURRENT_DATE + INTERVAL 1 DAY
            """;

    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countUsers() {
        return count("SELECT COUNT(*) FROM users");
    }

    public long countGenerations() {
        return count("SELECT COUNT(*) FROM generations");
    }

    public long countTodayGenerations() {
        return count("SELECT COUNT(*) FROM generations WHERE " + TODAY_RANGE);
    }

    public long countTodayActiveUsers() {
        return count("""
                SELECT COUNT(DISTINCT user_id)
                FROM generations
                WHERE user_id IS NOT NULL
                """ + " AND " + TODAY_RANGE);
    }

    public List<User> findAllUsers() {
        return jdbcTemplate.query("""
                SELECT id, username, role, created_at
                FROM users
                ORDER BY created_at DESC, id DESC
                """, (resultSet, rowNumber) -> {
            User user = new User();
            user.setId(resultSet.getLong("id"));
            user.setUsername(resultSet.getString("username"));
            user.setRole(resultSet.getString("role"));
            user.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
            return user;
        });
    }

    public List<AdminGenerationSummary> findAllGenerations() {
        return jdbcTemplate.query("""
                SELECT
                    g.id,
                    u.username,
                    g.status,
                    g.original_file_name,
                    g.image_content_type,
                    g.image_size,
                    g.image_path,
                    g.title,
                    g.created_at
                FROM generations g
                LEFT JOIN users u ON u.id = g.user_id
                ORDER BY g.created_at DESC, g.id DESC
                """, (resultSet, rowNumber) -> new AdminGenerationSummary(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("status"),
                resultSet.getString("original_file_name"),
                resultSet.getString("image_content_type"),
                resultSet.getObject("image_size", Long.class),
                resultSet.getString("image_path") != null
                        && !resultSet.getString("image_path").isBlank(),
                resultSet.getString("title"),
                resultSet.getTimestamp("created_at").toLocalDateTime()));
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
