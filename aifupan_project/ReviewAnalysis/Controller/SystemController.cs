
using CefSharp;
using douyin.Utils;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.bo.system;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Global;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.upload;

namespace ReviewAnalysis.Controller
{
    [RestController("系统接口", "api/system")]
    public class SystemController
    {

        [HttpGet("获取机器码", "/getMachineCode")]
        public async Task<string> GetMachineCode()
        {
            return await Task.Run(() => SystemUtils.GenerateMachineCode()).ConfigureAwait(false);
        }

        [HttpGet("打开官网", "/openOfficial")]
        public void OpenOfficial()
        {
            string url = Constant.GetOnlineUrl("");
            Process.Start(new ProcessStartInfo(url)
            {
                UseShellExecute = true
            });
        }

        [HttpPost("打开链接", "/openUrl")]
        public void OpenUrl(OpenUrlBo openUrlBo)
        {

            try
            {
                // 用默认浏览器打开
                Process.Start(new ProcessStartInfo(openUrlBo.url)
                {
                    UseShellExecute = true
                });
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"", $"用默认浏览器打开网页失败");
                // 尝试指定edge浏览器打开
                string[] edgePaths = {
                    @"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
                    @"C:\Program Files\Microsoft\Edge\Application\msedge.exe"
                };

                bool success = false;
                foreach (string path in edgePaths)
                {
                    if (File.Exists(path) && !success)
                    {
                        try
                        {
                            Process.Start(new ProcessStartInfo(path, openUrlBo.url)
                            {
                                UseShellExecute = false
                            });
                            success = true;
                        }
                        catch { /* 忽略Edge启动异常 */ }
                    }
                }

                if(!success)
                {
                    // 使用内置浏览器打开
                    FrontNotice.webBrowser.EvaluateScriptAsync($"window.open('{openUrlBo.url}');");
                }
            }
            
        }

        
        [HttpGet("打开企业后台页面", "/openGovernanceWeb")]
        public void openGovernanceWeb(string key)
        {
            string url = KvHelper.GetKvByKey(key);
            if (string.IsNullOrEmpty(url))
            {
                throw new CustomException("未知key");
            }
            OpenUrl(new OpenUrlBo{url = url});
        }
        
        [HttpPost("html转成pdf", "/htmlPrintPDF")]
        public bool htmlPrintPDF(HtmlPrintPDFBo bo)
        {
            if (bo == null || string.IsNullOrEmpty(bo.htmlContent))
            {
                throw new CustomException("内容不能为空");
            }
            if (string.IsNullOrEmpty(bo.fileName))
            {
                bo.fileName = $"{Guid.NewGuid().ToString("N")}.pdf";
            }

            if (!bo.fileName.ToLower().EndsWith(".pdf"))
            {
                throw new CustomException("文件名称只能以pdf结尾");
            }
            SystemBll systemBll = new SystemBll();
            return systemBll.htmlPrintPDF(bo);
        }

        [HttpPost("html转成pdf", "/htmlPrintPDF2")]
        public bool htmlPrintPDF2(HtmlPrintPDFBo bo)
        {
            bo.htmlContent = File.ReadAllText("C:\\Users\\JY\\Desktop\\测试 - 副本.html");
            if (string.IsNullOrEmpty(bo.fileName))
            {
                bo.fileName = $"{Guid.NewGuid().ToString("N")}.pdf";
            }

            if (!bo.fileName.ToLower().EndsWith(".pdf"))
            {
                throw new CustomException("文件名称只能以pdf结尾");
            }
            SystemBll systemBll = new SystemBll();
            return systemBll.htmlPrintPDF(bo);
        }
    }
}
