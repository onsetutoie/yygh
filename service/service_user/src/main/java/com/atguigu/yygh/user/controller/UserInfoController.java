package com.atguigu.yygh.user.controller;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.common.util.AuthContextHolder;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.user.utils.IpUtils;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Api(tags = "用户接口")
@RestController
@RequestMapping("/api/user")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    @ApiOperation(value = "用户登录")
    @PostMapping("login")
    public R login(@RequestBody LoginVo loginVo, HttpServletRequest request) {
        loginVo.setIp(IpUtils.getIpAddr(request));
        Map<String, Object> info = userInfoService.login(loginVo);
        return R.ok().data(info);
    }

    @ApiOperation(value = "用户认证提交接口")
    @PostMapping("auth/userAuth")
    public R userAuth(@RequestBody UserAuthVo userAuthVo, HttpServletRequest request) {
        //传递两个参数，第一个参数用户id，第二个参数认证数据vo对象
        Long userId = AuthContextHolder.getUserId(request);
        userInfoService.userAuth(userId, userAuthVo);
        return R.ok();
    }

    @ApiOperation(value = "根据用户id获取用户认证信息")
    @GetMapping("auth/getUserInfo")
    public R getUserInfo(HttpServletRequest request) {
        //1.取出用户id
        Long userId = AuthContextHolder.getUserId(request);
        //2.根据用户id获取用户认证
        UserInfo userInfo = userInfoService.getUserInfo(userId);
        return R.ok().data("userInfo", userInfo);

    }
}
