using douyin.Utils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// Tencent Cloud ASR adapter. Wraps existing MyCallableTask, preserving
    /// all retry/QPS/auth logic with zero changes to the original code.
    /// </summary>
    public class TencentASREngine : IASREngine
    {
        public TencentASREngine()
        {
        }

        public async Task<Dictionary<string, object>> RecognizeAsync(FileInfo audioFile, string engSerViceType)
        {
            try
            {
            string modelType = ResolveModelType(engSerViceType);

            FileUtils.LogAnalysis($"[ASR][Tencent] 开始识别, modelType={modelType}, file={audioFile.Name}");

            string dirPath = audioFile.DirectoryName ?? string.Empty;
            var task = new MyCallableTask(audioFile, dirPath);

            var result = await task.StartAsTaskAsync(null, modelType);

            // Normalize: MyCallableTask returns single ASRResultEntity,
            // but IASREngine contract requires List&lt;ASRResultEntity&gt;.
            if (result["data"] is ASRResultEntity entity)
                result["data"] = new List<ASRResultEntity> { entity };
            else if (result["data"] == null)
                result["data"] = new List<ASRResultEntity>();

            FileUtils.LogAnalysis($"[ASR][Tencent] 识别完成, code={result["code"]}");
            return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[ASR][Tencent] 未预期异常: {ex.Message}");
                return new Dictionary<string, object>
                {
                    { "code", 500 },
                    { "data", new List<ASRResultEntity> { new ASRResultEntity { Code = 500, Msg = "Tencent引擎异常: " + ex.Message } } }
                };
            }
        }

        private static string ResolveModelType(string engSerViceType)
        {
            if (string.IsNullOrEmpty(engSerViceType))
                return "16k_zh";

            // If it's a pipeline like "svs:auto:withitn|tencent", extract the tencent part.
            string[] parts = engSerViceType.Split('|');
            foreach (string part in parts)
            {
                string trimmed = part.Trim();
                if (trimmed == "tencent" || (!trimmed.StartsWith("svs:") && !trimmed.Contains(":")))
                    return trimmed == "tencent" ? "16k_zh" : trimmed;
            }

            // Fallback: first non-svs part or default
            return "16k_zh";
        }
    }
}
