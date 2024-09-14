package com.lihao.controller;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.config.AppConfig;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.dto.SysSettingDto;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.redis.RedisComponent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * 管理员系统设置信息表 Controller
 */
@RestController("adminSettingController")
@RequestMapping("/admin")
public class AdminSettingController extends ABaseController{

	@Resource
	private RedisComponent redisComponent;
	@Resource
	private AppConfig appConfig;

	@RequestMapping("/getSysSetting")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO getSysSetting(){
		SysSettingDto sysSettingDto = redisComponent.getSysSetting();
		return getSuccessResponseVO(sysSettingDto);
	}

	@RequestMapping("/saveSysSetting")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO saveSysSetting(@RequestPart(value = "sys") SysSettingDto sysSettingDto,
									 @RequestPart(value = "file")MultipartFile robotFile,
									 @RequestPart(value = "cover")MultipartFile robotCover) throws IOException {
		if(robotFile!=null){
			String baseFolder = appConfig.getProjectFolder()+ Constants.FILE_FOLDER_FILE;
			File targetFileFolder = new File(baseFolder+Constants.FILE_FOLDER_AVATAR_NAME);
			if(!targetFileFolder.exists()){
				targetFileFolder.mkdirs();
			}
			String filePath = targetFileFolder.getPath()+"/"+Constants.ROBOT_UID+Constants.IMAGE_SUFFIX;
			robotFile.transferTo(new File(filePath));
			robotCover.transferTo(new File(filePath+Constants.COVER_IMAGE_SUFFIX));
		}
		redisComponent.saveSysSetting(sysSettingDto);
		return getSuccessResponseVO(null);
	}

}