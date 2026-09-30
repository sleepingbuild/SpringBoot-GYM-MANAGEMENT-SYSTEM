package com.gym.management.repository;

import com.gym.management.entity.Booking;
import com.gym.management.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByMemberIdOrderByBookingDateDescStartTimeDesc(UUID memberId);

    List<Booking> findByTrainerIdOrderByBookingDateDescStartTimeDesc(UUID trainerId);

    List<Booking> findByBranchIdAndBookingDate(UUID branchId, LocalDate bookingDate);

    /** Kiểm tra "1 người/khung giờ/PT" — slot của 1 PT tại đúng ngày+giờ bắt đầu đã có ai giữ chưa. */
    @Query("select b from Booking b where b.trainer.id = :trainerId and b.bookingDate = :bookingDate " +
            "and b.startTime = :startTime and b.status <> com.gym.management.entity.BookingStatus.CANCELLED " +
            "and (:excludeId is null or b.id <> :excludeId)")
    List<Booking> findActiveByTrainerSlot(@Param("trainerId") UUID trainerId,
                                           @Param("bookingDate") LocalDate bookingDate,
                                           @Param("startTime") LocalTime startTime,
                                           @Param("excludeId") UUID excludeId);

    /** Kiểm tra member có bị trùng giờ với 1 PT KHÁC tại cùng ngày+giờ hay không. */
    @Query("select b from Booking b where b.member.id = :memberId and b.bookingDate = :bookingDate " +
            "and b.startTime = :startTime and b.status <> com.gym.management.entity.BookingStatus.CANCELLED " +
            "and (:excludeId is null or b.id <> :excludeId)")
    List<Booking> findActiveByMemberSlot(@Param("memberId") UUID memberId,
                                          @Param("bookingDate") LocalDate bookingDate,
                                          @Param("startTime") LocalTime startTime,
                                          @Param("excludeId") UUID excludeId);

    /** Đếm số buổi trong tuần (Thứ 2 -> Chủ nhật) của member để check max_sessions_per_week. */
    @Query("select count(b) from Booking b where b.member.id = :memberId " +
            "and b.bookingDate between :weekStart and :weekEnd " +
            "and b.status <> com.gym.management.entity.BookingStatus.CANCELLED " +
            "and (:excludeId is null or b.id <> :excludeId)")
    long countActiveInWeek(@Param("memberId") UUID memberId,
                            @Param("weekStart") LocalDate weekStart,
                            @Param("weekEnd") LocalDate weekEnd,
                            @Param("excludeId") UUID excludeId);

    /**
     * Tìm booking hôm nay của member đang chờ điểm danh (member chưa check-in), dùng cho
     * luồng "lần quét đầu = check-in" của Agent 4 (self/kiosk face attendance).
     * Ưu tiên buổi gần giờ hiện tại nhất — sắp theo start_time tăng dần, service layer chọn.
     */
    @Query("select b from Booking b where b.member.id = :memberId and b.bookingDate = :today " +
            "and b.checkInTime is null " +
            "and b.status in (com.gym.management.entity.BookingStatus.PENDING, com.gym.management.entity.BookingStatus.CONFIRMED) " +
            "order by b.startTime asc")
    List<Booking> findTodayPendingCheckIn(@Param("memberId") UUID memberId, @Param("today") LocalDate today);

    /**
     * Tìm booking hôm nay của member ĐÃ check-in nhưng CHƯA check-out, dùng cho lượt quét
     * thứ 2 trong ngày = check-out.
     */
    @Query("select b from Booking b where b.member.id = :memberId and b.bookingDate = :today " +
            "and b.checkInTime is not null and b.checkOutTime is null " +
            "order by b.checkInTime desc")
    List<Booking> findTodayPendingCheckOut(@Param("memberId") UUID memberId, @Param("today") LocalDate today);

    Optional<Booking> findByIdAndMemberId(UUID id, UUID memberId);

    Optional<Booking> findByIdAndTrainerId(UUID id, UUID trainerId);
}
