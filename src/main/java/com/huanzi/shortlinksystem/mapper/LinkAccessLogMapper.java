package com.huanzi.shortlinksystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huanzi.shortlinksystem.entity.LinkAccessLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LinkAccessLogMapper extends BaseMapper<LinkAccessLog> {
}
