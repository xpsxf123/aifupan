using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.user
{
    /// <summary>
    /// 用户信息
    /// </summary>
    public class UserInfoVo
    {
        /// <summary>
        /// 套餐名称
        /// </summary>
        public string packageName { get; set; }

        /// <summary>
        /// 套餐等级
        /// </summary>
        public int? packageLevel { get; set; }

        /// <summary>
        /// 有效期时间
        /// </summary>
        public string expirationDate { get; set; }

        /// <summary>
        /// logo地址
        /// </summary>
        public string logoImgAddress { get; set; }

        /// <summary>
        /// 角色id集合
        /// </summary>
        public List<long> roleIdList { get; set; }

        /// <summary>
        /// 头像地址
        /// </summary>
        public string avatar { get; set; }

        /// <summary>
        /// 未脱敏的手机号
        /// </summary>
        public string normalPhone { get; set; }

        /// <summary>
        /// 子账号数量
        /// </summary>
        public int? childAccountCount { get; set; }

        /// <summary>
        /// 子账号id集合
        /// </summary>
        public List<UserVo> childUserList { get; set; }
    }
}
