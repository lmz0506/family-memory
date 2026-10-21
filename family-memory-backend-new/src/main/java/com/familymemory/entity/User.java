package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号实体，对应表：fm_user。
 */
@Data
@TableName("fm_user")
public class User {
    /**
     * 主键ID，对应表：fm_user.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 登录用户名，家庭记忆应用内唯一。
     */
    private String username;
    /**
     * 登录密码的BCrypt摘要，不保存明文。
     */
    private String password;
    /**
     * 用户显示昵称。
     */
    private String nickname;
    /**
     * 头像图片地址。
     */
    private String avatar;
    /**
     * 手机号码。
     */
    private String phone;
    /**
     * 电子邮箱地址。
     */
    private String email;
    /**
     * 账号状态，1正常，0停用。
     */
    private Integer status;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 最近更新时间。
     */
    private LocalDateTime updatedAt;
    /**
     * 删除标志，0有效，1已删除。
     */
    @TableLogic
    private Integer deleted;
}
