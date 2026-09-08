using ReviewAnalysis.Asr;

namespace ReviewAnalysis.plugins.utils
{
    public class PathUtils
    {
        // 存储路径
        public static string PlatformPath => @"dataCollect\platform"+$"\\{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}";
    }
}