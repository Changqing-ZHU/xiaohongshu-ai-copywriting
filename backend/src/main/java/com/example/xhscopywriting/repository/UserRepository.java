package com.example.xhscopywriting.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.xhscopywriting.model.User;

@Repository
public class UserRepository {

    private static final String INSERT_SQL = """
            INSERT INTO users (username, password, role, created_at)
            VALUES (?, ?, ?, ?)
            """;

    private static final String FIND_BY_USERNAME_SQL = """
            SELECT id, username, password, role, created_at
            FROM users
            WHERE username = ?
            """;

    private static final RowMapper<User> USER_ROW_MAPPER = (resultSet, rowNumber) -> {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setPassword(resultSet.getString("password"));
        user.setRole(resultSet.getString("role"));
        user.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        return user;
    };

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    INSERT_SQL,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getRole());
            statement.setTimestamp(4, Timestamp.valueOf(user.getCreatedAt()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("User insert did not return an id");
        }
        user.setId(key.longValue());
        return user.getId();
    }

    public Optional<User> findByUsername(String username) {
        return jdbcTemplate.query(FIND_BY_USERNAME_SQL, USER_ROW_MAPPER, username)
                .stream()
                .findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username);
        return count != null && count > 0;
    }
}
