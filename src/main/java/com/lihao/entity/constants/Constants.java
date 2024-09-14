package com.lihao.entity.constants;

import com.lihao.entity.enums.UserContactTypeEnum;

public class Constants {
    public static final Integer REDIS_KEY_EXPIRES_HEART_BEAT = 6;
    public static final String REDIS_KEY_CHECK_CODE = "easychat:checkcode:";
    public static final String REDIS_KEY_WS_USER_HEART_BEAT = "easychat:ws:user:heartbeat";
    public static final String REDIS_KEY_WS_TOKEN = "easychat:ws:token:";
    public static final String REDIS_KEY_WS_TOKEN_USERID = "easychat:ws:token:userid";
    public static final Integer REDIS_TIME_1MIN = 60;
    public static final Integer REDIS_KEY_EXPIRES_DAY = REDIS_TIME_1MIN*60*24;
    //token失效时间
    public static final Integer REDIS_KEY_TOKEN_EXPIRES = REDIS_KEY_EXPIRES_DAY * 2;
    public static final Integer LENGTH_11=11;
    public static final Integer LENGTH_20=20;
    public static final String ROBOT_UID = UserContactTypeEnum.USER.getPrefix()+"robot";
    public static final String REDIS_KEY_SYS_SETTING = "easychat:syssetting:";
    public static final String FILE_FOLDER_FILE = "/file/";
    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";
    public static final String IMAGE_SUFFIX = ".png";
    public static final String COVER_IMAGE_SUFFIX = "_cover.png";
    public static final String APPLY_INFO_TEMPLATE = "我是%s";
    public static final String REGEX_PASSWORD = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,18}$";
    public static final String APP_UPDATE_FOLDER = "/app/";
    public static final String APP_EXE_SUFFIX = ".exe";
    public static final String APP_NAME = "EasyChatSetup.";
    //用户联系人列表
    public static final String REDIS_KEY_USER_CONTACT = "easychat:ws:user:contact:";
    public static final Long MILLISSECONDS_3DAYS_AGO = 3*24*60*1000L;
    public static final String[] IMAGE_SUFFIX_LIST = new String[]{".jpeg",".png",".jpg",".png",".gif",".bmp",".webp"};
    public static final String[] VIDEO_SUFFIX_LIST = new String[]{".mp4",".avi",".rmvb",".mkv",".mov"};
    public static final Long FILE_SIZE_MB = 1024*1024L;
    public static final Integer ZERO = 0;
    public static final Integer ONE = 1;
}
