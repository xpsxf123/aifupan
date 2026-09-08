using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.blessBag;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.barrage;

namespace ReviewAnalysis.api
{
    public class BlessBagApi
    {

        /// <summary>
        /// 查询视频对应的弹幕标注
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public async static Task save(BlessBagBo bo)
        {
            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/blessbag/save", bo);
        }


    }
}
