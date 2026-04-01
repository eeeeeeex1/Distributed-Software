package com.example.seckill_demo.mapper;

import com.example.seckill_demo.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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
    
    /**
     * 扣减库存（乐观锁）
     * 返回受影响的行数，0表示库存不足
     */
    @Update("UPDATE product SET stock = stock - #{quantity}, update_time = NOW() " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    /**
     * 增加库存（回滚用）
     */
    @Update("UPDATE product SET stock = stock + #{quantity}, update_time = NOW() WHERE id = #{productId}")
    int addStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
    
    /**
     * 更新商品库存
     */
    @Update("UPDATE product SET stock = #{stock}, update_time = NOW() WHERE id = #{id}")
    int updateStock(@Param("id") Long id, @Param("stock") Integer stock);
}