package com.example.seckill_demo.repository;

import com.example.seckill_demo.entity.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 商品ElasticSearch仓库
 */
@Repository
public interface ProductRepository extends ElasticsearchRepository<ProductDocument, Long> {
}
