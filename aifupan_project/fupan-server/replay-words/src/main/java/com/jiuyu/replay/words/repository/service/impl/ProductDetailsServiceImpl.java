package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.ProductDetailsEntity;
import com.jiuyu.replay.words.repository.dao.ProductDetailsDao;
import com.jiuyu.replay.words.repository.service.ProductDetailsService;
import org.springframework.stereotype.Service;

/**
 * 商品表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-19
 */
@Service("productDetailsService")
public class ProductDetailsServiceImpl extends ServiceImpl<ProductDetailsDao, ProductDetailsEntity> implements ProductDetailsService {

}
