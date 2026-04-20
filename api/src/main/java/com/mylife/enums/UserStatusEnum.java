package com.mylife.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    ACTIVE("ACTIVE"),
    DISABLED("DISABLED");

    @EnumValue
    private final String value;
}
