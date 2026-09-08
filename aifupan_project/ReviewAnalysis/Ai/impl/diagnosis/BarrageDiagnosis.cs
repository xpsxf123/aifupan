using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Ai.impl.diagnosis
{
    public class BarrageDiagnosis : AbstractDiagnosis
    {
        public override Task setAdditionalParams()
        {
            result.uploadBoard = 0;
            result.uploadScreenshot = 0;
            Dictionary<string, object> other = new Dictionary<string, object>();

            other["batchNumber"] = video.batchNumber;

            var dataDisplay = new {
                dateTime = 1,
                fansLevel = 1,
                isNew = 1,
                level = 1,
                nickName = 1,
            };
            other["dataDisplay"] = JsonConvert.DeserializeObject<dynamic>(JsonConvert.SerializeObject(dataDisplay));

            other["videoId"] = video.videoId;
            other["startTime"] = DateUtils.StringToTimestamp(video.startTime);
            other["endTime"] = DateUtils.StringToTimestamp(video.endTime);


            result.otherObj = other;

            return Task.CompletedTask;
        }
    }
}
