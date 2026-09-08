using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.Model
{
    // 作者信息
    public class AuthorInfo
    {
        // 作者的唯一ID
        public string uid { get; set; }

        // 作者的二级UID
        public string secUid { get; set; }

        // 作者的昵称
        public string nickname { get; set; }

        // 作者的备注名（如果有）
        public string remarkName { get; set; }

        // 作者的头像地址
        public string avatarUri { get; set; }

        // 作者的粉丝数量
        public long? followerCount { get; set; }

        // 作者的总点赞数
        public long? totalFavorited { get; set; }

        // 作者的关注状态（0: 未关注，1: 已关注）
        public long? followStatus { get; set; }

        // 作者的粉丝状态（0: 非粉丝，1: 粉丝）
        public long? followerStatus { get; set; }
    }

    // 文本中的标签信息
    public class TextExtra
    {
        // 标签的起始位置
        public int start { get; set; }

        // 标签的结束位置
        public int end { get; set; }

        // 标签类型（1 表示是 Hashtag 类型）
        public int type { get; set; }

        // 标签的ID
        public string hashtagId { get; set; }

        // 标签的名称
        public string hashtagName { get; set; }

        // 二级UID
        public string secUid { get; set; }

        // Aweme ID
        public string awemeId { get; set; }

        // 用户ID
        public string userId { get; set; }

        // 是否为电商相关
        public bool isCommerce { get; set; }

        // 搜索隐藏词标志
        public int searchHideWords { get; set; }

        // 搜索查询ID
        public string searchQueryId { get; set; }

        // 搜索排名
        public int searchRank { get; set; }

        // 搜索文本
        public string searchText { get; set; }
    }

    // 视频播放地址
    public class PlayAddr
    {
        // 播放地址
        public string src { get; set; }
    }

    // 视频相关信息
    public class Video
    {
        // 视频的宽度
        public int width { get; set; }

        // 视频的高度
        public int height { get; set; }

        // 视频的显示比例（如 540p）
        public string ratio { get; set; }

        // 视频的时长（单位：毫秒）
        public long? duration { get; set; }

        // 视频的文件大小（单位：字节）
        public long dataSize { get; set; }

        // 视频的播放地址列表
        public List<PlayAddr> playAddr { get; set; }

        // 播放地址的总大小
        public long playAddrSize { get; set; }

        // 播放API的链接
        public string playApi { get; set; }

        // 视频封面图的URL
        public string cover { get; set; }

        // 视频的动态封面图URL
        public string dynamicCover { get; set; }
    }

    // 视频统计信息
    public class Stats
    {
        // 评论数量
        public int commentCount { get; set; }

        // 点赞数量
        public int diggCount { get; set; }

        // 分享数量
        public int shareCount { get; set; }

        // 播放数量
        public int playCount { get; set; }

        // 收藏数量
        public int collectCount { get; set; }

        // 下载数量
        public int downloadCount { get; set; }

        // 转发数量
        public int forwardCount { get; set; }

        // 实时观看数量
        public int liveWatchCount { get; set; }

        // 推荐数量
        public int recommendCount { get; set; }
    }

    // 根数据类，包含所有信息
    public class VideoData
    {
        // 作者信息
        public AuthorInfo authorInfo { get; set; }

        // 视频描述文本
        public string desc { get; set; }

        // 视频创建时间（时间戳）
        public long createTime { get; set; }

        // 文本中的标签信息列表
        public List<TextExtra> textExtra { get; set; }

        // 视频相关信息
        public Video video { get; set; }

        // 视频的统计信息
        public Stats stats { get; set; }
    }
}
