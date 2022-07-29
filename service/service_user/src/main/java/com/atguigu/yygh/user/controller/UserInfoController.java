package com.atguigu.yygh.user.controller;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.user.utils.IpUtils;
import com.atguigu.yygh.vo.user.LoginVo;
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
    public R login(@RequestBody LoginVo loginVo, HttpServletRequest request){
       loginVo.setIp(IpUtils.getIpAddr(request));
       Map<String,Object> info = userInfoService.login(loginVo);
       return R.ok().data(info);
   }
}
