package com.huanzi.shortlinksystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_mq_message_log")
/**
 * MQ 消息日志实体。
 * 当前项目主流程还没有把它用作正式幂等控制，但表结构已预留，可用于后续消费状态跟踪和补偿。
 */
public class MqMessageLog {

    /** 消息日志主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 消息唯一标识。 */
    private String messageId;
    /** 业务类型，例如访问埋点、统计更新等。 */
    private String bizType;
    /** 业务键，例如短链ID或短码。 */
    private String bizKey;
    /** 消费状态：0未消费，1成功，2失败。 */
    private Integer consumeStatus;
    /** 当前消息已重试次数。 */
    private Integer retryCount;
    /** 消费失败时记录的错误信息。 */
    private String errorMsg;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 最后更新时间。 */
    private LocalDateTime updateTime;
}
