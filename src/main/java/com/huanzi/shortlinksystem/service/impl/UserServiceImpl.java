package com.huanzi.shortlinksystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huanzi.shortlinksystem.common.exception.BizException;
import com.huanzi.shortlinksystem.entity.User;
import com.huanzi.shortlinksystem.dto.LoginDTO;
import com.huanzi.shortlinksystem.mapper.UserMapper;
import com.huanzi.shortlinksystem.service.UserService;
import com.huanzi.shortlinksystem.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户基础能力实现，当前只包含简单登录校验。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        if (!StringUtils.hasText(loginDTO.getPassword())) {
            throw new BizException("password can not be blank");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(loginDTO.getUsername())) {
            queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        } else if (StringUtils.hasText(loginDTO.getPhone())) {
            queryWrapper.eq(User::getPhone, loginDTO.getPhone());
        } else {
            // 当前只允许“用户名+密码”或“手机号+密码”两种登录方式。
            throw new BizException("username or phone can not be blank");
        }

        User user = userMapper.selectOne(queryWrapper);
        // 当前阶段先使用明文密码比对，后续再替换成加密校验。
        if (user == null || !loginDTO.getPassword().equals(user.getPassword())) {
            throw new BizException("username/phone or password is incorrect");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("user is disabled");
        }

        LoginVO loginVO = new LoginVO();
        // 这里只返回登录确认所需基础信息，不生成 token。
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setPhone(user.getPhone());
        return loginVO;
    }
}
