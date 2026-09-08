namespace ReviewAnalysis.bo
{
    public class BasicSettingsBo
    {
        /// <summary>
        /// 账号归属类型 0：自有账号 1：同行账号
        /// </summary>
        public int? accountType { get; set; }
        
        /// <summary>
        /// 首播日期
        /// </summary>
        public string premiereDate { get; set; }
        
        /// <summary>
        /// 账号阶段，使用字典account_stage的值
        /// </summary>
        public int? accountStage { get; set; }
        
        /// <summary>
        /// 账号水平， 使用字典account_water_level的值
        /// </summary>
        public int? accountWaterLevel { get; set; }

        /// <summary>
        /// 浏览结构，使用字典 account_flow的值
        /// </summary>
        public int? accountFlow { get; set; }
        
        /// <summary>
        /// 直播目标，使用字典living_target的值
        /// </summary>
        public int? livingTarget { get; set; }
        
        /// <summary>
        /// 直播方式，使用字典living_modality的值
        /// </summary>
        public int? livingModality { get; set; }
        
        /// <summary>
        /// 营销方式，使用字典marketing的值
        /// </summary>
        public int? marketing { get; set; }
        
        /// <summary>
        /// 优化方向，使用字典optimize_direction的值
        /// </summary>
        public string optimizeDirection { get; set; }
        
        /// <summary>
        /// 学习方向，使用字典learning的值
        /// </summary>
        public string learning { get; set; }
        
        /// <summary>
        /// 直播模式，使用字典living_mode的值
        /// </summary>
        public int? livingMode { get; set; }
        
        /// <summary>
        /// 主播账号情况描述
        /// </summary>
        public string anchorSituation { get; set; }

        /// <summary>
        /// ROI观测精度，使用字典roi_accuracy的值
        /// </summary>
        public string roiAccuracy { get; set; }
    }
}