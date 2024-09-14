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
import com.lihao.entity.po.ChatSession;
import com.lihao.entity.po.UserContact;
import com.lihao.entity.query.ChatSessionQuery;
import com.lihao.entity.query.UserContactQuery;
import com.lihao.exception.BusinessException;
import com.lihao.mappers.ChatSessionMapper;
import com.lihao.mappers.UserContactMapper;
import com.lihao.redis.RedisComponent;
import com.lihao.utils.CopyTools;
import com.lihao.utils.DateUtil;
import com.lihao.webSocket.MessageHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lihao.entity.query.ChatMessageQuery;
import com.lihao.entity.po.ChatMessage;
import com.lihao.entity.vo.PaginationResultVO;
import com.lihao.entity.query.SimplePage;
import com.lihao.mappers.ChatMessageMapper;
import com.lihao.service.ChatMessageService;
import com.lihao.utils.StringTools;
import org.springframework.web.multipart.MultipartFile;


/**
 * 聊天消息表 业务接口实现
 */
@Service("chatMessageService")
public class ChatMessageServiceImpl implements ChatMessageService {
	private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

	@Resource
	private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;
	@Resource
	private RedisComponent redisComponent;
	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;
	@Resource
	private MessageHandler messageHandler;
	@Resource
	private AppConfig appConfig;
	@Resource
	private UserContactMapper<UserContact,UserContactQuery> userContactMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<ChatMessage> findListByParam(ChatMessageQuery param) {
		return this.chatMessageMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(ChatMessageQuery param) {
		return this.chatMessageMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<ChatMessage> list = this.findListByParam(param);
		PaginationResultVO<ChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(ChatMessage bean) {
		return this.chatMessageMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(ChatMessage bean, ChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatMessageMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(ChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatMessageMapper.deleteByParam(param);
	}

	/**
	 * 根据MessageId获取对象
	 */
	@Override
	public ChatMessage getChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.selectByMessageId(messageId);
	}

	/**
	 * 根据MessageId修改
	 */
	@Override
	public Integer updateChatMessageByMessageId(ChatMessage bean, Long messageId) {
		return this.chatMessageMapper.updateByMessageId(bean, messageId);
	}

	/**
	 * 根据MessageId删除
	 */
	@Override
	public Integer deleteChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.deleteByMessageId(messageId);
	}

	@Override
	public MessageSendDto saveMessage(ChatMessage chatMessage, TokenUserInfoDto tokenUserInfoDto) {
		//如果不是机器人回复，判断好友状态
		if(!Constants.ROBOT_UID.equals(tokenUserInfoDto.getUserId())){
			List<String> contactList = redisComponent.getUserContactList(tokenUserInfoDto.getUserId());
			if(!contactList.contains(chatMessage.getContactId())){
				UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(chatMessage.getContactId());
				if(UserContactTypeEnum.USER == userContactTypeEnum){
					throw new BusinessException(ResponseCodeEnum.CODE_902);
				}else{
					throw new BusinessException(ResponseCodeEnum.CODE_903);
				}
			}
		}
		String sessionId = null;
		String sendUserId = tokenUserInfoDto.getUserId();
		String contactId = chatMessage.getContactId();
		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
		if(UserContactTypeEnum.USER==contactTypeEnum){
			sessionId = StringTools.getChatSessionId4User(new String[]{sendUserId,contactId});
		}else{
			sessionId = StringTools.getChatSessionId4Group(contactId);
		}
		chatMessage.setSessionId(sessionId);
		Long curTime = System.currentTimeMillis();
		chatMessage.setSendTime(curTime);
		MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(chatMessage.getMessageType());
		if(messageTypeEnum == null || ArrayUtils.contains(new Integer[]{MessageTypeEnum.CHAT.getType(),MessageTypeEnum.MEDIA_CHAT.getType()},chatMessage.getMessageType())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		Integer status = MessageTypeEnum.MEDIA_CHAT == messageTypeEnum? MessageStatusEnum.SENDING.getStatus() : MessageStatusEnum.SENDED.getStatus();
		chatMessage.setStatus(status);
		String messageContent = StringTools.cleanHtmlTag(chatMessage.getMessageContent());
		chatMessage.setMessageContent(messageContent);
		//更新会话
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		if(UserContactTypeEnum.GROUP==contactTypeEnum){
			chatSession.setLastMessage(tokenUserInfoDto.getNickName()+"："+messageContent);
		}
		chatSession.setLastReceiveTime(curTime);
		chatSessionMapper.updateBySessionId(chatSession,sessionId);
		//记录消息表
		chatMessage.setSendUserId(sendUserId);
		chatMessage.setSendUserNickName(tokenUserInfoDto.getNickName());
		chatMessage.setContactType(contactTypeEnum.getType());
		chatMessageMapper.insert(chatMessage);
		MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
		if(Constants.ROBOT_UID.equals(contactId)){
			SysSettingDto sysSettingDto = redisComponent.getSysSetting();
			TokenUserInfoDto robot = new TokenUserInfoDto();
			robot.setUserId(sysSettingDto.getRobotUid());
			robot.setNickName(sysSettingDto.getRobotNickName());
			ChatMessage robotChatMessage = new ChatMessage();
			robotChatMessage.setContactId(sendUserId);
			//这里可以对接AI实现聊天
			robotChatMessage.setMessageContent("我只是一个机器人无法识别消息");
			robotChatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
			saveMessage(robotChatMessage,robot);
		}else{
			messageHandler.sendMessage(messageSendDto);
		}
		return messageSendDto;
	}

	@Override
	public void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover) {
		ChatMessage chatMessage = chatMessageMapper.selectByMessageId(messageId);
		if(chatMessage == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(!chatMessage.getSendUserId().equals(userId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		SysSettingDto sysSettingDto = redisComponent.getSysSetting();
		String fileSuffix = StringTools.getFileSuffix(file.getOriginalFilename());
		if(!StringTools.isEmpty(fileSuffix)
				&&ArrayUtils.contains(Constants.IMAGE_SUFFIX_LIST,fileSuffix.toLowerCase())
				&&file.getSize()> sysSettingDto.getMaxImageSize()*Constants.FILE_SIZE_MB){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}else if(!StringTools.isEmpty(fileSuffix)
				&&ArrayUtils.contains(Constants.VIDEO_SUFFIX_LIST,fileSuffix.toLowerCase())
				&&file.getSize()> sysSettingDto.getMaxVideoSize()*Constants.FILE_SIZE_MB){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}else if(!StringTools.isEmpty(fileSuffix)
				&&ArrayUtils.contains(Constants.VIDEO_SUFFIX_LIST,fileSuffix.toLowerCase())
				&&ArrayUtils.contains(Constants.IMAGE_SUFFIX_LIST,fileSuffix.toLowerCase())
				&&file.getSize()> sysSettingDto.getMaxVideoSize()*Constants.FILE_SIZE_MB){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		String fileName = file.getOriginalFilename();
		String fileExtName = StringTools.getFileSuffix(fileName);
		String fileRealName = messageId+fileExtName;
		String month = DateUtil.format(new Date(chatMessage.getSendTime()),DateTimePatternEnum.YYYY_MM.getPattern());
		File folder = new File(appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+month);
		if(!folder.exists()){
			folder.mkdirs();
		}
		File uploadFile = new File(folder.getPath()+"/"+fileRealName);
        try {
            file.transferTo(uploadFile);
			cover.transferTo(new File(uploadFile.getPath()+Constants.COVER_IMAGE_SUFFIX));
        } catch (IOException e) {
            logger.error("上传文件失败",e);
			throw new BusinessException("文件上传失败！");
        }
		ChatMessage uploadInfo = new ChatMessage();
		uploadInfo.setStatus(MessageStatusEnum.SENDED.getStatus());
		ChatMessageQuery messageQuery = new ChatMessageQuery();
		messageQuery.setMessageId(messageId);
		messageQuery.setStatus(MessageStatusEnum.SENDING.getStatus());
		chatMessageMapper.updateByParam(uploadInfo,messageQuery);

		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setStatus(MessageStatusEnum.SENDED.getStatus());
		messageSendDto.setMessageType(MessageTypeEnum.FILE_UPLOAD.getType());
		messageSendDto.setMessageId(messageId);
		messageSendDto.setContactId(chatMessage.getContactId());
		messageHandler.sendMessage(messageSendDto);
    }

	@Override
	public File downloadFile(TokenUserInfoDto tokenUserInfoDto, Long messageId, Boolean showCover) {
		ChatMessage message = chatMessageMapper.selectByMessageId(messageId);
		String contactId = message.getContactId();
		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
		if(UserContactTypeEnum.USER == contactTypeEnum
		&& !tokenUserInfoDto.getUserId().equals(message.getContactId())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(UserContactTypeEnum.GROUP == contactTypeEnum){
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setUserId(tokenUserInfoDto.getUserId());
			userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());
			userContactQuery.setContactId(contactId);
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			Integer contactCount = userContactMapper.selectCount(userContactQuery);
			if(contactCount == 0){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
		}
		String month = DateUtil.format(new Date(message.getSendTime()),DateTimePatternEnum.YYYY_MM.getPattern());
		File folder = new File(appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+month);
		if(!folder.exists()){
			folder.mkdirs();
		}
		String fileName = message.getFileName();
		String fileExtName = StringTools.getFileSuffix(fileName);
		String fileRealName = messageId+fileExtName;
		if(showCover!=null && showCover){
			fileRealName = fileRealName+Constants.COVER_IMAGE_SUFFIX;
		}
		File file = new File(folder.getPath()+"/"+fileRealName);
		if(!file.exists()){
			logger.error("文件不存在！",messageId);
			throw new BusinessException(ResponseCodeEnum.CODE_602);
		}
		return file;
	}
}