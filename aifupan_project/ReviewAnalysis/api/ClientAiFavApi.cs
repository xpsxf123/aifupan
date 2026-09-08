using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.api
{
    public class ClientAiFavApi
    {

        /// <summary>
        /// 新增或修改运营/违规收藏列表
        /// </summary>
        /// <param name="favType"></param>
        /// <param name="dataResourceType"></param>
        /// <param name="dataResourceUuid"></param>
        public static void saveOrUpdate(int favType, int dataResourceType, string dataResourceUuid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("favType", favType);
            param.Add("dataResourceType", dataResourceType);
            param.Add("dataResourceUuid", dataResourceUuid);
            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/clientaifav/saveOrUpdate", param);
        }

        /// <summary>
        /// 新增或修改运营/违规收藏列表
        /// </summary>
        /// <param name="favType"></param>
        /// <param name="dataResourceType"></param>
        /// <param name="dataResourceUuid"></param>
        public static void saveOrUpdateByNotExist(int favType, int dataResourceType, string dataResourceUuid)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("favType", favType);
            param.Add("dataResourceType", dataResourceType);
            param.Add("dataResourceUuid", dataResourceUuid);
            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/clientaifav/saveOrUpdateByNotExist", param);
        }

    }
}
