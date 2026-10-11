package com.gym.management.repository;

import com.gym.management.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * UUID "không loại trừ booking nào" — dùng thay cho null ở tham số {@code excludeId}.
     * Lý do: điều kiện kiểu {@code (:excludeId is null or b.id <> :excludeId)} với tham số UUID null
     * dễ bị Hibernate bind thành varbinary trên SQL Server và báo lỗi chuyển kiểu sang
     * uniqueidentifier. Truyền sentinel này thì điều kiện {@code b.id <> :excludeId} luôn đúng.
     */
    UUID NO_EXCLUDE = new UUID(0L, 0L);

    List<Booking> findByMemberIdOrderByBookingDateDescStartTimeDesc(UUID memberId);

    List<Booking> findByTrainerIdOrderByBookingDateDescStartTimeDesc(UUID trainerId);

    List<Booking> findByBranchIdAndBookingDate(UUID branchId, LocalDate bookingDate);

    /**
     * Booking của PT đang chiếm chỗ và CHỒNG KHOẢNG GIỜ với [startTime, endTime) — "1 người/khung
     * giờ/PT". Chồng khoảng: b.start < endTime và b.end > startTime (10:00-11:00 và 10:30-11:30
     * là trùng). Status nhả slot: CANCELLED, PT_NO_SHOW, LATE_CANCELLED.
     */
    @Query("select b from Booking b where b.trainer.id = :trainerId and b.bookingDate = :bookingDate " +
            "and b.startTime < :endTime and b.endTime > :startTime " +
            "and b.status not in (com.gym.management.entity.BookingStatus.CANCELLED, " +
            "com.gym.management.entity.BookingStatus.PT_NO_SHOW, " +
            "com.gym.management.entity.BookingStatus.LATE_CANCELLED) " +
            "and b.id <> :excludeId")
    List<Booking> findActiveByTrainerSlot(@Param("trainerId") UUID trainerId,
                                           @Param("bookingDate") LocalDate bookingDate,
                                           @Param("startTime") LocalTime startTime,
                                           @Param("endTime") LocalTime endTime,
                                           @Param("excludeId") UUID excludeId);

    /** Booking của member chồng khoảng giờ với [startTime, endTime) — chặn đặt 2 PT cùng lúc. */
    @Query("select b from Booking b where b.member.id = :memberId and b.bookingDate = :bookingDate " +
            "and b.startTime < :endTime and b.endTime > :startTime " +
            "and b.status not in (com.gym.management.entity.BookingStatus.CANCELLED, " +
            "com.gym.management.entity.BookingStatus.PT_NO_SHOW, " +
            "com.gym.management.entity.BookingStatus.LATE_CANCELLED) " +
            "and b.id <> :excludeId")
    List<Booking> findActiveByMemberSlot(@Param("memberId") UUID memberId,
                                          @Param("bookingDate") LocalDate bookingDate,
                                          @Param("startTime") LocalTime startTime,
                                          @Param("endTime") LocalTime endTime,
                                          @Param("excludeId") UUID excludeId);

    /**
     * Đếm số buổi trong tuần (Thứ 2 -> Chủ nhật) để check max_sessions_per_week. Loại CANCELLED và
     * PT_NO_SHOW (lỗi của PT, học viên không mất buổi). LATE_CANCELLED và NO_SHOW vẫn tính vì
     * học viên đã mất buổi đó.
     */
    @Query("select count(b) from Booking b where b.member.id = :memberId " +
            "and b.bookingDate between :weekStart and :weekEnd " +
            "and b.status not in (com.gym.management.entity.BookingStatus.CANCELLED, " +
            "com.gym.management.entity.BookingStatus.PT_NO_SHOW) " +
            "and b.id <> :excludeId")
    long countActiveInWeek(@Param("memberId") UUID memberId,
                            @Param("weekStart") LocalDate weekStart,
                            @Param("weekEnd") LocalDate weekEnd,
                            @Param("excludeId") UUID excludeId);

    /**
     * Booking hôm nay của member chờ điểm danh: học viên chưa check-in và booking đã CONFIRMED
     * (không check-in được vào PENDING). Sort theo giờ bắt đầu, service chọn cái đầu.
     */
    @Query("select b from Booking b where b.member.id = :memberId and b.bookingDate = :today " +
            "and b.checkInTime is null " +
            "and b.status = com.gym.management.entity.BookingStatus.CONFIRMED " +
            "order by b.startTime asc")
    List<Booking> findTodayPendingCheckIn(@Param("memberId") UUID memberId, @Param("today") LocalDate today);

    /** Booking hôm nay mà học viên ĐÃ check-in nhưng CHƯA check-out (lượt quét thứ 2 = check-out). */
    @Query("select b from Booking b where b.member.id = :memberId and b.bookingDate = :today " +
            "and b.checkInTime is not null and b.checkOutTime is null " +
            "order by b.checkInTime desc")
    List<Booking> findTodayPendingCheckOut(@Param("memberId") UUID memberId, @Param("today") LocalDate today);

    /** Booking CONFIRMED hôm nay của 1 PT, sort theo giờ bắt đầu — dùng cho PT quét mặt. */
    @Query("select b from Booking b where b.trainer.id = :trainerId and b.bookingDate = :today " +
            "and b.status = com.gym.management.entity.BookingStatus.CONFIRMED " +
            "order by b.startTime asc")
    List<Booking> findTodayConfirmedByTrainer(@Param("trainerId") UUID trainerId, @Param("today") LocalDate today);

    Optional<Booking> findByIdAndMemberId(UUID id, UUID memberId);

    Optional<Booking> findByIdAndTrainerId(UUID id, UUID trainerId);
}
