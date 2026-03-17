package com.example.seckill_demo.mapper;

import com.example.seckill_demo.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {
    // 注册：插入用户
    @Insert("INSERT INTO t_user(username, password, phone) VALUES(#{username}, #{password}, #{phone})")
    int insertUser(User user);

    // 登录：根据用户名查询用户
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User selectByUsername(String username);
}