package com.lihao.webSocket;

import com.lihao.entity.constants.Constants;
import com.lihao.entity.dto.MessageSendDto;
import com.lihao.entity.dto.WsInitData;
import com.lihao.entity.enums.MessageTypeEnum;
import com.lihao.entity.enums.UserContactApplyStatusEnum;
import com.lihao.entity.enums.UserContactTypeEnum;
import com.lihao.entity.po.ChatMessage;
import com.lihao.entity.po.ChatSessionUser;
import com.lihao.entity.po.UserContactApply;
import com.lihao.entity.po.UserInfo;
import com.lihao.entity.query.*;
import com.lihao.mappers.ChatMessageMapper;
import com.lihao.mappers.ChatSessionUserMapper;
import com.lihao.mappers.UserContactApplyMapper;
import com.lihao.mappers.UserInfoMapper;
import com.lihao.redis.RedisComponent;
import com.lihao.utils.JsonUtils;
import com.lihao.utils.StringTools;
import com.lihao.webSocket.netty.NettyWebSocketStarter;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Component
public class ChannelContextUtils {
    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketStarter.class);
    private static final ConcurrentHashMap<String,Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ChannelGroup> GROUP_CONTEXT_MAP = new ConcurrentHashMap<>();
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Resource
    private ChatSessionUserMapper<ChatSessionUser,ChatSessionUserQuery> chatSessionUserMapper;
    @Resource
    private ChatMessageMapper<ChatMessage,ChatMessageQuery> chatMessageMapper;

    @Resource
    private UserContactApplyMapper<UserContactApply,UserContactApplyQuery> userContactApplyMapper;

    public void addContext(String userId, Channel channel){
        String channelId = channel.id().toString();
        AttributeKey attributeKey = null;
        if(!AttributeKey.exists(channelId)){
            attributeKey = AttributeKey.newInstance(channelId);
        }else{
            attributeKey = AttributeKey.valueOf(channelId);
        }
        channel.attr(attributeKey).set(userId);
        List<String> contactIdList = redisComponent.getUserContactList(userId);
        for(String groupId:contactIdList){
            if(groupId.startsWith(UserContactTypeEnum.GROUP.getPrefix())){
                add2Group(groupId,channel);
            }
        }
        USER_CONTEXT_MAP.put(userId,channel);
        redisComponent.saveHeartBeat(userId);
        //更新用户最后连接时间
        UserInfo updateInfo = new UserInfo();
        updateInfo.setLastLoginTime(new Date());
        userInfoMapper.updateByUserId(updateInfo,userId);
        //给用户发送消息
        UserInfo userInfo = userInfoMapper.selectByUserId(userId);
        Long sourceLastOffTime = userInfo.getLastOffTime();
        Long lastOffTime = sourceLastOffTime;
        if(sourceLastOffTime!=null && System.currentTimeMillis() - Constants.MILLISSECONDS_3DAYS_AGO > sourceLastOffTime){
            lastOffTime = Constants.MILLISSECONDS_3DAYS_AGO;
        }

        //查询会话信息 查询用户所有的会话信息 保证换了设备会同步

        ChatSessionUserQuery sessionUserQuery = new ChatSessionUserQuery();
        sessionUserQuery.setUserId(userId);
        sessionUserQuery.setOrderBy("last_receive_time desc");
        List<ChatSessionUser> chatSessionUserList = chatSessionUserMapper.selectList(sessionUserQuery);


        WsInitData wsInitData = new WsInitData();
        wsInitData.setChatSessionList(chatSessionUserList);

        //查询聊天消息

        List<String> groupIdList = contactIdList.stream().filter(item->item.startsWith(UserContactTypeEnum.GROUP.getPrefix())).collect(Collectors.toList());
        groupIdList.add(userId);
        ChatMessageQuery messageQuery = new ChatMessageQuery();
        messageQuery.setContactIdList(groupIdList);
        messageQuery.setLastReceiveTime(lastOffTime);
        List<ChatMessage> chatMessageList = chatMessageMapper.selectList(messageQuery);
        wsInitData.setChatMessageList(chatMessageList);

        //查询好友申请

        UserContactApplyQuery applyQuery = new UserContactApplyQuery();
        applyQuery.setReceiveUserId(userId);
        applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
        applyQuery.setLastApplyTimestamp(lastOffTime);
        Integer applyCount = userContactApplyMapper.selectCount(applyQuery);
        wsInitData.setApplyCount(applyCount);
        //发送消息
        MessageSendDto messageSendDto = new MessageSendDto();
        messageSendDto.setMessageType(MessageTypeEnum.INIT.getType());
        messageSendDto.setContactId(userId);
        messageSendDto.setExtendData(wsInitData);
        sendMsg(messageSendDto,userId);
    }
    //发送群组

    public static void sendMsg(MessageSendDto messageSendDto , String receiveId){
        if(receiveId == null){
            return;
        }
        Channel sendChannel = USER_CONTEXT_MAP.get(receiveId);
        if(sendChannel == null){
            return;
        }
        //相对于客户端而言，联系人就是发送人，所以这里转换一下在发送,好友申请的时候不处理
        if(MessageTypeEnum.ADD_FRIEND_SELF.getType().equals(messageSendDto.getMessageType())){
            UserInfo userInfo = (UserInfo) messageSendDto.getExtendData();
            messageSendDto.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            messageSendDto.setContactId(userInfo.getUserId());
            messageSendDto.setContactName(userInfo.getNickName());
            messageSendDto.setExtendData(null);
        }else{
            messageSendDto.setContactId(messageSendDto.getSendUserId());
            messageSendDto.setContactName(messageSendDto.getSendUserNickName());
        }
        sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertobj2Json(messageSendDto)));
    }


    public void add2Group(String groupId,Channel channel){
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        if(group == null){
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            GROUP_CONTEXT_MAP.put(groupId,group);
        }
        if(channel == null){
            return;
        }
        group.add(channel);
    }
    public void removeContext(Channel channel){
        Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attribute.get();
        if(StringTools.isEmpty(userId)){
            USER_CONTEXT_MAP.remove(userId);
        }
        redisComponent.removeHeartBeat(userId);
        //更新用户最后离线时间
        UserInfo userInfo = new UserInfo();
        userInfo.setLastOffTime(System.currentTimeMillis());
        userInfoMapper.updateByUserId(userInfo,userId);
    }
    public void sendMessage(MessageSendDto messageSendDto){
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(messageSendDto.getContactId());
        switch (contactTypeEnum){
            case USER:
                send2User(messageSendDto);
                break;
            case GROUP:
                send2Group(messageSendDto);
                break;
        }
    }



    private void send2User(MessageSendDto messageSendDto){
        String contactId = messageSendDto.getContactId();
        if(StringTools.isEmpty(contactId)){
            return;
        }
        sendMsg(messageSendDto,contactId);
        //强制下线
        if(MessageTypeEnum.FORCE_OFF_LINE.getType().equals(messageSendDto.getMessageType())){
            closeContext(contactId);
        }
    }

    public void closeContext(String userId){
        if(StringTools.isEmpty(userId)){
            return;
        }
        redisComponent.cleanUserTokenByUserId(userId);
        Channel channel = USER_CONTEXT_MAP.get(userId);
        if(channel == null){
            return;
        }
        channel.close();
    }
    private void send2Group(MessageSendDto messageSendDto){
        if(StringTools.isEmpty(messageSendDto.getContactId())){
            return;
        }
        ChannelGroup channelGroup = GROUP_CONTEXT_MAP.get(messageSendDto.getContactId());
        if(channelGroup == null){
            return;
        }
        channelGroup.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertobj2Json(messageSendDto)));
        //移除群聊
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(messageSendDto.getMessageType());
        if(MessageTypeEnum.LEAVE_GROUP == messageTypeEnum || MessageTypeEnum.REMOVE_GROUP == messageTypeEnum){
            String userId = (String) messageSendDto.getExtendData();
            redisComponent.removeUserContact(userId, messageSendDto.getContactId());
            Channel channel = USER_CONTEXT_MAP.get(userId);
            if(channel == null){
                return;
            }
            channelGroup.remove(channel);
        }
        if(MessageTypeEnum.DISSOLUTION_GROUP == messageTypeEnum){
            GROUP_CONTEXT_MAP.remove(messageSendDto.getContactId());
            channelGroup.close();
        }
    }


    //发送消息

    public void addUser2Group(String groupId, String userId) {
        // 获取用户的 Channel
        Channel userChannel = USER_CONTEXT_MAP.get(userId);
        if (userChannel == null) {
            // 如果用户不在线，那么可能需要进行一些错误处理
            logger.warn("User {} is not online, cannot be added to group {}", userId, groupId);
            return;
        }

        // 获取群组的 ChannelGroup
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        if (group == null) {
            // 如果群组不存在，那么可能需要创建一个新的 ChannelGroup
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            GROUP_CONTEXT_MAP.put(groupId, group);
        }

        // 将用户的 Channel 添加到群组的 ChannelGroup 中
        group.add(userChannel);
    }

}
