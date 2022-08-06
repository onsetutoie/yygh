package com.atguigu.yygh.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.atguigu.yygh.common.R;
import com.atguigu.yygh.common.Result;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Api(tags = "订单接口")
@RestController
@RequestMapping("/api/order/orderInfo")
public class OrderApiController {

    @Autowired
    private OrderService orderService;

    //********************  Sentinel start  ******************************

    //构造方法
    public OrderApiController(){
        initRule();
    }
    /**
     * 导入热点值限流规则
     * 也可在Sentinel dashboard界面配置（仅测试）
     */
    public void initRule(){
        ParamFlowRule pRule = new ParamFlowRule("submitOrder")//资源名称，与SentinelResource值保持一致
                .setParamIdx(0) // 限流第一个参数
                .setCount(5);//单机阈值

        // 针对 热点参数值单独设置限流 QPS 阈值，而不是全局的阈值.
        //如：1000（北京协和医院）,可以通过数据库表一次性导入，目前为测试
        ParamFlowItem item = new ParamFlowItem().setObject("200040878")//热点值
                .setClassType(String.class.getName())//热点值类型
                .setCount(1);//热点值 QPS 阈值

        List<ParamFlowItem> list = new ArrayList<>();
        list.add(item);
        pRule.setParamFlowItemList(list);
        ParamFlowRuleManager.loadRules(Collections.singletonList(pRule));

    }

    //热点值超过 QPS 阈值，返回结果
    public R submitOrderBlockHandler(String scheduleId, Long patientId,
                                          BlockException e) {
        return R.error().message("系统业务繁忙，请稍后下单");
    }

    //********************  Sentinel end  ******************************


    @ApiOperation(value = "创建订单")
    @PostMapping("auth/submitOrder/{scheduleId}/{patientId}")
    @SentinelResource(value = "submitOrder",blockHandler = "submitOrderBlockHandler") // Sentinel 注解
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
