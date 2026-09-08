using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.Ai;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.diagnosis;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.diagnosis;

namespace ReviewAnalysis.Bll
{
    public class DiagnosisBll
    {
        /// <summary>
        /// 导出文件
        /// </summary>
        /// <param name="videoId"></param>
        public void generateReport(GenerateReportVo vo)
        {
            string currentPath = UploadUtils.uploadsFilePath + $"\\{vo.fileName}";
            if (File.Exists(currentPath))
            {
                FileUtils.openFile(currentPath);
                return;
            }

            string signedUrl = DiagnosisApi.getDiagnosisDownloadUrl(vo);

            if (!string.IsNullOrEmpty(signedUrl))
            {
                // 使用oss预签名下载连接，把文件下载到对应的filePath文件夹中
                string filePath = UploadUtils.uploadsFilePath;

                // 确保目录存在
                Directory.CreateDirectory(filePath);

                // 生成文件名
                string fileName = string.IsNullOrEmpty(filePath) ? $"诊断报告_{vo.videoId}_{DateTime.Now:yyyyMMddHHmmss}.pdf" : vo.fileName;
                string savePath = Path.Combine(filePath, fileName);

                // 下载文件
                if (OssUtils.DownloadFileAsync(signedUrl, savePath))
                {
                    // 打开文件夹
                    FileUtils.openFile(savePath);
                }
                else
                {
                    FileUtils.LogError($"下载诊断报告失败，videoId: {vo.videoId}", "诊断报告下载");
                }
            }
            else
            {
                FileUtils.LogError($"获取诊断报告下载链接失败，videoId: {vo.videoId}", "诊断报告下载");
            }
        }
    }
}
