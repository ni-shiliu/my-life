package com.mylife.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum YesNoEnum {

    YES("Y"),
    NO("N");

    @EnumValue
    private final String value;
}
