package com.huanzi.shortlinksystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_short_link")
/**
 * 短链主表实体。
 * 对应短链管理和跳转主链路的核心数据，既保存短链本身信息，也保存累计 PV/UV 等统计字段。
 */
public class ShortLink {

    /** 短链主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 创建该短链的用户ID，当前项目固定按 userId=1 演示。 */
    private Long userId;
    /** 短码本体，是跳转入口里最核心的业务标识。 */
    private String shortCode;
    /** 完整短链地址，通常由系统域名/端口 + shortCode 组装得到。 */
    private String shortUrl;
    /** 原始长链接，跳转成功后最终重定向到这里。 */
    private String originUrl;
    /** 短链标题，主要用于管理侧展示。 */
    private String title;
    /** 短链描述，便于区分不同用途的链接。 */
    private String description;
    /** 累计访问量 PV，每次有效跳转都会异步累加。 */
    private Long pvCount;
    /** 累计独立访客 UV，基于 visitorId 去重后异步累加。 */
    private Long uvCount;
    /** 状态值：1启用，0禁用，2预留过期态，3删除态。 */
    private Integer status;
    /** 过期时间，为 null 表示不过期；跳转时会参与有效性校验。 */
    private LocalDateTime expireTime;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 最后更新时间，编辑、状态变更、删除时会更新。 */
    private LocalDateTime updateTime;
}
