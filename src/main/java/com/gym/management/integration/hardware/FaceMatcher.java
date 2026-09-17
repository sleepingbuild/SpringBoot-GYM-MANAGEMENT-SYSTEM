package com.gym.management.integration.hardware;

import java.util.Optional;
import java.util.UUID;

/**
 * Agent 3 - Check-in Engine & Access Control.
 *
 * Interface trừu tượng cho việc nhận diện khuôn mặt (1:N — "đây là ai trong
 * toàn bộ hội viên đã đăng ký?", KHÁC với xác thực 1:1 "có đúng là user X không?").
 *
 * CheckInService chỉ phụ thuộc interface này, không phụ thuộc trực tiếp
 * {@link MockFaceMatcher} hay bất kỳ SDK cụ thể nào — nhờ vậy khi thay bằng
 * Azure Face API / AWS Rekognition / model local (DeepFace...) sau này, chỉ cần
 * viết thêm 1 implementation mới (vd AzureFaceMatcher) và đổi @Primary bean,
 * KHÔNG cần sửa CheckInService hay bất kỳ controller nào.
 */
public interface FaceMatcher {

    /**
     * Nhận diện khuôn mặt trong ảnh, trả về userId nếu khớp với 1 hồ sơ đã đăng ký.
     *
     * @param imageBytes dữ liệu ảnh chụp từ camera (JPEG/PNG)
     * @return userId nếu tìm được khớp đủ tin cậy, Optional.empty() nếu không nhận diện được
     */
    Optional<UUID> identify(byte[] imageBytes);
}
