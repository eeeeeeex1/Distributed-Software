package com.example.seckill_demo.service;

import com.example.seckill_demo.entity.User;

public interface UserService {
    // 注册
    boolean register(User user);
    // 登录
    User login(String username, String password);
}