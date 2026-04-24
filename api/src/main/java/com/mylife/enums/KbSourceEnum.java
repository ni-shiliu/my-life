package com.mylife.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KbSourceEnum {

    BAILIAN("BAILIAN");

    @EnumValue
    private final String value;
}
