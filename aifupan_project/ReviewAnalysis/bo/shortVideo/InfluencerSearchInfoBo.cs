using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.shortVideo
{
    /// <summary>
    /// 达人信息视图请求对象
    /// </summary>
    public class InfluencerSearchInfoBo
    {
        /// <summary>
        /// 平台类型: 1-抖音, 2-快手, 3-小红书
        /// </summary>
        public int platformType { get; set; }

        /// <summary>
        /// 平台用户ID
        /// </summary>
        public string platformUserId { get; set; }

        /// <summary>
        /// 平台账号（抖音号/快手号/视频号）
        /// </summary>
        public string platformAccount { get; set; }

        /// <summary>
        /// 昵称
        /// </summary>
        public string nickname { get; set; }

        /// <summary>
        /// 头像URL
        /// </summary>
        public string avatar { get; set; }

        /// <summary>
        /// 个人简介
        /// </summary>
        public string description { get; set; }

        /// <summary>
        /// 粉丝数
        /// </summary>
        public long? followersCount { get; set; }

        /// <summary>
        /// 关注数
        /// </summary>
        public long? followingCount { get; set; }

        /// <summary>
        /// 作品数
        /// </summary>
        public long? videoCount { get; set; }

        /// <summary>
        /// 点赞数
        /// </summary>
        public long? likeCount { get; set; }

        /// <summary>
        /// 认证状态: 0-未认证, 1-个人认证, 2-企业认证
        /// </summary>
        public int verificationStatus { get; set; }

        /// <summary>
        /// 认证信息
        /// </summary>
        public string verificationInfo { get; set; }
    }
}
