package com.gym.management.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Giữ cấu hình bắt buộc cho SQL Server: gửi tham số giờ đúng kiểu TIME. Nếu bỏ dòng này, mọi truy vấn so sánh
 * cột TIME với tham số LocalTime (đặt lịch PT, kiểm tra ca làm việc) sẽ lỗi 500 trên SQL Server thật,
 * mà test dùng database giả lập không phát hiện được.
 */
class SqlServerTimeConfigTest {

    @Test
    void datasourceSendsTimeParametersAsTime() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties props = yaml.getObject();

        assertEquals("false",
                String.valueOf(props.get("spring.datasource.hikari.data-source-properties.sendTimeAsDatetime")),
                "Thiếu spring.datasource.hikari.data-source-properties.sendTimeAsDatetime=false trong application.yml");
    }
}
