package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.entity.User;
import com.example.seckill_demo.mapper.UserMapper;
import com.example.seckill_demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    // 密码加密工具
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean register(User user) {
        // 密码加密存储
        String encodedPwd = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPwd);
        return userMapper.insertUser(user) > 0;
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) return null;
        // 校验密码
        if (passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }
}