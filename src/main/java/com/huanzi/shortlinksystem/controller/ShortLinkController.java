package com.huanzi.shortlinksystem.controller;

import com.huanzi.shortlinksystem.common.result.Result;
import com.huanzi.shortlinksystem.dto.ShortLinkCreateDTO;
import com.huanzi.shortlinksystem.dto.ShortLinkStatusUpdateDTO;
import com.huanzi.shortlinksystem.dto.ShortLinkUpdateDTO;
import com.huanzi.shortlinksystem.service.ShortLinkService;
import com.huanzi.shortlinksystem.vo.PageResultVO;
import com.huanzi.shortlinksystem.vo.ShortLinkCreateVO;
import com.huanzi.shortlinksystem.vo.ShortLinkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链管理接口。
 * 这一层只负责接收请求和返回统一结果，真正的管理规则都在 service 中：
 * 例如固定 userId=1、软删除、状态更新后的缓存清理等。
 */
@RestController
@RequestMapping("/link")
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    /**
     * 创建短链。
     */
    @PostMapping("/create")
    public Result<ShortLinkCreateVO> create(@RequestBody ShortLinkCreateDTO createDTO) {
        return Result.success(shortLinkService.createShortLink(createDTO));
    }

    /**
     * 查询当前用户的短链列表。
     * 当前已支持最基础分页，便于演示管理侧“我的短链”能力。
     */
    @GetMapping("/my")
    public Result<PageResultVO<ShortLinkVO>> myLinks(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(shortLinkService.listMyLinks(pageNum, pageSize));
    }

    /**
     * 编辑短链基础信息。
     * 本轮只开放 title、description、expireTime，避免修改 originUrl 带来更多一致性问题。
     */
    @PutMapping("/{id}")
    public Result<ShortLinkVO> update(@PathVariable Long id, @RequestBody ShortLinkUpdateDTO updateDTO) {
        return Result.success(shortLinkService.updateLink(id, updateDTO));
    }

    /**
     * 启用或禁用短链。
     * 短链状态变更后，service 层会同步处理缓存失效和布隆过滤器增量维护。
     */
    @PutMapping("/{id}/status")
    public Result<ShortLinkVO> updateStatus(@PathVariable Long id,
                                            @RequestBody ShortLinkStatusUpdateDTO statusUpdateDTO) {
        return Result.success(shortLinkService.updateLinkStatus(id, statusUpdateDTO));
    }

    /**
     * 软删除短链，删除后不再允许正常跳转。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        shortLinkService.deleteLink(id);
        return Result.success();
    }

    /**
     * 根据主键查询短链详情。
     * 当前只允许查看固定 userId=1 且未删除的短链。
     */
    @GetMapping("/{id}")
    public Result<ShortLinkVO> detail(@PathVariable Long id) {
        return Result.success(shortLinkService.getLinkDetail(id));
    }
}
