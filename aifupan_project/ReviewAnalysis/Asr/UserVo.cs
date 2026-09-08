using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    public class UserVo
    {
        /// <summary>
        /// ID
        /// </summary>
        public long Id { get; set; }

        /// <summary>
        /// 登录账号
        /// </summary>
        public string Username { get; set; }

        /// <summary>
        /// 昵称
        /// </summary>
        public string NickName { get; set; }

        /// <summary>
        /// 密码
        /// </summary>
        public string Password { get; set; }

        /// <summary>
        /// 手机号
        /// </summary>
        public string Phone { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string CreateDate { get; set; }

        /// <summary>
        /// 冻结状态 0：未冻结 1：已冻结
        /// </summary>
        public int Status { get; set; }

        /// <summary>
        /// 用户类型 0：普通用户 1：后台管理员
        /// </summary>
        public int UserType { get; set; }

        /// <summary>
        /// 当前账号的租户id
        /// </summary>
        public long? activeTenantId { get; set; }
    }
}
