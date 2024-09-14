package com.lihao.controller;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.po.UserLogin;
import com.lihao.entity.po.UserRegister;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.entity.vo.UserInfoVO;
import com.lihao.exception.BusinessException;
import com.lihao.redis.RedisComponent;
import com.lihao.redis.RedisUtils;
import com.lihao.service.UserInfoService;
import com.wf.captcha.ArithmeticCaptcha;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController("accountController")
@RequestMapping("/account")
@Validated
public class AccountController extends ABaseController{
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);
    @Resource
    private RedisUtils redisUtils ;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserInfoService userInfoService;
    @RequestMapping("/checkCode")
    public ResponseVO checkCode(){
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100,42);
        String code = captcha.text();
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey,code,Constants.REDIS_TIME_1MIN*10);
        String checkCodeBase64 = captcha.toBase64();
        Map<String,String> result = new HashMap<>();
        result.put("checkCode",checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);
        return getSuccessResponseVO(result);
    }
    @RequestMapping("/register")
    public ResponseVO register(@RequestBody UserRegister userRegister){
        try{
            if(!userRegister.getCheckCode().equalsIgnoreCase(
                    (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE+userRegister.getCheckCodeKey())
            )){
                throw new BusinessException("图片验证码不正确！");
            }
            userInfoService.register(userRegister);
            return getSuccessResponseVO(null);
        }finally {
            redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE+userRegister.getCheckCodeKey());
        }
    }
    @RequestMapping("/login")
    public ResponseVO login(@RequestBody UserLogin userLogin){
        try{
            if(!userLogin.getCheckCode().equalsIgnoreCase(
                    (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE+userLogin.getCheckCodeKey())
            )){
                throw new BusinessException("图片验证码不正确！");
            }
            //返回用户的一些地区等基本信息以及token
            UserInfoVO userInfoVO = userInfoService.login(userLogin);
            return getSuccessResponseVO(userInfoVO);
        }finally {
            redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE+userLogin.getCheckCodeKey());
        }
    }
    @GlobalInterceptor
    @RequestMapping("/getSysSetting")
    public ResponseVO getSysSetting(){
        return getSuccessResponseVO(redisComponent.getSysSetting());
    }
}
