package com.atguigu.yygh.user.service.impl;

import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.common.util.JwtHelper;
import com.atguigu.yygh.enums.AuthStatusEnum;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.mapper.UserInfoMapper;
import com.atguigu.yygh.user.service.PatientService;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import com.atguigu.yygh.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    
    @Autowired
    private PatientService patientService;
    
    //用户登录
    @Override
    public Map<String, Object> login(LoginVo loginVo) {
        //1.校验参数
        String phone = loginVo.getPhone();
        String code = loginVo.getCode();
        String openid = loginVo.getOpenid();
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)){
            throw new YyghException(20001,"数据为空");
        }
        //2.校验验证码
        //2.1根据手机号从redis取出验证码
        String rediscode = redisTemplate.opsForValue().get(phone);
        //2.2对比验证码
        if (!code.equals(rediscode)) {
            throw new YyghException(20001,"验证码有误");
        }
        //2.5判断openid，如果为空手机号验证码登录，不为空走绑定手机号
        Map<String, Object> map = new HashMap<>();
        UserInfo userInfo = new UserInfo();
        if (StringUtils.isEmpty(openid)) {
            //3.判断是否已经注册过
            QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("phone",phone);
            userInfo = baseMapper.selectOne(wrapper);
            if (null == userInfo) {
                //注册
                userInfo = new UserInfo();
                userInfo.setPhone(phone);
                userInfo.setStatus(1);
                baseMapper.insert(userInfo);
            }
        }else {
            // 根据openid查询用户信息
            QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("openid", openid);
            userInfo = baseMapper.selectOne(wrapper);
            if (userInfo == null) {
                throw new YyghException(20001,"用户注册信息有误");
            }
            // 更新用户手机号信息
            userInfo.setPhone(phone);
            baseMapper.updateById(userInfo);
        }


        if (userInfo.getStatus() == 0) {
            throw new YyghException(20001,"用户已经禁用");
        }
        //补全用户信息
        String name = userInfo.getName();
        if(StringUtils.isEmpty(name)) {
            name = userInfo.getNickName();
        }
        if(StringUtils.isEmpty(name)) {
            name = userInfo.getPhone();
        }
        if(StringUtils.isEmpty(userInfo.getHeadimgUrl())) {
            userInfo.setHeadimgUrl("");
        }
        //用户登录
        String token = JwtHelper.createToken(userInfo.getId(), name);
        map.put("name", name);
        map.put("token", token);
        map.put("headimgUrl",userInfo.getHeadimgUrl());

        return map;
    }

    //用户认证提交接口
    @Override
    public void userAuth(Long userId, UserAuthVo userAuthVo) {
        //1.根据userId查询用户信息
        UserInfo userInfo = baseMapper.selectById(userId);
        if (userInfo == null) {
            throw new YyghException(20001,"用户信息有误");
        }
        //2.更新认证信息
        BeanUtils.copyProperties(userAuthVo,userInfo);
        userInfo.setAuthStatus(AuthStatusEnum.AUTH_RUN.getStatus());
        baseMapper.updateById(userInfo);
    }

    //根据用户id获取用户信息
    @Override
    public UserInfo getUserInfo(Long userId) {
        //1.根据userId查询用户信息
        UserInfo userInfo = baseMapper.selectById(userId);
        if (userInfo == null) {
            throw new YyghException(20001,"用户信息有误");
        }
        //2.翻译相关字段
        userInfo = this.packUserInfo(userInfo);

        return userInfo;
    }

    //带条件带分页查询用户列表
    @Override
    public Page<UserInfo> selectPage(Page<UserInfo> pageParams, UserInfoQueryVo userInfoQueryVo) {
        //1.取出查询条件
        String name = userInfoQueryVo.getKeyword(); //用户名称
        Integer status = userInfoQueryVo.getStatus();//用户状态
        Integer authStatus = userInfoQueryVo.getAuthStatus(); //认证状态
        String createTimeBegin = userInfoQueryVo.getCreateTimeBegin(); //开始时间
        String createTimeEnd = userInfoQueryVo.getCreateTimeEnd(); //结束时间
        //2.验空进行条件拼装
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(name)) {
            wrapper.like("name",name);
        }
        if(!StringUtils.isEmpty(status)) {
            wrapper.eq("status",status);
        }
        if(!StringUtils.isEmpty(authStatus)) {
            wrapper.eq("auth_status",authStatus);
        }
        if(!StringUtils.isEmpty(createTimeBegin)) {
            wrapper.ge("create_time",createTimeBegin);
        }
        if(!StringUtils.isEmpty(createTimeEnd)) {
            wrapper.le("create_time",createTimeEnd);
        }
        //3.分页查询
        Page<UserInfo> pages = baseMapper.selectPage(pageParams, wrapper);
        //4.翻译字段
        pages.getRecords().stream().forEach(item ->{
            this.packUserInfo(item);

        });
        return pages;
    }

    @Override
    public void lock(Long userId, Integer status) {
        if (status.intValue() == 0 || status.intValue() == 1) {
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setStatus(status);
            baseMapper.updateById(userInfo);
        }
    }

    //用户详情
    @Override
    public Map<String, Object> show(Long userId) {
        //1根据用户id查询用户信息（翻译字段）
        UserInfo userInfo = this.packUserInfo(baseMapper.selectById(userId));
        //2根据用户id查询就诊人信息
        List<Patient> patientList = patientService.findList(userId);
        //3封装map返回
        Map<String, Object> map = new HashMap<>();
        map.put("userInfo",userInfo);
        map.put("patientList",patientList);
        return map;

    }

    //认证审批
    @Override
    public void approval(Long userId, Integer authStatus) {
        if (authStatus.intValue() == 2 || authStatus.intValue() == -1) {
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setAuthStatus(authStatus);
            baseMapper.updateById(userInfo);
        }
    }

    //翻译相关字段
    private UserInfo packUserInfo(UserInfo userInfo) {
        //处理认证状态编码
        userInfo.getParam().put("authStatusString",AuthStatusEnum.getStatusNameByStatus(userInfo.getAuthStatus()));
        //处理用户状态 0  1
        String statusString = userInfo.getStatus().intValue()==0 ?"锁定" : "正常";
        userInfo.getParam().put("statusString",statusString);
        return userInfo;
    }
}
