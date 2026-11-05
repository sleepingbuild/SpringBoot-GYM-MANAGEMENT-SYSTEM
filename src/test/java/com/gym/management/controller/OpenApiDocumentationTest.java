package com.gym.management.controller;

import com.gym.management.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent 6 (Issue 6.5) - Giữ tiêu chí Swagger cho mọi module: mỗi controller có @Tag, mỗi endpoint có
 * @Operation với summary không rỗng. PR nào thêm endpoint thiếu mô tả sẽ làm test này đỏ.
 */
class OpenApiDocumentationTest {

    private static List<Class<?>> controllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<BeanDefinition> found = scanner.findCandidateComponents("com.gym.management.controller");
        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition bd : found) {
            classes.add(Class.forName(bd.getBeanClassName()));
        }
        return classes;
    }

    @Test
    void everyControllerHasATag() throws Exception {
        List<Class<?>> controllers = controllers();
        assertTrue(controllers.size() >= 10, "Không quét được controller nào, kiểm tra package");
        List<String> missing = new ArrayList<>();
        for (Class<?> c : controllers) {
            Tag tag = c.getAnnotation(Tag.class);
            if (tag == null || tag.name().isBlank()) {
                missing.add(c.getSimpleName());
            }
        }
        assertEquals(List.of(), missing, "Controller thiếu @Tag(name = ...)");
    }

    @Test
    void everyEndpointHasAnOperationSummary() throws Exception {
        List<String> missing = new ArrayList<>();
        int endpoints = 0;
        for (Class<?> c : controllers()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class)) {
                    continue;
                }
                endpoints++;
                Operation op = m.getAnnotation(Operation.class);
                if (op == null || op.summary().isBlank()) {
                    missing.add(c.getSimpleName() + "#" + m.getName());
                }
            }
        }
        assertTrue(endpoints >= 50, "Quét được quá ít endpoint: " + endpoints);
        assertEquals(List.of(), missing, "Endpoint thiếu @Operation(summary = ...)");
    }

    @Test
    void everyTagUsedByAControllerIsInTheModuleOrderList() throws Exception {
        List<String> unknown = new ArrayList<>();
        for (Class<?> c : controllers()) {
            Tag tag = c.getAnnotation(Tag.class);
            if (tag != null && !OpenApiConfig.TAG_ORDER.contains(tag.name())) {
                unknown.add(tag.name());
            }
        }
        assertEquals(List.of(), unknown,
                "Nhóm tag mới chưa có trong OpenApiConfig.TAG_ORDER (thêm vào để Swagger xếp đúng module)");
    }

    @Test
    void tagCustomizerOrdersKnownTagsByModuleAndUnknownLast() {
        OpenAPI api = new OpenAPI().tags(new ArrayList<>(List.of(
                new io.swagger.v3.oas.models.tags.Tag().name("Zeta moi"),
                new io.swagger.v3.oas.models.tags.Tag().name("Reports"),
                new io.swagger.v3.oas.models.tags.Tag().name("Auth"),
                new io.swagger.v3.oas.models.tags.Tag().name("Alpha moi"),
                new io.swagger.v3.oas.models.tags.Tag().name("Leads/CRM"))));

        new OpenApiConfig().tagOrderCustomizer().customise(api);

        List<String> names = api.getTags().stream().map(io.swagger.v3.oas.models.tags.Tag::getName).toList();
        assertEquals(List.of("Auth", "Leads/CRM", "Reports", "Alpha moi", "Zeta moi"), names);
    }
}
