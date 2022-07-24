package com.atguigu.yygh.hosp.api;

import com.atguigu.yygh.common.Result;
import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.common.util.HttpRequestHelper;
import com.atguigu.yygh.common.util.MD5;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Api(tags = "医院管理API接口")
@RestController
@RequestMapping("/api/hosp")
public class ApiController {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private HospitalSetService hospitalSetService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    @ApiOperation(value = "上传医院")
    @PostMapping("saveHospital")
    public Result saveHospital(HttpServletRequest request) {
        //1.获取参数，转化类型
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(parameterMap);
        //2.校验签名
        //2.1从paramMap获取医院签名
        String sign = (String) paramMap.get("sign");
        String hoscode = (String) paramMap.get("hoscode");
        //2.2调用接口获取尚医通医院签名
        String signKey = hospitalSetService.getSignKey(hoscode);
        //2.3签名md5加密
        String signKeyMD5 = MD5.encrypt(signKey);
        System.out.println("signKeyMD5 = " + signKeyMD5);
        System.out.println("sign = " + sign);
        //2.4校验签名
        if (!signKeyMD5.equals(sign)) {
            throw new YyghException(20001, "签名有误");
        }
        //传输过程中“+”转换为了“ ”，因此我们要转换回来
        String logoData = (String) paramMap.get("logoData");
        logoData = logoData.replaceAll(" ", "+");
        paramMap.put("logoData", logoData);

        //3.调用接口数据入库
        hospitalService.save(paramMap);
        return Result.ok();
    }

    @ApiOperation(value = "查询医院")
    @PostMapping("hospital/show")
    public Result getHospital(HttpServletRequest request) {
        //获取参数,转换类型
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        //2.校验签名 省略
        String sign = (String) paramMap.get("sign");
        String hoscode = (String) paramMap.get("hoscode");
        //3.获取参数，校验
        if (StringUtils.isEmpty(hoscode)) {
            throw new YyghException(20001,"医院编码有误");
        }
        //4.调用接口获取数据
        Hospital hospital = hospitalService.getHospital(hoscode);
        return Result.ok(hospital);
    }

    @ApiOperation(value = "上传科室")
    @PostMapping("saveDepartment")
    public Result saveDepartment(HttpServletRequest request) {
        //获取参数,转换类型
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        //2.校验签名 省略
        String sign = (String) paramMap.get("sign");
        String hoscode = (String) paramMap.get("hoscode");

        //3.调用接口
        departmentService.save(paramMap);

        return Result.ok();
    }

    @ApiOperation(value = "带条件分页查询科室")
    @PostMapping("department/list")
    public Result getDepartmentQuery(HttpServletRequest request) {
        //获取参数,转换类型
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        //签名校验省略
        String sign = (String) paramMap.get("sign");
        String hoscode = (String) paramMap.get("hoscode");
        //封装参数
        int page = Integer.parseInt((String)paramMap.get("page"));
        page = StringUtils.isEmpty(page)?1:page;
        int limit =Integer.parseInt((String)paramMap.get("limit"));
        limit = StringUtils.isEmpty(limit)?10:limit;
        DepartmentQueryVo departmentQueryVo = new DepartmentQueryVo();
        departmentQueryVo.setHoscode(hoscode);
        //调用接口 带条件分页查询
        Page<Department> pageInfo =
                departmentService.selectPage(page,limit,departmentQueryVo);

        return Result.ok(pageInfo);
    }

    @ApiOperation(value = "删除科室信息")
    @PostMapping("department/remove")
    public Result deleteDepartment(HttpServletRequest request) {
        //转化数据
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        //检验参数省略
        String hoscode = (String) paramMap.get("hoscode");
        String depcode = (String) paramMap.get("depcode");
        //调用接口
        departmentService.delete(hoscode,depcode);

        return Result.ok();
    }

    @ApiOperation(value = "上传排班")
    @PostMapping("saveSchedule")
    public Result saveSchedule(HttpServletRequest request) {
        //转化参数
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        //获取参数 校验签名省略
        String sign = (String) paramMap.get("sign");
        String hoscode = (String) paramMap.get("hoscode");
        //调用接口
        scheduleService.save(paramMap);

        return Result.ok();

    }

    @ApiOperation(value = "带条件分页查询排班")
    @PostMapping("schedule/list")
    public Result getScheduleQuery(HttpServletRequest request) {
        //转化数据
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        //获取数据 校验签名省略
        String sign = (String) paramMap.get("sign");
        String hoscode = (String) paramMap.get("hoscode");
        String depcode = (String) paramMap.get("depcode");
        //校验page属性
        int page = StringUtils.isEmpty(paramMap.get("page"))?
                1:Integer.parseInt((String)paramMap.get("page"));
        int limit = StringUtils.isEmpty(paramMap.get("limit"))?
                10:Integer.parseInt((String)paramMap.get("limit"));
        //创建条件查询对象
        ScheduleQueryVo scheduleQueryVo = new ScheduleQueryVo();
        scheduleQueryVo.setHoscode(hoscode);
        scheduleQueryVo.setDepcode(depcode);

        Page<Schedule> pageInfo = scheduleService.selectPage(page,limit,scheduleQueryVo);
        return Result.ok(pageInfo);
    }

    @ApiOperation(value = "删除排版信息")
    @PostMapping("schedule/remove")
    public Result removeSchedule(HttpServletRequest request){
        //转化数据
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(request.getParameterMap());
        //获取数据 签名检验省略
        String hoscode = (String) paramMap.get("hoscode");
        String hosScheduleId = (String) paramMap.get("hosScheduleId");
        //调用接口
        scheduleService.remove(hoscode,hosScheduleId);
        return Result.ok();
    }

}