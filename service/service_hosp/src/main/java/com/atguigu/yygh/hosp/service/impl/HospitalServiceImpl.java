package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.HospitalRepository;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.Hospital;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class HospitalServiceImpl implements HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;


    @Override
    public void save(Map<String, Object> map) {
        //1.转化参数类型  map -> Hospital
        String jsonString = JSONObject.toJSONString(map);
        Hospital hospital = JSONObject.parseObject(jsonString, Hospital.class);
        //2.根据hoscode查询医院信息
        Hospital targetHospital =  hospitalRepository.getByHoscode(hospital.getHoscode());
        //3.判断 存在 更新
        if (targetHospital != null) {
            hospital.setId(targetHospital.getId());
            hospital.setStatus(targetHospital.getStatus());
            hospital.setCreateTime(targetHospital.getCreateTime());
            hospital.setUpdateTime(new Date());
            hospital.setIsDeleted(0);
            hospitalRepository.save(hospital);
        } else {
            // 不存在 新增
            // 0：未上线 1：已上线
            hospital.setStatus(0);
            hospital.setCreateTime(new Date());
            hospital.setUpdateTime(new Date());
            hospital.setIsDeleted(0);
            hospitalRepository.save(hospital);
        }
    }

    @Override
    public Hospital getHospital(String hoscode) {
        Hospital hospital = hospitalRepository.getByHoscode(hoscode);
        return hospital;
    }
}
