package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface UserService {
    /**
     * 登录接口
     */
    User userLogin(UserLoginDTO userLoginDTO);

}
