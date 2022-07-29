package com.atguigu.yygh.msm.controller;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.msm.service.MsmService;
import com.atguigu.yygh.msm.utils.RandomUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Api(tags = "短信接口")
@RestController
@RequestMapping("/api/msm")
public class MsmController {

    @Autowired
    private MsmService msmService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @ApiOperation(value = "发送验证码短信")
    @GetMapping("send/{phone}")
    public R code(@PathVariable("phone") String phone) {
        //1. 根据手机号查询redis，获取验证码
        String redisCode = redisTemplate.opsForValue().get(phone);
        if (redisCode != null) {
            return R.ok();
        }
        //2.获取新的验证码，封装验证码
        String code = RandomUtil.getFourBitRandom();
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("code", code);
        //3.调用接口发送短信
        boolean isSend = msmService.send(phone, paramMap);
        //4.发送验证码成功后，存入redis，时效1分钟
        if (isSend) {
            redisTemplate.opsForValue().set(phone,code,1, TimeUnit.MINUTES);
            return R.ok();
        }else {
            return R.error().message("发送短信失败");
        }
    }


}
