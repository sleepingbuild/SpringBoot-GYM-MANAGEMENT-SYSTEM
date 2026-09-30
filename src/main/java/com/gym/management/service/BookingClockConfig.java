package com.gym.management.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Cung cấp bean `Clock` cố định timezone Asia/Ho_Chi_Minh, dùng thay cho gọi trực tiếp
 * `LocalDateTime.now()` trong `BookingServiceImpl`/`TrainerScheduleServiceImpl` — mục đích
 * DUY NHẤT là để unit test giả lập thời gian bằng `Clock.fixed(...)` cho lazy auto-status
 * (Agent1/Phi yêu cầu ở "Việc còn thiếu" mục 6).
 *
 * ⚠️ Đặt tạm trong package `service/` (không phải `config/`) vì `config/` thuộc sở hữu
 * Agent 1 (ARCHITECTURE.md mục 2). Nếu Agent 1 đã có sẵn 1 bean `Clock` khác ở nơi khác
 * (trùng tên `clock`), Spring sẽ báo lỗi "duplicate bean definition" khi merge — chỉ cần XOÁ
 * FILE NÀY và dùng bean có sẵn, không cần sửa gì ở BookingServiceImpl/TrainerScheduleServiceImpl
 * (cả 2 chỉ inject theo type `Clock`, không quan tâm bean định nghĩa ở đâu).
 */
@Configuration
public class BookingClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
