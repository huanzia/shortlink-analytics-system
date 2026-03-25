package com.huanzi.shortlinksystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tb_link_access_log")
/**
 * 短链访问日志实体。
 * 这张表承接跳转成功后的访问埋点，后续的访问记录、趋势统计、维度统计都依赖这里的明细数据。
 */
public class LinkAccessLog {

    /** 访问日志主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的短链ID，对应 tb_short_link.id。 */
    private Long linkId;
    /** 被访问的短码，便于日志排查和按短码查询。 */
    private String shortCode;
    /** 访客标识，来自 Cookie，用于基础版 UV 去重。 */
    private String visitorId;
    /** 访问来源IP。 */
    private String userIp;
    /** 原始 User-Agent 字符串，便于后续做浏览器/设备解析。 */
    private String userAgent;
    /** 来源页面，空值通常会被统计口径归并为 DIRECT。 */
    private String referer;
    /** 设备类型，当前字段已预留，便于后续扩展更细粒度统计。 */
    private String deviceType;
    /** 浏览器名称，基础维度统计会按它聚合。 */
    private String browser;
    /** 操作系统名称，基础维度统计会按它聚合。 */
    private String os;
    /** 精确访问时间，用于访问记录展示。 */
    private LocalDateTime accessTime;
    /** 按天统计日期，用于趋势统计和当日 UV 去重。 */
    private LocalDate visitDate;
}
