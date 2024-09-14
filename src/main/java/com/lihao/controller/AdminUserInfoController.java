package com.lihao.controller;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.entity.po.UserInfo;
import com.lihao.entity.query.UserInfoQuery;
import com.lihao.entity.vo.PaginationResultVO;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.entity.vo.UserInfoVO;
import com.lihao.service.UserInfoService;
import com.lihao.utils.CopyTools;
import com.lihao.utils.StringTools;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.IOException;

/**
 * 用户信息表 Controller
 */
@RestController("adminUserInfoController")
@RequestMapping("/admin")
public class AdminUserInfoController extends ABaseController{

	@Resource
	private UserInfoService userInfoService;

	@RequestMapping("/loadUser")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO loadUser(@RequestBody UserInfoQuery userInfoQuery){
		userInfoQuery.setOrderBy("create_time desc");
		PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
		return getSuccessResponseVO(resultVO);
	}
	@RequestMapping("/updateUserStatus")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO updateUserStatus(@RequestParam @NotEmpty Integer status,
									   @RequestParam @NotEmpty String userId){
		userInfoService.updateUserStatus(status,userId);
		return getSuccessResponseVO(null);
	}
	@RequestMapping("/forceOffLine")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO forceOffLine(@RequestParam @NotEmpty String userId){
		userInfoService.forceOffLine(userId);
		return getSuccessResponseVO(null);
	}
}