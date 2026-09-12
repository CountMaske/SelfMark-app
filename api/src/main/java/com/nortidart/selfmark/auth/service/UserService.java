package com.nortidart.selfmark.auth.service;

import com.nortidart.selfmark.auth.dto.UserResponse;
import com.nortidart.selfmark.auth.entity.User;
import com.nortidart.selfmark.auth.mapper.UserMapper;
import com.nortidart.selfmark.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return UserResponse.from(user);
    }
}
