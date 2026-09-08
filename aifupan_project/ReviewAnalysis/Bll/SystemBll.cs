using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.bo.system;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Bll
{
    public class SystemBll
    {
        /// <summary>
        /// html转成pdf
        /// </summary>
        /// <param name="bo">参数</param>
        /// <returns></returns>
        public bool htmlPrintPDF(HtmlPrintPDFBo bo)
        {
            bo.fileName = UploadUtils.getCurrentFileName(Path.GetFileNameWithoutExtension(bo.fileName), ".pdf");
            String savePath = $"{UploadUtils.uploadsFilePath}\\{bo.fileName}";
            HtmlToPdfUtil.Print(bo.htmlContent, savePath, (isSuccess) =>
            {
                if (isSuccess)
                {
                    Dictionary<string, string> parameters = new Dictionary<string, string>();
                    parameters.Add("uploadType", bo.uploadType.ToString());
                    parameters.Add("otherObj", JsonConvert.SerializeObject(bo));
                    // 生成文件的后置处理
                    UploadUtils.uploadsPostProcessing(savePath, bo.fileName, bo.uploadType, bo.sourceId, (bo.notFolder??0) == 0);   
                }
            });
            return true;
        }
    }
}