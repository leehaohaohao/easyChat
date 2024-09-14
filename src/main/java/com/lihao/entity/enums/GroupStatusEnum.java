package com.lihao.entity.enums;

public enum GroupStatusEnum {
    NORMAL(1,"正常"),
    DISSOLUTION(0,"解散");
    private Integer status;
    private String desc;

    GroupStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }
    public static GroupStatusEnum getByStatus(Integer status){
        for(GroupStatusEnum groupStatusEnum : GroupStatusEnum.values()){
            if(groupStatusEnum.getStatus().equals(status)){
                return groupStatusEnum;
            }
        }
        return null;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}
