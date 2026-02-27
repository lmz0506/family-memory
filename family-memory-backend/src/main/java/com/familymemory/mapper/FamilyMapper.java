package com.familymemory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familymemory.entity.Family;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FamilyMapper extends BaseMapper<Family> {
}
