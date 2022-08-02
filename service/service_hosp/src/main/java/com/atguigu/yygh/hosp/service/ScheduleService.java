package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ScheduleService {
    //上传排班
    void save(Map<String, Object> paramMap);

    //带条件分页查询排班
    Page<Schedule> selectPage(int page, int limit, ScheduleQueryVo scheduleQueryVo);

    //删除排版
    void remove(String hoscode, String hosScheduleId);

    //根据医院编号 和 科室编号 ，查询排班规则数据
    Map<String, Object> getRuleSchedule(long page, long limit, String hoscode, String depcode);

    //根据医院编号 、科室编号和工作日期，查询排班详细信息
    List<Schedule> getScheduleDetail(String hoscode, String depcode, String workDate);

    //获取可预约排班数据
    Map<String, Object> getBookingSchedule(Integer page, Integer limit, String hoscode, String depcode);
}
