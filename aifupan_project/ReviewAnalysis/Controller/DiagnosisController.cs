using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Ai;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.bo.diagnosis;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.diagnosis;

namespace ReviewAnalysis.Controller
{
    [RestController("诊断报告", "api/diagnosis")]
    public class DiagnosisController
    {
        /// <summary>
        /// 诊断分析-提示词添加分析
        /// </summary>
        /// <param name="contrastId">对比唯一标识</param>
        [HttpPost("诊断分析-提示词添加分析", "/saveDiagnosis")]
        public void saveDiagnosis(SaveDiagnosisBo bo)
        {
            UserPropertyEntity userProperty = UserPropertyApi.GetPropertyInfoSync();

            if (userProperty == null || (userProperty?.AiTokenNum ?? 0) <= 0)
            {
                CustomException.create("AI助手分析余量不足，请联系产品顾问进行套餐外购买", 7001);
            }

            DiagnosisAuto.addAutoAsk(bo);
        }

        [HttpPost("诊断分析-查看诊断", "/generateReport")]
        public void generateReport(GenerateReportVo vo)
        {
            DiagnosisBll diagnosisBll = new DiagnosisBll();
            diagnosisBll.generateReport(vo);
        }


    }
}
