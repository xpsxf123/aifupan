using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.shortVideo
{
    /// <summary>
    /// 达人信息表实体类
    /// 对应数据库表: tb_video_influencer_info
    /// </summary>
    public class VideoInfluencerInfoVo
    {

        /// <summary>
        /// 主键ID（雪花ID）
        /// 对应字段: id
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 平台类型: 1-抖音, 2-快手, 3-视频号
        /// 对应字段: platform_type 
        /// </summary>
        public int? platformType { get; set; }

        /// <summary>
        /// 平台用户ID 
        /// 对应字段: platform_user_id 
        /// </summary>
        public string platformUserId { get; set; }

        /// <summary>
        /// 平台账号（抖音号/快手号/视频号）
        /// 对应字段: platform_account
        /// </summary>
        public string platformAccount { get; set; }

        /// <summary>
        /// 昵称 
        /// 对应字段: nickname 
        /// </summary>
        public string nickname { get; set; }

        /// <summary>
        /// 头像URL 
        /// 对应字段: avatar 
        /// </summary>
        public string avatar { get; set; }

        /// <summary>
        /// 个人简介 
        /// 对应字段: description 
        /// </summary>
        public string description { get; set; }

        /// <summary>
        /// 粉丝数 
        /// 对应字段: followers_count
        /// </summary>
        public long followersCount { get; set; } = 0;

        /// <summary>
        /// 关注数 
        /// 对应字段: following_count
        /// </summary>
        public long followingCount { get; set; } = 0;

        /// <summary>
        /// 作品数 
        /// 对应字段: video_count
        /// </summary>
        public int videoCount { get; set; } = 0;

        /// <summary>
        /// 获赞数
        /// 对应字段: like_count 
        /// </summary>
        public long likeCount { get; set; } = 0;

        /// <summary>
        /// 认证状态: 0-未认证, 1-个人认证, 2-企业/机构认证, 3-政府/官方组织认证, 4-媒体/特殊认证
        /// 对应字段: verification_status 
        /// </summary>
        public int? verificationStatus { get; set; } = 0;

        /// <summary>
        /// 认证信息
        /// 对应字段: verification_info 
        /// </summary>
        public string verificationInfo { get; set; }

        /// <summary>
        /// 最后同步时间 
        /// 对应字段: last_sync_time
        /// </summary>
        public string lastSyncTime { get; set; }

        /// <summary>
        /// 创建时间
        /// 对应字段: created_date 
        /// </summary>
        public string createdDate { get; set; }

        /// <summary>
        /// 更新时间
        /// 对应字段: update_date 
        /// </summary>
        public string updateDate { get; set; }

    }
}
