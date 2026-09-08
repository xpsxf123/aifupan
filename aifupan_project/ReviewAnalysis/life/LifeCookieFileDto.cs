using System;
using System.Collections.Generic;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace ReviewAnalysis.life
{
    /// <summary>
    /// 来客Cookie文件包装类（新格式）。
    /// 旧格式为 List&lt;LifeCookieDto&gt;，新格式在Cookie列表基础上增加了抖音号元数据。
    /// 读取时通过 IsNewFormat / TryParse 兼容两种格式。
    /// </summary>
    public class LifeCookieFileDto
    {
        /// <summary>
        /// 当前抖音号的 aweme_user_id（采集时用于区分不同抖音号）
        /// </summary>
        public string AwemeUserId { get; set; }

        /// <summary>
        /// 当前抖音号的 secUid（冗余备份）
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// Cookie 列表（同一来客商家下各抖音号内容相同）
        /// </summary>
        public List<LifeCookieDto> Cookies { get; set; } = new List<LifeCookieDto>();

        // ---------------------------------------------------------------------
        // 读取/序列化辅助方法
        // ---------------------------------------------------------------------

        /// <summary>
        /// 尝试从 JSON 字符串解析 Cookie 文件，兼容新旧格式。
        /// </summary>
        /// <param name="jsonContent">文件内容</param>
        /// <returns>解析结果（成功时 IsNewFormat=true 表示新格式）</returns>
        public static (LifeCookieFileDto result, bool isNewFormat) Parse(string jsonContent)
        {
            if (string.IsNullOrEmpty(jsonContent))
            {
                return (new LifeCookieFileDto(), false);
            }

            try
            {
                var token = JToken.Parse(jsonContent);

                if (token is JArray)
                {
                    // 旧格式：直接是 Cookie 数组
                    var oldCookies = token.ToObject<List<LifeCookieDto>>();
                    return (new LifeCookieFileDto { Cookies = oldCookies ?? new List<LifeCookieDto>() }, false);
                }

                if (token is JObject obj)
                {
                    // 新格式：带元数据的对象
                    // 兼容 PascalCase（ToJson序列化）和 camelCase 两种写法
                    var dto = new LifeCookieFileDto
                    {
                        AwemeUserId = (obj["AwemeUserId"] ?? obj["awemeUserId"])?.ToString(),
                        SecUid = (obj["SecUid"] ?? obj["secUid"])?.ToString(),
                        Cookies = (obj["Cookies"] ?? obj["cookies"])?.ToObject<List<LifeCookieDto>>() ?? new List<LifeCookieDto>()
                    };
                    return (dto, true);
                }
            }
            catch
            {
                // 解析失败，返回空
            }

            return (new LifeCookieFileDto(), false);
        }

        /// <summary>
        /// 序列化为 JSON 字符串（新格式）。
        /// </summary>
        public string ToJson()
        {
            return JsonConvert.SerializeObject(this, Formatting.Indented);
        }
    }

    // =====================================================================
    // SecUid.json 结构定义（最终版）
    // 顶层是数组 List<LifeAccountRootDto>，每项代表一个来客账号。
    // 每个来客账号下有 accounts[] 公司列表，每家公司下有 awemeUsers[] 拖音号列表。
    // =====================================================================

    /// <summary>
    /// SecUid.json 顶层数组每项：代表一个来客账号。
    /// 一个来客账号可管理多家公司。
    /// </summary>
    public class LifeAccountRootDto
    {
        /// <summary>
        /// 来客账号ID（来自 GroupAccountListShop 返回的 life_account_id）
        /// </summary>
        [JsonProperty("life_account_id")]
        public string LifeAccountId { get; set; }

        /// <summary>
        /// 授权批次ID：同一次授权（ResolveAllAccountsAsync）产生的所有记录共享同一个 batchId。
        /// 用于确认跨 life_account_id 的兄弟关系（同批次 = 同一个 cookie session = 可互相复用）。
        /// </summary>
        [JsonProperty("authBatchId")]
        public string AuthBatchId { get; set; }

        /// <summary>
        /// 该来客账号下的公司列表
        /// </summary>
        [JsonProperty("accounts")]
        public List<LifeCompanyDto> Accounts { get; set; } = new List<LifeCompanyDto>();
    }

    /// <summary>
    /// 公司（商户）信息，属于某个来客账号下的一家公司。
    /// </summary>
    public class LifeCompanyDto
    {
        /// <summary>
        /// 公司唯一标识（来自 GroupAccountListShop 返回的 account_id / key_account_id）
        /// </summary>
        [JsonProperty("groupId")]
        public string GroupId { get; set; }

        /// <summary>
        /// 公司名称（来自 GroupAccountListShop 返回的 account_name / life_account_name）
        /// </summary>
        [JsonProperty("accountName")]
        public string AccountName { get; set; }

        /// <summary>
        /// 此公司是否为线索版（来自 CluePcUserInfo + GetHomeMenusText 判断）
        /// </summary>
        [JsonProperty("iscue")]
        public bool Iscue { get; set; }

        /// <summary>
        /// 处理结果信息（成功时为空，失败/跳过时记录原因）
        /// </summary>
        [JsonProperty("msg")]
        public string Msg { get; set; } = "";

        /// <summary>
        /// 公司旗下的所有拖音号信息（来自 GetAwemeUsers 或 ClueGetAwemeUsers）
        /// </summary>
        [JsonProperty("awemeUsers")]
        public List<LifeAwemeUserInfoDto> AwemeUsers { get; set; } = new List<LifeAwemeUserInfoDto>();
    }

    /// <summary>
    /// 公司旗下的单个拖音号信息。
    /// </summary>
    public class LifeAwemeUserInfoDto
    {
        /// <summary>
        /// 拖音UID（来自 GetAwemeUsers.aweme_user_id 或 ClueGetAwemeUsers.douyinUID）
        /// </summary>
        [JsonProperty("awemeUserId")]
        public string AwemeUserId { get; set; }

        /// <summary>
        /// 拖音昵称（来自 GetAwemeUsers.nick_name 或 ClueGetAwemeUsers.douyinNickname）
        /// </summary>
        [JsonProperty("nickname")]
        public string Nickname { get; set; }

        /// <summary>
        /// 生效状态：active=生效 / inactive=无效。
        /// 同一个拖音号在多家公司或多个来客账号中出现时，只有一条为 active，其余为 inactive。
        /// </summary>
        [JsonProperty("status")]
        public string Status { get; set; } = "active";

        /// <summary>
        /// 拖音号对应的 SecUid（从本地主播缓存反查 AnchorUserId → SecUid，未录入时为空）
        /// </summary>
        [JsonProperty("secUid")]
        public string SecUid { get; set; } = "";

        /// <summary>
        /// Cookie 文件名（相对于 dataCollect/config/ 目录，例如 life-abc123def）。
        /// 未录入系统或未生成 cookie 时为空。
        /// </summary>
        [JsonProperty("cookiePath")]
        public string CookiePath { get; set; } = "";
    }

    // =====================================================================
    // 以下为旧版兼容 DTO，保留供现有采集代码读取（过渡期后可删除）
    // =====================================================================

    /// <summary>
    /// （旧版兼容）SecUid.json 旧格式单条记录。
    /// 新代码不应再使用此类，仅用于读取旧数据时反序列化。
    /// </summary>
    public class LifeAccountMappingDto
    {
        [JsonProperty("lifeAccountId")]
        public string LifeAccountId { get; set; }

        [JsonProperty("groupId")]
        public string GroupId { get; set; }

        [JsonProperty("rootLifeAccountId")]
        public string RootLifeAccountId { get; set; }

        [JsonProperty("accountName")]
        public string AccountName { get; set; }

        [JsonProperty("isClueVersion")]
        public bool IsClueVersion { get; set; }

        [JsonProperty("awemeUsers")]
        public List<LifeAwemeUserInfoDto> AwemeUsers { get; set; } = new List<LifeAwemeUserInfoDto>();

        [JsonProperty("otherUid")]
        public List<string> OtherUid { get; set; } = new List<string>();

        [JsonProperty("secUid")]
        public string SecUid { get; set; }

        [JsonProperty("awemeUserId")]
        public string AwemeUserId { get; set; }
    }

    // =====================================================================
    // 采集上下文DTO（只读，从新版 SecUid.json 中直接定位一个抖音号的所有采集入参）
    // =====================================================================

    /// <summary>
    /// 采集上下文：根据 secUid 从 SecUid.json 定位到 awemeUsers[] 记录后
    /// 拼装出的完整上下信息，包含采集所需的全部静态入参。
    /// </summary>
    public class LifeAwemeUserContextDto
    {
        /// <summary>主播 SecUid（当前记录）</summary>
        public string SecUid { get; set; }

        /// <summary>拖音UID（= anchorID = aweme_user_id）</summary>
        public string AwemeUserId { get; set; }

        /// <summary>公司 groupId（= account_id = key_account_id）</summary>
        public string GroupId { get; set; }

        /// <summary>来客账户ID（= root_life_account_id）</summary>
        public string LifeAccountId { get; set; }

        /// <summary>公司名（仅日志展示）</summary>
        public string AccountName { get; set; }

        /// <summary>是否为线索版（决定采集走线索版接口还是普通版 EOS 接口）</summary>
        public bool IsClueVersion { get; set; }

        /// <summary>Cookie 文件名（life-{md5}）</summary>
        public string CookiePath { get; set; }

        /// <summary>当前记录状态（active/inactive）</summary>
        public string Status { get; set; }

        /// <summary>授权批次ID（兄弟 cookie 关联）</summary>
        public string AuthBatchId { get; set; }
    }
}
