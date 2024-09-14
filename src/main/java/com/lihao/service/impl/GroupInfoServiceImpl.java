package com.lihao.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.lihao.entity.config.AppConfig;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.dto.MessageSendDto;
import com.lihao.entity.dto.SysSettingDto;
import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.entity.enums.*;
import com.lihao.entity.po.*;
import com.lihao.entity.query.*;
import com.lihao.exception.BusinessException;
import com.lihao.mappers.*;
import com.lihao.redis.RedisComponent;
import com.lihao.service.ChatSessionUserService;
import com.lihao.service.UserContactService;
import com.lihao.utils.CopyTools;
import com.lihao.webSocket.ChannelContextUtils;
import com.lihao.webSocket.MessageHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.lihao.entity.vo.PaginationResultVO;
import com.lihao.service.GroupInfoService;
import com.lihao.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


/**
 *  业务接口实现
 */
@Service("groupInfoService")
public class GroupInfoServiceImpl implements GroupInfoService {
	@Resource
	private RedisComponent redisComponent;

	@Resource
	private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;
	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;
	@Resource
	private AppConfig appConfig;
	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;
	@Resource
	private ChatSessionUserMapper<ChatSessionUser,ChatSessionUserQuery> chatSessionUserMapper;
	@Resource
	private ChatMessageMapper<ChatMessage,ChatMessageQuery> chatMessageMapper;
	@Resource
	private MessageHandler messageHandler;
	@Resource
	private ChannelContextUtils channelContextUtils;
	@Resource
	private ChatSessionUserService chatSessionUserService;
	@Resource
	private UserContactService userContactService;
	@Resource
	private UserInfoMapper<UserInfo,UserInfoQuery> userInfoMapper;
	@Resource
	@Lazy
	private GroupInfoService groupInfoService;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<GroupInfo> findListByParam(GroupInfoQuery param) {
		return this.groupInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(GroupInfoQuery param) {
		return this.groupInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<GroupInfo> list = this.findListByParam(param);
		PaginationResultVO<GroupInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(GroupInfo bean) {
		return this.groupInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<GroupInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<GroupInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(GroupInfo bean, GroupInfoQuery param) {
		StringTools.checkParam(param);
		return this.groupInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(GroupInfoQuery param) {
		StringTools.checkParam(param);
		return this.groupInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据GroupId获取对象
	 */
	@Override
	public GroupInfo getGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.selectByGroupId(groupId);
	}

	/**
	 * 根据GroupId修改
	 */
	@Override
	public Integer updateGroupInfoByGroupId(GroupInfo bean, String groupId) {
		return this.groupInfoMapper.updateByGroupId(bean, groupId);
	}

	/**
	 * 根据GroupId删除
	 */
	@Override
	public Integer deleteGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.deleteByGroupId(groupId);
	}

	@Override
	public void saveCroup(GroupInfo groupInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
		Date curDate=  new Date();
		if(StringTools.isEmpty(groupInfo.getGroupId())){
			GroupInfoQuery groupInfoQuery = new GroupInfoQuery();
			groupInfoQuery.setGroupOwnerId(groupInfo.getGroupOwnerId());
			Integer count = this.groupInfoMapper.selectCount(groupInfoQuery);
			SysSettingDto sysSettingDto = redisComponent.getSysSetting();
			if(count>= sysSettingDto.getMaxGroupCount()){
				throw new BusinessException("最多支持能创建"+sysSettingDto.getMaxGroupCount()+"个群聊！");
			}
			if(avatarFile == null){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			groupInfo.setCreateTime(curDate);
			groupInfo.setGroupId(StringTools.getGroupId());
			this.groupInfoMapper.insert(groupInfo);
			//将群组添加为联系人
			UserContact userContact = new UserContact();
			userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			userContact.setContactType(UserContactTypeEnum.GROUP.getType());
			userContact.setContactId(groupInfo.getGroupId());
			userContact.setUserId(groupInfo.getGroupOwnerId());
			userContact.setCreateTime(curDate);
			userContact.setLastUpdateTime(curDate);
			this.userContactMapper.insert(userContact);
			//创建会话
			String sessionId = StringTools.getChatSessionId4Group(groupInfo.getGroupId());
			ChatSession chatSession = new ChatSession();
			chatSession.setSessionId(sessionId);
			chatSession.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatSession.setLastReceiveTime(curDate.getTime());
			this.chatSessionMapper.insertOrUpdate(chatSession);

			ChatSessionUser chatSessionUser = new ChatSessionUser();
			chatSessionUser.setUserId(groupInfo.getGroupOwnerId());
			chatSessionUser.setContactId(groupInfo.getGroupId());
			chatSessionUser.setContactName(groupInfo.getGroupName());
			chatSessionUser.setSessionId(sessionId);
			this.chatSessionUserMapper.insert(chatSessionUser);
			//创建消息
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSessionId(sessionId);
			chatMessage.setMessageType(MessageTypeEnum.GROUP_CREATE.getType());
			chatMessage.setMessageContent(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatMessage.setSendTime(curDate.getTime());
			chatMessage.setContactId(groupInfo.getGroupId());
			chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
			chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
			chatMessageMapper.insert(chatMessage);
			//将群组添加到联系人
			redisComponent.addUserContact(groupInfo.getGroupOwnerId(),groupInfo.getGroupId());
			//将联系人通道添加到群组通道
			channelContextUtils.addUser2Group(groupInfo.getGroupOwnerId(),groupInfo.getGroupId());
			//发送ws消息
			chatSessionUser.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatSessionUser.setLastReceiveTime(curDate.getTime());
			chatSessionUser.setMemberCount(1);

			MessageSendDto messageSendDto = CopyTools.copy(chatMessage,MessageSendDto.class);
			messageSendDto.setExtendData(chatSessionUser);
			messageSendDto.setLastMessage(chatSession.getLastMessage());
			messageHandler.sendMessage(messageSendDto);
		}else{
			GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupInfo.getGroupId());
			if(!dbInfo.getGroupOwnerId().equals(groupInfo.getGroupOwnerId())){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			this.groupInfoMapper.updateByGroupId(groupInfo,groupInfo.getGroupId());
			//更新相关表的冗余信息
			String contactNameUpdate = null;
			if(!dbInfo.getGroupName().equals(groupInfo.getGroupName())){
				contactNameUpdate = groupInfo.getGroupName();
			}
			if(contactNameUpdate == null){
				return;
			}
			chatSessionUserService.updateRedundanceInfo(contactNameUpdate,groupInfo.getGroupId());
		}
		if(avatarFile == null){
			return;
		}
		String baseFolder = appConfig.getProjectFolder()+ Constants.FILE_FOLDER_FILE;
		File targetFileFolder = new File(baseFolder+Constants.FILE_FOLDER_AVATAR_NAME);
		if(!targetFileFolder.exists()){
			targetFileFolder.mkdirs();
		}
		String filePath = targetFileFolder.getPath()+"/"+groupInfo.getGroupId()+Constants.IMAGE_SUFFIX;
		avatarFile.transferTo(new File(filePath));
		avatarCover.transferTo(new File(filePath+Constants.COVER_IMAGE_SUFFIX));

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void dissolutionGroup(String groupOwnerId, String groupId) {
		GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupId);
		if(dbInfo == null || !dbInfo.getGroupOwnerId().equals(groupOwnerId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//删除群组
		GroupInfo updateInfo = new GroupInfo();
		updateInfo.setStatus(GroupStatusEnum.DISSOLUTION.getStatus());
		this.groupInfoMapper.updateByGroupId(updateInfo,groupId);

		//更新联系人信息
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());

		UserContact updateUserContact = new UserContact();
		updateUserContact.setStatus(UserContactStatusEnum.DEL.getStatus());
		this.userContactMapper.updateByParam(updateUserContact,userContactQuery);
		//移除相关群员的联系人缓存
		//发消息 1：更新会话信息 2：记录群消息 3：发送解散通知消息
		List<UserContact> userContactList = this.userContactMapper.selectList(userContactQuery);
		for(UserContact userContact:userContactList){
			redisComponent.removeUserContact(userContact.getUserId(),userContact.getContactId());
		}
		String sessionId = StringTools.getChatSessionId4Group(groupId);
		Date curDate = new Date();
		String messageContent = MessageTypeEnum.DISSOLUTION_GROUP.getInitMessage();

		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		chatSession.setLastReceiveTime(curDate.getTime());
		chatSessionMapper.updateBySessionId(chatSession,sessionId);

		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setSessionId(sessionId);
		chatMessage.setSendTime(curDate.getTime());
		chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
		chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		chatMessage.setMessageType(MessageTypeEnum.DISSOLUTION_GROUP.getType());
		chatMessage.setContactId(groupId);
		chatMessage.setMessageContent(messageContent);
		chatMessageMapper.insert(chatMessage);
		MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
		messageHandler.sendMessage(messageSendDto);
	}

	@Override
	public void addOrRemoveGroupUser(TokenUserInfoDto tokenUserInfoDto, String groupId, String contactIds, Integer opType) {
		GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
		if(groupInfo == null || !groupInfo.getGroupOwnerId().equals(tokenUserInfoDto.getUserId())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		String[] contactIdList = contactIds.split(",");
		for(String contactId:contactIdList){
			if(Constants.ZERO.equals(opType)){
				groupInfoService.leaveGroup(contactId,groupId,MessageTypeEnum.REMOVE_GROUP);
			}else {
				userContactService.addContact(contactId,null,groupId,UserContactTypeEnum.GROUP.getType(),null);
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void leaveGroup(String userId, String groupId, MessageTypeEnum messageTypeEnum) {
		GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
		if(groupInfo == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(userId.equals(groupInfo.getGroupOwnerId())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		Integer count = userContactMapper.deleteByUserIdAndContactId(userId,groupId);
		if(count == 0){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserInfo userInfo = userInfoMapper.selectByUserId(userId);

		String sessionId = StringTools.getChatSessionId4Group(groupId);
		Date curDate = new Date();
		String messageContent = String.format(messageTypeEnum.getInitMessage(),userInfo.getNickName());
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		chatSession.setLastReceiveTime(curDate.getTime());
		chatSessionMapper.updateBySessionId(chatSession,sessionId);

		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setSessionId(sessionId);
		chatMessage.setSendTime(curDate.getTime());
		chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
		chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		chatMessage.setMessageType(messageTypeEnum.getType());
		chatMessage.setContactId(groupId);
		chatMessage.setMessageContent(messageContent);
		chatMessageMapper.insert(chatMessage);

		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		Integer memberCount = this.userContactMapper.selectCount(userContactQuery);
		MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
		messageSendDto.setExtendData(userId);
		messageSendDto.setMemberCount(memberCount);
		messageHandler.sendMessage(messageSendDto);

	}
}