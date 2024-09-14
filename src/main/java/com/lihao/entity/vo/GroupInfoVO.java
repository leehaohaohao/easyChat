package com.lihao.entity.vo;

import com.lihao.entity.po.GroupInfo;
import com.lihao.entity.po.UserContact;

import java.util.List;

public class GroupInfoVO {
    private GroupInfo groupInfo;
    private List<UserContact> userContactList;

    public GroupInfo getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfoVO(GroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }

    public List<UserContact> getUserContactList() {
        return userContactList;
    }

    public void setUserContactList(List<UserContact> userContactList) {
        this.userContactList = userContactList;
    }
}
