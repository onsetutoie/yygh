package com.atguigu.yygh.order.controller;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.common.util.AuthContextHolder;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.order.service.OrderService;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Api(tags = "订单接口")
@RestController
@RequestMapping("/api/order/orderInfo")
public class OrderApiController {

    @Autowired
    private OrderService orderService;

    @ApiOperation(value = "创建订单")
    @PostMapping("auth/submitOrder/{scheduleId}/{patientId}")
    public R submitOrder(@PathVariable String scheduleId, @PathVariable Long patientId) {
        Long orderId = orderService.saveOrder(scheduleId, patientId);
        return R.ok().data("orderId",orderId);
    }

    @ApiOperation(value = "带条件带分页查询订单列表")
    @GetMapping("auth/{page}/{limit}")
    public R list(@PathVariable Long page, @PathVariable Long limit,
                  OrderQueryVo orderQueryVo, HttpServletRequest request){
        //获取id存入
        Long userId = AuthContextHolder.getUserId(request);
        orderQueryVo.setUserId(userId);
        //封装分页参数
        Page<OrderInfo> pageParams = new Page<>(page,limit);

        IPage<OrderInfo> pageModel = orderService.selectPage(pageParams,orderQueryVo);
        return R.ok().data("pageModel",pageModel);

    }

    @ApiOperation(value = "获取订单状态")
    @GetMapping("auth/getStatusList")
    public R getStatusList() {
        return R.ok().data("statusList", OrderStatusEnum.getStatusList());
    }

    @ApiOperation(value = "根据订单id查询订单详情")
    @GetMapping("auth/getOrders/{orderId}")
    public R getOrders(@PathVariable Long orderId) {
        OrderInfo orderInfo = orderService.getOrderById(orderId);
        return R.ok().data("orderInfo",orderInfo);
    }

    @ApiOperation(value = "取消预约")
    @GetMapping("auth/cancelOrder/{orderId}")
    public R cancelOrder(@PathVariable("orderId") Long orderId) {
        Boolean flag = orderService.cancelOrder(orderId);
        return R.ok().data("flag",flag);
    }

    @ApiOperation(value = "获取订单统计数据")
    @PostMapping("inner/getCountMap")
    public Map<String, Object> getCountMap(@RequestBody OrderCountQueryVo orderCountQueryVo) {
        return orderService.getCountMap(orderCountQueryVo);
    }
}
