package com.atguigu.yygh.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.R;
import com.atguigu.yygh.common.handler.YyghException;
import com.atguigu.yygh.common.util.JwtHelper;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.user.utils.ConstantPropertiesUtil;
import com.atguigu.yygh.user.utils.HttpClientUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "微信登录")
@Controller
@RequestMapping("/api/ucenter/wx")
public class WeixinApiController {

    @Autowired
    private UserInfoService userInfoService;


    @ApiOperation(value = "获取微信登录参数")
    @GetMapping("getLoginParam")
    @ResponseBody
    public R genQrConnect(HttpSession session) throws UnsupportedEncodingException {
        Map<String,Object> map = new HashMap<>();
        String redirectUri =
                URLEncoder.encode(ConstantPropertiesUtil.WX_OPEN_REDIRECT_URL, "UTF-8");
        map.put("appid", ConstantPropertiesUtil.WX_OPEN_APP_ID);
        map.put("redirectUri", redirectUri);
        map.put("scope", "snsapi_login");
        map.put("state", System.currentTimeMillis()+"");//System.currentTimeMillis()+""
        return R.ok().data(map);
    }

    @GetMapping("callback")
    public String callback(String code, String state, HttpSession session) {
        //1.获取微信回调验证码
        System.out.println("code = " + code);
        System.out.println("state = " + state);
        //2.用code访问微信接口 换取access_token、open_id
        //2.1拼写请求url
        //方式一 字符串 String url = ""
        //方式二
        StringBuffer baseAccessTokenUrl = new StringBuffer()
                .append("https://api.weixin.qq.com/sns/oauth2/access_token")
                .append("?appid=%s")
                .append("&secret=%s")
                .append("&code=%s")
                .append("&grant_type=authorization_code");
        String accessTokenUrl = String.format(baseAccessTokenUrl.toString(),
                ConstantPropertiesUtil.WX_OPEN_APP_ID,
                ConstantPropertiesUtil.WX_OPEN_APP_SECRET,
                code);
        try {
            //2.2 借助工具发送求情，获得响应
            String accesstokenInfo = HttpClientUtils.get(accessTokenUrl);
            System.out.println("accesstokenInfo = " + accesstokenInfo);
            //2.3 从json串中获取access_token、open_id
            JSONObject accesstokenJson = JSONObject.parseObject(accesstokenInfo);
            String access_token = accesstokenJson.getString("access_token");
            String openid = accesstokenJson.getString("openid");

            //3.根据open_id查询用户信息
            QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("openid",openid);
            UserInfo userInfo = userInfoService.getOne(wrapper);

            //4.用户信息为空 走注册流程
            if (userInfo == null) {
                //5.根据access_token、open_id获取用户信息完成注册流程
                //5.1拼写url
                String baseUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo" +
                        "?access_token=%s" +
                        "&openid=%s";
                String userInfoUrl = String.format(baseUserInfoUrl, access_token, openid);
                //5.2借助工具发送求情，获得响应
                String resultInfo = HttpClientUtils.get(userInfoUrl);
                System.out.println("resultInfo:"+resultInfo);
                //5.3 转化json串，获取返回值
                JSONObject resultUserInfoJson = JSONObject.parseObject(resultInfo);
                //解析用户信息
                //用户昵称
                String nickname = resultUserInfoJson.getString("nickname");
                //用户头像
                String headimgurl = resultUserInfoJson.getString("headimgurl");
                //5.4 userInfo 中存入信息完成注册
                userInfo = new UserInfo();
                userInfo.setOpenid(openid);
                userInfo.setStatus(1);
                userInfo.setNickName(nickname);
                userInfoService.save(userInfo);
            }
            //6.验证用户是否被锁定
            if (userInfo.getStatus() == 0) {
                throw new YyghException(20001,"用户被锁定");
            }
            //7.验证用户是否绑定手机号
            //判断userInfo是否有手机号，如果手机号为空，返回openid
            //如果手机号不为空，返回openid值是空字符串
            //前端判断：如果openid不为空，绑定手机号，如果openid为空，不需要绑定手机号
            Map<String,Object> map = new HashMap<>();
            if (userInfo.getPhone() == null) {
                map.put("openid",openid);
            }else {
                map.put("openid","");
            }
            //8.补全用户信息、并登录
            String name = userInfo.getName();
            if(StringUtils.isEmpty(name)) {
                name = userInfo.getNickName();
            }
            if(StringUtils.isEmpty(name)) {
                name = userInfo.getPhone();
            }
            String token = JwtHelper.createToken(userInfo.getId(), name);
            map.put("name", name);
            map.put("token", token);
            //9.重定向回相关页面
            return "redirect:http://localhost:3000/weixin/callback?" +
                    "token="+map.get("token")+ "&openid="+map.get("openid")+
                    "&name="+URLEncoder.encode((String) map.get("name"),"utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            throw new YyghException(20001,"用户扫码登录失败");
        }
    }
}
