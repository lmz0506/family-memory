package com.familymemory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familymemory.entity.FamilyMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FamilyMemberMapper extends BaseMapper<FamilyMember> {

    @Select("SELECT fm.*, u.avatar as user_avatar, u.nickname as user_nickname " +
            "FROM fm_family_member fm " +
            "LEFT JOIN fm_user u ON fm.user_id = u.id " +
            "WHERE fm.family_id = #{familyId} AND fm.deleted = 0")
    List<FamilyMember> selectByFamilyIdWithUser(@Param("familyId") Long familyId);
}
