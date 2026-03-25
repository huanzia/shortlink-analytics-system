package com.huanzi.shortlinksystem.controller;

import com.huanzi.shortlinksystem.common.exception.BizException;
import com.huanzi.shortlinksystem.common.result.Result;
import com.huanzi.shortlinksystem.constant.WebConstants;
import com.huanzi.shortlinksystem.service.RedirectResult;
import com.huanzi.shortlinksystem.service.RedirectService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

/**
 * 短链跳转入口控制器。
 * 这里尽量保持轻量，只做三件事：
 * 1. 从 Cookie 中解析 visitorId，供 UV 去重使用
 * 2. 调用 service 获取跳转目标
 * 3. 把 service 返回结果转换成 HTTP 302 或明确的失败响应
 */
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;

    /**
     * 短链跳转入口。
     * 成功时返回 302 并带上目标地址，失败时直接返回统一错误体，避免前端拿到模糊状态。
     */
    @GetMapping({"/s/{shortCode}", "/r/{shortCode}"})
    public ResponseEntity<?> redirect(@PathVariable String shortCode, HttpServletRequest request, HttpServletResponse response) {
        // visitorId 是当前基础版 UV 方案的访客标识，优先复用已有 Cookie，
        // 没有则生成新的 UUID 并写回响应。
        String visitorId = resolveVisitorId(request);
        writeVisitorIdCookie(response, visitorId);
        try {
            RedirectResult redirectResult = redirectService.getRedirectUrl(shortCode, request, visitorId);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectResult.getOriginUrl()))
                    .build();
        } catch (BizException exception) {
            // 跳转链路里不存在短链属于 404，禁用/过期属于业务不可用，返回 400 更直接。
            HttpStatus status = exception.getCode() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(Result.fail(exception.getCode(), exception.getMessage()));
        }
    }

    /**
     * 解析 visitorId。
     * 这样同一个浏览器后续重复访问时，会带上同一个访客标识，便于 Redis Set 做 UV 去重。
     */
    private String resolveVisitorId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return UUID.randomUUID().toString();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> WebConstants.VISITOR_ID_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    /**
     * 把 visitorId 写回浏览器。
     * 当前只做基础版 Cookie 存储，不接入正式登录态或更复杂的访客追踪方案。
     */
    private void writeVisitorIdCookie(HttpServletResponse response, String visitorId) {
        Cookie cookie = new Cookie(WebConstants.VISITOR_ID_COOKIE_NAME, visitorId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(WebConstants.VISITOR_ID_COOKIE_MAX_AGE);
        response.addCookie(cookie);
    }
}
