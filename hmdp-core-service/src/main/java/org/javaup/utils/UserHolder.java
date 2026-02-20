package org.javaup.utils;

import org.javaup.dto.UserDTO;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 用户持有者
 * @author: 阿星不是程序员
 **/
public class UserHolder {
    private static final ThreadLocal<UserDTO> TL = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        TL.set(user);
    }

    public static UserDTO getUser(){
        return TL.get();
    }

    public static void removeUser(){
        TL.remove();
    }
}
