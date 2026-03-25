package com.huanzi.shortlinksystem.constant;

/**
 * Web 层固定常量。
 * 当前主要围绕 visitorId Cookie，用于基础版 UV 去重。
 */
public final class WebConstants {

    // 浏览器里保存 visitorId 的 Cookie 名称。
    public static final String VISITOR_ID_COOKIE_NAME = "sl_vid";
    // 先给一年有效期，保证同一浏览器在较长时间内能稳定携带 visitorId。
    public static final int VISITOR_ID_COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

    private WebConstants() {
    }
}
