package com.wn.utils.content;

public class UserContent {
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> usernameHolder = new ThreadLocal<>();
    public static void setUserIdHolder(Long userId){
        userIdHolder.set(userId);
    }
    public static void setUsernameHolder(String username){
        usernameHolder.set(username);
    }
    public static Long getUserIdHolder(){
        return userIdHolder.get();
    }
    public static String getUsernameHolder(){
        return usernameHolder.get();
    }

    /**
     * 清理ThreadLocal中的 缓存信息！
     */
    public static void clear(){
        userIdHolder.remove();
        usernameHolder.remove();
    }
}
