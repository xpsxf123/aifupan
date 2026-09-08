namespace ReviewAnalysis.bo.system
{
    public class HtmlPrintPDFBo
    {
        /// <summary>
        /// html的代码
        /// </summary>
        public string htmlContent { get; set; }
        
        /// <summary>
        /// 文件名称
        /// </summary>
        public string fileName { get; set; }

        /// <summary>
        /// 上传类型 0：正常pdf导出，1：内容诊断报告pdf导出
        /// </summary>
        public int? uploadType { get; set; } = 0;

        /// <summary>
        /// 来源类型(默认0) 0视频，1文件
        /// </summary>
        public int? sourceType { get; set; } = 0;

        /// <summary>
        /// 视频id
        /// </summary>
        public string sourceId { get; set; }
        
        /// <summary>
        /// 是否打开文件夹,默认打开，0打开 1不打开
        /// </summary>
        public int? notFolder { get; set; } = 0;
    }
}