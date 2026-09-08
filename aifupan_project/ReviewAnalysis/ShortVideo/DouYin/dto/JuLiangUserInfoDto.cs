using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Policy;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.DouYin.dto
{
    /// <summary>
    /// 抖音达人信息实体类
    /// 创建时间：2025年8月21日 
    /// </summary>
    public class JuLiangUserInfoDto
    {

        /// <summary>
        /// 用户唯一标识ID 
        /// 示例值：idajgbieccj
        /// </summary>
        public string user_id { get; set; }

        /// <summary>
        /// 用户昵称
        /// 示例值：王者荣耀 
        /// </summary>
        public string user_name { get; set; }

        /// <summary>
        /// 作品数量 
        /// 示例值：5178
        /// </summary>
        public string item_count { get; set; }

        /// <summary>
        /// 粉丝关注数量
        /// 示例值：24163245
        /// </summary>
        public string follow_count { get; set; }

        /// <summary>
        /// 获赞总数 
        /// 示例值：755970599 
        /// </summary>
        public string like_count { get; set; }

        /// <summary>
        /// 用户头像URL地址 
        /// 示例值：https://p11.douyinpic.com/... （完整URL） 
        /// </summary>
        public string user_head_logo { get; set; }

        /// <summary>
        /// 一级分类标签名称 
        /// 示例值：游戏 
        /// </summary>
        public string first_tag_name { get; set; }

        /// <summary>
        /// 二级分类标签名称
        /// 示例值：内容类型 
        /// </summary>
        public string second_tag_name { get; set; }

        /// <summary>
        /// 抖音号/唯一标识 
        /// 示例值：honorofkings 
        /// </summary>
        public string aweme_id { get; set; }

        /// <summary>
        /// 用户主页URL地址 
        /// 示例值：https://www.douyin.com/... （完整URL）
        /// </summary>
        public string aweme_url { get; set; }

        /// <summary>
        /// 达人的唯一id
        /// </summary>
        public string secUid {
            get
            {
                if (string.IsNullOrEmpty(aweme_url)) return null;
                return Path.GetFileName(aweme_url.TrimEnd('/'));
            }
        }

    }
}
