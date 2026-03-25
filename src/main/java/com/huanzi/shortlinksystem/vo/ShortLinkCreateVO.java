package com.huanzi.shortlinksystem.vo;

import lombok.Data;

@Data
/**
 * 短链创建结果返回对象。
 * 只返回创建后最关键信息，便于前端或测试方立即拿到 shortCode / shortUrl 做后续验证。
 */
public class ShortLinkCreateVO {

    /** 新创建短链的主键ID。 */
    private Long id;
    /** 系统生成的短码。 */
    private String shortCode;
    /** 可直接访问的完整短链地址。 */
    private String shortUrl;
    /** 创建时传入的标题。 */
    private String title;
    /** 创建时绑定的原始长链接。 */
    private String originUrl;
}
