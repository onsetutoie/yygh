package com.atguigu.yygh.user.controller;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "用户管理接口")
@RestController
@RequestMapping("/admin/user")
public class UserController {

    @Autowired
    private UserInfoService userInfoService;

    @ApiOperation(value = "带条件带分页查询用户列表")
    @GetMapping("{page}/{limit}")
    public R list(@PathVariable Long page,
                  @PathVariable Long limit,
                  UserInfoQueryVo userInfoQueryVo){
        Page<UserInfo> pageParams = new Page<>(page, limit);
        Page<UserInfo> pageModel = userInfoService.selectPage(pageParams,userInfoQueryVo);
        return R.ok().data("pageModel",pageModel);
    }

    @ApiOperation(value = "锁定")
    @GetMapping("lock/{userId}/{status}")
    public R lock(
            @PathVariable("userId") Long userId,
            @PathVariable("status") Integer status){
        userInfoService.lock(userId,status);
        return R.ok();
    }

    @ApiOperation(value = "用户详情")
    @GetMapping("show/{userId}")
    public R show(@PathVariable Long userId) {
        Map<String,Object> map = userInfoService.show(userId);
        return R.ok().data(map);
    }

    @ApiOperation(value = "认证审批")
    @GetMapping("approval/{userId}/{authStatus}")
    public R approval(@PathVariable Long userId,@PathVariable Integer authStatus) {
        userInfoService.approval(userId,authStatus);
        return R.ok();
    }



}
