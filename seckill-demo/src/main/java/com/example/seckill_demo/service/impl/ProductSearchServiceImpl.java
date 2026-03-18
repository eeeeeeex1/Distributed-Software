package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.entity.Product;
import com.example.seckill_demo.entity.ProductDocument;
import com.example.seckill_demo.mapper.ProductMapper;
import com.example.seckill_demo.repository.ProductRepository;
import com.example.seckill_demo.service.ProductSearchService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品搜索服务实现类
 * 仅在elasticsearch.enabled=true时启用
 */
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ProductSearchServiceImpl implements ProductSearchService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public void syncProductToEs(Product product) {
        if (product == null) {
            return;
        }
        ProductDocument document = new ProductDocument();
        BeanUtils.copyProperties(product, document);
        productRepository.save(document);
    }

    @Override
    public void syncProductsToEs(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<ProductDocument> documents = products.stream()
                .map(product -> {
                    ProductDocument document = new ProductDocument();
                    BeanUtils.copyProperties(product, document);
                    return document;
                })
                .collect(Collectors.toList());
        productRepository.saveAll(documents);
    }

    @Override
    public void deleteProductFromEs(Long productId) {
        if (productId == null) {
            return;
        }
        productRepository.deleteById(productId);
    }

    @Override
    public Page<ProductDocument> searchProducts(String keyword, Pageable pageable) {
        // 使用Repository的搜索方法
        // 这里简化实现，使用findAll后再过滤
        // 实际项目中可以使用更复杂的查询
        Iterable<ProductDocument> allProducts = productRepository.findAll();
        List<ProductDocument> filteredProducts = new java.util.ArrayList<>();
        for (ProductDocument doc : allProducts) {
            if (doc.getName() != null && doc.getName().contains(keyword)) {
                filteredProducts.add(doc);
            } else if (doc.getDesc() != null && doc.getDesc().contains(keyword)) {
                filteredProducts.add(doc);
            }
        }
        
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredProducts.size());
        List<ProductDocument> pageContent = filteredProducts.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, filteredProducts.size());
    }

    @Override
    public void fullSyncProductsToEs() {
        // 清空索引
        productRepository.deleteAll();
        // 批量同步所有商品
        List<Product> products = productMapper.selectList();
        syncProductsToEs(products);
    }
}
