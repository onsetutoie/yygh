package com.atguigu.yygh.task.scheduled;

import com.atguigu.yygh.common.service.MqConst;
import com.atguigu.yygh.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@EnableScheduling
public class ScheduledTask {
    @Autowired
    private RabbitService rabbitService;

    //@Scheduled(cron = "0/5 * * * * ?") 七域表达式
    @Scheduled(cron = "0 0 8 * * ?")
    public void task(){
        //System.out.println("定时任务执行---------");
        System.out.println(new Date().toLocaleString());
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_8,"");
    }


}
