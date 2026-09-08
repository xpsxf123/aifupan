using System.Collections.Generic;

namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// Tencent ASR language code constants and normalization helpers.
    /// </summary>
    public static class AsrLanguageCode
    {
        public const string Default = "16k_zh";

        /// <summary>
        /// SenseVoiceSmall internal language token strings — the values returned by
        /// <see cref="ToSvsLang"/>. Use these constants for cross-module dictionary
        /// keys to avoid magic-string drift.
        /// </summary>
        public static class SvsLang
        {
            public const string Auto = "auto";
            public const string Zh   = "zh";
            public const string En   = "en";
            public const string Yue  = "yue";
            public const string Ja   = "ja";
            public const string Ko   = "ko";
        }

        private static readonly HashSet<string> ValidCodes = new HashSet<string>
        {
            "16k_zh", "16k_zh-PY", "16k_zh_medical", "16k_en",
            "16k_yue", "16k_ja", "16k_ko", "16k_vi", "16k_ms",
            "16k_id", "16k_fil", "16k_th", "16k_pt", "16k_tr",
            "16k_ar", "16k_es", "16k_hi", "16k_fr", "16k_de"
        };

        // Languages natively supported by SenseVoiceSmall
        private static readonly HashSet<string> SvsSupportedCodes = new HashSet<string>
        {
            "16k_zh", "16k_zh-PY", "16k_zh_medical",
            "16k_en", "16k_yue", "16k_ja", "16k_ko"
        };

        /// <summary>
        /// Returns the code unchanged if valid, otherwise returns "16k_zh".
        /// </summary>
        public static string Normalize(string code) =>
            !string.IsNullOrEmpty(code) && ValidCodes.Contains(code) ? code : Default;

        /// <summary>
        /// Returns true if SenseVoiceSmall natively supports the given language code.
        /// </summary>
        public static bool IsSvsSupported(string normalizedCode) =>
            SvsSupportedCodes.Contains(normalizedCode);

        /// <summary>
        /// Maps a normalized Tencent code to the backend engine identifier string.
        /// Used when reporting engine choice to the backend API.
        /// SVS-supported → "sense-voice"; unsupported → "tencent".
        /// </summary>
        public static string ToBackendEngine(string normalizedCode) =>
            IsSvsSupported(normalizedCode) ? "sense-voice" : "tencent";

        /// <summary>
        /// Maps a normalized Tencent code to the SenseVoiceSmall language token.
        /// Languages not natively supported fall back to "auto".
        /// </summary>
        public static string ToSvsLang(string normalizedCode)
        {
            switch (normalizedCode)
            {
                case "16k_zh":
                case "16k_zh-PY":
                case "16k_zh_medical": return SvsLang.Zh;
                case "16k_en":         return SvsLang.En;
                case "16k_yue":        return SvsLang.Yue;
                case "16k_ja":         return SvsLang.Ja;
                case "16k_ko":         return SvsLang.Ko;
                default:               return SvsLang.Auto;
            }
        }
    }
}
