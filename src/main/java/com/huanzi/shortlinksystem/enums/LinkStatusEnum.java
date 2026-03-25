package com.huanzi.shortlinksystem.enums;

import lombok.Getter;

@Getter
public enum LinkStatusEnum {

    // 可正常跳转的状态。
    ENABLED(1, "enabled"),
    // 管理侧禁用，不允许继续跳转。
    DISABLED(0, "disabled"),
    // 预留的过期态，当前主要还是通过 expireTime 动态判断。
    EXPIRED(2, "expired"),
    // 软删除状态，对外统一按 not found 处理。
    DELETED(3, "deleted");

    private final Integer code;
    private final String desc;

    LinkStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
