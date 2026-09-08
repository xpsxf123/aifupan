using Newtonsoft.Json.Linq;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Diagnostics;
using douyin.Utils;
using ReviewAnalysis.api;

namespace ReviewAnalysis.Asr
{
    public class MyCallableTask
    {
        private FileInfo _file;
        private string _directoryPath;

        public MyCallableTask(FileInfo file, string directoryPath)
        {
            _file = file;
            _directoryPath = directoryPath;
        }

        /// <summary>
        /// Async version of StartAsTask. Uses Task.Delay instead of Thread.Sleep
        /// so the calling thread is not blocked during retry waits.
        /// </summary>
        public async Task<Dictionary<string, object>> StartAsTaskAsync(AudioTempTokenEntity audioTempTokenEntity, string engSerViceType = "16k_zh")
        {
            Dictionary<string, object> result = new Dictionary<string, object>();
            int qpsCount = 300;
            int asrCount = 3;

            while (asrCount > 0)
            {
                try
                {
                    if (audioTempTokenEntity == null)
                        audioTempTokenEntity = AsrApi.GetTempToken("");

                    if (audioTempTokenEntity != null)
                    {
                        dynamic bodyJsonObject = new JObject();
                        byte[] fileContent = File.ReadAllBytes(Path.Combine(_directoryPath, _file.Name));
                        bodyJsonObject.DataLen = fileContent.Length;
                        bodyJsonObject.Data = Convert.ToBase64String(fileContent);

                        ASRResultEntity asrResultEntity = ASRHttpUtils.DoRequest(
                            audioTempTokenEntity.TempSecretId,
                            audioTempTokenEntity.TempSecretKey,
                            audioTempTokenEntity.Token,
                            bodyJsonObject,
                            engSerViceType);

                        if (asrResultEntity == null)
                        {
                            if (asrCount <= 0)
                            {
                                FileUtils.LogAnalysis($"{_file.Name}", "识别错误超出次数，放弃本条语音识别");
                                result.Add("code", 500);
                                result.Add("data", new ASRResultEntity { FileName = _file.Name, Result = "识别失败", Code = 500 });
                                return result;
                            }
                            await Task.Delay(5000);
                            asrCount--;
                            continue;
                        }
                        else if (asrResultEntity.Error != null && !string.IsNullOrEmpty(asrResultEntity.Error.Code)
                            && asrResultEntity.Error.Code.Contains("AuthFailure.SignatureExpire"))
                        {
                            result.Add("code", 601);
                            asrResultEntity.Code = 601;
                            result.Add("data", asrResultEntity);
                            return result;
                        }
                        else if (asrResultEntity.Error != null && !string.IsNullOrEmpty(asrResultEntity.Error.Code)
                            && asrResultEntity.Error.Code.Contains("RequestLimitExceeded"))
                        {
                            if (qpsCount <= 0)
                            {
                                FileUtils.LogAnalysis($"{_file.Name}QPS 超过了限制");
                                result.Add("code", 603);
                                result.Add("data", new ASRResultEntity { FileName = _file.Name, Result = "识别失败", Code = 500 });
                                return result;
                            }
                            await Task.Delay(5000);
                            qpsCount--;
                            continue;
                        }
                        else if (asrResultEntity.Error != null && !string.IsNullOrEmpty(asrResultEntity.Error.Code))
                        {
                            await Task.Delay(5000);
                            asrCount--;
                            continue;
                        }

                        asrResultEntity.FileName = _file.Name;
                        asrResultEntity.Code = 0;
                        result.Add("code", 0);
                        result.Add("data", asrResultEntity);
                        return result;
                    }
                    else
                    {
                        FileUtils.LogAnalysis("获取语音识别接口临时调用凭证失败");
                        if (asrCount <= 0)
                        {
                            result.Add("code", 602);
                            result.Add("data", new ASRResultEntity { FileName = _file.Name, Result = "识别失败", Code = 500 });
                            return result;
                        }
                        await Task.Delay(5000);
                        asrCount--;
                        continue;
                    }
                }
                catch (Exception e)
                {
                    FileUtils.LogAnalysis($"腾讯语音识别接口异常：{e}");
                    asrCount--;
                }
            }

            result.Add("code", 500);
            result.Add("data", new ASRResultEntity { FileName = _file.Name, Result = "识别失败", Code = 500 });
            return result;
        }
    }
}

