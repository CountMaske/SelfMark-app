package com.nortidart.selfmark.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nortidart.selfmark.auth.dto.AuthResponse;
import com.nortidart.selfmark.auth.dto.LoginRequest;
import com.nortidart.selfmark.auth.dto.RegisterRequest;
import com.nortidart.selfmark.auth.dto.UserResponse;
import com.nortidart.selfmark.auth.entity.User;
import com.nortidart.selfmark.auth.mapper.UserMapper;
import com.nortidart.selfmark.auth.security.JwtUtil;
import com.nortidart.selfmark.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userMapper.selectCount(Wrappers.<User>query().eq("username", username)) > 0) {
            throw new BusinessException(409, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "用户名已存在");
        }
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>query().eq("username", request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return response(user);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtUtil.issue(user.getId(), user.getUsername(), "USER"),
                UserResponse.from(user));
    }
}
