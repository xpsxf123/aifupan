using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.uploadFile
{
    public class UploadFileDetailVo
    {
        private static readonly long serialVersionUID = 1L;

        /// <summary>
        /// ID
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 文件唯一标识
        /// </summary>
        public string fileId { get; set; }

        /// <summary>
        /// 自然原文生成状态 0待生成，1生成中，2生成成功，3生成失败
        /// </summary>
        public int? natureContentStatus { get; set; }

        /// <summary>
        /// 优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败
        /// </summary>
        public int? optimizeContentStatus { get; set; }

        /// <summary>
        /// 是否上传诊断报告 0否，1是
        /// </summary>
        public int? hasDiagnosisReport { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate { get; set; }

        /// <summary>
        /// 更新时间
        /// </summary>
        public string updateDate { get; set; }

        /// <summary>
        /// 是否已删除
        /// </summary>
        public int? isDeleted { get; set; }

        /// <summary>
        /// 自然原文发送时间
        /// </summary>
        public long? natSetTime { get; set; }

        /// <summary>
        /// 优化原文发送时间
        /// </summary>
        public long? optSetTime { get; set; }

        /// <summary>
        /// 是否已推荐行业 0未推荐，1已推荐
        /// </summary>
        public int? suggestTrade { get; set; }

        /// <summary>
        /// 推荐行业Id
        /// </summary>
        public long? suggestTradeId { get; set; }
    }
}
