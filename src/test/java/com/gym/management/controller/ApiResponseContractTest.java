package com.gym.management.controller;

import com.gym.management.exception.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent 6 (Issue 6.5) - Mọi endpoint phải trả {@code ApiResponse<T>} với T cụ thể (dùng ApiResponse&lt;Void&gt;
 * khi không có dữ liệu). {@code ApiResponse<?>} khiến Swagger không hiện được cấu trúc của "data".
 * Ngoại lệ duy nhất: phương thức trả void phải khai báo rõ {@code @ResponseStatus(NO_CONTENT)}.
 */
class ApiResponseContractTest {

    private static List<Method> handlers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Method> methods = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("com.gym.management.controller")) {
            for (Method m : Class.forName(bd.getBeanClassName()).getDeclaredMethods()) {
                if (AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class)) {
                    methods.add(m);
                }
            }
        }
        return methods;
    }

    @Test
    void everyEndpointReturnsApiResponseWithConcreteType() throws Exception {
        List<Method> handlers = handlers();
        assertTrue(handlers.size() >= 50, "Quét được quá ít endpoint: " + handlers.size());

        List<String> violations = new ArrayList<>();
        for (Method m : handlers) {
            String where = m.getDeclaringClass().getSimpleName() + "#" + m.getName();
            if (m.getReturnType() == void.class) {
                ResponseStatus status = m.getAnnotation(ResponseStatus.class);
                if (status == null || status.value() != HttpStatus.NO_CONTENT) {
                    violations.add(where + ": trả void nhưng không có @ResponseStatus(NO_CONTENT)");
                }
                continue;
            }
            if (m.getReturnType() != ApiResponse.class) {
                violations.add(where + ": không trả ApiResponse (" + m.getReturnType().getSimpleName() + ")");
                continue;
            }
            Type generic = m.getGenericReturnType();
            if (!(generic instanceof ParameterizedType parameterized)) {
                violations.add(where + ": ApiResponse không có kiểu tham số");
                continue;
            }
            Type arg = parameterized.getActualTypeArguments()[0];
            if (arg instanceof WildcardType) {
                violations.add(where + ": ApiResponse<?> cần đổi thành kiểu cụ thể (hoặc ApiResponse<Void>)");
            }
        }
        assertEquals(List.of(), violations);
    }
}
