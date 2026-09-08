using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.DouYin.dto
{
    /// <summary>
    /// 抖音视频信息实体类
    /// </summary>
    public class DouYinVideoInfo
    {
        /// <summary>
        /// 视频ID
        /// </summary>
        public string aweme_id { get; set; }

        /// <summary>
        /// 视频描述/标题
        /// </summary>
        public string desc { get; set; }

        /// <summary>
        /// 创建时间（Unix时间戳）
        /// </summary>
        public long? create_time { get; set; }

        /// <summary>
        /// 作者信息
        /// </summary>
        public DouYinAuthorInfo author { get; set; }

        /// <summary>
        /// 安全用户ID
        /// </summary>
        public string sec_uid { get; set; }

        /// <summary>
        /// 视频信息
        /// </summary>
        public DouYinVideoDetail video { get; set; }

        /// <summary>
        /// 统计信息
        /// </summary>
        public DouYinStatistics statistics { get; set; }
    }

    /// <summary>
    /// 抖音作者信息
    /// </summary>
    public class DouYinAuthorInfo
    {
        /// <summary>
        /// 用户ID
        /// </summary>
        public string uid { get; set; }

        /// <summary>
        /// 用户昵称
        /// </summary>
        public string nickname { get; set; }

        /// <summary>
        /// secUid
        /// </summary>
        public string sec_uid { get; set; }

        /// <summary>
        /// 收藏数
        /// </summary>
        public long? follower_count { get; set; }

        /// <summary>
        /// 头像-拇指版
        /// </summary>
        public avatarThumb avatar_thumb { get; set; }
    }

    /// <summary>
    /// 头像-拇指版
    /// </summary>
    public class avatarThumb
    {

        public string uri { get; set; }

        public List<string> url_list { get; set; }

        public int? width { get; set; }

        public int? height { get; set; }
    }

    /// <summary>
    /// 抖音视频详细信息
    /// </summary>
    public class DouYinVideoDetail
    {

        /// <summary>
        /// 播放地址
        /// </summary>
        public playAddr play_addr { get; set; }

        /// <summary>
        /// 视频封面
        /// </summary>
        public cover cover { get; set; }

        /// <summary>
        /// 视频高度
        /// </summary>
        public int? height { get; set; }

        /// <summary>
        /// 视频宽度
        /// </summary>
        public int? width { get; set; }

        /// <summary>
        /// 视频分辨率比例
        /// </summary>
        public string ratio { get; set; }

        /// <summary>
        /// 视频时长（毫秒）
        /// </summary>
        public long? duration { get; set; }

    }

    public class playAddr
    {
        /// <summary>
        /// uri
        /// </summary>
        public string uri { get; set; }

        /// <summary>
        /// 下载地址
        /// </summary>
        public List<string> url_list { get; set; }


        /// <summary>
        /// 视频高度
        /// </summary>
        public int height { get; set; }

        /// <summary>
        /// 视频宽度
        /// </summary>
        public int width { get; set; }

        /// <summary>
        /// url_key
        /// </summary>
        public string url_key { get; set; }

        /// <summary>
        /// 数据带大小
        /// </summary>
        public long? data_size { get; set; }

        /// <summary>
        /// 文件的hash
        /// </summary>
        public string file_hash { get; set; }

        /// <summary>
        /// file_cs
        /// </summary>
        public string file_cs { get; set; }

    }

    /// <summary>
    /// 视频封面
    /// </summary>
    public class cover
    {
        /// <summary>
        /// url
        /// </summary>
        public string uri { get; set; }

        /// <summary>
        /// 图片集合
        /// </summary>
        public List<string> url_list { get; set; }

        /// <summary>
        /// 高度
        /// </summary>
        public int? height { get; set; }

        /// <summary>
        /// 宽度
        /// </summary>
        public int? width { get; set; }

    }

    /// <summary>
    /// 抖音视频统计信息
    /// </summary>
    public class DouYinStatistics
    {
        /// <summary>
        /// 评论数
        /// </summary>
        public long? comment_count { get; set; }

        /// <summary>
        /// 点赞数
        /// </summary>
        public long? digg_count { get; set; }

        /// <summary>
        /// 下载数
        /// </summary>
        public long? download_count { get; set; }

        /// <summary>
        /// 播放数
        /// </summary>
        public long? play_count { get; set; }

        /// <summary>
        /// 分享数
        /// </summary>
        public long? share_count { get; set; }

        /// <summary>
        /// 转发数
        /// </summary>
        public long? forward_count { get; set; }

        /// <summary>
        /// 直播观看数
        /// </summary>
        public long? live_watch_count { get; set; }

        /// <summary>
        /// 收藏数
        /// </summary>
        public long? collect_count { get; set; }
    }
}
