package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Hospital;

import java.util.Map;

public interface HospitalService {
    //上传医院
    void save(Map<String, Object> map);

    //查询医院
    Hospital getHospital(String hoscode);
}
