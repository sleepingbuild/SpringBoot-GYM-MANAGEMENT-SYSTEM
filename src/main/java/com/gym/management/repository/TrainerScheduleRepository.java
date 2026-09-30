package com.gym.management.repository;

import com.gym.management.entity.TrainerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TrainerScheduleRepository extends JpaRepository<TrainerSchedule, UUID> {

    List<TrainerSchedule> findByTrainerIdAndWorkDate(UUID trainerId, LocalDate workDate);

    List<TrainerSchedule> findByTrainerIdAndWorkDateAndActiveTrue(UUID trainerId, LocalDate workDate);

    List<TrainerSchedule> findByTrainerIdAndWorkDateBetween(UUID trainerId, LocalDate from, LocalDate to);

    /**
     * Lấy các ca CÙNG 1 PT bị trùng giờ trong CÙNG 1 ngày (không cấm trùng giờ giữa 2 PT khác
     * nhau — chỉ cấm trùng của cùng 1 PT). Dùng khi tạo/sửa ca để cảnh báo/hiển thị lane,
     * KHÔNG dùng để chặn cứng (REQUIREMENTS.md mục 3: xử lý bằng thuật toán "lane" khi hiển thị).
     */
    List<TrainerSchedule> findByTrainerIdAndWorkDateAndIdNot(UUID trainerId, LocalDate workDate, UUID excludeId);
}
