package com.huanzi.shortlinksystem.dto;

import lombok.Data;

@Data
/**
 * 登录入参对象。
 * 当前支持两种方式：
 * - username + password
 * - phone + password
 */
public class LoginDTO {

    /** 用户名登录时使用的账号。 */
    private String username;
    /** 手机号登录时使用的账号。 */
    private String phone;
    /** 登录密码，当前基础版做明文校验。 */
    private String password;
}
