package com.lihao.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.swing.*;

import com.lihao.entity.config.AppConfig;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.dto.MessageSendDto;
import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.entity.enums.*;
import com.lihao.entity.po.*;
import com.lihao.entity.query.UserContactQuery;
import com.lihao.entity.vo.UserInfoVO;
import com.lihao.exception.BusinessException;
import com.lihao.mappers.UserContactMapper;
import com.lihao.mappers.UserInfoBeautyMapper;
import com.lihao.redis.RedisComponent;
import com.lihao.service.ChatSessionUserService;
import com.lihao.service.UserContactService;
import com.lihao.utils.CopyTools;
import com.lihao.webSocket.MessageHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import com.lihao.entity.query.UserInfoQuery;
import com.lihao.entity.vo.PaginationResultVO;
import com.lihao.entity.query.SimplePage;
import com.lihao.mappers.UserInfoMapper;
import com.lihao.service.UserInfoService;
import com.lihao.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


/**
 * 用户信息表 业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
	@Resource
	private UserInfoBeautyMapper<UserInfoBeauty,UserInfoQuery> userInfoBeautyMapper;
	@Resource
	private UserContactMapper<UserContact,UserContactQuery> userContactMapper;
	@Resource
	private AppConfig appConfig;
	@Resource
	private RedisComponent redisComponent;
	@Resource
	private UserContactService userContactService;
	@Resource
	private ChatSessionUserService chatSessionUserService;
	@Resource
	private MessageHandler messageHandler;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email获取对象
	 */
	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email修改
	 */
	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	@Override
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void register(UserRegister userRegister) {
		UserInfo userInfo = this.userInfoMapper.selectByEmail(userRegister.getEmail());
		if(userInfo!=null){
			throw new BusinessException("邮箱已经存在！");
		}
		//获取一个UUID
		String userId = StringTools.getUserId();
		//判断是不是靓号库里的id
		UserInfoBeauty beauty = this.userInfoBeautyMapper.selectByEmail(userRegister.getEmail());
		Boolean userBeauty = beauty!=null &&
				BeautyAccountStatusEnum.NO_USE.getStatus().equals(beauty.getStatus());
		if(userBeauty){
			userId = UserContactTypeEnum.USER.getPrefix()+beauty.getUserId();
		}
		Date curDate = new Date();
		userInfo = new UserInfo();
		userInfo.setUserId(userId);
		userInfo.setNickName(userRegister.getNickName());
		userInfo.setEmail(userRegister.getEmail());
		userInfo.setPassword(StringTools.encodeMd5(userRegister.getPassword()));
		userInfo.setCreateTime(curDate);
		userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
		userInfo.setLastOffTime(curDate.getTime());
		userInfo.setJoinType(JoinTypeEnum.APPLY.getType());
		this.userInfoMapper.insert(userInfo);
		if(userBeauty){
			UserInfoBeauty updateBeauty = new UserInfoBeauty();
			updateBeauty.setStatus(BeautyAccountStatusEnum.USEED.getStatus());
			this.userInfoBeautyMapper.updateByUserId(updateBeauty, beauty.getUserId());
		}

		userContactService.addContactRobot(userId);
	}

	@Override
	public UserInfoVO login(UserLogin userLogin) {
		UserInfo userInfo = this.userInfoMapper.selectByEmail(userLogin.getEmail());
		if(userInfo==null ||!userInfo.getPassword().equals(
				StringTools.encodeMd5(userLogin.getPassword()))
		){
			throw new BusinessException("账号或密码不对！");
		}
		if(userInfo.getStatus().equals(UserStatusEnum.DISABLE.getStatus())){
			throw new BusinessException("账号已禁用！");
		}
		//查询联系人信息
		UserContactQuery contactQuery = new UserContactQuery();
		contactQuery.setUserId(userInfo.getUserId());
		contactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		List<UserContact> contactList = userContactMapper.selectList(contactQuery);
		List<String> contactIdList = contactList.stream().map(item->item.getContactId()).collect(Collectors.toList());
		redisComponent.cleanUserContact(userInfo.getUserId());
		if(!contactIdList.isEmpty()){
			redisComponent.addUserContactBatch(userInfo.getUserId(),contactIdList);
		}
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(userInfo);
		Long lastHeartBeat = redisComponent.getHeartBeat(userInfo.getUserId());
		if(lastHeartBeat!=null){
			throw new BusinessException("此账号已经在别处登录，请退出后再登录！");
		}
		//保存登陆信息到redis中
		String token = StringTools.encodeMd5(
				tokenUserInfoDto.getUserId()+StringTools.getRandString(Constants.LENGTH_20)
		);
		tokenUserInfoDto.setToken(token);
		redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);
		//准备用户基本信息并返回
		UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
		userInfoVO.setToken(tokenUserInfoDto.getToken());
		userInfoVO.setAdmin(tokenUserInfoDto.getAdmin());

        return userInfoVO;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
		if(avatarFile!=null){
			String baseFoder = appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE;
			File targetFileFolder = new File(baseFoder+Constants.FILE_FOLDER_AVATAR_NAME);
			if(!targetFileFolder.exists()){
				targetFileFolder.mkdirs();
			}
			String filePath = targetFileFolder.getPath()+"/"+userInfo.getUserId()+Constants.IMAGE_SUFFIX;
			avatarFile.transferTo(new File(filePath));
			avatarCover.transferTo(new File(filePath+Constants.COVER_IMAGE_SUFFIX));
		}
		UserInfo dbInfo = this.userInfoMapper.selectByUserId(userInfo.getUserId());
		this.userInfoMapper.updateByUserId(userInfo, userInfo.getUserId());
		String contactNameUpdate = null;
		if(!dbInfo.getNickName().equals(userInfo.getNickName())){
			contactNameUpdate = userInfo.getNickName();
		}
		if(contactNameUpdate == null){
			return;
		}
		//更新token中的昵称
		TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenUserInfoDtoByUserId(userInfo.getUserId());
		tokenUserInfoDto.setNickName(contactNameUpdate);
		redisComponent.saveTokenUserInfoDto(tokenUserInfoDto);

		chatSessionUserService.updateRedundanceInfo(contactNameUpdate,userInfo.getUserId());

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateUserStatus(Integer status, String userId) {
		UserStatusEnum userStatusEnum = UserStatusEnum.getByStatus(status);
		if(userStatusEnum==null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserInfo userInfo = new UserInfo();
		userInfo.setStatus(userStatusEnum.getStatus());
		this.userInfoMapper.updateByUserId(userInfo,userId);
	}

	@Override
	public void forceOffLine(String userId) {
		//强制下线
		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setContactType(UserContactTypeEnum.USER.getType());
		messageSendDto.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType());
		messageSendDto.setContactId(userId);
		messageHandler.sendMessage(messageSendDto);
	}

	private TokenUserInfoDto getTokenUserInfoDto(UserInfo userInfo){
		TokenUserInfoDto tokenUserInfoDto = new TokenUserInfoDto();
		tokenUserInfoDto.setUserId(userInfo.getUserId());
		tokenUserInfoDto.setNickName(userInfo.getNickName());
		//判断邮箱是不是管理员邮箱
		String adminEamils = appConfig.getAdminEmails();
		String[] emailArray = adminEamils.split(",");
		if(!StringTools.isEmpty(adminEamils) &&
				ArrayUtils.contains(emailArray,userInfo.getEmail())){
			tokenUserInfoDto.setAdmin(true);
		}else {
			tokenUserInfoDto.setAdmin(false);
		}
		return tokenUserInfoDto;
	}
}