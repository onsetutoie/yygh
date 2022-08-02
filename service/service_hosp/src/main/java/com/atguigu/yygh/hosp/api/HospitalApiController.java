package com.atguigu.yygh.hosp.api;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "医院显示接口")
@RestController
@RequestMapping("/api/hosp/hospital")
public class HospitalApiController {
    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    @ApiOperation(value = "获取分页列表")
    @GetMapping("{page}/{limit}")
    public R index(@PathVariable Integer page, @PathVariable Integer limit,
            HospitalQueryVo hospitalQueryVo) {
        Page<Hospital> pageModel = hospitalService.selectPage(page,limit,hospitalQueryVo);
        return R.ok().data("pageModel",pageModel);
    }

    @ApiOperation(value = "根据医院名称获取医院列表")
    @GetMapping("findByHosname/{hosname}")
    public R findByHosname(@PathVariable String hosname) {
        List<Hospital> list = hospitalService.findByHosnameLike(hosname);
        return R.ok().data("list",list);
    }

    @ApiOperation(value = "根据医院编码查询所有科室信息")
    @GetMapping("department/{hoscode}")
    public R getDepartment(@PathVariable("hoscode") String hoscode){
        List<DepartmentVo> list = departmentService.findDeptTree(hoscode);
        return R.ok().data("list",list);
    }

    @ApiOperation(value = "医院预约挂号详情")
    @GetMapping("{hoscode}")
    public R getHospInfo(@PathVariable("hoscode") String hoscode){
        Map<String,Object> map = hospitalService.getHospByHoscode(hoscode);
        return R.ok().data(map);
    }

    @ApiOperation(value = "获取可预约排班数据")
    @GetMapping("auth/getBookingScheduleRule/{page}/{limit}/{hoscode}/{depcode}")
    public R getBookingSchedule(@PathVariable Integer page ,@PathVariable Integer limit,
                                @PathVariable String hoscode,@PathVariable String depcode){
        Map<String,Object> map =
                scheduleService.getBookingSchedule(page,limit,hoscode,depcode);
        return R.ok().data(map);
    }

    @ApiOperation(value = "获取排班数据")
    @GetMapping("auth/findScheduleList/{hoscode}/{depcode}/{workDate}")
    public R findScheduleList( @PathVariable String hoscode,@PathVariable String depcode,
                               @PathVariable String workDate){
        List<Schedule> scheduleList =
                scheduleService.getScheduleDetail(hoscode, depcode, workDate);
        return R.ok().data("scheduleList",scheduleList);
    }

    @ApiOperation(value = "根据排班id获取排班详情")
    @GetMapping("getSchedule/{id}")
    public R findScheduleById(@PathVariable String id ){
        Schedule schedule = scheduleService.findScheduleById(id);
        return R.ok().data("schedule",schedule);
    }

    @ApiOperation(value = "根据排班id获取预约下单数据")
    @GetMapping("inner/getScheduleOrderVo/{scheduleId}")
    public ScheduleOrderVo getScheduleOrderVo(@PathVariable String scheduleId){
        ScheduleOrderVo scheduleOrderVo =
                scheduleService.getScheduleOrderVo(scheduleId);
        return scheduleOrderVo;
    }

}
