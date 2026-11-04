package com.gym.management.config;

import com.gym.management.security.CurrentUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Cấu hình Swagger UI / OpenAPI (Agent 6, Issue 6.5): mô tả chung của API và thứ tự các nhóm tag theo
 * module. Mô tả từng nhóm lấy từ @Tag trên controller, mô tả từng endpoint lấy từ @Operation.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Thứ tự hiển thị các nhóm trong Swagger UI, theo luồng sử dụng. Tên phải trùng @Tag(name) trên
     * controller. Nhóm chưa có trong danh sách (module mới) được xếp sau cùng, theo thứ tự chữ cái.
     */
    public static final List<String> TAG_ORDER = List.of(
            "Auth",
            "Profile",
            "Packages",
            "Membership",
            "Payments",
            "Lịch làm việc PT",
            "Booking (đặt lịch PT)",
            "Check-in",
            "Face Profiles",
            "Face Attendance",
            "Staff Attendance",
            "Products",
            "POS",
            "Commissions",
            "TrainerPayRates",
            "Leads/CRM",
            "Reports");

    static {
        // Ẩn khỏi Swagger UI mọi tham số đánh dấu @CurrentUser (ở BẤT KỲ controller
        // nào, kể cả của Agent 2-6 sau này) — vì giá trị này luôn tự lấy từ JWT,
        // không phải input người dùng nhập tay. Thiếu dòng này Swagger sẽ hiện nhầm
        // thành 1 field bắt buộc, gây hiểu lầm là phải tự điền UUID.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    public OpenAPI gmsOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Gym Management System (GMS) API")
                        .description(DESCRIPTION)
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    /** Sắp các nhóm tag theo TAG_ORDER; nhóm lạ xếp sau cùng theo chữ cái. */
    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            if (openApi.getTags() == null) {
                return;
            }
            List<io.swagger.v3.oas.models.tags.Tag> sorted = new ArrayList<>(openApi.getTags());
            sorted.sort(Comparator
                    .comparingInt((io.swagger.v3.oas.models.tags.Tag t) -> orderOf(t.getName()))
                    .thenComparing(io.swagger.v3.oas.models.tags.Tag::getName,
                            Comparator.nullsLast(Comparator.naturalOrder())));
            openApi.setTags(sorted);
        };
    }

    private static int orderOf(String name) {
        int index = TAG_ORDER.indexOf(name);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static final String DESCRIPTION = """
            Backend RESTful API cho hệ thống quản lý phòng gym.

            **Xác thực.** Gọi `POST /api/v1/auth/login` để lấy `accessToken` (hết hạn sau 15 phút), bấm nút \
            **Authorize** rồi dán token (không cần gõ chữ Bearer). Khi token hết hạn, dùng `refreshToken` \
            (hiệu lực 7 ngày) gọi `POST /api/v1/auth/refresh`.

            **Vai trò.** SUPER_ADMIN, RECEPTIONIST, TRAINER, SALES, MEMBER. Quyền của từng endpoint ghi trong \
            mô tả; gọi sai quyền trả 403, chưa đăng nhập trả 401.

            **Định dạng phản hồi.** Mọi phản hồi có dạng `{ success, message, data, errorCode, timestamp }`. \
            Khi lỗi, `success = false` và `errorCode` là mã lỗi nghiệp vụ (ví dụ `VALIDATION_ERROR`, \
            `LEAD_NOT_FOUND`).

            **Ngày giờ.** Mọi ngày giờ là giờ Việt Nam (Asia/Ho_Chi_Minh), không kèm múi giờ. Tiền tệ là VND.
            """;
}
