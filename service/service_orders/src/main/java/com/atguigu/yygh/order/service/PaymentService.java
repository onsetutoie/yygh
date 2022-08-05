package com.atguigu.yygh.order.service;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface PaymentService extends IService<PaymentInfo> {

    /**
     * 保存交易记录
     * @param order
     * @param paymentType 支付类型（1：支付宝 2：微信）
     */
    void savePaymentInfo(OrderInfo order, Integer paymentType);

    //交易成功 更新交易记录
    void paySuccess(String outTradeNo, Integer paymentType, Map<String, String> resultMap);

    /**
     * 获取支付记录
     * @param orderId
     * @param paymentType
     * @return
     */
    PaymentInfo getPaymentInfo(Long orderId, Integer paymentType);
}
