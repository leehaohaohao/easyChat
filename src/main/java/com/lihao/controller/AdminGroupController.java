package com.lihao.controller;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.enums.ResponseCodeEnum;
import com.lihao.entity.po.GroupInfo;
import com.lihao.entity.query.GroupInfoQuery;
import com.lihao.entity.vo.PaginationResultVO;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.exception.BusinessException;
import com.lihao.service.GroupInfoService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;

/**
 * 管理员群组信息表 Controller
 */
@RestController("adminGroupController")
@RequestMapping("/admin")
public class AdminGroupController extends ABaseController{

	@Resource
	private GroupInfoService groupInfoService;

	@RequestMapping("/loadGroup")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO loadGroup(@RequestBody GroupInfoQuery query){
		query.setOrderBy("create_time desc");
		query.setQueryMemberCount(true);
		query.setQueryGroupOwnerName(true);
		PaginationResultVO resultVO = groupInfoService.findListByPage(query);
		return getSuccessResponseVO(resultVO);
	}

	@RequestMapping("/dissolutionGroup")
	@GlobalInterceptor(checkAdmin = true)
	public ResponseVO dissolutionGroup(@RequestParam @NotEmpty String groupId){
		GroupInfo groupInfo = groupInfoService.getGroupInfoByGroupId(groupId);
		if(groupInfo == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		groupInfoService.dissolutionGroup(groupInfo.getGroupOwnerId(),groupId);
		return getSuccessResponseVO(null);
	}

}