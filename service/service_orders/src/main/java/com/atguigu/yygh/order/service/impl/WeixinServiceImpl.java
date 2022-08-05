package com.atguigu.yygh.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.enums.PaymentStatusEnum;
import com.atguigu.yygh.enums.PaymentTypeEnum;
import com.atguigu.yygh.enums.RefundStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.model.order.RefundInfo;
import com.atguigu.yygh.order.service.OrderService;
import com.atguigu.yygh.order.service.PaymentService;
import com.atguigu.yygh.order.service.RefundInfoService;
import com.atguigu.yygh.order.service.WeixinService;
import com.atguigu.yygh.order.utils.ConstantPropertiesUtils;
import com.atguigu.yygh.order.utils.HttpClient;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WeixinServiceImpl implements WeixinService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundInfoService refundInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    //下单 生成二维码
    @Override
    public Map<String, Object> createNative(Long orderId) {
        try {
            //1.查询订单信息
            OrderInfo orderInfo = orderService.getById(orderId);
            if (orderInfo == null) {
                throw new YyghException(20001,"订单信息有误");
            }
            //2.生成交易记录
            paymentService.savePaymentInfo(orderInfo, PaymentTypeEnum.WEIXIN.getStatus());
            //3.封装调用微信接口参数
            Map<String,String> paramMap = new HashMap<>();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            String body = orderInfo.getReserveDate() + "就诊"+ orderInfo.getDepname();
            paramMap.put("body", body);
            paramMap.put("out_trade_no", orderInfo.getOutTradeNo());
            //paramMap.put("total_fee", order.getAmount().multiply(new BigDecimal("100")).longValue()+"");
            paramMap.put("total_fee", "1");//为了测试
            paramMap.put("spbill_create_ip", "127.0.0.1");
            paramMap.put("notify_url", "http://guli.shop/api/order/weixinPay/weixinNotify");
            paramMap.put("trade_type", "NATIVE");
            //4.创建客户端对象（设置url）
            HttpClient client = new HttpClient(
                    "https://api.mch.weixin.qq.com/pay/unifiedorder");
            //5.设置参数（map=>xml）
            client.setXmlParam(WXPayUtil.generateSignedXml(
                    paramMap,ConstantPropertiesUtils.PARTNERKEY
            ));
            //6.客户端发送请求
            client.setHttps(true);
            client.post();
            //7.获取响应转化响应类型（xml=>map）
            String xml = client.getContent();
            System.out.println("xml = " + xml);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            //8.封装返回结果
            Map map = new HashMap();
            map.put("orderId", orderId);
            map.put("totalFee", orderInfo.getAmount());
            map.put("resultCode", resultMap.get("result_code"));
            map.put("codeUrl", resultMap.get("code_url"));
         /*   if(null != resultMap.get("result_code")) {
                //微信支付二维码2小时过期，可采取2小时未支付取消订单
                redisTemplate.opsForValue().set(orderId.toString(), map, 1000, TimeUnit.MINUTES);
            }*/
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

//    根据参数查询交易状态
    @Override
    public Map<String, String> queryPayStatus(Long orderId, Integer paymentType) {
        try {
            //1.查询订单信息
            OrderInfo orderInfo = orderService.getById(orderId);
            if (orderInfo == null) {
                throw new YyghException(20001,"订单信息有误");
            }
            //2.封装调接口参数
            Map<String,String> paramMap = new HashMap<>();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paramMap.put("out_trade_no", orderInfo.getOutTradeNo());
            //3.创建客户端对象（设置url）
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
            //4.设置参数（map=>xml）
            client.setXmlParam(WXPayUtil.generateSignedXml(
                    paramMap,ConstantPropertiesUtils.PARTNERKEY));
            //5.客户端发送请求
            client.setHttps(true);
            client.post();
            //6.获取响应转化响应类型（xml=>map）
            String xml = client.getContent();
            System.out.println("xml = " + xml);
            Map<String, String> map = WXPayUtil.xmlToMap(xml);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    ////退款
    @Override
    public Boolean refund(Long orderId) {
        try {
            //1.根据参数查询交易记录
            PaymentInfo paymentInfo = paymentService.getPaymentInfo(orderId, PaymentTypeEnum.WEIXIN.getStatus());
            if (paymentInfo == null) {
                throw new YyghException(20001,"未找到交易记录");
            }
            //2.根据交易记录添加退款记录，确认退款状态
            RefundInfo refundInfo = refundInfoService.saveRefundInfo(paymentInfo);
            if (refundInfo.getRefundStatus() == RefundStatusEnum.REFUND.getStatus()) {
                return true;
            }
            //3.封装调用接口参数
            Map<String,String> paramMap = new HashMap<>();
            paramMap.put("appid",ConstantPropertiesUtils.APPID);       //公众账号ID
            paramMap.put("mch_id",ConstantPropertiesUtils.PARTNER);   //商户编号
            paramMap.put("nonce_str",WXPayUtil.generateNonceStr());
            paramMap.put("transaction_id",paymentInfo.getTradeNo()); //微信订单号
            paramMap.put("out_trade_no",paymentInfo.getOutTradeNo()); //商户订单编号
            paramMap.put("out_refund_no","tk"+paymentInfo.getOutTradeNo()); //商户退款单号
            //       paramMap.put("total_fee",paymentInfoQuery.getTotalAmount().multiply(new BigDecimal("100")).longValue()+"");
            //       paramMap.put("refund_fee",paymentInfoQuery.getTotalAmount().multiply(new BigDecimal("100")).longValue()+"");
            paramMap.put("total_fee","1");
            paramMap.put("refund_fee","1");

            //4.创建客户端对象 （设置url）
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/secapi/pay/refund");
            //5.设置参数（map=>xml）开启读取证书开关
            client.setXmlParam(WXPayUtil.generateSignedXml(
                    paramMap,ConstantPropertiesUtils.PARTNERKEY
            ));
            client.setHttps(true);
            client.setCert(true);
            client.setCertPassword(ConstantPropertiesUtils.PARTNER);
            //6.客户端发送请求
            client.post();
            //7.获取响应转化类型（xml=>map）
            String xml = client.getContent();
            System.out.println("refund xml = " + xml);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            //8.如果退款成功更新退款记录状态信息
            if (null != resultMap &&
                    WXPayConstants.SUCCESS.equalsIgnoreCase(resultMap.get("result_code"))) {
                refundInfo.setCallbackTime(new Date());
                refundInfo.setOutTradeNo(resultMap.get("refund_id"));
                refundInfo.setRefundStatus(RefundStatusEnum.REFUND.getStatus());
                refundInfo.setCallbackContent(JSONObject.toJSONString(resultMap));
                refundInfoService.updateById(refundInfo);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
