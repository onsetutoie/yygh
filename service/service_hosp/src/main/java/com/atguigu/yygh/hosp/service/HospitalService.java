package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface HospitalService {
    //上传医院
    void save(Map<String, Object> map);

    //查询医院
    Hospital getHospital(String hoscode);

    //分页查询
    Page<Hospital> selectPage(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo);

    //上下线
    void updateStatus(String id, Integer status);

    //获取医院详情
    Map<String, Object> getHospitalById(String id);
}
