package com.huanzi.shortlinksystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huanzi.shortlinksystem.entity.ShortLink;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLink> {
}
