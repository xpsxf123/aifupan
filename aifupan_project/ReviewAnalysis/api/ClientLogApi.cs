using System;
using System.Collections.Generic;
using System.Data.Entity.Core.Metadata.Edm;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Global;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.api
{
    public class ClientLogApi
    {
        public static void saveLog(string actionName, string actionInfo, int logType = 1, string errorMessage = "")
        {
            Dictionary<string, object> parameters = new Dictionary<string, object>();
            parameters.Add("actionName", actionName);
            parameters.Add("actionInfo", actionInfo);
            parameters.Add("errorMsg", errorMessage);
            parameters.Add("clientVersion", Constant.VERSION);
            parameters.Add("logType", logType);
            HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/openapi/clientlog/save", parameters);
        }


    }
}
