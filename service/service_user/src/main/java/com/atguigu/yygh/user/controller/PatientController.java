package com.atguigu.yygh.user.controller;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.common.util.AuthContextHolder;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.user.service.PatientService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(tags = "就诊接口")
@RestController
@RequestMapping("/api/user/patient")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @ApiOperation(value = "获取就诊人列表")
    @GetMapping("auth/findAll")
    public R findList(HttpServletRequest request){
        //取出用户ID
        Long userId = AuthContextHolder.getUserId(request);
        List<Patient> list = patientService.findList(userId);
        return R.ok().data("list",list);
    }

    @ApiOperation(value = "添加就诊人信息")
    @PostMapping("auth/save")
    public R save(@RequestBody Patient patient, HttpServletRequest request){
        Long userId = AuthContextHolder.getUserId(request);
        patient.setUserId(userId);
        patientService.save(patient);
        return R.ok();
    }

    @ApiOperation(value = "根据id查询就诊人信息")
    @GetMapping("auth/get/{id}")
    public R findById(@PathVariable("id") Long id){
        Patient patient = patientService.getById(id);
        return R.ok().data("patient",patient);
    }

    @ApiOperation(value = "修改就诊人信息")
    @PostMapping("auth/update")
    public R updateById(@RequestBody Patient patient){
        patientService.updateById(patient);
        return R.ok();
    }

    @ApiOperation(value = "删除就诊人信息")
    @Delete("auth/delete/{id}")
    public R delete(@PathVariable("id") Long id){
        patientService.removeById(id);
        return R.ok();
    }

    @ApiOperation(value = "获取就诊人(远程调用)")
    @GetMapping("inner/get/{id}")
    public Patient getPatientOrder(@PathVariable("id") Long id){
        Patient patient = patientService.getById(id);
        return patient;
    }

}
