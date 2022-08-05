package com.atguigu.yygh.order.service;

import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderService extends IService<OrderInfo> {
    //创建订单
    Long saveOrder(String scheduleId, Long patientId);

    //带条件带分页查询订单列表
    IPage<OrderInfo> selectPage(Page<OrderInfo> pageParams, OrderQueryVo orderQueryVo);

    //根据订单id查询订单详情
    OrderInfo getOrderById(Long orderId);

    //取消预约
    Boolean cancelOrder(Long orderId);

    //获取订单统计数据
    Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo);

    //就诊提醒
    void patientTips();
}
