package com.jiuyu.replay.video.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.video.project.entity.VideoInfluencerInfoEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 达人信息表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Mapper
public interface VideoInfluencerInfoDao extends BaseMapper<VideoInfluencerInfoEntity> {

    /**
     * 批量查询达人信息
     * 根据多个(platformType, platformUserId)组合查询
     *
     * @param influencerConditions 达人查询条件列表
     * @return 达人信息实体列表
     */
    List<VideoInfluencerInfoEntity> batchQueryByPlatformAndUserIds(@Param("influencerConditions") List<InfluencerQueryCondition> influencerConditions);

    /**
     * 分页取当前租户关联的达人 id + 行业 id（游标分页，按 id 降序）。
     *
     * <p>达人来源 = 该租户「达人订阅」{@code tb_video_user_influencer_subscription}。<b>只按 tenantId 过滤，不过滤用户。</b>
     * 同一达人被租户内多个用户订阅（industry_id 可能不同）时，按 {@code influencer_id} 分组去重，
     * industry_id 取 {@code MAX(industry_id)}（单表聚合，避免自连接）。
     * 只出 id 两列（id-first，不扫大表 tb_video_influencer_info）；达人明细由调用方 {@code selectBatchIds} 内存装配。
     * 仅当带 platform/keyword/采集时间(inf.last_sync_time) 过滤时才 JOIN 达人库 inf。</p>
     *
     * @param tenantId         租户 id
     * @param cursor           上一页最后一条达人 id（首页 null）
     * @param platformTypes    平台类型集合（可空）
     * @param keyword          昵称/抖音号模糊关键词（可空）
     * @param influencerIds    达人 id 集合（可空）
     * @param collectStartTime 采集时间-起（= inf.last_sync_time，yyyy-MM-dd HH:mm:ss，可空）
     * @param collectEndTime   采集时间-止（= inf.last_sync_time，可空）
     * @param industryId       行业 id
     * @param collectAccountId 采集账号 id(谁采集的)
     * @param limit            取数条数（页大小 + 1 用于判断 hasMore）
     *
     * @return 达人行（influencerId + industryId，去重、按 influencerId 降序）
     */
    List<TenantInfluencerRow> pageTenantInfluencerIds(@Param("tenantId") Long tenantId,
                                                      @Param("cursor") Long cursor,
                                                      @Param("platformTypes") Collection<Byte> platformTypes,
                                                      @Param("keyword") String keyword,
                                                      @Param("influencerIds") Collection<Long> influencerIds,
                                                      @Param("collectStartTime") String collectStartTime,
                                                      @Param("collectEndTime") String collectEndTime,
                                                      @Param("industryId") Long industryId, @Param("collectAccountId") Long collectAccountId, @Param("limit") int limit);

    /**
     * 按平台用户 id（= secUid）/ 平台账号（抖音号等）集合反查达人主键 id。
     *
     * <p>供 AI Agent 短视频检索按达人的 platform_user_id / platform_account 维度过滤时，先解析出达人 id
     * （= tb_video_info.author_id 字符串化前的原值）。两个集合都给时为 AND（同时命中）；只给其一时按该维度。
     * 达人库为公共数据、不做租户隔离。</p>
     *
     * @param platformUserIds 平台用户 id 集合（可空）
     * @param platformAccounts 平台账号集合（可空）
     * @return 命中的达人主键 id 列表
     */
    List<Long> selectIdsByPlatform(@Param("platformUserIds") Collection<String> platformUserIds,
                                   @Param("platformAccounts") Collection<String> platformAccounts);

    /**
     * 取本租户已订阅的达人 id（tb_video_user_influencer_subscription，按租户过滤、跨租户内所有子账号去重）。
     *
     * <p>「订阅了达人 → 该达人名下全部作品可见」是租户可见范围的唯一依据，供统计 / 分析类接口先把
     * 调用方给的达人集合收窄到已订阅子集，再直查公共爬取库。</p>
     *
     * @param tenantId         租户 id
     * @param collectAccountId 采集账号 id（= 订阅记录 user_id，可空=租户下全部订阅）
     * @param influencerIds    达人 id 集合（可空=返回全部订阅达人；调用方须保证 ≤500）
     * @return 命中的已订阅达人 id 列表
     */
    List<Long> selectSubscribedInfluencerIds(@Param("tenantId") Long tenantId,
                                             @Param("collectAccountId") Long collectAccountId,
                                             @Param("influencerIds") Collection<Long> influencerIds);

    /**
     * 租户关联达人行（{@link #pageTenantInfluencerIds} 的投影结果）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class TenantInfluencerRow {
        /**
         * 达人 id（tb_video_influencer_info.id）
         */
        private Long influencerId;

        /**
         * 行业 id（tb_video_user_influencer_subscription.industry_id，可空）
         */
        private Long industryId;

        /**
         * 账号归属类型 0：自由账号 1：同行账号（来自基础设置）
         */
        private Integer accountType;
    }

    /**
     * 达人查询条件内部类
     */
    class InfluencerQueryCondition {
        private Byte platformType;
        private String platformUserId;

        public InfluencerQueryCondition() {
        }

        public InfluencerQueryCondition(Byte platformType, String platformUserId) {
            this.platformType = platformType;
            this.platformUserId = platformUserId;
        }

        public Byte getPlatformType() {
            return platformType;
        }

        public void setPlatformType(Byte platformType) {
            this.platformType = platformType;
        }

        public String getPlatformUserId() {
            return platformUserId;
        }

        public void setPlatformUserId(String platformUserId) {
            this.platformUserId = platformUserId;
        }
    }
}
