package com.atguigu.yygh.user.service.impl;

import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.common.util.JwtHelper;
import com.atguigu.yygh.enums.AuthStatusEnum;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.mapper.UserInfoMapper;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;
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
        //用户登录
        String token = JwtHelper.createToken(userInfo.getId(), name);
        map.put("name", name);
        map.put("token", token);

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

    //翻译相关字段
    private UserInfo packUserInfo(UserInfo userInfo) {
        String statusNameByStatus =
                AuthStatusEnum.getStatusNameByStatus(userInfo.getStatus());
        userInfo.getParam().put("authStatusString",statusNameByStatus);
        return userInfo;
    }
}
