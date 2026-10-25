package com.gym.management.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Agent 6 - Truy vấn báo cáo, CHỈ ĐỌC. Dùng SQL thuần trên bảng (không phụ thuộc entity
 * của module khác). Ngày/giờ là giá trị lưu trong DB, coi như giờ Asia/Ho_Chi_Minh.
 */
@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public record RevenueRow(LocalDate day, String relatedType, BigDecimal total) {
    }

    public record RenewalCounts(long expired, long renewed) {
    }

    public record TrafficRow(LocalDate day, int hour, long count) {
    }

    public record TopPackageRow(UUID packageId, String packageName, long registrations, BigDecimal revenue) {
    }

    /** Doanh thu theo ngày và loại. Ngày ghi nhận = updated_at của giao dịch SUCCESS (fallback created_at). */
    public List<RevenueRow> revenueByDay(LocalDateTime from, LocalDateTime toExclusive) {
        String sql = """
                SELECT CAST(COALESCE(updated_at, created_at) AS DATE) AS day_value,
                       related_type,
                       SUM(amount) AS total
                FROM payment_transactions
                WHERE status = 'SUCCESS'
                  AND COALESCE(updated_at, created_at) >= :from
                  AND COALESCE(updated_at, created_at) < :to
                GROUP BY CAST(COALESCE(updated_at, created_at) AS DATE), related_type
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Timestamp.valueOf(from))
                .addValue("to", Timestamp.valueOf(toExclusive));
        return jdbc.query(sql, params, (rs, i) -> new RevenueRow(
                rs.getDate("day_value").toLocalDate(),
                rs.getString("related_type"),
                rs.getBigDecimal("total")));
    }

    /**
     * Số gói hết hạn trong kỳ và số gói đã gia hạn (cùng user có gói khác, bắt đầu sau gói cũ
     * và không muộn hơn end_date + windowDays). Tính theo end_date vì status chỉ cập nhật lazy.
     * Bọc bằng bảng dẫn xuất vì SQL Server không cho SUM trực tiếp trên biểu thức chứa subquery.
     */
    public RenewalCounts renewalCounts(LocalDate from, LocalDate to, LocalDate today, int windowDays) {
        String sql = """
                SELECT COUNT(*) AS expired_count, COALESCE(SUM(t.renewed), 0) AS renewed_count
                FROM (
                    SELECT CASE WHEN EXISTS (
                               SELECT 1 FROM member_packages m2
                               WHERE m2.user_id = m1.user_id
                                 AND m2.id <> m1.id
                                 AND m2.status IN ('ACTIVE', 'EXPIRED', 'SCHEDULED')
                                 AND m2.start_date > m1.start_date
                                 AND m2.start_date <= DATEADD(DAY, :window, m1.end_date)
                           ) THEN 1 ELSE 0 END AS renewed
                    FROM member_packages m1
                    WHERE m1.end_date IS NOT NULL
                      AND m1.end_date >= :from
                      AND m1.end_date <= :to
                      AND m1.end_date < :today
                      AND m1.status NOT IN ('PENDING', 'CANCELLED', 'SCHEDULED')
                ) t
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("window", windowDays)
                .addValue("from", Date.valueOf(from))
                .addValue("to", Date.valueOf(to))
                .addValue("today", Date.valueOf(today));
        return jdbc.queryForObject(sql, params, (rs, i) ->
                new RenewalCounts(rs.getLong("expired_count"), rs.getLong("renewed_count")));
    }

    /** Lượt học viên quét mặt vào buổi PT, theo ngày và giờ. */
    public List<TrafficRow> checkinTraffic(LocalDateTime from, LocalDateTime toExclusive) {
        String sql = """
                SELECT CAST(check_in_time AS DATE) AS day_value,
                       DATEPART(HOUR, check_in_time) AS hour_value,
                       COUNT(*) AS cnt
                FROM bookings
                WHERE check_in_time IS NOT NULL
                  AND check_in_time >= :from
                  AND check_in_time < :to
                GROUP BY CAST(check_in_time AS DATE), DATEPART(HOUR, check_in_time)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Timestamp.valueOf(from))
                .addValue("to", Timestamp.valueOf(toExclusive));
        return jdbc.query(sql, params, (rs, i) -> new TrafficRow(
                rs.getDate("day_value").toLocalDate(),
                rs.getInt("hour_value"),
                rs.getLong("cnt")));
    }

    /** Gói bán chạy: mỗi giao dịch MEMBERSHIP SUCCESS tính một lượt đăng ký. */
    public List<TopPackageRow> topPackages(LocalDateTime from, LocalDateTime toExclusive, int limit) {
        String sql = """
                SELECT TOP (:limit)
                       p.id AS package_id,
                       p.name AS package_name,
                       COUNT(*) AS registrations,
                       SUM(pt.amount) AS revenue
                FROM payment_transactions pt
                JOIN member_packages mp ON mp.id = pt.related_id
                JOIN packages p ON p.id = mp.package_id
                WHERE pt.related_type = 'MEMBERSHIP'
                  AND pt.status = 'SUCCESS'
                  AND COALESCE(pt.updated_at, pt.created_at) >= :from
                  AND COALESCE(pt.updated_at, pt.created_at) < :to
                GROUP BY p.id, p.name
                ORDER BY COUNT(*) DESC, SUM(pt.amount) DESC, p.name ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("from", Timestamp.valueOf(from))
                .addValue("to", Timestamp.valueOf(toExclusive));
        return jdbc.query(sql, params, (rs, i) -> new TopPackageRow(
                UUID.fromString(rs.getString("package_id")),
                rs.getString("package_name"),
                rs.getLong("registrations"),
                rs.getBigDecimal("revenue")));
    }
}
