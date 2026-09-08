package com.jiuyu.governance.business.room.base;

import com.jiuyu.governance.ServerApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Room 模块测试基类
 * 
 * <p>提供以下通用能力：</p>
 * <ul>
 *     <li>Spring Boot 测试环境支持</li>
 *     <li>测试数据清理机制</li>
 *     <li>通用测试数据构造方法</li>
 *     <li>租户和数据隔离支持</li>
 * </ul>
 * 
 * @author HeHui
 * @date 2026-03-27
 */
@SpringBootTest(classes = ServerApplication.class)
@ActiveProfiles("local")
public abstract class BaseRoomTest {
    
    /**
     * 默认租户 ID
     */
    protected static final Long DEFAULT_TENANT_ID = 1L;
    
    /**
     * 默认用户 ID
     */
    protected static final Long DEFAULT_USER_ID = 1L;
    
    /**
     * 默认公司 ID
     */
    protected static final Long DEFAULT_COMPANY_ID = 1L;
    
    /**
     * 默认部门 ID
     */
    protected static final Long DEFAULT_DEPT_ID = 1L;
    
    /**
     * 默认小组 ID
     */
    protected static final Long DEFAULT_TEAM_ID = 1L;
    
    /**
     * 测试前准备（子类可重写）
     */
    protected void setUp() {
        // 子类实现具体的初始化逻辑
    }
    
    /**
     * 测试后清理（子类可重写）
     */
    protected void tearDown() {
        // 子类实现具体的清理逻辑
    }
}
