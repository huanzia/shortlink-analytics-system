package com.huanzi.shortlinksystem.service;

import com.huanzi.shortlinksystem.dto.LoginDTO;
import com.huanzi.shortlinksystem.vo.LoginVO;

public interface UserService {

    LoginVO login(LoginDTO loginDTO);
}
