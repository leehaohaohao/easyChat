package com.lihao.aspect;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.entity.enums.ResponseCodeEnum;
import com.lihao.exception.BusinessException;
import com.lihao.redis.RedisUtils;
import com.lihao.utils.StringTools;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Component("globalOperationAspect")
@Aspect
public class GlobalOperationAspect {
    @Resource
    private RedisUtils redisUtils;
    private static final Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);
    @Before("@annotation(com.lihao.annotation.GlobalInterceptor)")
    public void interceptorDo(JoinPoint point){
        try {
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if(interceptor==null){
                return;
            }
            if(interceptor.checkLogin() || interceptor.checkAdmin()){
                checkLogin(interceptor.checkAdmin());
            }
        }catch (BusinessException e){
            logger.error("全局拦截异常",e);
            throw e;
        }catch (Exception e){
            logger.error("全局拦截异常",e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }catch (Throwable e){
            logger.error("全局拦截异常",e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }
    private void checkLogin(Boolean checkAdmin){
        ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        HttpServletRequest request =attributes.getRequest();
        String token = request.getHeader("token");
        if(StringTools.isEmpty(token)){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN+token);
        if(tokenUserInfoDto==null){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if(checkAdmin && !tokenUserInfoDto.getAdmin()){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }


}
