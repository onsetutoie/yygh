package com.atguigu.yygh.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.common.service.MqConst;
import com.atguigu.yygh.common.service.RabbitService;
import com.atguigu.yygh.common.util.HttpRequestHelper;
import com.atguigu.yygh.enums.OrderStatusEnum;
import com.atguigu.yygh.enums.PaymentTypeEnum;
import com.atguigu.yygh.hosp.client.HospFeighCient;
import com.atguigu.yygh.model.order.OrderInfo;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.order.mapper.OrderInfoMapper;
import com.atguigu.yygh.order.service.OrderService;
import com.atguigu.yygh.order.service.PaymentService;
import com.atguigu.yygh.order.service.WeixinService;
import com.atguigu.yygh.user.client.PatientFeignClient;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderCountQueryVo;
import com.atguigu.yygh.vo.order.OrderCountVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.atguigu.yygh.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private HospFeighCient hospFeighCient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WeixinService weixinService;


    //创建订单
    @Override
    public Long saveOrder(String scheduleId, Long patientId) {
        //1根据patientId跨模块查询user模块，获取就诊人信息
        Patient patient = patientFeignClient.getPatientOrder(patientId);
        if (patient == null) {
            throw new YyghException(20001, "获取就诊人信息失败");
        }
        //2根据scheduleId跨模块查询hosp模块，获取排班相关信息（排班、科室、医院、预约规则）
        ScheduleOrderVo scheduleOrderVo = hospFeighCient.getScheduleOrderVo(scheduleId);
        if (scheduleOrderVo == null) {
            throw new YyghException(20001, "获取排班相关信息失败");
        }
        //2.5创建订单之前，进行合法性校验
        //确认时间
        if (new DateTime(scheduleOrderVo.getStartTime()).isAfterNow()
                || new DateTime(scheduleOrderVo.getEndTime()).isBeforeNow()) {
            throw new YyghException(20001, "未到挂号时间");
        }
        //确认号源
        if (scheduleOrderVo.getAvailableNumber() <= 0) {
            throw new YyghException(20001, "已挂满");
        }

        //3整合数据，创建订单
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(scheduleOrderVo, orderInfo);
        String outTradeNo = System.currentTimeMillis() + "" + new Random().nextInt(100);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setUserId(patient.getUserId());
        orderInfo.setPatientId(patientId);
        orderInfo.setPatientName(patient.getName());
        orderInfo.setPatientPhone(patient.getPhone());
        orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());
        this.save(orderInfo);

        //4调用医院系统，进行挂号确认
        //4.1封装调用医院接口参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("hoscode", orderInfo.getHoscode());
        paramMap.put("depcode", orderInfo.getDepcode());
        paramMap.put("hosScheduleId", orderInfo.getHosScheduleId());
        paramMap.put("reserveDate", new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd"));
        paramMap.put("reserveTime", orderInfo.getReserveTime());
        paramMap.put("amount", orderInfo.getAmount());
        paramMap.put("name", patient.getName());
        paramMap.put("certificatesType", patient.getCertificatesType());
        paramMap.put("certificatesNo", patient.getCertificatesNo());
        paramMap.put("sex", patient.getSex());
        paramMap.put("birthdate", patient.getBirthdate());
        paramMap.put("phone", patient.getPhone());
        paramMap.put("isMarry", patient.getIsMarry());
        paramMap.put("provinceCode", patient.getProvinceCode());
        paramMap.put("cityCode", patient.getCityCode());
        paramMap.put("districtCode", patient.getDistrictCode());
        paramMap.put("address", patient.getAddress());
        //联系人
        paramMap.put("contactsName", patient.getContactsName());
        paramMap.put("contactsCertificatesType", patient.getContactsCertificatesType());
        paramMap.put("contactsCertificatesNo", patient.getContactsCertificatesNo());
        paramMap.put("contactsPhone", patient.getContactsPhone());
        paramMap.put("timestamp", HttpRequestHelper.getTimestamp());
        //String sign = HttpRequestHelper.getSign(paramMap, signInfoVo.getSignKey());
        paramMap.put("sign", "");
        //4.2通过工具发送请求，获取响应
        JSONObject result =
                HttpRequestHelper.sendRequest(paramMap, "http://localhost:9998/order/submitOrder");
        //4.3判断挂号确认是否成功
        if (result.getInteger("code") == 200) {
            //4.4取出返回结果，更新订单信息
            JSONObject jsonObject = result.getJSONObject("data");
            //预约记录唯一标识（医院预约记录主键）
            String hosRecordId = jsonObject.getString("hosRecordId");
            //预约序号
            Integer number = jsonObject.getInteger("number");
            //取号时间
            String fetchTime = jsonObject.getString("fetchTime");
            //取号地址
            String fetchAddress = jsonObject.getString("fetchAddress");
            //更新订单
            orderInfo.setHosRecordId(hosRecordId);
            orderInfo.setNumber(number);
            orderInfo.setFetchTime(fetchTime);
            orderInfo.setFetchAddress(fetchAddress);
            baseMapper.updateById(orderInfo);
            //排班可预约数
            Integer reservedNumber = jsonObject.getInteger("reservedNumber");
            //排班剩余预约数
            Integer availableNumber = jsonObject.getInteger("availableNumber");
            //5  发送MQ消息进行更新号源、通知就诊人
            //发送mq信息更新号源和短信通知
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setHoscode(orderInfo.getHoscode());
            orderMqVo.setScheduleId(orderInfo.getHosScheduleId());
            orderMqVo.setReservedNumber(reservedNumber);
            orderMqVo.setAvailableNumber(availableNumber);

            //短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            String reserveDate =
                    new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd")
                            + (orderInfo.getReserveTime()==0 ? "上午": "下午");
            Map<String,Object> param = new HashMap<String,Object>(){{
                put("title", orderInfo.getHosname()+"|"+orderInfo.getDepname()+"|"+orderInfo.getTitle());
                put("amount", orderInfo.getAmount());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
                put("quitTime", new DateTime(orderInfo.getQuitTime()).toString("yyyy-MM-dd HH:mm"));
            }};
            msmVo.setParam(param);

            orderMqVo.setMsmVo(msmVo);
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);


        } else {
            throw new YyghException(20001, "确认挂号失败");
        }

        return orderInfo.getId();
    }

    //带条件带分页查询订单列表
    @Override
    public IPage<OrderInfo> selectPage(Page<OrderInfo> pageParams, OrderQueryVo orderQueryVo) {
        //1.取出参数
        //orderQueryVo获取条件值
        Long userId = orderQueryVo.getUserId();
        String name = orderQueryVo.getKeyword(); //医院名称
        Long patientId = orderQueryVo.getPatientId(); //就诊人名称
        String orderStatus = orderQueryVo.getOrderStatus(); //订单状态
        String reserveDate = orderQueryVo.getReserveDate();//安排时间
        String createTimeBegin = orderQueryVo.getCreateTimeBegin();
        String createTimeEnd = orderQueryVo.getCreateTimeEnd();

        //2.验空，拼写筛选条件
        QueryWrapper<OrderInfo> wrapper = new QueryWrapper<>();
        if (StringUtils.isEmpty(userId)){
            wrapper.eq("user_id",userId);
        }
        if(!StringUtils.isEmpty(name)) {
            wrapper.like("hosname",name);
        }
        if(!StringUtils.isEmpty(patientId)) {
            wrapper.eq("patient_id",patientId);
        }
        if(!StringUtils.isEmpty(orderStatus)) {
            wrapper.eq("order_status",orderStatus);
        }
        if(!StringUtils.isEmpty(reserveDate)) {
            wrapper.ge("reserve_date",reserveDate);
        }
        if(!StringUtils.isEmpty(createTimeBegin)) {
            wrapper.ge("create_time",createTimeBegin);
        }
        if(!StringUtils.isEmpty(createTimeEnd)) {
            wrapper.le("create_time",createTimeEnd);
        }
        //3.分页查询
        IPage<OrderInfo> pageModel = baseMapper.selectPage(pageParams, wrapper);
        //4.翻译字段 编号变成对应值封装
        pageModel.getRecords().stream().forEach(item ->{
            this.packOrderInfo(item);
        });
        return pageModel;
    }

    //根据订单id查询订单详情
    @Override
    public OrderInfo getOrderById(Long orderId) {
        OrderInfo orderInfo = this.packOrderInfo(baseMapper.selectById(orderId));
        return orderInfo;
    }

    //取消预约
    @Override
    public Boolean cancelOrder(Long orderId) {
        //1.查询订单信息
        OrderInfo orderInfo = orderService.getById(orderId);
        if (orderInfo == null) {
            throw new YyghException(20001, "订单信息有误");
        }
        //2.判断是否过退号时间
        Date quitTime = orderInfo.getQuitTime();
        DateTime quitDateTime = new DateTime(quitTime);
        if (quitDateTime.isBeforeNow()) {
            throw new YyghException(20001, "已过取消预约截至时间");
        }
        //3.调用医院接口，取消预约
        Map<String,Object> map = new HashMap();
        map.put("hoscode",orderInfo.getHoscode());
        map.put("hosRecordId",orderInfo.getHosRecordId());
        map.put("timestamp",HttpRequestHelper.getTimestamp());
        map.put("sign","");
        JSONObject result
                = HttpRequestHelper.sendRequest(map, "http://localhost:9998/order/updateCancelStatus");
        if (result.getInteger("code") != 200) {
            throw new YyghException(20001, "取消预约失败");
        }else {
            //4.医院取消预约成功，判断是否已支付
            if (orderInfo.getOrderStatus() == OrderStatusEnum.PAID.getStatus()) {
                //5.如果已支付 调用微信退款
                Boolean refund = weixinService.refund(orderId);
                if (!refund) {
                    throw new YyghException();
                }
            }
            //6.更新订单状态
            orderInfo.setOrderStatus(OrderStatusEnum.CANCLE.getStatus());
            this.updateById(orderInfo);
            //7.发送MQ消息，更新号源，通知就诊人
            // 发送mq信息更新预约数 我们与下单成功更新预约数使用相同的mq信息
            // ，不设置可预约数与剩余预约数，接收端可预约数加1即可
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(orderInfo.getHosScheduleId());
            orderMqVo.setHoscode(orderInfo.getHoscode());
            //短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            orderMqVo.setMsmVo(msmVo);
            rabbitService.sendMessage(
                    MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);

        }
        return true;
    }

    //获取订单统计数据
    @Override
    public Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo) {
        Map<String, Object> map = new HashMap<>();

        //查询统计信息
        List<OrderCountVo> orderCountVos = baseMapper.selectOrderCount(orderCountQueryVo);
        //2.分别收集x、y轴数据
        List<String> dateList = orderCountVos.stream()
                .map(OrderCountVo::getReserveDate).collect(Collectors.toList());
        List<Integer> countList = orderCountVos.stream()
                .map(OrderCountVo::getCount).collect(Collectors.toList());
        //3.封装数据返回
        map.put("dateList", dateList);
        map.put("countList", countList);
        return map;
    }

    //就诊提醒
    @Override
    public void patientTips() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("reserve_date",new DateTime().toString("yyyy-MM-dd"));
        List<OrderInfo> orderInfoList = baseMapper.selectList(queryWrapper);
        for(OrderInfo orderInfo : orderInfoList) {
            //短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            String reserveDate = new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd") + (orderInfo.getReserveTime() == 0 ? "上午" : "下午");
            Map<String, Object> param = new HashMap<String, Object>() {{
                put("title", orderInfo.getHosname() + "|" + orderInfo.getDepname() + "|" + orderInfo.getTitle());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
            }};
            msmVo.setParam(param);
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM,MqConst.ROUTING_MSM_ITEM,msmVo);
        }
    }

    //翻译字段 编号变成对应值封装
    private OrderInfo packOrderInfo(OrderInfo orderInfo) {
        orderInfo.getParam().put("orderStatusString", OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus()));
        return orderInfo;
    }
}


