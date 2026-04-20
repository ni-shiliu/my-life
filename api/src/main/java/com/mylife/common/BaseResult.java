package com.mylife.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResult<T> {

    private String code;
    private String message;
    private T data;

    public static <T> BaseResult<T> success(T data) {
        return new BaseResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> BaseResult<T> fail(String code, String message) {
        return new BaseResult<>(code, message, null);
    }
}
