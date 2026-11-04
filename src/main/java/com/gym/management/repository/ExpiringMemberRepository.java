package com.gym.management.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Agent 6 - Truy vấn hội viên sắp hết hạn, CHỈ ĐỌC. SQL thuần để không phụ thuộc repository của
 * module Membership (Agent 2). Ngày là giá trị lưu trong DB, coi như Asia/Ho_Chi_Minh.
 */
@Repository
@RequiredArgsConstructor
public class ExpiringMemberRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public record Row(UUID userId, String fullName, String phone, String email, String packageName,
                      LocalDate endDate, Integer remainingSessions) {
    }

    /**
     * Gói ACTIVE có end_date trong [from, to] của hội viên còn hoạt động, bỏ những hội viên đã có
     * gói ACTIVE hoặc SCHEDULED khác kết thúc muộn hơn (đã gia hạn rồi). Sắp theo ngày hết hạn tăng dần.
     */
    public List<Row> findExpiringBetween(LocalDate from, LocalDate to) {
        String sql = """
                SELECT u.id AS user_id, u.full_name, u.phone, u.email,
                       p.name AS package_name, mp.end_date, mp.remaining_sessions
                FROM member_packages mp
                JOIN users u ON u.id = mp.user_id
                JOIN packages p ON p.id = mp.package_id
                WHERE mp.status = 'ACTIVE'
                  AND u.status = 'ACTIVE'
                  AND mp.end_date >= :from
                  AND mp.end_date <= :to
                  AND NOT EXISTS (
                      SELECT 1 FROM member_packages n
                      WHERE n.user_id = mp.user_id
                        AND n.id <> mp.id
                        AND n.status IN ('ACTIVE', 'SCHEDULED')
                        AND (n.end_date IS NULL OR n.end_date > mp.end_date))
                ORDER BY mp.end_date ASC, u.full_name ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Date.valueOf(from))
                .addValue("to", Date.valueOf(to));
        return jdbc.query(sql, params, (rs, i) -> {
            int sessions = rs.getInt("remaining_sessions");
            Integer remaining = rs.wasNull() ? null : sessions;
            return new Row(
                    UUID.fromString(rs.getString("user_id")),
                    rs.getString("full_name"),
                    rs.getString("phone"),
                    rs.getString("email"),
                    rs.getString("package_name"),
                    rs.getDate("end_date").toLocalDate(),
                    remaining);
        });
    }
}
