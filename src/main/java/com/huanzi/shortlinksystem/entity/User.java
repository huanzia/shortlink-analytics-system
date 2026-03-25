package com.huanzi.shortlinksystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_user")
/**
 * 用户表实体。
 * 当前项目的登录和管理能力还是基础版，但短链归属、登录返回信息都依赖这张表。
 */
public class User {

    /** 用户主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 登录用户名，可用于 username + password 登录。 */
    private String username;
    /** 手机号，可用于 phone + password 登录。 */
    private String phone;
    /** 登录密码，当前基础版直接做明文比较。 */
    private String password;
    /** 用户昵称，用于登录成功后展示。 */
    private String nickname;
    /** 用户状态，当前基础语义为 1正常、0禁用。 */
    private Integer status;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 最后更新时间。 */
    private LocalDateTime updateTime;
}
