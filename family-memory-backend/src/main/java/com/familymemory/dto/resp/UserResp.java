package com.familymemory.dto.resp;

import lombok.Data;

/**
 * 用户响应
 */
@Data
public class UserResp {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private FamilySimpleResp currentFamily;

    @Data
    public static class FamilySimpleResp {
        private Long id;
        private String name;
    }
}
