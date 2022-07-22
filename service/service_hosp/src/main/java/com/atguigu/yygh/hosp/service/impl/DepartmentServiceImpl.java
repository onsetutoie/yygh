package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;


    //上传科室
    @Override
    public void save(Map<String, Object> paramMap) {
        //1.转化参数类型
        Department department = JSONObject.parseObject(JSONObject.toJSONString(paramMap), Department.class);
        //2.根据 医院 查询 科室信息
        Department targetDepartment = departmentRepository.getByHoscodeAndDepcode(
                paramMap.get("hoscode"), paramMap.get("depcode"));

        //3.存在 更新
        if (targetDepartment != null) {
            department.setId(targetDepartment.getId());
            department.setCreateTime(targetDepartment.getCreateTime());
            department.setUpdateTime(new Date());
            department.setIsDeleted(targetDepartment.getIsDeleted());
            departmentRepository.save(department);
        }else {
            //不存在 新增
            department.setCreateTime(new Date());
            department.setUpdateTime(new Date());
            department.setIsDeleted(0);
            departmentRepository.save(department);
        }
    }

    //带条件分页查询科室
    @Override
    public Page<Department> selectPage(int page, int limit, DepartmentQueryVo departmentQueryVo) {
        //1创建分页查询对象
        //1.1创建排序对象
        Sort sort = Sort.by(Sort.Direction.DESC,"createTime");
        //1.2创建分页对象（第一页为0）
        Pageable pageable = PageRequest.of(page-1,limit,sort);
        //2创建条件查询模板
        //2.1设置筛选条件
        Department department = new Department();
        BeanUtils.copyProperties(departmentQueryVo,department);
        //2.1设置模板构造器
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        //2.3创建条件查询模板
        Example<Department> example = Example.of(department,matcher);
        //3进行带条件带分页查询
        Page<Department> pageInfo = departmentRepository.findAll(example, pageable);
        return pageInfo;

    }

    @Override
    public void delete(String hoscode, String depcode) {
        Department department = departmentRepository.getByHoscodeAndDepcode(hoscode, depcode);
        if (department == null) {
            throw new YyghException(20001,"科室编号有误");
        }
        departmentRepository.deleteById(department.getId());
    }

}
