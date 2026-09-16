package com.gym.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * GMS - Gym Management System
 * Entry point của toàn bộ backend RESTful API.
 *
 * @EnableScheduling bật cho các job định kỳ dùng chung
 * (vd: tự động EXPIRED member_packages - Agent 2,
 *      nhắc gia hạn qua Zalo ZNS/SMS - Agent 6).
 */
@SpringBootApplication
@EnableScheduling
public class GymManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(GymManagementApplication.class, args);
    }
}
