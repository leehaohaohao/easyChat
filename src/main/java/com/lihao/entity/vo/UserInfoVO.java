package com.lihao.entity.vo;

import java.io.Serializable;

public class UserInfoVO implements Serializable {
    private static final long serialVersionUID = -720784055540385571L;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 用户昵称
     */
    private String nickName;
    /**
     * 性别 0:女  1:男
     */
    private Integer sex;
    private Integer joinType;
    /**
     * 个性签名
     */
    private String personalSignature;
    private String areaCode;
    private String areaName;
    private String token;
    private Boolean admin;
    private Integer contactStatus;

    @Override
    public String toString() {
        return "UserInfoVO{" +
                "userId='" + userId + '\'' +
                ", nickName='" + nickName + '\'' +
                ", sex=" + sex +
                ", joinType=" + joinType +
                ", personalSignature='" + personalSignature + '\'' +
                ", areaCode='" + areaCode + '\'' +
                ", areaName='" + areaName + '\'' +
                ", token='" + token + '\'' +
                ", admin=" + admin +
                ", contactStatus=" + contactStatus +
                '}';
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public Integer getJoinType() {
        return joinType;
    }

    public void setJoinType(Integer joinType) {
        this.joinType = joinType;
    }

    public String getPersonalSignature() {
        return personalSignature;
    }

    public void setPersonalSignature(String personalSignature) {
        this.personalSignature = personalSignature;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public Integer getContactStatus() {
        return contactStatus;
    }

    public void setContactStatus(Integer contactStatus) {
        this.contactStatus = contactStatus;
    }
}
