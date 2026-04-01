package com.example.seckill_demo.mapper;

import com.example.seckill_demo.entity.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper {
    
    @Insert("INSERT INTO t_order(id, user_id, product_id, quantity, total_price, status, order_no, create_time) " +
            "VALUES(#{id}, #{userId}, #{productId}, #{quantity}, #{totalPrice}, #{status}, #{orderNo}, NOW())")
    int insertOrder(Order order);
    
    @Select("SELECT id, user_id, product_id, quantity, total_price, status, order_no, create_time, update_time " +
            "FROM t_order WHERE id = #{id}")
    Order selectById(Long id);
    
    @Select("SELECT id, user_id, product_id, quantity, total_price, status, order_no, create_time, update_time " +
            "FROM t_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> selectByUserId(Long userId);
    
    @Select("SELECT id, user_id, product_id, quantity, total_price, status, order_no, create_time, update_time " +
            "FROM t_order WHERE order_no = #{orderNo}")
    Order selectByOrderNo(String orderNo);
    
    @Update("UPDATE t_order SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    @Update("UPDATE product SET stock = stock - #{quantity}, update_time = NOW() " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
