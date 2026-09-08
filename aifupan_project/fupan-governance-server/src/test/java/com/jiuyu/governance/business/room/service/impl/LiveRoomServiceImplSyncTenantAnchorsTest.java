package com.jiuyu.governance.business.room.service.impl;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.business.org.service.DeptService;
import com.jiuyu.governance.business.org.service.PositionService;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import com.jiuyu.governance.business.org.service.TeamService;
import com.jiuyu.governance.business.org.service.impl.ManagerConnectorProcessor;
import com.jiuyu.governance.business.room.mapper.LiveRoomScheduleAttributeMapper;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import com.jiuyu.governance.business.room.pojo.request.LiveRoomSyncTenantAnchorsRequest;
import com.jiuyu.governance.business.room.pojo.response.LiveRoomSyncResultResponse;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.openfeign.collect.LiveAnchorOpenService;
import com.jiuyu.governance.openfeign.replay.AnchorInfoService;
import com.jiuyu.governance.openfeign.replay.request.TenantAnchorQueryRequest;
import com.jiuyu.governance.openfeign.replay.response.TenantAnchorInfoResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LiveRoomServiceImpl 同步直播间单测
 *
 * @author HeHui
 * @date 2026-04-24
 */
class LiveRoomServiceImplSyncTenantAnchorsTest {

    @Test
    void testSyncTenantAnchors_CompanyNotExists() {
        SubCompanyService companyService = mock(SubCompanyService.class);
        when(companyService.getTenantFirst(1L)).thenReturn(Optional.empty());

        AnchorInfoService anchorInfoService = mock(AnchorInfoService.class);
        LiveRoomServiceImpl service = buildService(companyService, anchorInfoService, new TestDbBehavior());

        ApiResponse<LiveRoomSyncResultResponse> response = service.syncTenantAnchors(new LiveRoomSyncTenantAnchorsRequest(), 1L, 1L);
        assertEquals(SystemErrorCode.NOT_FOUND.getCode(), response.getCode());

        verifyNoInteractions(anchorInfoService);
    }

    @Test
    void testSyncTenantAnchors_RemoteEmpty() {
        SubCompanyService companyService = mock(SubCompanyService.class);
        when(companyService.getTenantFirst(1L)).thenReturn(Optional.of(1L));

        AnchorInfoService anchorInfoService = mock(AnchorInfoService.class);
        when(anchorInfoService.getTenantAnchors(any())).thenReturn(List.of());

        TestDbBehavior db = new TestDbBehavior();
        LiveRoomServiceImpl service = buildService(companyService, anchorInfoService, db);

        ApiResponse<LiveRoomSyncResultResponse> response = service.syncTenantAnchors(new LiveRoomSyncTenantAnchorsRequest(), 1L, 1L);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(0, response.getData().getTotal());
        assertEquals(0, response.getData().getCreated());
        assertEquals(0, response.getData().getUpdated());
        assertTrue(db.savedRooms.isEmpty());
        assertTrue(db.updatedRooms.isEmpty());
    }

    @Test
    void testSyncTenantAnchors_DeduplicateAndUpsert() {
        SubCompanyService companyService = mock(SubCompanyService.class);
        when(companyService.getTenantFirst(1L)).thenReturn(Optional.of(1L));

        TenantAnchorInfoResponse a1 = new TenantAnchorInfoResponse();
        a1.setPlatform(0);
        a1.setSecUid("a");
        a1.setAnchorName("A");
        a1.setAnchorNumber("a_no");
        a1.setSystemTradeId(55L);

        TenantAnchorInfoResponse a2Dup = new TenantAnchorInfoResponse();
        a2Dup.setPlatform(0);
        a2Dup.setSecUid("a");
        a2Dup.setAnchorName("A2");
        a2Dup.setAnchorNumber("a_no_2");
        a2Dup.setSystemTradeId(66L);

        TenantAnchorInfoResponse b = new TenantAnchorInfoResponse();
        b.setPlatform(0);
        b.setSecUid("b");
        b.setAnchorName("B");
        b.setAnchorNumber("b_no");
        b.setSystemTradeId(77L);

        AnchorInfoService anchorInfoService = mock(AnchorInfoService.class);
        when(anchorInfoService.getTenantAnchors(any())).thenReturn(List.of(a1, a2Dup, b));

        LiveRoom existing = new LiveRoom();
        existing.setId(10L);
        existing.setSecUid("a");
        existing.setTradeId(0L);

        TestDbBehavior db = new TestDbBehavior();
        db.existingRooms = List.of(existing);

        LiveRoomServiceImpl service = buildService(companyService, anchorInfoService, db);

        LiveRoomSyncTenantAnchorsRequest request = new LiveRoomSyncTenantAnchorsRequest();
        ApiResponse<LiveRoomSyncResultResponse> response = service.syncTenantAnchors(request, 1L, 1L);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(2, response.getData().getTotal());
        assertEquals(1, response.getData().getCreated());
        assertEquals(1, response.getData().getUpdated());

        ArgumentCaptor<TenantAnchorQueryRequest> captor = ArgumentCaptor.forClass(TenantAnchorQueryRequest.class);
        verify(anchorInfoService).getTenantAnchors(captor.capture());
        TenantAnchorQueryRequest query = captor.getValue();
        assertEquals(1L, query.getTenantId());
        assertEquals(0, query.getAccountType());
        assertEquals(List.of(0), query.getPlatformList());

        assertEquals(1, db.savedRooms.size());
        LiveRoom saved = db.savedRooms.get(0);
        assertEquals("b", saved.getSecUid());
        assertEquals(1L, saved.getCompanyId());
        assertEquals(0L, saved.getDeptId());
        assertEquals(0L, saved.getTeamId());
        assertEquals(77L, saved.getTradeId());

        assertEquals(1, db.updatedRooms.size());
        LiveRoom updated = db.updatedRooms.get(0);
        assertEquals(10L, updated.getId());
        assertEquals(55L, updated.getTradeId());
    }

    private static LiveRoomServiceImpl buildService(SubCompanyService companyService, AnchorInfoService anchorInfoService,
        TestDbBehavior db) {
        DeptService deptService = mock(DeptService.class);
        PositionService positionService = mock(PositionService.class);
        TeamService teamService = mock(TeamService.class);
        ManagerConnectorProcessor connectorProcessor = mock(ManagerConnectorProcessor.class);
        LiveAnchorOpenService liveAnchorOpenService = mock(LiveAnchorOpenService.class);
        LiveRoomScheduleAttributeMapper scheduleAttributeMapper = mock(LiveRoomScheduleAttributeMapper.class);

        return new TestableLiveRoomServiceImpl(companyService, deptService, positionService, teamService, connectorProcessor,
            liveAnchorOpenService, scheduleAttributeMapper, anchorInfoService, db);
    }

    private static class TestDbBehavior {
        private List<LiveRoom> existingRooms = List.of();
        private final List<LiveRoom> savedRooms = new ArrayList<>();
        private final List<LiveRoom> updatedRooms = new ArrayList<>();
    }

    private static class TestableLiveRoomServiceImpl extends LiveRoomServiceImpl {

        private final TestDbBehavior db;

        TestableLiveRoomServiceImpl(SubCompanyService companyService, DeptService deptService, PositionService positionService,
            TeamService teamService, ManagerConnectorProcessor connectorProcessor, LiveAnchorOpenService liveAnchorOpenService,
            LiveRoomScheduleAttributeMapper scheduleAttributeMapper, AnchorInfoService anchorInfoService, TestDbBehavior db) {
            super(companyService, deptService, positionService, teamService, connectorProcessor, liveAnchorOpenService,
                scheduleAttributeMapper, anchorInfoService);
            this.db = db;
        }

        @Override
        protected List<LiveRoom> listExistingRooms(long tenantId, int platform, List<String> secUids) {
            return db.existingRooms;
        }

        @Override
        protected void saveRooms(List<LiveRoom> rooms) {
            long baseId = 100L;
            for (int i = 0; i < rooms.size(); i++) {
                if (rooms.get(i).getId() == null) {
                    rooms.get(i).setId(baseId + i);
                }
            }
            db.savedRooms.addAll(rooms);
        }

        @Override
        protected void updateRooms(List<LiveRoom> rooms) {
            db.updatedRooms.addAll(rooms);
        }
    }
}

