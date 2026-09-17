package com.gym.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agent 3 - Check-in Engine & Access Control.
 * Ảnh khuôn mặt do Admin/Lễ tân đăng ký trước cho hội viên/PT (upload 1 lần),
 * dùng để đối chiếu khi quét cam lúc check-in.
 *
 * Giai đoạn MOCK: so khớp bằng imageHash (SHA-256 byte-for-byte) — xem
 * {@link com.gym.management.integration.hardware.MockFaceMatcher}.
 * Khi thay bằng nhận diện khuôn mặt thật (Cloud API / model local), thêm cột
 * face_embedding (vector) vào bảng này qua migration mới, KHÔNG sửa lại V3.
 */
@Entity
@Table(name = "face_profiles")
@Getter
@Setter
@NoArgsConstructor
public class FaceProfile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Lob
    @Column(nullable = false)
    private byte[] image;

    @Column(name = "image_hash", nullable = false, length = 64)
    private String imageHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;
}
