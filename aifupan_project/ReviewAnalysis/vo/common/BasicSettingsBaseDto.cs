namespace ReviewAnalysis.vo.common
{
    /// <summary>
    /// 基础设置 DTO 基类
    /// </summary>
    public class BasicSettingsBaseDto
    {
        /// <summary>
        /// 账号归属类型 0：自由账号 1：同行账号
        /// </summary>
        public int? accountType { get; set; }

        /// <summary>
        /// 话术质检开关 0：关闭 1：开启
        /// </summary>
        public int? isScriptQualityInspection { get; set; }

        /// <summary>
        /// 话术还原度开关 0：关闭 1：开启
        /// </summary>
        public int? isScriptFidelityMonitor { get; set; }

        /// <summary>
        /// 互动巡检开关 0：关闭 1：开启
        /// </summary>
        public int? isInteractionPatrol { get; set; }

        /// <summary>
        /// 已确认标准直播稿 ID；isScriptFidelityMonitor=1 时由前端必传
        /// </summary>
        public long? standardScriptId { get; set; }

        /// <summary>
        /// 首播日期
        /// </summary>
        public string premiereDate { get; set; }

        /// <summary>
        /// 账号阶段，使用字典 account_stage 的值
        /// </summary>
        public int? accountStage { get; set; }

        /// <summary>
        /// 账号水平，使用字典 account_water_level 的值
        /// </summary>
        public int? accountWaterLevel { get; set; }

        /// <summary>
        /// 流量结构，使用字典 account_flow 的值
        /// </summary>
        public int? accountFlow { get; set; }

        /// <summary>
        /// 直播目标 使用字典 living_target 的值
        /// </summary>
        public int? livingTarget { get; set; }

        /// <summary>
        /// 直播形式 使用字典 living_modality 的值
        /// </summary>
        public int? livingModality { get; set; }

        /// <summary>
        /// 营销组件 使用字典 marketing 的值
        /// </summary>
        public int? marketing { get; set; }

        /// <summary>
        /// 优化方向多选 使用字典 optimize_direction 的值
        /// </summary>
        public string optimizeDirection { get; set; }
        
        /// <summary>
        /// 学习方向，使用字典learning的值
        /// </summary>
        public string learning { get; set; }

        /// <summary>
        /// 直播间模式 使用字典 living_mode 的值
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

        /// <summary>
        /// 字段拷贝
        /// </summary>
        /// <param name="source">源对象</param>
        /// <param name="target">目标对象</param>
        /// <typeparam name="T">泛型必须继承BasicSettingsBaseDto类</typeparam>
        public void copyProperties<T>(T source) where T : BasicSettingsBaseDto
        {
            this.accountType = source.accountType ?? this.accountType;
            this.premiereDate = source.premiereDate ?? this.premiereDate;
            this.accountStage = source.accountStage ?? this.accountStage;
            this.accountWaterLevel = source.accountWaterLevel ?? this.accountWaterLevel;
            this.accountFlow = source.accountFlow ?? this.accountFlow;
            this.livingTarget = source.livingTarget ?? this.livingTarget;
            this.livingModality = source.livingModality ?? this.livingModality;
            this.marketing = source.marketing ?? this.marketing;
            this.optimizeDirection = source.optimizeDirection ?? this.optimizeDirection;
            this.learning = source.learning ?? this.learning;
            this.livingMode = source.livingMode ?? this.livingMode;
            this.anchorSituation = source.anchorSituation ?? this.anchorSituation;
            this.roiAccuracy = source.roiAccuracy ?? this.roiAccuracy;
        }
    }
}