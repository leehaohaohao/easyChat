package com.lihao.entity.po;

import com.lihao.entity.constants.Constants;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

public class UserRegister implements Serializable {
    @NotEmpty
    private String checkCodeKey;
    @NotEmpty
    @Email
    private String email;
    @NotEmpty
    @Pattern(regexp = Constants.REGEX_PASSWORD)
    private String password;
    @NotEmpty
    private String nickName;
    @NotEmpty
    private String checkCode;

    @Override
    public String toString() {
        return "UserRegister{" +
                "checkCodeKey='" + checkCodeKey + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", nickName='" + nickName + '\'' +
                ", checkCode='" + checkCode + '\'' +
                '}';
    }

    public String getCheckCodeKey() {
        return checkCodeKey;
    }

    public void setCheckCodeKey(String checkCodeKey) {
        this.checkCodeKey = checkCodeKey;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }
}
