package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface ScheduleService {
    //上传排班
    void save(Map<String, Object> paramMap);

    //带条件分页查询排班
    Page<Schedule> selectPage(int page, int limit, ScheduleQueryVo scheduleQueryVo);

    //删除排版
    void remove(String hoscode, String hosScheduleId);
}
