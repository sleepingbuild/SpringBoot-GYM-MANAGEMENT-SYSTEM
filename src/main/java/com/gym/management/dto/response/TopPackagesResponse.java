package com.gym.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Agent 6 - Gói bán chạy theo số lượt đăng ký đã thanh toán thành công. */
public record TopPackagesResponse(LocalDate from, LocalDate to, List<Item> items) {

    public record Item(UUID packageId, String packageName, long registrations, BigDecimal revenue) {
    }
}
