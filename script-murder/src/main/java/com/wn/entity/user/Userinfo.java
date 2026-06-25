package com.wn.entity.user;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息的JPA实现类，对应表名：sys_user
 */
@Entity()
@Table(name = "sys_user")
@Data
public class Userinfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)//设置自增
    @Column(name = "user_id")
    private Long Id;

    //用户名
    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    //密码，准备用md5加密
    @Column(name = "password", length = 100, nullable = false)
    private String password; // 存储加密后的密码

    //昵称
    @Column(name = "nickname", length = 50)
    private String nickname;

    //头像，存URL
    @Column(name = "avatar", length = 255)
    private String avatar;

    //手机号
    @Column(name = "phone", length = 20)
    private String phone;

    //邮箱
    @Column(name = "email", length = 100)
    private String email;

    //性别
    @Column(name = "gender", columnDefinition = "TINYINT default 0")
    private Integer gender; // 0未知 1男 2女

    //用户类型
    @Column(name = "user_type", columnDefinition = "TINYINT default 1")
    private Integer userType; // 1普通玩家 2主持人 3管理员

    //状态
    @Column(name = "status", columnDefinition = "TINYINT default 1")
    private Integer status; // 0禁用 1正常

    //余额
    @Column(name = "balance", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) default 0.00")
    private BigDecimal balance;//BigDecimal :是解决java浮点数精度不足的问题，确保能够正确传输余额，构建的时候要用字符串或者传输

    //游玩次数
    @Column(name = "play_count", columnDefinition = "INT default 0")
    private Integer playCount;

    //创建时间
    @CreationTimestamp
    @Column(name = "create_time", updatable = false, columnDefinition = "DATETIME default CURRENT_TIMESTAMP")
    private LocalDateTime createTime;

    //修改时间
    @UpdateTimestamp
    @Column(name = "update_time", columnDefinition = "DATETIME default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP")
    private LocalDateTime updateTime;

    //逻辑删除判断，默认0
    @Column(name = "deleted", columnDefinition = "TINYINT default 0")
    private Integer deleted = 0; // 0未删 1已删,默认为0
}
