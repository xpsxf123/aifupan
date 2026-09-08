using System;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.plugins.models;
using Newtonsoft.Json.Linq;
using douyin.Utils;

namespace ReviewAnalysis.plugins.adapters
{
    /// <summary>
    /// 主播后台数据适配器
    /// 将主播后台原始JSON转换为统一数据模型
    /// </summary>
    public class AnchorLiveAdapter : IDataAdapter
    {
        public string PlatformId => "anchorLive";
        public string PlatformName => "主播后台";

        /// <summary>
        /// 适配主播后台数据
        /// </summary>
        public UnifiedDataModel Adapt(string rawJson, string secUid, string roomId, string videoId = null)
        {
            var model = CreateBaseModel(secUid, roomId, videoId);

            if (string.IsNullOrEmpty(rawJson))
            {
                return model;
            }

            try
            {
                var jo = JObject.Parse(rawJson);

                // 解析主播后台数据
                if (jo["data"] != null)
                {
                    var data = jo["data"];

                    // 主播后台特有字段
                    // TODO: 根据实际API返回字段补充
                    model.viewCount = data["view_count"]?.Value<long?>();
                    model.salesRevenue = data["sales_revenue"]?.Value<double?>();
                }

                model.CalculateDerivedFields();
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"主播后台数据适配异常: {ex.Message}", "AnchorLiveAdapter");
            }

            return model;
        }

        /// <summary>
        /// 创建基础模型
        /// </summary>
        private UnifiedDataModel CreateBaseModel(string secUid, string roomId, string videoId)
        {
            return new UnifiedDataModel
            {
                secUid = secUid,
                batchNumber = roomId,
                platform = 0,
                startTime = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                oceanEngineProcessList = new System.Collections.Generic.List<OceanEngineProcessBo>(),
                productList = new System.Collections.Generic.List<VideoProductRequest>()
            };
        }
    }
}
