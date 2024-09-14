package com.lihao.controller;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.config.AppConfig;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.enums.AppUpdateFileTypeEnum;
import com.lihao.entity.po.AppUpdate;
import com.lihao.entity.vo.AppUpdateVO;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.service.AppUpdateService;
import com.lihao.utils.CopyTools;
import com.lihao.utils.StringTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;

/**
 * 检查更新
 */
@RestController("updateController")
@RequestMapping("/update")
public class UpdateController extends ABaseController{

	@Resource
	private AppUpdateService appUpdateService;
	@Resource
	private AppConfig appConfig;

	@RequestMapping("/checkVersion")
	@GlobalInterceptor
	public ResponseVO checkVersion(@RequestParam String appVersion,
								   @RequestParam String uid){
		if(StringTools.isEmpty(appVersion)){
			return getSuccessResponseVO(null);
		}
		AppUpdate appUpdate = appUpdateService.getLastestUpdate(appVersion,uid);
		if(appUpdate == null){
			return getSuccessResponseVO(null);
		}
		AppUpdateVO updateVO = CopyTools.copy(appUpdate, AppUpdateVO.class);
		if(AppUpdateFileTypeEnum.LOCAL.getType().equals(appUpdate.getFileType())){
			File file = new File(appConfig.getProjectFolder()+
					Constants.APP_UPDATE_FOLDER+appUpdate.getId()+
					Constants.APP_EXE_SUFFIX);
			updateVO.setSize(file.length());
		}else{
			updateVO.setSize(0L);
		}
		updateVO.setUpdateList(Arrays.asList(appUpdate.getUpdateDescArray()));
		String fileName = Constants.APP_NAME+appUpdate.getVersion()+Constants.APP_EXE_SUFFIX;
		updateVO.setFileName(fileName);
		return getSuccessResponseVO(updateVO);
	}



}