using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.vo;

namespace ReviewAnalysis.Asr
{
    public class WordsMarkVo
    {
        /// <summary>
        /// 词语类型  0：敏感词 1：关键词
        /// </summary>
        [JsonProperty("wordsType")]
        public int WordsType { get; set; }

        /// <summary>
        /// 记录要标注的词语位置，比如[0, 2]则表示标注当前段落第0个、第2个词语
        /// </summary>
        [JsonProperty("recordNeedsWordList")]
        public List<RecordNeedsWordVo> RecordNeedsWordList { get; set; }

        /// <summary>
        /// 匹配中的限定词
        /// </summary>
        [JsonProperty("restrictWord")]
        public string RestrictWord { get; set; }
        /// <summary>
        /// 限定词范围
        /// </summary>
        [JsonProperty("restrictRange")]
        public int RestrictRange { get; set; }

        /// <summary>
        /// 词语类型
        /// </summary>
        [JsonProperty("wordsTypeStr")]
        public string WordsTypeStr { get; set; }

        /// <summary>
        /// 来源类型 0：系统 1：客户自定义
        /// </summary>
        [JsonProperty("resourceType")]
        public int ResourceType { get; set; }

        /// <summary>
        /// 来源类型
        /// </summary>
        [JsonProperty("resourceTypeStr")]
        public string ResourceTypeStr { get; set; }

        /// <summary>
        /// 细分类型
        /// 关键词 0：促单 1：互动 2：其他
        /// 敏感词 0：广告 1：品牌 2：国家 3：限制词 4：其他
        /// </summary>
        [JsonProperty("type")]
        public int Type { get; set; }

        /// <summary>
        /// 细分类型
        /// </summary>
        [JsonProperty("typeStr")]
        public string TypeStr { get; set; }

        /// <summary>
        /// 平台类型 0：全平台 1：抖音 2：快手 3：微视
        /// </summary>
        [JsonProperty("platformType")]
        public int PlatformType { get; set; }

        /// <summary>
        /// 平台类型
        /// </summary>
        [JsonProperty("platformTypeStr")]
        public string PlatformTypeStr { get; set; }

        /// <summary>
        /// 关键词名字
        /// </summary>
        [JsonProperty("name")]
        public string Name { get; set; }

        /// <summary>
        /// 敏感词等级 0：1级,封号  1：2级,严重警告  2：3级警告
        /// </summary>
        [JsonProperty("level")]
        public int? Level { get; set; }

        /// <summary>
        /// 敏感词等级
        /// </summary>
        [JsonProperty("levelStr")]
        public string LevelStr { get; set; }

        /// <summary>
        /// 出现次数
        /// </summary>
        [JsonProperty("countNum")]
        public int CountNum { get; set; }

        /// <summary>
        /// 描述
        /// </summary>
        [JsonProperty("remarks")]
        public string Remarks { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        [JsonProperty("tradeId")]
        public string TradeId { get; set; }

        /// <summary>
        /// 行业
        /// </summary>
        [JsonProperty("tradeStr")]
        public string TradeStr { get; set; }

        /// <summary>
        /// 分组
        /// </summary>
        [JsonProperty("groupStr")]
        public string GroupStr { get; set; }
        /// <summary>
        /// 关键词类型id
        /// </summary>
        [JsonProperty("cruxTypeId")]
        public string CruxTypeId { get; set; }
        /// <summary>
        /// 关键词类型信息
        /// </summary>
        [JsonProperty("cruxTypeInfo")]
        public CruxTypeInfoVo CruxTypeInfo { get; set; }
        /// <summary>
        /// 词语在文中出现的总次数
        /// </summary>
        [JsonProperty("totalNum")]
        public int TotalNum { get; set; }
        /// <summary>
        /// 概览
        /// </summary>
        [JsonProperty("overView")]
        public string OverView { get; set; }
    }
}
