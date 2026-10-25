package com.gym.management.repository;

import com.gym.management.entity.StaffAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, UUID> {

    Optional<StaffAttendance> findByStaffIdAndDate(UUID staffId, LocalDate date);

    List<StaffAttendance> findByStaffIdOrderByDateDesc(UUID staffId);

    @Query("SELECT sa FROM StaffAttendance sa WHERE sa.date = :date")
    List<StaffAttendance> findAllByDate(@Param("date") LocalDate date);
}
