package com.gym.management.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FaceDescriptorRequest {

    @NotEmpty
    @Size(min = 128, max = 128, message = "Descriptor phải có đúng 128 chiều")
    private List<Double> descriptor;
}
