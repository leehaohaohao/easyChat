package com.lihao.entity.enums;

import java.time.Year;

public enum AppUpdateFileTypeEnum {
    LOCAL(0,"本地"),
    OUTER_LINK(1,"外链");
    private Integer type;
    private String description;

    AppUpdateFileTypeEnum(Integer type, String description) {
        this.type = type;
        this.description = description;
    }

    public Integer getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
    public static AppUpdateFileTypeEnum getByType(Integer type){
        for(AppUpdateFileTypeEnum item:AppUpdateFileTypeEnum.values()){
            if(item.getType().equals(type)){
                return item;
            }
        }
        return null;
    }
}
