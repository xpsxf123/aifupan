using ReviewAnalysis.vo.common;

namespace ReviewAnalysis.vo.video
{
    /// <summary>
    /// 基础设置视图对象
    /// </summary>
    public class BasicSettingsVo : BasicSettingsBaseDto
    {
        /// <summary>
        /// 主键
        /// </summary>
        public string id { get; set; }

        /// <summary>
        /// 来源 id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 来源类型 0：主播，1：视频，2：文件，
        /// </summary>
        public int? sourceType { get; set; }

        /// <summary>
        /// 用户表 id
        /// </summary>
        public string userId { get; set; }

        /// <summary>
        /// 租户 id
        /// </summary>
        public string tenantId { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate { get; set; }

        /// <summary>
        /// 更新时间
        /// </summary>
        public string updateDate { get; set; }
    }
}