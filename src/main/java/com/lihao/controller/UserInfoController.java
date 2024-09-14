package com.lihao.controller;

import java.io.IOException;
import java.util.List;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.entity.enums.UserContactStatusEnum;
import com.lihao.entity.po.UserContact;
import com.lihao.entity.query.UserInfoQuery;
import com.lihao.entity.po.UserInfo;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.entity.vo.UserInfoVO;
import com.lihao.service.UserInfoService;
import com.lihao.utils.CopyTools;
import com.lihao.utils.StringTools;
import com.lihao.webSocket.ChannelContextUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 用户信息表 Controller
 */
@RestController("userInfoController")
@RequestMapping("/userInfo")
public class UserInfoController extends ABaseController{

	@Resource
	private UserInfoService userInfoService;
	@Resource
	private ChannelContextUtils channelContextUtils;

	@RequestMapping("/getUserInfo")
	@GlobalInterceptor
	public ResponseVO getUserInfo(HttpServletRequest request){
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		UserInfo userInfo = userInfoService.getUserInfoByUserId(tokenUserInfoDto.getUserId());
		UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
		userInfoVO.setAdmin(tokenUserInfoDto.getAdmin());
		return getSuccessResponseVO(userInfoVO);
	}

	@RequestMapping("/saveUserInfo")
	@GlobalInterceptor
	public ResponseVO saveUserInfo(HttpServletRequest request,
								   @RequestPart UserInfo userInfo,
								   @RequestPart MultipartFile avatarFile ,
								   @RequestPart MultipartFile avatarCover) throws IOException {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		userInfo.setUserId(tokenUserInfoDto.getUserId());
		userInfo.setPassword(null);
		userInfo.setStatus(null);
		userInfo.setCreateTime(null);
		userInfo.setLastLoginTime(null);
		this.userInfoService.updateUserInfo(userInfo,avatarFile,avatarCover);
		return getUserInfo(request);
	}
	@RequestMapping("/updatePassword")
	@GlobalInterceptor
	public ResponseVO updatePassword(HttpServletRequest request,
									 @RequestParam @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD)
									 String password) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		UserInfo userInfo = new UserInfo();
		userInfo.setPassword(StringTools.encodeMd5(password));
		this.userInfoService.updateUserInfoByUserId(userInfo,tokenUserInfoDto.getUserId());
		//强制退出，重新登陆
		channelContextUtils.closeContext(tokenUserInfoDto.getUserId());
		return getSuccessResponseVO(null);
	}

	@RequestMapping("/logout")
	@GlobalInterceptor
	public ResponseVO logout(HttpServletRequest request) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		//退出登陆，关闭WS连接
		channelContextUtils.closeContext(tokenUserInfoDto.getUserId());
		return getSuccessResponseVO(null);
	}
}