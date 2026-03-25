package com.huanzi.shortlinksystem.enums;

import lombok.Getter;

@Getter
public enum MessageConsumeStatusEnum {

    WAITING(0, "waiting"),
    SUCCESS(1, "success"),
    FAILED(2, "failed");

    private final Integer code;
    private final String desc;

    MessageConsumeStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
