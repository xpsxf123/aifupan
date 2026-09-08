using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Linq;

namespace ReviewAnalysis.Asr
{
    public class ASRResultEntity
    {
        /// <summary>
        /// 第几段音频
        /// </summary>
        public int Paragraph { get; set; }
        /// <summary>
        /// 音频文件名
        /// </summary>
        public string FileName { get; set; }
        /// <summary>
        /// 请求id
        /// </summary>
        public string RequestId { get; set; }
        /// <summary>
        /// 一句话识别错误码
        /// </summary>
        public ASRResultErrorEntity Error { get; set; }
        /// <summary>
        /// 识别的文字结果
        /// </summary>
        public string Result { get; set; }
        /// <summary>
        /// 音频时长，单位：毫秒
        /// </summary>
        public long AudioDuration { get; set; }
        /// <summary>
        /// 词语数量
        /// </summary>
        public string WordSize { get; set; }
        /// <summary>
        /// 词语列表
        /// </summary>
        public List<ASRWordEntity> WordList { get; set; }
        /// <summary>
        /// 询问是否还有QPS服务器返回的code
        /// </summary>
        public int Code { get; set; }
        /// <summary>
        /// 询问是否还有QPS服务器返回的msg
        /// </summary>
        public string Msg { get; set; }

        /// <summary>
        /// SVS CTC 解码总 token 数（含 blank/meta/content）。
        /// SVS 引擎填写；其他引擎默认 0，不参与 CompositeASREngine.IsLowQuality 判定。
        /// </summary>
        public int DecodedTokens { get; set; }

        /// <summary>
        /// 经 FilterBlanksAndMetas 过滤后的有效内容 token 数。SVS 引擎填写。
        /// </summary>
        public int ContentTokens { get; set; }

        /// <summary>
        /// 折叠 token 数 = DecodedTokens - ContentTokens（含 blank + meta）。
        /// SVS 引擎填写；CompositeASREngine 用 CollapsedTokens / DecodedTokens 判定唱歌段。
        /// </summary>
        public int CollapsedTokens { get; set; }

        /// <summary>
        /// 标记本条 chunk 结果是否来自 CompositeASREngine 的 Tencent fallback (action=REPLACED)。
        /// 用于 AsrUtils 在 video 级输出 FALLBACK 比例汇总，方便运营观察。其他引擎 / SVS 原始结果默认 false。
        /// </summary>
        public bool FromFallback { get; set; }

        /// <summary>
        /// 标记本条 chunk 曾被 CompositeASREngine.IsLowQuality 判定为低质（命中 collapsed_ratio 或 char_density 规则）。
        /// 不论 Tencent 兜底是否成功，只要被判低质就 = true；最终保留的 entity（SVS 或被替换的 Tencent）都会带此标记。
        /// 用于 AsrUtils 在 video 级输出低质 chunk 数汇总。单引擎链不评分时永远 false。
        /// </summary>
        public bool LowQuality { get; set; }
    }
}
