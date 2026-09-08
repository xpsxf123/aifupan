
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Model;
using System.Collections.Generic;
using System.IO;
using System.Windows.Forms;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Bll;
using ReviewAnalysis.vo;
using ReviewAnalysis.api;
using ReviewAnalysis.entity.video;
using System;
using ReviewAnalysis.entity.uploadFile;

namespace ReviewAnalysis.Controller
{
    [RestController("配置相关的接口", "api/export")]
    public class ExportFileController
    {

        /// <summary>
        /// 导出云空间视频/文件文字内容
        /// </summary>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <param name="id">视频ID/文件ID</param>
        /// <param name="fileName">视频名/文件名</param>
        [HttpGet("导出视频/文件文字内容", "/alysesCloudTxt")]
        public string AlysesCloudTxt(int type, string id, string fileName)
        {
            string content = "";

            if (type == 0)
            {

                AnchorVideoBll anchorVideoBll = new AnchorVideoBll();
                CloudAnalysisVo cloudAnalysisVo = anchorVideoBll.LockCloudAnalysis(id);

                if(cloudAnalysisVo != null)
                {
                    List<OnlineAnalysisItemVo> analysisList = cloudAnalysisVo.AnalysisList;
                    if (analysisList != null && analysisList.Count > 0)
                    {
                        analysisList.Sort((x, y) => x.Paragraph.CompareTo(y.Paragraph));
                        foreach (var item in analysisList)
                        {
                            if (item.Status == 0)
                            {
                                JObject jsonObject = JObject.Parse(item.DataJson);
                                content += (string)jsonObject["content"] + "\r\n\r\n";
                            }
                        }
                    }
                }
            }

            SaveFileDialog saveFileDialog = new SaveFileDialog
            {
                Filter = "Text files (*.txt)|*.txt|All files (*.*)|*.*",
                FilterIndex = 1,
                RestoreDirectory = true,
                FileName = fileName + ".txt"
            };

            if (saveFileDialog.ShowDialog() == DialogResult.OK)
            {
                string filePath = saveFileDialog.FileName;
                File.WriteAllText(filePath, content);
                return "success";
            }
            else
            {
                return "";
            }
        }

        /// <summary>
        /// 导出视频/文件文字内容
        /// </summary>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <param name="id">视频ID/文件ID</param>
        /// <param name="fileName">视频名/文件名</param>
        [HttpGet("导出视频/文件文字内容", "/alysestxt")]
        public string AlysesTxt(int type, string id, string fileName)
        {
            string content = "";

            if (type == 0)
            {

                // 获取视频信息
                VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(id);
                if(videoEntity == null)
                {
                    throw new Exception("视频信息不存在");
                }

                string filePath = Path.GetFullPath($"analysisData\\video\\{videoEntity.startTime.Substring(0, 10)}\\{id}.txt");
                if(File.Exists(filePath))
                {
                    string jsonStr = File.ReadAllText(filePath);
                    AnalysisResultTxtVo analysisResultTxtVo = JsonConvert.DeserializeObject<AnalysisResultTxtVo>(jsonStr);
                    if (analysisResultTxtVo != null)
                    {
                        List<AudioaAlysis> audioaAlyses = analysisResultTxtVo.audioaAlyses;
                        if (audioaAlyses != null && audioaAlyses.Count > 0)
                        {
                            audioaAlyses.Sort((x, y) => x.Paragraph.CompareTo(y.Paragraph));
                            foreach (var item in audioaAlyses)
                            {
                                if (item.Status == 0)
                                {
                                    JObject jsonObject = JObject.Parse(item.DataJson);
                                    content += (string)jsonObject["content"] + "\r\n\r\n";
                                }
                            }
                        }
                    }
                }
            }
            else
            {

                UploadFileEntity uploadFileEntity = UploadFileApi.GetFileByFileIdSync(id);
                if (uploadFileEntity == null)
                {
                    throw new Exception("文件信息不存在");
                }

                string filePath = Path.GetFullPath($"analysisData\\file\\{uploadFileEntity.uploadTime.Substring(0, 10)}\\{id}.txt");
                if (File.Exists(filePath))
                {
                    string jsonStr = File.ReadAllText(filePath);
                    AnalysisResultTxtVo analysisResultTxtVo = JsonConvert.DeserializeObject<AnalysisResultTxtVo>(jsonStr);
                    if (analysisResultTxtVo != null)
                    {
                        List<UploadFileAlysis> fileAudioaAlyses = analysisResultTxtVo.fileAudioaAlyses;
                        if (fileAudioaAlyses != null && fileAudioaAlyses.Count > 0)
                        {
                            fileAudioaAlyses.Sort((x, y) => x.Paragraph.CompareTo(y.Paragraph));
                            foreach (var item in fileAudioaAlyses)
                            {
                                if (item.Status == 0)
                                {
                                    JObject jsonObject = JObject.Parse(item.DataJson);
                                    content += (string)jsonObject["content"] + "\r\n\r\n";
                                }
                            }
                        }
                    }
                }
            }

            SaveFileDialog saveFileDialog = new SaveFileDialog
            {
                Filter = "Text files (*.txt)|*.txt|All files (*.*)|*.*",
                FilterIndex = 1,
                RestoreDirectory = true,
                FileName = fileName + ".txt"
            };

            if (saveFileDialog.ShowDialog() == DialogResult.OK)
            {
                string filePath = saveFileDialog.FileName;
                File.WriteAllText(filePath, content);
                return "success";
            }else
            {
                return "";
            }
        }

        /// <summary>
        /// 导出敏感词/关键词列表excel
        /// </summary>
        /// <param name="dataJson">列表json数据</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <param name="id">视频ID/文件ID</param>
        //[HttpPost("导出敏感词/关键词列表excel", "/wordsexcel")]
        //public string WordsList(string dataJson, int type, int id)
        //{

        //    // 创建一个新的 Excel 工作簿
        //    using (var package = new ExcelPackage())
        //    {
        //        // 添加一个新的工作表
        //        var worksheet = package.Workbook.Worksheets.Add("Sheet1");

        //        List<WordsMarkVo> wordsMarks = JsonConvert.DeserializeObject<List<WordsMarkVo>>(dataJson);

        //        // 填充数据
        //        worksheet.Cells["A1"].Value = "类型";
        //        worksheet.Cells["B1"].Value = "词语";
        //        worksheet.Cells["C1"].Value = "次数";
        //        worksheet.Cells["D1"].Value = "来源";
        //        worksheet.Cells["E1"].Value = "描述";
        //        worksheet.Cells["F1"].Value = "行业";
        //        worksheet.Cells["G1"].Value = "分类";
        //        worksheet.Cells["H1"].Value = "等级";

        //        for (int i = 0; i < wordsMarks.Count; i++)
        //        {
        //            var item = wordsMarks[i];
        //            worksheet.Cells["A" + (i + 2)].Value = item.WordsTypeStr;
        //            worksheet.Cells["B" + (i + 2)].Value = item.Name;
        //            worksheet.Cells["C" + (i + 2)].Value = item.CountNum;
        //            worksheet.Cells["D" + (i + 2)].Value = item.ResourceTypeStr;
        //            worksheet.Cells["E" + (i + 2)].Value = item.Remarks;
        //            worksheet.Cells["F" + (i + 2)].Value = item.TradeStr;
        //            worksheet.Cells["G" + (i + 2)].Value = item.TypeStr;
        //            worksheet.Cells["H" + (i + 2)].Value = item.LevelStr;
        //        }

        //        string fileName = "test";
        //        if (type == 0)
        //        {
        //            AnchorVideo anchorVideo = new AnchorVideo();
        //            anchorVideo.GetModelById(id);
        //            fileName = anchorVideo.VideoName;
        //        }
        //        else if (type == 1)
        //        {
        //            UploadFile uploadFile = new UploadFile();
        //            uploadFile.GetModelById(id);
        //            fileName = uploadFile.FileName;
        //        }

        //        // 使用 SaveFileDialog 让用户选择保存位置
        //        SaveFileDialog saveFileDialog = new SaveFileDialog
        //        {
        //            Filter = "Excel files (*.xlsx)|*.xlsx|All files (*.*)|*.*",
        //            FilterIndex = 1,
        //            RestoreDirectory = true,
        //            FileName = fileName + ".xlsx"
        //        };

        //        if (saveFileDialog.ShowDialog() == DialogResult.OK)
        //        {
        //            // 保存文件
        //            FileInfo file = new FileInfo(saveFileDialog.FileName);
        //            package.SaveAs(file);
        //            return "success";
        //        }else
        //        {
        //            return "";
        //        }
        //    }
        //}

    }

}
