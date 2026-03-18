package com.example.seckill_demo.mapper;

import com.example.seckill_demo.entity.Product;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 商品Mapper（注解版，无需XML）
 */
@Repository
public interface ProductMapper {

    /**
     * 根据ID查询商品
     */
    @Select("SELECT id, name, price, stock, `desc`, create_time, update_time FROM product WHERE id = #{id}")
    Product selectById(Long id);

    /**
     * 查询所有商品
     */
    @Select("SELECT id, name, price, stock, `desc`, create_time, update_time FROM product")
    java.util.List<Product> selectList();
}