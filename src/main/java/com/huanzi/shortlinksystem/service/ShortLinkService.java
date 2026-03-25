package com.huanzi.shortlinksystem.service;

import com.huanzi.shortlinksystem.dto.ShortLinkCreateDTO;
import com.huanzi.shortlinksystem.dto.ShortLinkStatusUpdateDTO;
import com.huanzi.shortlinksystem.dto.ShortLinkUpdateDTO;
import com.huanzi.shortlinksystem.vo.PageResultVO;
import com.huanzi.shortlinksystem.vo.ShortLinkCreateVO;
import com.huanzi.shortlinksystem.vo.ShortLinkVO;

public interface ShortLinkService {

    /**
     * 创建短链并返回基础创建结果。
     */
    ShortLinkCreateVO createShortLink(ShortLinkCreateDTO createDTO);

    /**
     * 查询当前用户的短链分页列表。
     */
    PageResultVO<ShortLinkVO> listMyLinks(Long pageNum, Long pageSize);

    /**
     * 查询单条短链详情。
     */
    ShortLinkVO getLinkDetail(Long id);

    /**
     * 编辑短链基础信息。
     */
    ShortLinkVO updateLink(Long id, ShortLinkUpdateDTO updateDTO);

    /**
     * 更新短链启用状态。
     */
    ShortLinkVO updateLinkStatus(Long id, ShortLinkStatusUpdateDTO statusUpdateDTO);

    /**
     * 软删除短链。
     */
    void deleteLink(Long id);
}
