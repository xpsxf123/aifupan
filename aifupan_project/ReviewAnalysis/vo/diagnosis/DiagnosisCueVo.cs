using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.diagnosis
{

    /// <summary>
    /// ai诊断提示词配置信息
    /// </summary>
    public class DiagnosisCueVo
    {
        /// <summary>
        /// id
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 来源id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 来源类型 0主播 1视频
        /// </summary>
        public int? sourceType { get; set; }

        /// <summary>
        /// 提示词类型
        /// </summary>
        public int? cueType { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public long? tradeId { get; set; }

        /// <summary>
        /// 提示词id
        /// </summary>
        public long? cueWordsId { get; set; }

        /// <summary>
        /// 状态 0未处理 1处理中 2处理完成 3处理失败
        /// </summary>
        public int? qaStatus { get; set; }

        /// <summary>
        /// 错误内容
        /// </summary>
        public string errorContent { get; set; }

        /// <summary>
        /// 已选中，0否，1是
        /// </summary>
        public int? isSelected { get; set; }

        /// <summary>
        /// 诊断报告类型 0：内容诊断，1：数据诊断
        /// </summary>
        public int? diagnosisType { get; set; }
        
        /// <summary>
        /// 是否选择数据截图 0没有，1有
        /// </summary>
        public int? selectDataScreenshot { get; set; }

        /// <summary>
        /// 是否选择数据看版 0没有，1有
        /// </summary>
        public int? selectBoard { get; set; }
        
        /// <summary>
        /// 用户id
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId { get; set; }

        /// <summary>
        /// 更新人
        /// </summary>
        public long? updateUserId { get; set; }

        /// <summary>
        /// 更新时间
        /// </summary>
        public DateTime updateDate { get; set; }

        /// <summary>
        /// 创建人
        /// </summary>
        public long? createUserId { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public DateTime createDate { get; set; }
    }
}
