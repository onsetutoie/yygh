package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.model.hosp.HospitalSet;
import com.atguigu.yygh.vo.hosp.HospitalSetQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(description = "医院设置接口")
@RestController
@RequestMapping("/admin/hosp/hospitalSet")
//@CrossOrigin //解决跨域问题
public class HospitalSetController {

    @Autowired
    HospitalSetService hospitalSetService;

    @ApiOperation(value = "模拟登录")
    @PostMapping("login")
    public R login(){
        return R.ok().data("token","admin-token");
    }

    @ApiOperation(value = "模拟获取用户信息")
    @GetMapping("info")
    //info ：
    //{"code":20000,"data":{"roles":["admin"],
    //"introduction":"I am a super administrator",
    //"avatar":"https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif",
    //"name":"Super Admin"}}
    public R info(){
        Map<String ,Object> map = new HashMap<>();
        map.put("roles","admin");
        map.put("introduction","I am a super administrator");
        map.put("avatar","https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
        map.put("name","Super Admin");

        return R.ok().data(map);
    }

    @ApiOperation(value = "医院设置状态")
    @PutMapping("lockHospitalSet/{id}/{status}")
    public R lockHospitalSet(@PathVariable("id") Long id, @PathVariable("status") Integer status) {
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        hospitalSet.setStatus(status);
        hospitalSetService.updateById(hospitalSet);
        return R.ok();
    }

    @ApiOperation(value = "修改医院设置")
    @PutMapping
    public R update(@RequestBody HospitalSet hospitalSet) {
        hospitalSetService.updateById(hospitalSet);
        return R.ok();
    }


    @ApiOperation(value = "根据id查询医院设置")
    @GetMapping("{id}")
    public R getById(@PathVariable("id") Long id){
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        return R.ok().data("hospitalSet",hospitalSet);
    }

    @ApiOperation(value = "新增医院设置")
    @PostMapping
    public R save(@RequestBody HospitalSet hospitalSet){
        boolean save = hospitalSetService.save(hospitalSet);
        if (save) {
            return R.ok();
        }else {
            return R.error();
        }

    }

    @ApiOperation(value = "医院设置列表")
    @GetMapping("/findAll")
    public R findAll(){
//        try {
//            int i = 1/0;
//        } catch (Exception e) {
//            throw new YyghException(20001,"自定义异常");
//        }
        List<HospitalSet> list = hospitalSetService.list();
        return R.ok().data("list",list);
    }

    @ApiOperation(value = "医院设置删除")
    @DeleteMapping("/{id}")
    public R removeById(@PathVariable("id") Long id){
        boolean result = hospitalSetService.removeById(id);
        return R.ok();
    }

    @ApiOperation(value = "医院设置批量删除")
    @DeleteMapping("/batchRemove")
    public R batchRemove(@RequestBody List<Long> idList ){
        hospitalSetService.removeByIds(idList);
        return R.ok();
    }

    @ApiOperation(value = "医院设置分页列表")
    @GetMapping("{page}/{limit}")
    public R pageList(@PathVariable("page") Long page,@PathVariable("limit") Long limit){
        Page<HospitalSet> pageParam = new Page<>(page,limit);
        Page<HospitalSet> pageModel = hospitalSetService.page(pageParam);
        return R.ok().data("pageModel",pageModel);
    }

    @ApiOperation(value = "分页条件医院设置列表")
    @PostMapping("{page}/{limit}")
    public R pageQuery(@PathVariable("page") Long page, @PathVariable("limit") Long limit,
                       @RequestBody HospitalSetQueryVo hospitalSetQueryVo){

        QueryWrapper<HospitalSet> wrapper = new QueryWrapper<>();

        String hosname = hospitalSetQueryVo.getHosname();
        String hoscode = hospitalSetQueryVo.getHoscode();
        if (!StringUtils.isEmpty(hosname)) {
            wrapper.like("hosname",hosname);
        }
        if (!StringUtils.isEmpty(hoscode)) {
            wrapper.like("hoscode",hoscode);
        }
        Page<HospitalSet> pageParam = new Page<>(page,limit);
        Page<HospitalSet> pageModel = hospitalSetService.page(pageParam,wrapper);
        return R.ok().data("pageModel",pageModel);

    }


}
