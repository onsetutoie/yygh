package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface DepartmentService {
    //上传科室
    void save(Map<String, Object> paramMap);

    //带条件分页查询科室
    Page<Department> selectPage(int page, int limit, DepartmentQueryVo departmentQueryVo);


    void delete(String hoscode, String depcode);

    //查询医院所有科室列表
    List<DepartmentVo> findDeptTree(String hoscode);

    //查询科室名称
    String getDepName(String hoscode, String depcode);
}
