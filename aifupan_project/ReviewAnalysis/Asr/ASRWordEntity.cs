using Newtonsoft.Json;

namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// 单个识别词条 (word + 时间戳)。
    ///
    /// 重要：JSON 字段名必须是 PascalCase ("Word" / "StartTime" / "EndTime")。
    /// 后端 `com.jiuyu.replay.words.bo.WordsMarkItemBo` 用 Jackson 注解
    /// `@JsonProperty("Word"/"StartTime"/"EndTime")` 强制匹配 PascalCase，
    /// 缺这个映射时 endTime 反序列化为 null，引发 NPE。
    ///
    /// 使用显式 [JsonProperty] PascalCase 注解，可以覆盖任何
    /// CamelCaseContractResolver / 全局 JsonConvert.DefaultSettings —
    /// 不管上层用什么序列化设置，本类输出永远是 PascalCase。
    /// </summary>
    public class ASRWordEntity
    {
        /// <summary>
        /// 词语
        /// </summary>
        [JsonProperty("Word")]
        public string Word { get; set; }

        /// <summary>
        /// 词语在音频的开始时间，单位：毫秒
        /// </summary>
        [JsonProperty("StartTime")]
        public long StartTime { get; set; }

        /// <summary>
        /// 词语在音频的结束时间，单位：毫秒
        /// </summary>
        [JsonProperty("EndTime")]
        public long EndTime { get; set; }
    }
}
