package com.huanzi.shortlinksystem.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 定义短链跳转主链路。
 * 接口层只暴露“给我 shortCode，返回是否能跳转以及跳到哪里”，
 * 具体的 BloomFilter、Redis、MySQL、状态校验和埋点发送都收敛在实现类里。
 */
public interface RedirectService {

    /**
     * 根据短码获取可跳转的原始地址。
     * 输入：
     * - shortCode：用户访问的短码
     * - request：当前请求，主要用于提取 IP、UA、referer 等埋点字段
     * - visitorId：访客标识，用于后续 UV 去重
     *
     * 输出：
     * - RedirectResult：包含最终要跳转的 originUrl
     *
     * 异常：
     * - 不存在、删除、禁用、过期等情况统一抛业务异常，由 controller 转换成 HTTP 状态码
     */
    RedirectResult getRedirectUrl(String shortCode, HttpServletRequest request, String visitorId);
}
