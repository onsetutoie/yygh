package com.atguigu.yygh.msm.service;

import java.util.Map;

public interface MsmService {

    //发送短信
    boolean send(String phone, Map<String, String> paramMap);
}
