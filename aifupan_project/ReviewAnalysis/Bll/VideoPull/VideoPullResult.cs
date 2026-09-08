using ReviewAnalysis.Bll.VideoPull.Models;
using System.Collections.Generic;

namespace ReviewAnalysis.Bll.VideoPull
{
    /// <summary>
    /// 拉取结果，由各平台 Puller 返回。
    ///
    /// 判断流程:
    ///   Success == true  → 成功, Data 有数据（可能空数组表示真的没数据）
    ///   Success == false → 失败，看 ErrorCode + Error 定位原因
    ///
    /// ErrorCode 取值:
    ///   "COOKIE_NOT_FOUND"  Cookie 文件不存在（主播未授权）
    ///   "COOKIE_EMPTY"      Cookie 文件存在但内容为空
    ///   "HTTP_ERROR"        HTTP 请求失败（网络问题/服务端拒绝）
    ///   "HTTP_EMPTY"        HTTP 响应为空
    ///   "API_ERROR"         接口返回 st != 0（Cookie 过期/权限不足等）
    ///   "PARSE_ERROR"       响应 JSON 解析失败
    /// </summary>
    public class VideoPullResult
    {
        /// <summary>true=拉取成功（Data 可能为空数组）, false=失败</summary>
        public bool Success { get; set; }

        /// <summary>直播列表</summary>
        public List<LiveSessionInfo> Data { get; set; }

        /// <summary>错误码, Success=false 时有值, 见类注释</summary>
        public string ErrorCode { get; set; }

        /// <summary>错误描述, Success=false 时有值</summary>
        public string Error { get; set; }

        /// <summary>API 返回的总记录数（成功时有值）</summary>
        public int Total { get; set; }
    }
}
