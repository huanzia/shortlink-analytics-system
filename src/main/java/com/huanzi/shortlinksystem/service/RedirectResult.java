package com.huanzi.shortlinksystem.service;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 封装短链跳转结果，避免 controller 关心埋点链路内部细节。
 */
@Data
@AllArgsConstructor
public class RedirectResult {

    private String originUrl;
    private String visitorId;
}
