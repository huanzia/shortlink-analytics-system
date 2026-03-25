package com.huanzi.shortlinksystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tb_link_uv_record")
/**
 * UV 去重记录实体。
 * 当前项目主链路里的 UV 去重先使用 Redis Set，这张表主要作为后续持久化去重方案的预留结构。
 */
public class LinkUvRecord {

    /** UV 记录主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的短链ID。 */
    private Long linkId;
    /** 被访问的短码。 */
    private String shortCode;
    /** 访客标识，同一短链同一天同一 visitorId 只应出现一次。 */
    private String visitorId;
    /** 访问日期，通常作为按天去重维度的一部分。 */
    private LocalDate visitDate;
    /** 记录创建时间。 */
    private LocalDateTime createTime;
}
