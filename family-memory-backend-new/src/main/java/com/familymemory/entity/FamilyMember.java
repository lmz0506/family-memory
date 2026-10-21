package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭成员关系实体，对应表：fm_family_member。
 */
@Data
@TableName("fm_family_member")
public class FamilyMember {
    /**
     * 主键ID，对应表：fm_family_member.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 家庭ID，来源表：fm_family.id。
     */
    private Long familyId;
    /**
     * 用户ID，来源表：fm_user.id。
     */
    private Long userId;
    /**
     * 家庭角色名称，自由文本，例如家庭成员、家庭管理员。
     */
    private String role;
    /**
     * 家庭内称呼。
     */
    private String displayName;
    /**
     * 成员主题色，peach桃色，sky天蓝，amber琥珀，emerald翠绿，可扩展。
     */
    private String color;
    /**
     * 管理员标志，0普通成员，1管理员。
     */
    private Integer isAdmin;
    /**
     * 加入家庭时间。
     */
    private LocalDateTime joinedAt;
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
