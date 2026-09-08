using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.DouYin.dto
{
    /// <summary>
    /// 抖音用户信息实体类（数据版本：2025.08.21）
    /// 对应抖音用户主页数据结构
    /// </summary>
    public class JuLiangUserInfo
    {
        /// <summary>
        /// 用户唯一标识ID
        /// 示例值：idajgbieccj
        /// </summary>
        public string user_id;

        /// <summary>
        /// 用户昵称（含表情符号）
        /// 示例值：王者荣耀 
        /// </summary>
        public string user_name;

        /// <summary>
        /// 用户头像高清URL
        /// 分辨率：1080x1080 
        /// </summary>
        public string user_head_logo;

        /// <summary>
        /// 用户性别 
        /// 枚举值：male/female/unknown
        /// </summary>
        public string user_gender;

        /// <summary>
        /// 用户地理位置（可能为空）
        /// </summary>
        public string user_location;

        /// <summary>
        /// 用户个性签名
        /// 示例：诡秘诡秘要和我玩王者荣耀喵喵喵 
        /// </summary>
        public string user_introduction;

        /// <summary>
        /// 粉丝数量（字符串格式）
        /// 示例：24163467
        /// </summary>
        public string fans_count;

        /// <summary>
        /// 获赞总数（字符串格式）
        /// 示例：756047417 
        /// </summary>
        public string like_count;

        /// <summary>
        /// 作品数量（字符串格式）
        /// 示例：5180
        /// </summary>
        public string item_count;

        /// <summary>
        /// 一级分类标签 
        /// 示例：游戏
        /// </summary>
        public string first_tag_name;

        /// <summary>
        /// 二级分类标签 
        /// 示例：内容类型 
        /// </summary>
        public string second_tag_name;

        /// <summary>
        /// 抖音号唯一标识
        /// 示例：honorofkings 
        /// </summary>
        public string aweme_id;

        /// <summary>
        /// 用户主页完整URL
        /// 包含MS4wLjAB...等加密参数
        /// </summary>
        public string user_aweme_url;

        /// <summary>
        /// 用户作品封面图URL
        /// 包含动态签名参数 
        /// </summary>
        public string aweme_pic;
    }
}
