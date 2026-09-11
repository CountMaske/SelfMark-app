package com.nortidart.selfmark.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nortidart.selfmark.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> { }
