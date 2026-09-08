using System;

namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// ASR 业务消费方。后端按业务方差异化下发引擎优先级。
    /// 三大消费方与调用入口的映射见 wiki/domain/asr_consumers.md。
    /// </summary>
    public enum AsrConsumerType
    {
        AnchorReplay,
        UserUpload,
        ShortVideo
    }

    public static class AsrConsumerTypeExtensions
    {
        /// <summary>
        /// 序列化为后端约定的字符串码。固定映射，重命名 enum 不影响线协议。
        /// </summary>
        public static string ToWireCode(this AsrConsumerType consumer)
        {
            switch (consumer)
            {
                case AsrConsumerType.AnchorReplay: return "anchor_replay";
                case AsrConsumerType.UserUpload:   return "user_upload";
                case AsrConsumerType.ShortVideo:   return "short_video";
                default:
                    throw new ArgumentOutOfRangeException(
                        nameof(consumer), consumer, "Unknown AsrConsumerType");
            }
        }
    }
}
