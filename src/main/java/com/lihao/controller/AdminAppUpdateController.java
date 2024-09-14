package com.lihao.controller;

import java.io.IOException;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.po.AppUpdateInfo;
import com.lihao.entity.query.AppUpdateQuery;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.service.AppUpdateService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * app发布 Controller
 */
@RestController("appAppUpdateController")
@RequestMapping("/admin")
public class AdminAppUpdateController extends ABaseController{

	@Resource
	private AppUpdateService appUpdateService;

	@RequestMapping("/loadUpdateList")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO loadUpdateList(AppUpdateQuery query){
		query.setOrderBy("id desc");
		return getSuccessResponseVO(appUpdateService.findListByPage(query));
	}

	@RequestMapping("/saveUpdate")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO saveUpdate(@RequestPart(value = "app") AppUpdateInfo appUpdateInfo,
								 @RequestPart(value = "file") MultipartFile file) throws IOException {
		appUpdateService.saveUpdate(appUpdateInfo,file);
		return getSuccessResponseVO(null);
	}
	@RequestMapping("/delUpdate")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO delUpdate(@RequestParam @NotNull Integer id){
		appUpdateService.deleteAppUpdateById(id);
		return getSuccessResponseVO(null);
	}
	@RequestMapping("/postUpdate")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO postUpdate(@RequestParam @NotNull Integer id ,
								 @RequestParam @NotNull Integer status,
								 String grayscaleUid){
		appUpdateService.postUpdate(id,status,grayscaleUid);
		return getSuccessResponseVO(null);
	}

}