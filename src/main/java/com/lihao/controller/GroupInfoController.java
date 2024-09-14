package com.lihao.controller;


import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.entity.enums.GroupStatusEnum;
import com.lihao.entity.enums.MessageTypeEnum;
import com.lihao.entity.enums.UserContactStatusEnum;
import com.lihao.entity.po.GroupInfo;
import com.lihao.entity.po.SaveGroup;
import com.lihao.entity.po.UserContact;
import com.lihao.entity.query.GroupInfoQuery;
import com.lihao.entity.query.UserContactQuery;
import com.lihao.entity.vo.GroupInfoVO;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.exception.BusinessException;
import com.lihao.service.GroupInfoService;
import com.lihao.service.UserContactService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

/**
 *  Controller
 */
@RestController("groupInfoController")
@RequestMapping("/group")
public class GroupInfoController extends ABaseController{

	@Resource
	private GroupInfoService groupInfoService;
	@Resource
	private UserContactService userContactService;

	@GlobalInterceptor
	@RequestMapping("/saveGroup")
	public ResponseVO saveGroup(HttpServletRequest request,
								@RequestPart("group") SaveGroup saveGroup ,
								@RequestPart("file") MultipartFile avatarFile,
								@RequestPart("cover") MultipartFile avatarCover) throws IOException {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);

		GroupInfo groupInfo = new GroupInfo();
		groupInfo.setGroupId(saveGroup.getGroupId());
		groupInfo.setGroupOwnerId(tokenUserInfoDto.getUserId());
		groupInfo.setGroupName(saveGroup.getGroupName());
		groupInfo.setGroupNotice(saveGroup.getGroupNotice());
		groupInfo.setJoinType(saveGroup.getJoinType());
		this.groupInfoService.saveCroup(groupInfo,avatarFile,avatarCover);

		return getSuccessResponseVO(null);
	}
	@GlobalInterceptor
	@RequestMapping("/loadMyGroup")
	public ResponseVO loadMyGroup(HttpServletRequest request){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		GroupInfoQuery groupInfoQuery = new GroupInfoQuery();
		groupInfoQuery.setGroupOwnerId(tokenUserInfoDto.getUserId());
		groupInfoQuery.setOrderBy("create_time desc");
		List<GroupInfo> groupInfoList = this.groupInfoService.findListByParam(groupInfoQuery);
		return getSuccessResponseVO(groupInfoList);
	}
	@GlobalInterceptor
	@RequestMapping("/getGroupInfo")
	public ResponseVO getGroupInfo(HttpServletRequest request , @RequestParam @NotEmpty String groupId){
		GroupInfo groupInfo = getGroupDetailCommon(request,groupId);
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		Integer memberCount = this.userContactService.findCountByParam(userContactQuery);
		groupInfo.setMemberCount(memberCount);
		return getSuccessResponseVO(groupInfo);
	}
	private GroupInfo getGroupDetailCommon(HttpServletRequest request , String groupId){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		UserContact userContact = this.userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(),groupId);
		if(userContact==null || !UserContactStatusEnum.FRIEND.getStatus().equals(userContact.getStatus())){
			throw new BusinessException("你不在群聊或者群聊不存在或已解散！");
		}
		GroupInfo groupInfo = this.groupInfoService.getGroupInfoByGroupId(groupId);
		if(groupInfo==null || GroupStatusEnum.DISSOLUTION.getStatus().equals(groupInfo.getStatus())){
			throw new BusinessException("群聊不存在或者已解散！");
		}
		return groupInfo;
	}
	@GlobalInterceptor
	@RequestMapping("/getGroupInfo4Chat")
	public ResponseVO getGroupInfo4Chat (HttpServletRequest request , @RequestParam @NotEmpty String groupId){
		GroupInfo groupInfo = getGroupDetailCommon(request,groupId);
		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setQueryUserInfo(true);
		userContactQuery.setOrderBy("create_time asc");
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		List<UserContact> userContactList = this.userContactService.findListByParam(userContactQuery);

		GroupInfoVO groupInfoVO = new GroupInfoVO();
		groupInfoVO.setGroupInfoVO(groupInfo);
		groupInfoVO.setUserContactList(userContactList);
		return getSuccessResponseVO(groupInfoVO);
	}


	@GlobalInterceptor
	@RequestMapping("/addOrRemoveGroupUser")
	public ResponseVO addOrRemoveGroupUser (HttpServletRequest request ,
											@RequestParam @NotEmpty String groupId,
											@NotEmpty String selectContacts,
											@NotNull Integer opType){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		groupInfoService.addOrRemoveGroupUser(tokenUserInfoDto,groupId,selectContacts,opType);
		return getSuccessResponseVO(null);
	}
	@GlobalInterceptor
	@RequestMapping("/leaveGroup")
	public ResponseVO leaveGroup (HttpServletRequest request ,
											@RequestParam @NotEmpty String groupId){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		groupInfoService.leaveGroup(tokenUserInfoDto.getUserId(),groupId, MessageTypeEnum.LEAVE_GROUP);
		return getSuccessResponseVO(null);
	}
	@GlobalInterceptor
	@RequestMapping("/dissolutionGroup")
	public ResponseVO dissolutionGroup (HttpServletRequest request ,
								  @RequestParam @NotEmpty String groupId){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		groupInfoService.dissolutionGroup(tokenUserInfoDto.getUserId(),groupId);
		return getSuccessResponseVO(null);
	}
}