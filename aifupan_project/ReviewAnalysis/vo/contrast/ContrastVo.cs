using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.contrast
{
    public class ContrastVo
    {
        /// <summary>
        /// 用户id
        /// </summary>
        public long userId { get; set; }

        /// <summary>
        /// 视频1的视频ID
        /// </summary>
        public string videoOneId { get; set; }

        /// <summary>
        /// 视频2的视频ID
        /// </summary>
        public string videoTwoId { get; set; }

        /// <summary>
        /// 对比时间
        /// </summary>
        public string contrastTime { get; set; }

        /// <summary>
        /// 主播1的secUid
        /// </summary>
        public string anchorOneId { get; set; }

        /// <summary>
        /// 主播2的secUid
        /// </summary>
        public string anchorTwoId { get; set; }

        /// <summary>
        /// 文件1的文件id
        /// </summary>
        public string fileOneId { get; set; }

        /// <summary>
        /// 文件2的文件id
        /// </summary>
        public string fileTwoId { get; set; }

        /// <summary>
        /// 对比的唯一标识
        /// </summary>
        public string contrastId { get; set; }

        /// <summary>
        /// 对比类型 0：视频对比 1：文件对比
        /// </summary>
        public int contrastType { get; set; }

        /// <summary>
        /// 对比行业1
        /// </summary>
        public long tradeOneId { get; set; }

        /// <summary>
        /// 对比行业2
        /// </summary>
        public long tradeTwoId { get; set; }

        /// <summary>
        /// 是否已分享 0：否 1：是
        /// </summary>
        public int isShard { get; set; }

        /// <summary>
        /// 在线复盘的url
        /// </summary>
        public string shareUrl { get; set; }

        /// <summary>
        /// 删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除
        /// </summary>
        public int deleteStatus { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long tenantId { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public DateTime createDate { get; set; }

        /// <summary>
        /// 最后修改时间
        /// </summary>
        public DateTime updateDate { get; set; }
        /// <summary>
        /// 云空间备注
        /// </summary>
        public string cloudRemarks { get; set; }
        /// <summary>
        /// 对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
        /// </summary>
        public int? syncScene { get; set; }
    }
}
