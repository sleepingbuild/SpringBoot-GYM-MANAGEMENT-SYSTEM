package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

/**
 * Thống kê cá nhân của hội viên (GET /members/me/stats). Cửa sổ thời gian là 30 ngày gần nhất,
 * gồm cả hôm nay: [from, to] với to = hôm nay theo Clock của dự án.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberStatsResponse {

    /** Đầu cửa sổ 30 ngày (gồm cả ngày này). */
    private LocalDate from;
    /** Cuối cửa sổ, bằng hôm nay. */
    private LocalDate to;

    /** Có gói ACTIVE hay không. Khi false, các trường gói bên dưới là null. */
    private boolean hasActiveMembership;
    private String packageName;
    private LocalDate membershipEndDate;
    /** Số buổi còn lại của gói; null = gói không giới hạn buổi hoặc chưa có gói. */
    private Integer remainingSessions;
    /** Số buổi đang giữ chỗ: booking PENDING và CONFIRMED (chưa trừ vào gói). */
    private int reservedSessions;
    /** remainingSessions trừ reservedSessions, tối thiểu 0; null khi remainingSessions là null. */
    private Integer availableSessions;

    /** Tổng số buổi đã hoàn thành (COMPLETED) từ trước tới nay. */
    private int completedSessionsTotal;
    /** Số buổi COMPLETED có ngày đặt trong cửa sổ 30 ngày. */
    private int completedSessionsLast30Days;
    /** Số lượt vào phòng tập trong 30 ngày: lượt tự do (gym_check_ins) cộng buổi có PT đã quét vào. */
    private int gymVisitsLast30Days;
    /** Số booking theo trạng thái (ngày đặt trong 30 ngày). Có đủ mọi trạng thái, trạng thái không có là 0. */
    private Map<String, Integer> bookingsByStatusLast30Days;
}
