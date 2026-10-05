package com.gym.management.service.impl;

import com.gym.management.dto.request.PackageRequest;
import com.gym.management.dto.response.PackageResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.entity.Package;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.PackageRepository;
import com.gym.management.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageServiceImpl implements PackageService {

    private final PackageRepository packageRepository;

    @Override
    @Transactional
    public PackageResponse create(PackageRequest request) {
        Package pkg = new Package();
        applyRequest(pkg, request);
        return toResponse(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public PackageResponse update(UUID id, PackageRequest request) {
        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PACKAGE_NOT_FOUND));
        applyRequest(pkg, request);
        return toResponse(packageRepository.save(pkg));
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PACKAGE_NOT_FOUND));
        pkg.setActive(false);
        packageRepository.save(pkg);
    }

    @Override
    public PackageResponse getById(UUID id) {
        return toResponse(packageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PACKAGE_NOT_FOUND)));
    }

    @Override
    public List<PackageResponse> listActive() {
        return packageRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<PackageResponse> listActive(Pageable pageable) {
        return PageResponse.from(packageRepository.findByActiveTrue(pageable), this::toResponse);
    }

    @Override
    public PageResponse<PackageResponse> listAll(Pageable pageable) {
        return PageResponse.from(packageRepository.findAll(pageable), this::toResponse);
    }

    private void applyRequest(Package pkg, PackageRequest request) {
        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        pkg.setPrice(request.getPrice());
        pkg.setDurationDays(request.getDurationDays());
        pkg.setSessionCount(request.getSessionCount());
        pkg.setPackageType(request.getPackageType());
        pkg.setPeakType(request.getPeakType());
        pkg.setMaxSessionsPerWeek(request.getMaxSessionsPerWeek());
        if (request.getActive() != null) {
            pkg.setActive(request.getActive());
        }
    }

    private PackageResponse toResponse(Package pkg) {
        return PackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .price(pkg.getPrice())
                .durationDays(pkg.getDurationDays())
                .sessionCount(pkg.getSessionCount())
                .packageType(pkg.getPackageType())
                .peakType(pkg.getPeakType())
                .maxSessionsPerWeek(pkg.getMaxSessionsPerWeek())
                .active(pkg.isActive())
                .build();
    }
}
