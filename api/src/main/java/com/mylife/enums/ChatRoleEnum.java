package com.mylife.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatRoleEnum {

    USER("USER"),
    ASSISTANT("ASSISTANT"),
    SYSTEM("SYSTEM"),
    TOOL_RESULT("TOOL_RESULT");

    @EnumValue
    private final String value;
}
