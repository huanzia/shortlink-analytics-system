package com.huanzi.shortlinksystem.controller;

import com.huanzi.shortlinksystem.common.result.Result;
import com.huanzi.shortlinksystem.dto.LoginDTO;
import com.huanzi.shortlinksystem.service.UserService;
import com.huanzi.shortlinksystem.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 处理用户认证相关入口。
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 支持用户名或手机号登录。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }
}
