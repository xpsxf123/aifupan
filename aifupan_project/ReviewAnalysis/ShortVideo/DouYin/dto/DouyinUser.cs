using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.DouYin.dto
{
    /// <summary>
    /// 抖音用户信息实体类
    /// </summary>
    public class DouyinUser
    {

        /// <summary>
        /// 头像
        /// </summary>
        public avatarThumb avatar_thumb { get; set; }
        /// <summary>
        /// 用户 UID
        /// </summary>
        public string uid { get; set; }

        /// <summary>
        /// 用户 sec_uid
        /// </summary>
        public string sec_uid { get; set; }

        /// <summary>
        /// 抖音号（unique_id）
        /// </summary>
        public string unique_id { get; set; }

        /// <summary>
        /// 用户昵称
        /// </summary>
        public string nickname { get; set; }

        /// <summary>
        /// 个性签名
        /// </summary>
        public string signature { get; set; }

        /// <summary>
        /// 企业认证信息
        /// </summary>
        public string enterprise_verify_reason { get; set; }

        /// <summary>
        /// 粉丝数量
        /// </summary>
        public long? follower_count { get; set; }

        /// <summary>
        /// 关注数量
        /// </summary>
        public long? following_count { get; set; }

        /// <summary>
        /// 发布作品数量
        /// </summary>
        public long? aweme_count { get; set; }

        /// <summary>
        /// 获赞总数
        /// </summary>
        public long? total_favorited { get; set; }

        /// <summary>
        /// 全平台粉丝数（可能包含抖音火山、西瓜等）
        /// </summary>
        public long? mplatform_followers_count { get; set; }

        /// <summary>
        /// 作品数校正阈值
        /// </summary>
        public long? aweme_count_correction_threshold { get; set; }

        /// <summary>
        /// 获赞数校正阈值
        /// </summary>
        public long? total_favorited_correction_threshold { get; set; }


        // 手动设置的值

        /// <summary>
        /// 认证状态: 0-未认证, 1-个人认证, 2-企业/机构认证
        /// </summary>
        public int verificationStatus { get; set; }

        /// <summary>
        /// 认证信息
        /// </summary>
        public string verificationInfo { get; set; }
    }

}
