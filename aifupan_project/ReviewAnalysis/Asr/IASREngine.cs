using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// Unified ASR engine interface. All engines (local, cloud, composite) implement this.
    /// </summary>
    public interface IASREngine
    {
        /// <param name="audioFile">Sliced MP3 file (16kHz, mono, ≤59s)</param>
        /// <param name="engSerViceType">Engine config string (e.g. "16k_zh", "svs:auto:withitn|tencent")</param>
        /// <returns>
        /// Dictionary with "code" (int) and "data" (List&lt;ASRResultEntity&gt;).
        /// code=0 success, 500=error, 702=model not ready, 703=decode failed, 704=input too long.
        /// </returns>
        Task<Dictionary<string, object>> RecognizeAsync(FileInfo audioFile, string engSerViceType);
    }
}
