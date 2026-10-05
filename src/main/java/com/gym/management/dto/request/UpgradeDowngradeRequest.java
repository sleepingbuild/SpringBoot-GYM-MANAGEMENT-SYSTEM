package com.gym.management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Dùng cho cả POST /member-packages/{id}/upgrade và /downgrade.
 * {id} trên URL = member_package đang ACTIVE muốn đổi; newPackageId = gói muốn đổi sang.
 */
@Getter
@Setter
public class UpgradeDowngradeRequest {

    @NotNull(message = "newPackageId không được để trống")
    private UUID newPackageId;

    private UUID branchId; // để trống = giữ nguyên branch của gói hiện tại
}
