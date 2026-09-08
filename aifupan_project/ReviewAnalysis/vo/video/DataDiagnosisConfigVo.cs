namespace ReviewAnalysis.vo.video
{
    /// <summary>
    /// 数据诊断配置信息项
    /// </summary>
    public class DataDiagnosisConfigVo
    {
        /// <summary>
        /// 基础设置
        /// </summary>
        public BasicSettingsVo basicSettingsVo { get; set; }

        /// <summary>
        /// 行业 id
        /// </summary>
        public string tradeId { get; set; }

        /// <summary>
        /// 模型 id
        /// </summary>
        public string modelId { get; set; }

        /// <summary>
        /// 模型名称
        /// </summary>
        public string modelName { get; set; }

        /// <summary>
        /// 是否有数据截图 0 没有，1 有
        /// </summary>
        public int? hasDataScreenshot { get; set; }

        /// <summary>
        /// 是否有数据看版 0 没有，1 有
        /// </summary>
        public int? hasBoard { get; set; }
        
        /// <summary>
        /// 是否选择数据截图 0 没有，1 有
        /// </summary>
        public int? selectDataScreenshot { get; set; }

        /// <summary>
        /// 是否选择数据看版 0 没有，1 有
        /// </summary>
        public int? selectBoard { get; set; }

        /// <summary>
        /// 问题 id
        /// </summary>
        public string cueWordsId { get; set; }
        
        /// <summary>
        /// 新问题 id
        /// </summary>
        public string newCueWordsId { get; set; }

        /// <summary>
        /// 生成状态 null: 没有生成过，0 准备开始，1 进行中，2 已完成，3 失败
        /// </summary>
        public int? qaStatus { get; set; }

        /// <summary>
        /// 错误内容
        /// </summary>
        public string errorContent { get; set; }

        /// <summary>
        /// 来源 id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 来源类型 0：视频，1：文件，
        /// </summary>
        public int? sourceType { get; set; }
    }
}