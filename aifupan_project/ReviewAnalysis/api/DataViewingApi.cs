using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.user;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class DataViewingApi
    {

        /// <summary>
        /// 生成数据看盘
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="anchorOnlineStatus">主播是否已下播 0：否 1：是</param>
        /// <returns></returns>
        public static async Task CreateDataViewing(string videoId, int anchorOnlineStatus)
        {
            try
            {
                Dictionary<string, object> param = new Dictionary<string, object>();
                param.Add("videoId", videoId);
                param.Add("isAuto", 1);
                param.Add("anchorOnlineStatus", anchorOnlineStatus);
                HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/videodataviewing/createDataViewing", param);
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"生成数据看盘发生异常");
            }
            
        }
    }
}
