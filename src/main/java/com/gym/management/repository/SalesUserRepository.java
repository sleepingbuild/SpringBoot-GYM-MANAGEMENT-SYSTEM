package com.gym.management.repository;

import com.gym.management.dto.response.SalesUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Agent 6 - Tra cứu Sales đang hoạt động, CHỈ ĐỌC. SQL thuần để không phụ thuộc repository của
 * module người dùng (Agent 1).
 */
@Repository
@RequiredArgsConstructor
public class SalesUserRepository {

    private static final String FROM_ACTIVE_SALES = """
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id
            JOIN roles r ON r.id = ur.role_id
            WHERE r.name = 'SALES' AND u.status = 'ACTIVE'
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public List<SalesUserResponse> findActiveSales() {
        String sql = "SELECT u.id, u.full_name, u.phone " + FROM_ACTIVE_SALES + " ORDER BY u.full_name ASC";
        return jdbc.query(sql, (rs, i) -> new SalesUserResponse(
                UUID.fromString(rs.getString("id")), rs.getString("full_name"), rs.getString("phone")));
    }

    public boolean isActiveSales(UUID userId) {
        String sql = "SELECT COUNT(*) " + FROM_ACTIVE_SALES + " AND u.id = :id";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("id", userId.toString()), Integer.class);
        return count != null && count > 0;
    }
}
