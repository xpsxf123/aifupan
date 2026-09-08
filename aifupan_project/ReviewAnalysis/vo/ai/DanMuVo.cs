using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    public class DanMuVo
    {
        /* 原Java中被注释的字段（按需取消注释）
        /// <summary>租户ID</summary>
        public long TenantId { get; set; }

        /// <summary>主播ID</summary>
        public string SecUid { get; set; }

        /// <summary>消息ID</summary>
        public string MsgId { get; set; }

        /// <summary>录制用户ID</summary>
        public long UserId { get; set; }

        /// <summary>视频ID</summary>
        public string VideoId { get; set; }
        */

        /// <summary>记录时间戳（Unix毫秒）</summary>
        public long? recordDate { get; set; }

        /// <summary>直播场次唯一标识号</summary>
        public string batchNumber { get; set; }

        /// <summary>发送弹幕的用户昵称</summary>
        public string nickName { get; set; }

        /// <summary>标识是否为新用户（首次发言）</summary>
        public bool? isNew { get; set; }

        /// <summary>用户等级（平台等级体系）</summary>
        public long? level { get; set; }

        /// <summary>本场直播中用户的最小粉丝团等级</summary>
        public long? fansLevelMin { get; set; }

        /// <summary>本场直播中用户的最大粉丝团等级</summary>
        public long? fansLevelMax { get; set; }

        /// <summary>用户当前的粉丝团等级</summary>
        public long? fansLevelCurrent { get; set; }

        /// <summary>弹幕文本内容（需注意敏感词过滤）</summary>
        public string content { get; set; }

        /// <summary>本场直播中该用户的弹幕发送总次数</summary>
        public int? countSendNum { get; set; }

        /// <summary>弹幕排序权重值（越大优先级越高）</summary>
        public long? sort { get; set; }

        /// <summary>标记是否已向用户显示等级提升提示（默认false）</summary>
        public bool? showUpgradeTips { get; set; } = false;

        /// <summary>标识内容是否已进行脱敏处理（如昵称打码）</summary>
        public bool? isDesensitization { get; set; }
    }
}
