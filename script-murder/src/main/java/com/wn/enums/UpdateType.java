package com.wn.enums;
import lombok.Getter;
import lombok.Setter;

/**
*sh上传文件类型的枚举类，方便根据文件类型进行不同的处理
 */
@Getter
public enum UpdateType {

    AVATAR("avatar", "avatars"), //用户图像
    COVER_IMAGE("coverImage", "coverImages");    //剧本杀封面

    private final String code;  // 上传文件类型的编码，比如avatar、cover
    private final String ossDir;// oss文件夹路径

    UpdateType(String code, String ossDir) {
        this.code = code;
        this.ossDir = ossDir;
    }

    public static UpdateType fromCode(String code) {
        for (UpdateType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的图片类型: " + code);
    }
}
