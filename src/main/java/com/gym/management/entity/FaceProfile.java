package com.gym.management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "face_profiles")
@Getter
@Setter
@NoArgsConstructor
public class FaceProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String descriptor; // JSON array 128 số thực

    // v1: luôn null (quyết định A.4) — server không xử lý/lưu ảnh gốc, chỉ lưu descriptor
    @Column(name = "reference_image")
    private String referenceImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;
}
