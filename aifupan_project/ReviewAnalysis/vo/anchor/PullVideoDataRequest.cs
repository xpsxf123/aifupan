namespace ReviewAnalysis.vo.anchor
{
    /// <summary>
    /// 拉取视频数据请求参数
    /// </summary>
    public class PullVideoDataRequest
    {
        /// <summary>主播 sec_uid</summary>
        public string secUid { get; set; }

        /// <summary>平台标识: juliang / laike，默认 juliang</summary>
        public string platform { get; set; } = "juliang";
    }
}
