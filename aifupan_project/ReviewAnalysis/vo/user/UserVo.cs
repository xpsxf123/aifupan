using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.user
{
    /// <summary>
    /// 用户
    /// </summary>
    public class UserVo
    {
        /// <summary>
        /// ID
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 登录账号
        /// </summary>
        public string username { get; set; }

        /// <summary>
        /// 昵称
        /// </summary>
        public string nickName { get; set; }

        /// <summary>
        /// 密码
        /// </summary>
        public string password { get; set; }

        /// <summary>
        /// 手机号
        /// </summary>
        public string phone { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate { get; set; }

        /// <summary>
        /// 用户登录过的ip,前后用_隔开
        /// </summary>
        public string ips { get; set; }

        /// <summary>
        /// 冻结状态 0：未冻结 1：已冻结
        /// </summary>
        public int? status { get; set; }

        /// <summary>
        /// 上级用户id
        /// </summary>
        public long? parentId { get; set; }

        /// <summary>
        /// 用户类型 0：普通用户 1：后台管理员 2：子账号
        /// </summary>
        public int? userType { get; set; }

        /// <summary>
        /// 当前激活的租户id
        /// </summary>
        public long? activeTenantId { get; set; }
    }
}
