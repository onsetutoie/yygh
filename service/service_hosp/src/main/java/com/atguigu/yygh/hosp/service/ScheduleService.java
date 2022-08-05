package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
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

    //根据排班id获取排班详情
    Schedule findScheduleById(String id);

    //根据排班id获取预约下单数据
    ScheduleOrderVo getScheduleOrderVo(String scheduleId);

    //修改排班
    void update(Schedule schedule);

    //根据医院编码、医院排班id获取排班详情
    Schedule getScheduleByIds(String hoscode,String hosScheduleId);
}
