package com.jiuyu.replay.api.listener;

import com.jiuyu.replay.api.bo.crm.CrmOrderEventPushBo;
import com.jiuyu.replay.api.service.crm.CrmOrderEventPushService;
import com.jiuyu.replay.api.vo.crm.CrmOrderEventPushResultVo;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.event.CrmOrderChangedEvent;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.producer.SalesProducer;
import com.jiuyu.replay.power.vo.UserVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
@AllArgsConstructor
@Slf4j
public class CrmOrderChangedEventListener {

    private static final String LOG_PREFIX = "[CRM-ORDER-EVENT]";
    private static final DateTimeFormatter ISO_OFFSET_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final UserBll userBll;
    private final SalesProducer salesProducer;
    private final OrderBll orderBll;
    private final CrmOrderEventPushService crmOrderEventPushService;

    @EventListener
    public void handleCrmOrderChangedEvent(CrmOrderChangedEvent event) {
        try {
            UserVo user = ResultUtil.getResult(userBll.getById(event.getUserId()));
            if (user == null || !StringUtils.hasText(user.getPhone())) {
                log.warn("{} skip push, customer missing phone, userId={}, orderId={}",
                        LOG_PREFIX, event.getUserId(), event.getOrderId());
                return;
            }

            OrderInfoVo order = ResultUtil.getResult(orderBll.info(event.getOrderId()));
            if (order == null) {
                log.warn("{} skip push, order missing, userId={}, orderId={}",
                        LOG_PREFIX, event.getUserId(), event.getOrderId());
                return;
            }

            SalesInfoVo sales = salesProducer.getByUserId(event.getUserId());
            CrmOrderEventPushBo pushBo = buildPushBo(event, user, sales, order);
            CrmOrderEventPushResultVo result = crmOrderEventPushService.pushOrderEvent(pushBo);
            log.info("{} push success, eventId={}, orderId={}, accepted={}, dedup={}",
                    LOG_PREFIX,
                    pushBo.getEventId(),
                    event.getOrderId(),
                    result == null ? null : result.getAccepted(),
                    result == null ? null : result.getDedup());
        } catch (Exception ex) {
            log.error("{} push failed, userId={}, orderId={}", LOG_PREFIX, event.getUserId(), event.getOrderId(), ex);
        }
    }

    private CrmOrderEventPushBo buildPushBo(CrmOrderChangedEvent event,
                                            UserVo user,
                                            SalesInfoVo sales,
                                            OrderInfoVo order) {
        Date occurredAt = event.getOccurredAt() == null ? new Date() : event.getOccurredAt();

        CrmOrderEventPushBo bo = new CrmOrderEventPushBo();
        bo.setEventId("order_evt_%d_%d".formatted(event.getOrderId(), occurredAt.getTime()));
        bo.setEventType("PAY_SUCCESS");
        bo.setOccurredAt(formatIsoOffset(occurredAt));
        bo.setCustomer(new CrmOrderEventPushBo.Customer(user.getPhone().trim(), user.getId()));
        bo.setSales(new CrmOrderEventPushBo.Sales(resolveSalesPhone(event, sales), resolveSalesId(event, sales)));
        bo.setOrder(new CrmOrderEventPushBo.OrderPayload(
                order.getId(),
                resolveOrderType(event, order),
                resolveCommodityType(order),
                Integer.valueOf(1).equals(order.getTrialOrder()),
                resolveStatus(order),
                formatIsoOffset(order.getStartDate()),
                formatIsoOffset(order.getEndDate()),
                order.getTotalPrice()
        ));
        return bo;
    }

    private String resolveOrderType(CrmOrderChangedEvent event, OrderInfoVo order) {
        if (order == null) {
            return normalizeEventType(event == null ? null : event.getEventType());
        }
        if (Integer.valueOf(1).equals(order.getOrderType())) {
            return "UPGRADE";
        }
        if (Integer.valueOf(3).equals(order.getOrderType())) {
            return "RENEWAL";
        }
        if (Integer.valueOf(4).equals(order.getOrderType()) || Integer.valueOf(0).equals(order.getCommodityType())) {
            return "INCREMENT";
        }
        if (Integer.valueOf(0).equals(order.getOrderType())) {
            return "FREE";
        }
        if (Integer.valueOf(5).equals(order.getOrderType())
                || Integer.valueOf(7).equals(order.getOrderType())
                || Integer.valueOf(2).equals(order.getCommodityType())
                || Integer.valueOf(3).equals(order.getCommodityType())) {
            return "ACTIVITY";
        }
        return normalizeEventType(event == null ? null : event.getEventType());
    }

    private String resolveCommodityType(OrderInfoVo order) {
        if (order == null) {
            return "VERSION";
        }
        if (Integer.valueOf(0).equals(order.getCommodityType())) {
            return "INCREMENT";
        }
        return "VERSION";
    }

    private String resolveStatus(OrderInfoVo order) {
        if (order == null || order.getStatus() == null) {
            return "PAID";
        }
        return switch (order.getStatus()) {
            case 2 -> "ACTIVE";
            case 3 -> "EXPIRED";
            case 4 -> "REFUNDED";
            case 5, 6, 7, 8 -> "CANCELED";
            default -> "PAID";
        };
    }

    private Long resolveSalesId(CrmOrderChangedEvent event, SalesInfoVo sales) {
        if (sales != null && sales.getId() != null) {
            return sales.getId();
        }
        return event == null ? null : event.getSalesId();
    }

    private String resolveSalesPhone(CrmOrderChangedEvent event, SalesInfoVo sales) {
        if (sales != null && StringUtils.hasText(sales.getPhone())) {
            return sales.getPhone().trim();
        }
        return event != null && StringUtils.hasText(event.getSalesPhone()) ? event.getSalesPhone().trim() : null;
    }

    private String normalizeEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return "PAY_SUCCESS";
        }
        String normalized = eventType.trim();
        return switch (normalized) {
            case "PAY_SUCCESS", "UPGRADE", "RENEWAL", "INCREMENT", "FREE", "ACTIVITY" -> normalized;
            default -> "PAY_SUCCESS";
        };
    }

    private String formatIsoOffset(Date value) {
        if (value == null) {
            return null;
        }
        return value.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(ISO_OFFSET_FORMATTER);
    }
}
