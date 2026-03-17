package com.example.seckill_demo;

import com.example.seckill_demo.entity.User;
import com.example.seckill_demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * 用户注册登录功能测试类
 * 直接运行@Test注解的方法即可测试对应功能
 */
@SpringBootTest  // 启动Spring容器，加载所有Bean
public class TestUserFunction {
    @LocalServerPort
    private int port;

    // 注入用户服务（自动装配）
    @Autowired
    private UserService userService;

    // 测试注册功能
    @Test
    public void testRegister() {
        // 1. 构造测试用户
        User testUser = new User();
        testUser.setUsername("test001");  // 测试用户名（确保数据库中不存在）
        testUser.setPassword("123456");   // 测试密码
        testUser.setPhone("13800138000");// 测试手机号

        // 2. 调用注册方法
        boolean isSuccess = userService.register(testUser);

        // 3. 输出测试结果
        if (isSuccess) {
            System.out.println("✅ 注册测试成功！用户：" + testUser.getUsername());
        } else {
            System.err.println("❌ 注册测试失败！可能是用户名已存在");
        }
    }

    // 测试登录功能（需先运行注册测试，确保用户存在）
    @Test
    public void testLogin() {
        // 1. 测试正确的账号密码
        String username = "test001";
        String correctPwd = "123456";
        String wrongPwd = "654321";

        // 2. 测试正确密码登录
        User loginUser1 = userService.login(username, correctPwd);
        if (loginUser1 != null) {
            System.out.println("✅ 正确密码登录成功！用户ID：" + loginUser1.getId() + "，用户名：" + loginUser1.getUsername());
        } else {
            System.err.println("❌ 正确密码登录失败！");
        }

        // 3. 测试错误密码登录（预期失败）
        User loginUser2 = userService.login(username, wrongPwd);
        if (loginUser2 == null) {
            System.out.println("✅ 错误密码登录验证成功（预期失败）！");
        } else {
            System.err.println("❌ 错误密码登录验证失败（不应该登录成功）！");
        }

        // 4. 测试不存在的用户名登录（预期失败）
        User loginUser3 = userService.login("non_exist_user", "123456");
        if (loginUser3 == null) {
            System.out.println("✅ 不存在用户名登录验证成功（预期失败）！");
        } else {
            System.err.println("❌ 不存在用户名登录验证失败（不应该登录成功）！");
        }
    }
}