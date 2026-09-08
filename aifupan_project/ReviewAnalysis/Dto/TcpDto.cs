using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class TcpDto
    {
        /// <summary>
        /// 动作类型 0 上线通知  1 心跳通知 2业务通知
        /// </summary>
        public int ActionType { get; set; }

        /// <summary>
        /// 0业务失败 1 业务成功
        /// </summary>
        public int ActionStatus { get; set; }

        /// <summary>
        /// 业务返回结果 
        /// </summary>
        public Dictionary<string, object> ActionResult { get; set; }
    }
}
