using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Controller
{
    [RestController("用户资产", "api/userproperty")]
    public class UserPropertyAsyncController
    {
        [HttpGet("获取用户资产信息", "/info")]
        public async Task<UserPropertyEntity> Info()
        {
            UserPropertyEntity userProperty = await UserPropertyApi.GetPropertyInfo();
            return userProperty;
        }
    }
}
