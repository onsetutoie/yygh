package com.atguigu.yygh.hosp.receiver;

import com.atguigu.yygh.common.service.MqConst;
import com.atguigu.yygh.common.service.RabbitService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.msm.MsmVo;
import com.atguigu.yygh.vo.order.OrderMqVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;
import org.springframework.util.StringUtils;

import java.util.Date;


@Component
public class HospitalReceiver {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private RabbitService rabbitService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ORDER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_ORDER),
            key = {MqConst.ROUTING_ORDER}
    ))
    public void recevier(OrderMqVo orderMqVo, Message message, Channel channel) {
        //1取出参数
        String hoscode = orderMqVo.getHoscode();
        String hosScheduleId = orderMqVo.getScheduleId();
        Integer reservedNumber = orderMqVo.getReservedNumber();
        Integer availableNumber = orderMqVo.getAvailableNumber();
        MsmVo msmVo = orderMqVo.getMsmVo();
        //2根据参数查询排班信息
        Schedule schedule = scheduleService.getScheduleByIds(hoscode, hosScheduleId);
        //2.5判断是创建订单、取消预约
        if (StringUtils.isEmpty(availableNumber)) {
            //取消预约，更新号源
            availableNumber = schedule.getAvailableNumber().intValue() + 1;
            schedule.setAvailableNumber(availableNumber);
        }else {
           //创建订单，更新号源
            schedule.setReservedNumber(reservedNumber);
            schedule.setAvailableNumber(availableNumber);
        }
        //3更新排班信息
        schedule.setUpdateTime(new Date());
        scheduleService.update(schedule);
        //4发送短信相关MQ消息
        if(msmVo!=null){
            rabbitService.sendMessage(
                    MqConst.EXCHANGE_DIRECT_MSM,MqConst.ROUTING_MSM_ITEM,msmVo);
        }

    }


}
