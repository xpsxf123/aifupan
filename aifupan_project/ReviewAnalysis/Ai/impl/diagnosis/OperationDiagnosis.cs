using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.Ai.impl.diagnosis
{
    /// <summary>
    /// 运营助手的参数
    /// </summary>
    public class OperationDiagnosis : AbstractDiagnosis
    {
        public override Task setAdditionalParams()
        {
            result.uploadBoard = 1;
            result.uploadScreenshot = 1;
            Dictionary<string, object> other = new Dictionary<string, object>();
            other["dataDisplay"] = new { };


            var aiAssistantDisplay = new
            {
                startTime = 1,
                natureTime = 1,
                onlineNum = 1,
            };

            other["aiAssistantDisplay"] = JsonConvert.DeserializeObject<dynamic>(JsonConvert.SerializeObject(aiAssistantDisplay));
            result.otherObj = other;

            return Task.CompletedTask;
        }
    }
}
