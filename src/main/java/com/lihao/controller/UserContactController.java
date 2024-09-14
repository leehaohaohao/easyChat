package com.lihao.controller;

import java.util.List;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.entity.dto.UserContactSearchResultDto;
import com.lihao.entity.enums.PageSize;
import com.lihao.entity.enums.ResponseCodeEnum;
import com.lihao.entity.enums.UserContactStatusEnum;
import com.lihao.entity.enums.UserContactTypeEnum;
import com.lihao.entity.po.UserContactApply;
import com.lihao.entity.po.UserInfo;
import com.lihao.entity.query.UserContactApplyQuery;
import com.lihao.entity.query.UserContactQuery;
import com.lihao.entity.po.UserContact;
import com.lihao.entity.vo.PaginationResultVO;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.entity.vo.UserInfoVO;
import com.lihao.exception.BusinessException;
import com.lihao.service.UserContactApplyService;
import com.lihao.service.UserContactService;
import com.lihao.service.UserInfoService;
import com.lihao.utils.CopyTools;
import jodd.util.ArraysUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 联系人 Controller
 */
@RestController("userContactController")
@RequestMapping("/contact")
public class UserContactController extends ABaseController{

	@Resource
	private UserContactService userContactService;
	@Resource
	private UserInfoService userInfoService;
	@Resource
	private UserContactApplyService userContactApplyService;
	/**
	 * 根据条件分页查询
	 */
	@RequestMapping("/search")
	@GlobalInterceptor
	public ResponseVO search(HttpServletRequest request, @RequestParam @NotEmpty String contactId){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		UserContactSearchResultDto resultDto = userContactService.searchContact(tokenUserInfoDto.getUserId(),contactId);

		return getSuccessResponseVO(resultDto);
	}
	@RequestMapping("/applyAdd")
	@GlobalInterceptor
	public ResponseVO applyAdd(HttpServletRequest request, @RequestParam @NotEmpty String contactId,
							   @RequestParam String applyInfo){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		Integer joinType = userContactApplyService.applyAdd(tokenUserInfoDto,contactId,applyInfo);
		return getSuccessResponseVO(joinType);
	}
	@RequestMapping("/loadApply")
	@GlobalInterceptor
	public ResponseVO loadApply(HttpServletRequest request, @RequestParam Integer pageNo){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		UserContactApplyQuery applyQuery = new UserContactApplyQuery();
		applyQuery.setOrderBy("last_apply_time desc");
		applyQuery.setReceiveUserId(tokenUserInfoDto.getUserId());
		applyQuery.setPageNo(pageNo);
		applyQuery.setPageSize(PageSize.SIZE15.getSize());
		applyQuery.setQueryContactInfo(true);
		PaginationResultVO resultVO = userContactApplyService.findListByPage(applyQuery);
		return getSuccessResponseVO(resultVO);
	}
	@RequestMapping("/dealWithApply")
	@GlobalInterceptor
	public ResponseVO dealWithApply(HttpServletRequest request,
									@RequestParam @NotNull Integer applyId,
									@RequestParam @NotNull Integer status){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		this.userContactApplyService.dealWithApply(tokenUserInfoDto.getUserId(),applyId,status);
		return getSuccessResponseVO(null);
	}

	@RequestMapping("/loadContact")
	@GlobalInterceptor
	public ResponseVO loadContact(HttpServletRequest request,
									@RequestParam @NotNull String contactType){

		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByName(contactType);
		if(contactTypeEnum == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		UserContactQuery contactQuery = new UserContactQuery();
		contactQuery.setUserId(tokenUserInfoDto.getUserId());
		contactQuery.setContactType(contactTypeEnum.getType());
		if(contactTypeEnum.USER == contactTypeEnum){
			contactQuery.setQueryUserInfo(true);
		}else if(contactTypeEnum.GROUP == contactTypeEnum){
			contactQuery.setQueryContactUserInfo(true);
			contactQuery.setExcludeMyGroup(true);
		}
		contactQuery.setOrderBy("last_update_time desc");
		contactQuery.setStatusArray(new Integer[]{
				UserContactStatusEnum.FRIEND.getStatus(),
				UserContactStatusEnum.DEL_BE.getStatus(),
				UserContactStatusEnum.BLACKLIST.getStatus()
		});
		List<UserContact> contactList = userContactService.findListByParam(contactQuery);
		return getSuccessResponseVO(contactList);
	}

	/**
	 * 获取联系人信息（不必须是好友）
	 * @param request
	 * @param contactId
	 * @return
	 */
	@RequestMapping("/getContactInfo")
	@GlobalInterceptor
	public ResponseVO getContactInfo(HttpServletRequest request,
									@RequestParam @NotNull String contactId){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
		UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
		userInfoVO.setContactStatus(UserContactStatusEnum.NOT_FRIEND.getStatus());
		UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(),contactId);
		if(userContact != null){
			userInfoVO.setContactStatus(UserContactStatusEnum.FRIEND.getStatus());
		}
		return getSuccessResponseVO(userInfoVO);
	}

	/**
	 * 获取联系人信息（必须是好友）
	 * @param request
	 * @param contactId
	 * @return
	 */
	@RequestMapping("/getContactUserInfo")
	@GlobalInterceptor
	public ResponseVO getContactUserInfo(HttpServletRequest request,
									 @RequestParam @NotNull String contactId){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(),contactId);
		if(userContact == null || !ArraysUtil.contains(new Integer[]{
				UserContactStatusEnum.FRIEND.getStatus(),
				UserContactStatusEnum.DEL_BE.getStatus(),
				UserContactStatusEnum.BLACKLIST_BE.getStatus()
		},userContact.getStatus())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
		UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
		return getSuccessResponseVO(userInfoVO);
	}
	@RequestMapping("/delContact")
	@GlobalInterceptor
	public ResponseVO delContact(HttpServletRequest request,
									@RequestParam @NotNull String contactId){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		userContactService.removeUserContact(tokenUserInfoDto.getUserId(),contactId,UserContactStatusEnum.DEL);
		return getSuccessResponseVO(null);
	}
	@RequestMapping("/addContact2BlackList")
	@GlobalInterceptor
	public ResponseVO addContact2BlackList(HttpServletRequest request,
								 @RequestParam @NotNull String contactId){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		userContactService.removeUserContact(tokenUserInfoDto.getUserId(),contactId,UserContactStatusEnum.BLACKLIST);
		return getSuccessResponseVO(null);
	}
}