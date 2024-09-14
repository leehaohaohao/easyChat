package com.lihao.redis;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.dto.SysSettingDto;
import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.utils.StringTools;
import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component("redisComponent")
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils;

    /**
     * 获取心跳
     * @param userId 用户id
     * @return 返回心跳信息
     */
    public Long getHeartBeat(String userId){
        return (Long)redisUtils.get(Constants.REDIS_KEY_WS_USER_HEART_BEAT+userId);
    }
    public void removeHeartBeat(String userId){
        redisUtils.delete(Constants.REDIS_KEY_WS_USER_HEART_BEAT+userId);
    }
    public void saveHeartBeat(String userId){
        redisUtils.setex(Constants.REDIS_KEY_WS_USER_HEART_BEAT+userId,System.currentTimeMillis(),Constants.REDIS_KEY_EXPIRES_HEART_BEAT);
    }
    public void saveTokenUserInfoDto(TokenUserInfoDto tokenUserInfoDto){
        redisUtils.setex(Constants.REDIS_KEY_WS_TOKEN+tokenUserInfoDto.getToken(),tokenUserInfoDto,Constants.REDIS_KEY_TOKEN_EXPIRES);
        redisUtils.setex(Constants.REDIS_KEY_WS_TOKEN_USERID+tokenUserInfoDto.getToken(),tokenUserInfoDto.getToken(),Constants.REDIS_KEY_TOKEN_EXPIRES);
    }

    public SysSettingDto getSysSetting(){
        SysSettingDto sysSettingDto = (SysSettingDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        sysSettingDto = sysSettingDto == null?new SysSettingDto() : sysSettingDto;
        return sysSettingDto;
    }
    public void saveSysSetting(SysSettingDto sysSettingDto){
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING,sysSettingDto);
    }
    public TokenUserInfoDto getTokenUserInfoDto(String token){
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN+token);
        return tokenUserInfoDto;
    }
    public TokenUserInfoDto getTokenUserInfoDtoByUserId(String userId){
        String token = (String)redisUtils.get(Constants.REDIS_KEY_WS_TOKEN_USERID+userId);
        return getTokenUserInfoDto(token);
    }
    public void cleanUserTokenByUserId(String userId){
        String token = (String)redisUtils.get(Constants.REDIS_KEY_WS_TOKEN_USERID+userId);
        if(StringTools.isEmpty(token)){
            return;
        }
        redisUtils.delete(Constants.REDIS_KEY_WS_TOKEN+token);
    }
    //清空联系人
    public void cleanUserContact(String userId){
        redisUtils.delete(Constants.REDIS_KEY_USER_CONTACT+userId);
    }
    //批量添加联系人
    public void addUserContactBatch(String userId, List<String> contactIdList){
        redisUtils.lpushAll(Constants.REDIS_KEY_USER_CONTACT+userId,contactIdList,Constants.REDIS_KEY_TOKEN_EXPIRES);
    }
    //添加单个联系人
    public void addUserContact(String userId, String contactId){
        List<String> contactIdList = getUserContactList(userId);
        if(contactIdList.contains(contactId)){
            return;
        }
        redisUtils.lpush(Constants.REDIS_KEY_USER_CONTACT+userId,contactId,Constants.REDIS_KEY_TOKEN_EXPIRES);
    }
    //获取用户列表
    public List<String> getUserContactList(String userId){
        return (List<String>) redisUtils.getQueueList(Constants.REDIS_KEY_USER_CONTACT+userId);
    }
    //删除联系人
    public void removeUserContact(String userId,String contactId){
        redisUtils.remove(Constants.REDIS_KEY_USER_CONTACT+userId,contactId);
    }
}
