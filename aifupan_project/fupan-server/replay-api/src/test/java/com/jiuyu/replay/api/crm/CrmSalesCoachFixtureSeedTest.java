package com.jiuyu.replay.api.crm;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.api.ReplayApiApplication;
import com.jiuyu.replay.api.service.crm.CrmIntegrationService;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.entity.OrderEntity;
import com.jiuyu.replay.order.repository.dao.OrderDao;
import com.jiuyu.replay.power.bo.crm.CrmBatchAggregateQueryBo;
import com.jiuyu.replay.power.entity.SalesEntity;
import com.jiuyu.replay.power.entity.UserDetailsEntity;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.repository.dao.SalesDao;
import com.jiuyu.replay.power.repository.dao.UserDao;
import com.jiuyu.replay.power.repository.dao.UserDetailsDao;
import com.jiuyu.replay.power.vo.crm.CrmBatchAggregateQueryVo;
import com.jiuyu.replay.power.vo.crm.CrmOrderQueryByPhoneVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SpringBootTest(classes = ReplayApiApplication.class)
@ActiveProfiles("test")
public class CrmSalesCoachFixtureSeedTest {

    private static final String ENABLE_ENV = "CRM_REAL_DATA_SCAN";
    private static final int MAX_SIZE = 30;

    @Resource
    private UserDao userDao;
    @Resource
    private SalesDao salesDao;
    @Resource
    private UserDetailsDao userDetailsDao;
    @Resource
    private OrderDao orderDao;
    @Resource
    private CrmIntegrationService crmIntegrationService;

    @Test
    void queryRealDataBatchForSalesCoachFixture() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv(ENABLE_ENV)));

        List<RealFixtureItem> fixtures = new ArrayList<>(MAX_SIZE);

        List<UserDetailsEntity> userDetailsList = userDetailsDao.selectList(new LambdaQueryWrapper<UserDetailsEntity>()
                .isNotNull(UserDetailsEntity::getSaleId)
                .eq(UserDetailsEntity::getIsDeleted, 0)
                .last("limit 500"));

        for (UserDetailsEntity userDetails : userDetailsList) {
            if (userDetails == null || userDetails.getUserId() == null || userDetails.getSaleId() == null) {
                continue;
            }

            UserEntity user = userDao.selectById(userDetails.getUserId());
            if (user == null || user.getPhone() == null || user.getPhone().trim().isEmpty()) {
                continue;
            }
            if (!isUniquePhone(fixtures, user.getPhone().trim())) {
                continue;
            }

            OrderEntity order = orderDao.selectOne(new LambdaQueryWrapper<OrderEntity>()
                    .eq(OrderEntity::getUserId, user.getId())
                    .eq(OrderEntity::getCommodityType, 1)
                    .in(OrderEntity::getStatus, 1, 2)
                    .last("limit 1"));
            if (order == null) {
                continue;
            }

            SalesEntity sales = salesDao.selectById(userDetails.getSaleId());

            RealFixtureItem item = new RealFixtureItem();
            item.setPhone(user.getPhone().trim());
            item.setUserId(user.getId());
            item.setSalesId(sales == null ? null : sales.getId());
            item.setSalesPhone(sales == null ? null : sales.getPhone());
            item.setOrderId(order.getId());
            item.setOrderStatus(order.getStatus());
            fixtures.add(item);

            if (fixtures.size() >= MAX_SIZE) {
                break;
            }
        }

        Assertions.assertFalse(fixtures.isEmpty());
        Assertions.assertTrue(fixtures.size() <= MAX_SIZE);
        writeFixturesToLocalFiles(fixtures);
        System.out.println(JSON.toJSONString(fixtures));

        CrmBatchAggregateQueryBo batchBo = new CrmBatchAggregateQueryBo();
        batchBo.setPhones(fixtures.stream().map(RealFixtureItem::getPhone).toList());
        R<CrmBatchAggregateQueryVo> batchResult = crmIntegrationService.batchAggregateQuery(batchBo);
        Assertions.assertNotNull(batchResult);
        Assertions.assertEquals(0, batchResult.getCode());
        Assertions.assertNotNull(batchResult.getData());
        Assertions.assertNotNull(batchResult.getData().getCustomers());
        Assertions.assertFalse(batchResult.getData().getCustomers().isEmpty());

        R<CrmOrderQueryByPhoneVo> orderQueryResult = crmIntegrationService.queryOrderByPhone(fixtures.get(0).getPhone());
        Assertions.assertNotNull(orderQueryResult);
        Assertions.assertEquals(0, orderQueryResult.getCode());
        Assertions.assertNotNull(orderQueryResult.getData());
        Assertions.assertEquals(Boolean.TRUE, orderQueryResult.getData().getExists());
    }

    private boolean isUniquePhone(List<RealFixtureItem> fixtures, String phone) {
        return fixtures.stream().map(RealFixtureItem::getPhone).filter(Objects::nonNull).noneMatch(phone::equals);
    }

    private void writeFixturesToLocalFiles(List<RealFixtureItem> fixtures) throws Exception {
        Path outputDir = Paths.get("target", "crm-fixtures");
        Files.createDirectories(outputDir);

        Path jsonFile = outputDir.resolve("crm_real_fixtures.json");
        Files.writeString(jsonFile, JSON.toJSONString(fixtures), StandardCharsets.UTF_8);

        Path mdFile = outputDir.resolve("crm_real_fixtures.md");
        Files.writeString(mdFile, buildMarkdown(fixtures), StandardCharsets.UTF_8);
    }

    private String buildMarkdown(List<RealFixtureItem> fixtures) {
        StringBuilder sb = new StringBuilder();
        sb.append("# CRM Real Fixtures\n\n");
        sb.append("- generatedAt: ").append(OffsetDateTime.now()).append('\n');
        sb.append("- size: ").append(fixtures == null ? 0 : fixtures.size()).append("\n\n");
        sb.append("| phone | userId | salesPhone | salesId | orderId | orderStatus |\n");
        sb.append("|---|---:|---|---:|---:|---:|\n");

        if (fixtures != null) {
            for (RealFixtureItem item : fixtures) {
                sb.append('|').append(safe(item.getPhone()))
                        .append('|').append(safe(item.getUserId()))
                        .append('|').append(safe(item.getSalesPhone()))
                        .append('|').append(safe(item.getSalesId()))
                        .append('|').append(safe(item.getOrderId()))
                        .append('|').append(safe(item.getOrderStatus()))
                        .append("|\n");
            }
        }
        return sb.toString();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static class RealFixtureItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String phone;
        private Long userId;
        private Long salesId;
        private String salesPhone;
        private Long orderId;
        private Integer orderStatus;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getSalesId() {
            return salesId;
        }

        public void setSalesId(Long salesId) {
            this.salesId = salesId;
        }

        public String getSalesPhone() {
            return salesPhone;
        }

        public void setSalesPhone(String salesPhone) {
            this.salesPhone = salesPhone;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Integer getOrderStatus() {
            return orderStatus;
        }

        public void setOrderStatus(Integer orderStatus) {
            this.orderStatus = orderStatus;
        }
    }
}
