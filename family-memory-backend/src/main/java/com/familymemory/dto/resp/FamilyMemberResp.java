package com.familymemory.dto.resp;

import lombok.Data;

/**
 * 家庭成员响应
 */
@Data
public class FamilyMemberResp {

    private Long id;
    private Long userId;
    private String nickname;
    private String role;
    private String avatar;
    private String color;
    private Boolean isAdmin;
}
