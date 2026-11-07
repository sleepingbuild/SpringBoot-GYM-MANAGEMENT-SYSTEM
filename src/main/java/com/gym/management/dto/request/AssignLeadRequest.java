package com.gym.management.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Agent 6 - Giao lead cho một Sales. salesId để trống (null) nghĩa là bỏ giao, lead về trạng thái chưa gán. */
@Getter
@Setter
public class AssignLeadRequest {

    private UUID salesId;
}
