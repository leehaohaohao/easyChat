package com.lihao.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.lihao.entity.enums.ResponseCodeEnum;
import com.lihao.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    private static SerializerFeature[] FEATURES = new SerializerFeature[]{SerializerFeature.WriteMapNullValue};
    public static String convertobj2Json(Object obj){
        return JSON.toJSONString(obj,FEATURES);
    }
    public static <T> T convertJson2Obj(String json,Class<T> classz){
        try{
            return JSONObject.parseObject(json,classz);
        }catch (Exception e){
            logger.error("convertJson2Obj异常，json:{}",json);
            throw new BusinessException(ResponseCodeEnum.CODE_601);
        }
    }
    public static <T> List<T> convertJsonArray2List(String json , Class<T> classz){
        try{
            return JSONArray.parseArray(json,classz);
        }catch (Exception e){
            logger.error("convertJsonArray2List异常，json:{}",json,e);
            throw new BusinessException(ResponseCodeEnum.CODE_601);
        }
    }
}
