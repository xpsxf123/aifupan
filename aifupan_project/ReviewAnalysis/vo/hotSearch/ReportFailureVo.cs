using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.hotSearch
{
    public class ReportFailureVo
    {
        /// <summary>
        /// 上报不可用邮箱账号BO
        /// </summary>
        public Int64 accountId { get; set; }
        /// <summary>
        /// 邮箱账号
        /// </summary>
        public string email { get; set; }
        /// <summary>
        /// 客户端IP
        /// </summary>
        public string clientIp { get; set; }
        /// <summary>
        /// 失败类型：1-密码错误 2-账号被封 3-网络超时 4-验证码错误 5-其他
        /// </summary>
        public int failureType { get; set; }
        /// <summary>
        /// 失败原因详情
        /// </summary>
        public string failureReason { get; set; }
        /// <summary>
        /// 错误码
        /// </summary>
        public string errorCode { get; set; }
        /// <summary>
        /// 错误信息
        /// </summary>
        public string errorMessage { get; set; }
    }
}
