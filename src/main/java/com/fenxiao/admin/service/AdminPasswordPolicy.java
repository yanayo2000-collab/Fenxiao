package com.fenxiao.admin.service;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class AdminPasswordPolicy {
    private static final Set<String> BLOCKED=Set.of("password123!","admin123456!","qwerty123456!","1234567890Aa!");
    public void validate(String username,String password){
        if(password==null||password.length()<12||password.length()>128)throw new IllegalArgumentException("password must be 12-128 characters");
        if(username!=null&&password.toLowerCase().contains(username.toLowerCase()))throw new IllegalArgumentException("password must not contain username");
        int groups=0; if(password.chars().anyMatch(Character::isLowerCase))groups++; if(password.chars().anyMatch(Character::isUpperCase))groups++;
        if(password.chars().anyMatch(Character::isDigit))groups++; if(password.chars().anyMatch(c->!Character.isLetterOrDigit(c)))groups++;
        if(groups<3)throw new IllegalArgumentException("password must contain at least three character groups");
        if(BLOCKED.contains(password.toLowerCase()))throw new IllegalArgumentException("password is too common");
    }
}
