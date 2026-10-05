package com.gym.management.integration.hardware;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gms.face-attendance")
@Getter
@Setter
public class FaceMatchProperties {
    private double matchThreshold = 0.45;
}
