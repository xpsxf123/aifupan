using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class AnchorBaseInfoDto
    {
        /// <summary>
        /// 主播SecUid
        /// </summary>
        public string secUid { get; set; }
        /// <summary>
        /// 主播名称
        /// </summary>
        public string anchorName { get; set; }
        /// <summary>
        /// 主播抖音用户id
        /// </summary>
        public string anchorUserId { get; set; }
        /// <summary>
        /// 主播抖音号
        /// </summary>
        public string anchorNumber { get; set; }
        /// <summary>
        /// 主播头像地址
        /// </summary>
        public string anchorAvatar { get; set; }

    }
}
