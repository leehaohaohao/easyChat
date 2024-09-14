package com.lihao.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lihao.utils.StringTools;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSendDto<T> implements Serializable {
    //信息id
    private Long messageId;
    //会话id
    private String sessionId;
    //发送人id
    private String sendUserId;
    //发送人昵称
    private String sendUserNickName;
    //联系人id
    private String contactId;
    //联系人昵称
    private String contactName;
    //消息内容
    private String messageContent;
    //最后的消息
    private String lastMessage;
    //消息类型
    private Integer messageType;
    //发送时间
    private Long sendTime;
    //联系人类型
    private Integer contactType;
    //拓展信息
    private T extendData;
    //消息状态 0：发送中 1：已发送 对于文件是异步上传用状态处理
    private Integer Status;
    //文件信息
    private String flieName;
    private Long fileSize;
    private Integer fileType;
    //群员
    private Integer memberCount;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSendUserId() {
        return sendUserId;
    }

    public void setSendUserId(String sendUserId) {
        this.sendUserId = sendUserId;
    }

    public String getSendUserNickName() {
        return sendUserNickName;
    }

    public void setSendUserNickName(String sendUserNickName) {
        this.sendUserNickName = sendUserNickName;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getLastMessage() {
        if(StringTools.isEmpty(lastMessage)){
            return messageContent;
        }
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public Long getSendTime() {
        return sendTime;
    }

    public void setSendTime(Long sendTime) {
        this.sendTime = sendTime;
    }

    public Integer getContactType() {
        return contactType;
    }

    public void setContactType(Integer contactType) {
        this.contactType = contactType;
    }

    public T getExtendData() {
        return extendData;
    }

    public void setExtendData(T extendData) {
        this.extendData = extendData;
    }

    public Integer getStatus() {
        return Status;
    }

    public void setStatus(Integer status) {
        Status = status;
    }

    public String getFlieName() {
        return flieName;
    }

    public void setFlieName(String flieName) {
        this.flieName = flieName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
}
