package com.nortidart.selfmark.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nortidart.selfmark.auth.dto.AuthResponse;
import com.nortidart.selfmark.auth.dto.LoginRequest;
import com.nortidart.selfmark.auth.dto.RegisterRequest;
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
        String mobile = request.mobile().trim();
        if (userMapper.selectCount(Wrappers.<User>query().eq("mobile", mobile)) > 0) {
            throw new BusinessException(409, "手机号已注册");
        }
        User user = new User();
        user.setMobile(mobile);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setUsername(request.username().trim());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "手机号已注册");
        }
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>query().eq("mobile", request.mobile().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(401, "手机号或密码错误");
        }
        return response(user);
    }

    private AuthResponse response(User user) {
        String token = jwtUtil.issue(user.getId(), user.getMobile(), "USER");
        return AuthResponse.from(user, token);
    }
}
