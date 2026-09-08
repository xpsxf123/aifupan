using System;
using System.Collections.Generic;

namespace ReviewAnalysis.enterprise
{
    /// <summary>
    /// 企业号汇总数据实体类
    /// </summary>
    public class EnterpriseGatherDataEntity
    {
        /// <summary>
        /// 视频ID
        /// </summary>
        public string videoId { get; set; }
        
        /// <summary>
        /// 主播secUid
        /// </summary>
        public string secUid { get; set; }
        
        /// <summary>
        /// 直播场次号（房间ID）
        /// </summary>
        public string batchNumber { get; set; }
        
        /// <summary>
        /// 数据类型
        /// </summary>
        public string dataType { get; set; }
        
        /// <summary>
        /// 数据JSON
        /// </summary>
        public string dataJson { get; set; }
        
        /// <summary>
        /// 创建时间
        /// </summary>
        public string createTime { get; set; }
        
        /// <summary>
        /// OSS路径
        /// </summary>
        public string ossPath { get; set; }

        #region 直播大屏核心数据字段（企业号线索平台）
        
        // 人均观看时长
        public string lp_screen_live_avg_watch_duration { get; set; }
        // 粉丝停留
        public string lp_screen_live_fans_avg_watch_duration { get; set; }
        // 全场景线索人数
        public string lp_screen_clue_uv { get; set; }
        // 线索转化率
        public string lp_screen_live_clue_convert_ratio { get; set; }
        // 私信人数
        public string lp_screen_msg_conversation_count { get; set; }
        // 私信长效转化
        public string lp_screen_longterm_msg_clue_uv { get; set; }
        // 实时在线人数
        public string lp_screen_live_user_realtime { get; set; }
        // 看过
        public string lp_screen_uv_with_preview { get; set; }
        // 表单提交人数
        public string lp_screen_card_clue_uv { get; set; }
        // 企业微信添加数
        public string lp_screen_ad_biz_wechat_add_count { get; set; }
        // 加微信成本
        public string lp_screen_ad_biz_wechat_cost { get; set; }
        // 表单提交数
        public string lp_screen_ad_form_count { get; set; }
        // 表单成本
        public string lp_screen_ad_form_cost { get; set; }
        // 场观
        public string lp_screen_live_watch_uv { get; set; }
        // 场观粉丝占比
        public string lp_screen_live_fans_watch_ratio { get; set; }
        // 曝光进入率
        public string lp_screen_live_enter_ratio { get; set; }
        // 粉丝
        public string lp_screen_live_fans_enter_rate_by_room { get; set; }
        // 最高在线人数
        public string lp_screen_live_max_watch_uv_by_minute { get; set; }
        // 平均在线人数
        public string lp_screen_live_avg_online_uv_by_room { get; set; }
        // 广告消耗
        public string lp_screen_live_stat_cost { get; set; }
        // 线索成本
        public string lp_screen_clue_cost { get; set; }
        // 小风车点击次数
        public string lp_screen_live_icon_click_count { get; set; }
        // 小风车点击率
        public string lp_screen_live_icon_click_rate { get; set; }
        // 涨粉量
        public string lp_screen_live_follow_uv { get; set; }
        // 关注率
        public string lp_screen_live_follow_ratio { get; set; }
        // 分享率
        public string lp_screen_live_share_ratio { get; set; }
        // 分享人数
        public string lp_screen_live_share_uv { get; set; }
        // 点赞率
        public string lp_screen_live_like_ratio { get; set; }
        // 点赞人数
        public string lp_screen_live_like_uv { get; set; }
        // 评论率
        public string lp_screen_live_comment_ratio { get; set; }
        // 评论人数
        public string lp_screen_live_comment_uv { get; set; }
        // 互动率
        public string lp_screen_live_interaction_ratio { get; set; }
        // 互动人数
        public string lp_screen_live_interaction_uv_count { get; set; }
        // 曝光次数
        public string lp_screen_live_show_count { get; set; }
        // 粉丝占比
        public string lp_screen_live_fans_show_rate_by_room { get; set; }
        // >1分钟观看人次
        public string lp_screen_live_watch_gt_1min_count { get; set; }
        // 观看次数
        public string lp_screen_live_watch_count { get; set; }
        // 分享次数
        public string lp_screen_live_share_count { get; set; }
        // 点赞次数
        public string lp_screen_live_like_count { get; set; }
        // 评论次数
        public string lp_screen_live_comment_count { get; set; }
        // 互动次数
        public string lp_screen_live_interaction_count { get; set; }
        // 加粉丝团人数
        public string lp_screen_live_fans_club_join_uv { get; set; }
        // 加团率
        public string lp_screen_live_fans_club_join_uv_ratio { get; set; }
        // 卡片点击次数
        public string lp_screen_live_clue_business_card_click_count { get; set; }
        // 卡片点击率
        public string lp_screen_live_clue_business_card_click_rate { get; set; }
        // 打赏次数
        public string lp_screen_live_gift_count { get; set; }
        // 打赏金额
        public string lp_screen_live_gift_amount { get; set; }
        // 不感兴趣次数
        public string live_dislike_count { get; set; }
        // 不感兴趣人数
        public string live_dislike_uv_by_room { get; set; }
        // 曝光人数
        public string lp_screen_live_show_uv { get; set; }
        // 直播时长
        public string live_duration { get; set; }
        // 卡片曝光次数
        public string lp_screen_live_clue_business_card_show_count { get; set; }
        
        #endregion
    }

    /// <summary>
    /// 企业号实时数据实体类
    /// </summary>
    public class EnterpriseRealTimeDataEntity
    {
        public string roomId { get; set; }
        public string videoId { get; set; }
        public string secUid { get; set; }
        public string dataType { get; set; }
        public string dataJson { get; set; }
        public string createTime { get; set; }
    }

    /// <summary>
    /// 企业号数据采集事件参数
    /// </summary>
    public class EnterpriseDataCollectEventArgs : EventArgs
    {
        public string batchNumber { get; set; }
        public string videoId { get; set; }
        public string secUid { get; set; }
        public string key { get; set; }
        public string dataJson { get; set; }
        public EnterpriseForm enterpriseForm { get; set; }
    }
}
