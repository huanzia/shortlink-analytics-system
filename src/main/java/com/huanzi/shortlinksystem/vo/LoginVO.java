package com.huanzi.shortlinksystem.vo;

import lombok.Data;

@Data
/**
 * 登录成功返回对象。
 * 当前只返回基础用户信息，不返回 token 或复杂会话数据。
 */
public class LoginVO {

    /** 当前登录用户ID。 */
    private Long userId;
    /** 用户名。 */
    private String username;
    /** 昵称。 */
    private String nickname;
    /** 手机号。 */
    private String phone;
}
