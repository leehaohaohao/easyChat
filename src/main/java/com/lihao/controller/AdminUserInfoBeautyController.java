package com.lihao.controller;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.po.UserInfoBeauty;
import com.lihao.entity.query.UserInfoBeautyQuery;
import com.lihao.entity.query.UserInfoQuery;
import com.lihao.entity.vo.PaginationResultVO;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.service.UserInfoBeautyService;
import com.lihao.service.UserInfoService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 管理员靓号信息表 Controller
 */
@RestController("adminUserInfoBeautyController")
@RequestMapping("/admin")
public class AdminUserInfoBeautyController extends ABaseController{

	@Resource
	private UserInfoBeautyService userInfoBeautyService;

	@RequestMapping("/loadBeautyAccountList")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO loadUser(@RequestBody UserInfoBeautyQuery query){
		query.setOrderBy("id desc");
		PaginationResultVO resultVO = userInfoBeautyService.findListByPage(query);
		return getSuccessResponseVO(resultVO);
	}
	@RequestMapping("/saveBeautyAccount")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO saveBeautyAccount(@RequestBody UserInfoBeauty beauty){
		userInfoBeautyService.saveAccount(beauty);
		return getSuccessResponseVO(null);
	}
	@RequestMapping("/delBeautyAccount")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO delBeautyAccount(@RequestParam @NotNull Integer id){
		userInfoBeautyService.deleteUserInfoBeautyById(id);
		return getSuccessResponseVO(null);
	}
}