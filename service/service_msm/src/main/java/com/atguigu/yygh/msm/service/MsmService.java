package com.atguigu.yygh.msm.service;

import com.atguigu.yygh.vo.msm.MsmVo;

import java.util.Map;

public interface MsmService {

    //发送短信
    boolean send(String phone, Map<String, String> paramMap);

    //发送短信接口
    boolean send(MsmVo msmVo);
}
