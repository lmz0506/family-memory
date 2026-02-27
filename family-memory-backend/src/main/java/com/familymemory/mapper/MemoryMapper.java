package com.familymemory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familymemory.entity.Memory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MemoryMapper extends BaseMapper<Memory> {

    @Select("SELECT DISTINCT memory_year FROM fm_memory WHERE family_id = #{familyId} AND deleted = 0 ORDER BY memory_year DESC")
    List<String> selectYearsByFamilyId(@Param("familyId") Long familyId);
}
