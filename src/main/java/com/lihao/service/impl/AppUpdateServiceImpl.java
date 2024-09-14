package com.lihao.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.lihao.entity.config.AppConfig;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.enums.AppUpdateFileTypeEnum;
import com.lihao.entity.enums.AppUpdateStatusEnum;
import com.lihao.entity.enums.ResponseCodeEnum;
import com.lihao.entity.po.AppUpdateInfo;
import com.lihao.exception.BusinessException;
import org.springframework.stereotype.Service;

import com.lihao.entity.enums.PageSize;
import com.lihao.entity.query.AppUpdateQuery;
import com.lihao.entity.po.AppUpdate;
import com.lihao.entity.vo.PaginationResultVO;
import com.lihao.entity.query.SimplePage;
import com.lihao.mappers.AppUpdateMapper;
import com.lihao.service.AppUpdateService;
import com.lihao.utils.StringTools;
import org.springframework.web.multipart.MultipartFile;


/**
 * app发布 业务接口实现
 */
@Service("appUpdateService")
public class AppUpdateServiceImpl implements AppUpdateService {

	@Resource
	private AppUpdateMapper<AppUpdate, AppUpdateQuery> appUpdateMapper;
	@Resource
	private AppConfig appConfig;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<AppUpdate> findListByParam(AppUpdateQuery param) {
		return this.appUpdateMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(AppUpdateQuery param) {
		return this.appUpdateMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<AppUpdate> findListByPage(AppUpdateQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<AppUpdate> list = this.findListByParam(param);
		PaginationResultVO<AppUpdate> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(AppUpdate bean) {
		return this.appUpdateMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<AppUpdate> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.appUpdateMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<AppUpdate> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.appUpdateMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(AppUpdate bean, AppUpdateQuery param) {
		StringTools.checkParam(param);
		return this.appUpdateMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(AppUpdateQuery param) {
		StringTools.checkParam(param);
		return this.appUpdateMapper.deleteByParam(param);
	}

	/**
	 * 根据Id获取对象
	 */
	@Override
	public AppUpdate getAppUpdateById(Integer id) {
		return this.appUpdateMapper.selectById(id);
	}

	/**
	 * 根据Id修改
	 */
	@Override
	public Integer updateAppUpdateById(AppUpdate bean, Integer id) {
		return this.appUpdateMapper.updateById(bean, id);
	}

	/**
	 * 根据Id删除
	 */
	@Override
	public Integer deleteAppUpdateById(Integer id) {
		AppUpdate dbInfo = this.getAppUpdateById(id);
		if(!AppUpdateStatusEnum.INIT.getStatus().equals(dbInfo.getStatus())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		return this.appUpdateMapper.deleteById(id);
	}

	/**
	 * 根据Version获取对象
	 */
	@Override
	public AppUpdate getAppUpdateByVersion(String version) {
		return this.appUpdateMapper.selectByVersion(version);
	}

	/**
	 * 根据Version修改
	 */
	@Override
	public Integer updateAppUpdateByVersion(AppUpdate bean, String version) {
		return this.appUpdateMapper.updateByVersion(bean, version);
	}

	/**
	 * 根据Version删除
	 */
	@Override
	public Integer deleteAppUpdateByVersion(String version) {
		return this.appUpdateMapper.deleteByVersion(version);
	}
	@Override
		public void saveUpdate(AppUpdateInfo appUpdateInfo, MultipartFile file) throws IOException {
			AppUpdateFileTypeEnum fileTypeEnum = AppUpdateFileTypeEnum.getByType(appUpdateInfo.getFileType());
			if(fileTypeEnum == null){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			if(appUpdateInfo.getId()!=null){
				AppUpdate dbInfo = this.getAppUpdateById(appUpdateInfo.getId());
				if(!AppUpdateStatusEnum.INIT.getStatus().equals(dbInfo.getStatus())){
					throw new BusinessException(ResponseCodeEnum.CODE_600);
				}
			}
			AppUpdateQuery updateQuery = new AppUpdateQuery();
			updateQuery.setOrderBy("id desc");
			updateQuery.setSimplePage(new SimplePage(0,1));
			List<AppUpdate> appUpdateList = appUpdateMapper.selectList(updateQuery);
			if(!appUpdateList.isEmpty()){
				AppUpdate lastest = appUpdateList.get(0);
				Long dbVersion = Long.parseLong(lastest.getVersion().replace(".",""));
				Long currentVersion = Long.parseLong(appUpdateInfo.getVersion().replace(".",""));
				if(appUpdateInfo.getId() ==null && currentVersion<=dbVersion){
					throw new BusinessException("当前版本必须大于历史版本！");
				}
				if(appUpdateInfo.getId()!=null && currentVersion>=dbVersion &&
				!appUpdateInfo.getId().equals(lastest.getId())){
					throw new BusinessException("当前版本必须大于历史版本！");
				}
				AppUpdate versionDb = appUpdateMapper.selectByVersion(appUpdateInfo.getVersion());
				if(appUpdateInfo.getId()!=null && versionDb!=null &&
						!versionDb.getId().equals(appUpdateInfo.getId())){
					throw new BusinessException("版本号已存在！");
				}
			}
			AppUpdate appUpdate = new AppUpdate();
			appUpdate.setId(appUpdateInfo.getId());
			appUpdate.setUpdateDesc(appUpdate.getUpdateDesc());
			appUpdate.setVersion(appUpdateInfo.getVersion());
			appUpdate.setOuterLink(appUpdateInfo.getOuterLink());
			appUpdate.setFileType(appUpdateInfo.getFileType());
	
			if(appUpdate.getId()==null){
				appUpdate.setCreateTime(new Date());
				appUpdate.setStatus(AppUpdateStatusEnum.INIT.getStatus());
				appUpdateMapper.insert(appUpdate);
			}else{
				appUpdate.setStatus(null);
				appUpdate.setGrayscaleUid(null);
				appUpdateMapper.updateById(appUpdate,appUpdate.getId());
			}
			if(file!=null){
				File folder = new File(appConfig.getProjectFolder()+ Constants.APP_UPDATE_FOLDER);
				if(!folder.exists()){
					folder.mkdirs();
				}
				file.transferTo(new File(folder.getPath()+"/"+appUpdate.getId()+Constants.APP_EXE_SUFFIX));
			}
		}

	@Override
	public void postUpdate(Integer id, Integer status, String grayscaleUid) {
		AppUpdateStatusEnum statusEnum = AppUpdateStatusEnum.getByStatus(status);
		if(statusEnum == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(AppUpdateStatusEnum.GRAYSCALE == statusEnum && StringTools.isEmpty(grayscaleUid)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(AppUpdateStatusEnum.GRAYSCALE != statusEnum){
			grayscaleUid ="";
		}
		AppUpdate appUpdate = new AppUpdate();
		appUpdate.setStatus(status);
		appUpdate.setGrayscaleUid(grayscaleUid);
		appUpdateMapper.updateById(appUpdate,id);
	}

	@Override
	public AppUpdate getLastestUpdate(String appVersion, String uid) {
		return appUpdateMapper.selectLatestUpdate(appVersion,uid);
	}
}