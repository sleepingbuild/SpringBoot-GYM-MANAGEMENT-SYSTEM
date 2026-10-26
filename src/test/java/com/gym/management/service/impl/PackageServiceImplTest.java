package com.gym.management.service.impl;

import com.gym.management.dto.response.PackageResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.entity.Package;
import com.gym.management.exception.BusinessException;
import com.gym.management.repository.PackageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Story G3 — xem gói tập công khai: danh sách và chi tiết chỉ lộ gói đang mở bán,
 * còn đường dành cho Super Admin vẫn xem được gói đã ngừng bán.
 */
@ExtendWith(MockitoExtension.class)
class PackageServiceImplTest {

    @Mock
    private PackageRepository packageRepository;

    @InjectMocks
    private PackageServiceImpl packageService;

    private Package pkg(String name, boolean active) {
        Package p = new Package();
        p.setName(name);
        p.setPrice(new BigDecimal("500000"));
        p.setDurationDays(30);
        p.setPackageType("TIME_BASED");
        p.setPeakType("FULL_TIME");
        p.setActive(active);
        return p;
    }

    @Test
    @DisplayName("listActive: chỉ truy vấn gói đang mở bán, không đụng findAll")
    void listActive_queriesOnlyActivePackages() {
        Pageable pageable = PageRequest.of(0, 20);
        when(packageRepository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(pkg("Gói 1 tháng", true)), pageable, 1));

        PageResponse<PackageResponse> result = packageService.listActive(pageable);

        assertThat(result).isNotNull();
        verify(packageRepository).findByActiveTrue(pageable);
        verify(packageRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("listAll: dành cho Super Admin, truy vấn toàn bộ kể cả gói đã ngừng bán")
    void listAll_queriesAllPackages() {
        Pageable pageable = PageRequest.of(0, 20);
        when(packageRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(pkg("Gói cũ", false)), pageable, 1));

        PageResponse<PackageResponse> result = packageService.listAll(pageable);

        assertThat(result).isNotNull();
        verify(packageRepository).findAll(pageable);
        verify(packageRepository, never()).findByActiveTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("getActiveById: gói đang mở bán thì trả về đúng thông tin")
    void getActiveById_activePackage_returnsResponse() {
        UUID id = UUID.randomUUID();
        when(packageRepository.findById(id)).thenReturn(Optional.of(pkg("Gói 1 tháng", true)));

        PackageResponse response = packageService.getActiveById(id);

        assertThat(response.getName()).isEqualTo("Gói 1 tháng");
        assertThat(response.getPrice()).isEqualByComparingTo("500000");
        assertThat(response.getPackageType()).isEqualTo("TIME_BASED");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("getActiveById: gói đã ngừng bán coi như không tồn tại")
    void getActiveById_inactivePackage_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(packageRepository.findById(id)).thenReturn(Optional.of(pkg("Gói cũ", false)));

        assertThatThrownBy(() -> packageService.getActiveById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getActiveById: id không tồn tại thì báo không tìm thấy")
    void getActiveById_missingPackage_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(packageRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packageService.getActiveById(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getById (đường Super Admin): vẫn xem được gói đã ngừng bán")
    void getById_inactivePackage_stillReturnedForAdmin() {
        UUID id = UUID.randomUUID();
        when(packageRepository.findById(id)).thenReturn(Optional.of(pkg("Gói cũ", false)));

        PackageResponse response = packageService.getById(id);

        assertThat(response.getName()).isEqualTo("Gói cũ");
        assertThat(response.isActive()).isFalse();
    }
}
