package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
        } else {
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
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        //1.2创建分页对象（第一页为0）
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        //2创建条件查询模板
        //2.1设置筛选条件
        Department department = new Department();
        BeanUtils.copyProperties(departmentQueryVo, department);
        //2.1设置模板构造器
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        //2.3创建条件查询模板
        Example<Department> example = Example.of(department, matcher);
        //3进行带条件带分页查询
        Page<Department> pageInfo = departmentRepository.findAll(example, pageable);
        return pageInfo;

    }

    @Override
    public void delete(String hoscode, String depcode) {
        Department department = departmentRepository.getByHoscodeAndDepcode(hoscode, depcode);
        if (department == null) {
            throw new YyghException(20001, "科室编号有误");
        }
        departmentRepository.deleteById(department.getId());
    }

    //根据医院编号，查询医院所有科室列表
    @Override
    public List<DepartmentVo> findDeptTree(String hoscode) {
        //1.创建返回集合对象
        List<DepartmentVo> departmentVoList = new ArrayList<>();
//        2.根据hoscode查询所有科室信息
        List<Department> departmentList = departmentRepository.getByHoscode(hoscode);
//        3.实现根据bigcode进行分组
        Map<String,List<Department>> deptListMap =
                departmentList.stream().collect(
                        Collectors.groupingBy(Department::getBigcode));
//        4.封装大科室信息
        for (Map.Entry<String, List<Department>> entry : deptListMap.entrySet()) {
            //4.1创建大科室对象
            DepartmentVo bigDeptVo = new DepartmentVo();
            bigDeptVo.setDepcode(entry.getKey());
            bigDeptVo.setDepname(entry.getValue().get(0).getBigname());
            //5.封装小科室信息
            //5.1创建小科室集合
            List<DepartmentVo> deptVoList = new ArrayList<>();
            List<Department> deptList = entry.getValue();
            //5.2遍历集合 进行封装
            for (Department department : deptList) {
                DepartmentVo departmentVo = new DepartmentVo();
                departmentVo.setDepname(department.getDepname());
                departmentVo.setDepcode(department.getDepcode());
                deptVoList.add(departmentVo);
            }
            //        6.把小科室集合存入大科室对象
            bigDeptVo.setChildren(deptVoList);
            //        7.把大科室对象存入最终返回集合
            departmentVoList.add(bigDeptVo);
        }
        return departmentVoList;


    }

    //查询科室名称
    @Override
    public String getDepName(String hoscode, String depcode) {
        Department department = departmentRepository.getByHoscodeAndDepcode(hoscode, depcode);
        if (department == null) {
            throw new YyghException(20001, "科室编号有误");
        }
        return department.getDepname();
    }

    //根据医院，科室编号查找科室
    @Override
    public Department getDepartment(String hoscode, String depcode) {
        Department department = departmentRepository.getByHoscodeAndDepcode(hoscode, depcode);
        if (department == null) {
            throw new YyghException(20001, "科室编号有误");
        }
        return department;
    }

}
