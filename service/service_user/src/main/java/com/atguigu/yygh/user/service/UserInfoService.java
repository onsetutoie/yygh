package com.atguigu.yygh.user.service;

import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {
    //用户登录
    Map<String, Object> login(LoginVo loginVo);

    //用户认证提交接口
    void userAuth(Long userId, UserAuthVo userAuthVo);

    //根据用户id获取用户信息
    UserInfo getUserInfo(Long userId);
}
