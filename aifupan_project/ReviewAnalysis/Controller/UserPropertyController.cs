using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Concurrent;
using System.Threading;
using System.Windows.Forms;

namespace ReviewAnalysis.Controller
{
    [RestAsyncController("用户资产", "api/userproperty")]
    public class UserPropertyController
    {
        [HttpGet("获取用户资产信息", "/info")]
        public UserPropertyEntity Info() 
        {
            UserPropertyEntity userProperty = UserPropertyApi.GetPropertyInfoSync();
            return userProperty;
        }
    }
}
