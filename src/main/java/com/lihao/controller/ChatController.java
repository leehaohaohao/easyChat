package com.lihao.controller;

import com.lihao.annotation.GlobalInterceptor;
import com.lihao.entity.config.AppConfig;
import com.lihao.entity.constants.Constants;
import com.lihao.entity.dto.MessageSendDto;
import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.entity.enums.MessageTypeEnum;
import com.lihao.entity.enums.ResponseCodeEnum;
import com.lihao.entity.po.ChatMessage;
import com.lihao.entity.vo.ResponseVO;
import com.lihao.exception.BusinessException;
import com.lihao.redis.RedisComponent;
import com.lihao.service.ChatMessageService;
import com.lihao.service.ChatSessionUserService;
import com.lihao.utils.StringTools;
import okhttp3.Cache;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

@RestController
@RequestMapping("/chat")
public class ChatController extends ABaseController{
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    @Resource
    private ChatMessageService chatMessageService;
    @Resource
    private ChatSessionUserService chatSessionUserService;
    @Resource
    private AppConfig appConfig;
    @RequestMapping("/sendMessage")
    @GlobalInterceptor
    public ResponseVO sendMessage(HttpServletRequest request,
                                  @RequestParam @NotEmpty String contactId,
                                  @RequestParam @NotEmpty @Max(500) String messageContent,
                                  @RequestParam @NotNull Integer messageType,
                                  @RequestParam Long fileSize,
                                  @RequestParam String fileName,
                                  @RequestParam Integer fileType){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContactId(contactId);
        chatMessage.setMessageContent(messageContent);
        chatMessage.setFileSize(fileSize);
        chatMessage.setFileType(fileType);
        chatMessage.setFileName(fileName);
        chatMessage.setMessageType(messageType);
        MessageSendDto messageSendDto = chatMessageService.saveMessage(chatMessage,tokenUserInfoDto);
        return getSuccessResponseVO(messageSendDto);
    }
    @RequestMapping("/uploadFile")
    @GlobalInterceptor
    public ResponseVO uploadFile(HttpServletRequest request,
                                 @RequestPart @NotNull Long messageId,
                                 @RequestPart(value = "file") @NotNull MultipartFile file,
                                 @RequestPart(value = "cover") @NotNull MultipartFile cover){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        chatMessageService.saveMessageFile(tokenUserInfoDto.getUserId(),messageId,file,cover);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/downloadFile")
    @GlobalInterceptor
    public void downloadFile(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @RequestParam @NotEmpty String fileId,
                                   @RequestParam @NotNull Boolean showCover){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        OutputStream out = null;
        InputStream in = null;
        try{
            File file = null;
            if(StringTools.isNumber(fileId)){
                String avatarFolderName = Constants.FILE_FOLDER_FILE+Constants.FILE_FOLDER_AVATAR_NAME;
                String avatarPath = appConfig.getProjectFolder()+avatarFolderName+fileId+Constants.IMAGE_SUFFIX;
                if(showCover){
                    avatarPath = avatarPath+Constants.COVER_IMAGE_SUFFIX;
                }
                file = new File(avatarPath);
                if(!file.exists()){
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
            }else{
                file = chatMessageService.downloadFile(tokenUserInfoDto,Long.parseLong(fileId),showCover);
            }
            response.setContentType("application/x-msdownload;charset=UTF-8");
            response.setHeader("Content-Disposition","attachment");
            response.setContentLengthLong(file.length());
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len;
            while((len = in.read(byteData))!=-1){
                out.write(byteData,0,len);
            }
            out.flush();
        }catch(Exception e){
            logger.error("下载文件失败！",e);
        }finally{
            if(out!=null){
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("IO异常！",e);
                }
            }
            if(in!=null){
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("IO异常！",e);
                }
            }

        }
    }
}
