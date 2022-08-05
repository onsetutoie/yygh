package com.atguigu.yygh.order.service;


import java.util.Map;

public interface WeixinService {
    //下单 生成二维码
    Map<String, Object> createNative(Long orderId);

    //根据参数查询交易状态
    Map<String, String> queryPayStatus(Long orderId, Integer paymentType);

    //退款
    Boolean refund(Long orderId);
}
