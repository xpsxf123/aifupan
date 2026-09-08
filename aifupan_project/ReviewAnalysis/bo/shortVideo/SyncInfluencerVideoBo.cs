using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.shortVideo
{
    public class SyncInfluencerVideoBo
    {
        /// <summary>
        /// 平台账号
        /// </summary>
        public string platformAccount {  get; set; }

        /// <summary>
        /// 平台用户ID
        /// </summary>
        public string platformUserId {  get; set; }

        /// <summary>
        /// 平台类型
        /// </summary>
        public int? platformType { get; set; }

        /// <summary>
        /// 操作类型 0搜索处，1历史页面 2订阅达人处的更新数据按钮
        /// </summary>
        public int? actionType { get; set; } = 0;

        /// <summary>
        /// 达人id
        /// </summary>
        public long? influencerId { get; set; }

    }
}
