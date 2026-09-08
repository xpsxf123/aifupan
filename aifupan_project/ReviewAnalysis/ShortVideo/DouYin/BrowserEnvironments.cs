using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    public class BrowserEnvironments
    {

        public static Dictionary<string, BrowserEnvironment> getBrowserEnvironments()
        {
            return new Dictionary<string, BrowserEnvironment>
            {
                {
                    "sougou", new BrowserEnvironment
                    {
                        Headers = new Dictionary<string, string>
                        {
                            ["authority"] = "www.douyin.com",
                            ["accept"] = "application/json, text/plain, */*",
                            ["accept-language"] = "zh-CN,zh;q=0.9",
                            ["cache-control"] = "no-cache",
                            ["pragma"] = "no-cache",
                            ["referer"] = "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user",
                            ["sec-ch-ua"] = "\"Not)A;Brand\";v=\"24\", \"Chromium\";v=\"116\"",
                            ["sec-ch-ua-mobile"] = "?0",
                            ["sec-ch-ua-platform"] = "\"Windows\"",
                            ["sec-fetch-dest"] = "empty",
                            ["sec-fetch-mode"] = "cors",
                            ["sec-fetch-site"] = "same-origin",
                            ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.97 Safari/537.36 SE 2.X MetaSr 1.0"
                        },
                        Params = new Dictionary<string, string>
                        {
                            ["update_version_code"] = "170400",
                            ["pc_client_type"] = "1",
                            ["pc_libra_divert"] = "Windows",
                            ["support_h265"] = "1",
                            ["support_dash"] = "1",
                            ["cpu_core_num"] = "12",
                            ["cookie_enabled"] = "true",
                            ["screen_width"] = "1920",
                            ["screen_height"] = "1080",
                            ["browser_language"] = "zh-CN",
                            ["browser_platform"] = "Win32",
                            ["browser_name"] = "Sogou Explorer",
                            ["browser_version"] = "1.0",
                            ["browser_online"] = "true",
                            ["engine_name"] = "Blink",
                            ["engine_version"] = "116.0.5845.97",
                            ["os_name"] = "Windows",
                            ["os_version"] = "10",
                            ["device_memory"] = "8",
                            ["platform"] = "PC",
                            ["downlink"] = "10",
                            ["effective_type"] = "4g",
                            ["round_trip_time"] = "0"
                        }
                    }
                },
                {
                    "firefox", new BrowserEnvironment
                    {
                        Headers = new Dictionary<string, string>
                        {
                            { "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/116.0" },
                            { "Accept", "application/json, text/plain, */*" },
                            { "Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2" },
                            { "uifid", "undefined" },
                            { "Connection", "keep-alive" },
                            { "Referer", "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user" },
                            { "Sec-Fetch-Dest", "empty" },
                            { "Sec-Fetch-Mode", "cors" },
                            { "Sec-Fetch-Site", "same-origin" },
                            { "Pragma", "no-cache" },
                            { "Cache-Control", "no-cache" }
                        },
                        Params = new Dictionary<string, string>
                        {
                            { "update_version_code", "170400" },
                            { "pc_client_type", "1" },
                            { "pc_libra_divert", "Windows" },
                            { "support_h265", "0" },
                            { "support_dash", "0" },
                            { "cpu_core_num", "8" },
                            { "cookie_enabled", "true" },
                            { "screen_width", "1536" },
                            { "screen_height", "864" },
                            { "browser_language", "zh-CN" },
                            { "browser_platform", "Win32" },
                            { "browser_name", "Firefox" },
                            { "browser_version", "116.0" },
                            { "browser_online", "true" },
                            { "engine_name", "Gecko" },
                            { "engine_version", "109.0" },
                            { "os_name", "Windows" },
                            { "os_version", "10" },
                            { "device_memory", "" },
                            { "platform", "PC" }
                        }
                    }
                },
                {
                    "chrome", new BrowserEnvironment
                    {
                        Headers = new Dictionary<string, string>
                        {
                            { "accept", "application/json, text/plain, */*" },
                            { "accept-language", "zh-CN,zh;q=0.9" },
                            { "cache-control", "no-cache" },
                            { "pragma", "no-cache" },
                            { "priority", "u=1, i" },
                            { "referer", "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user" },
                            { "sec-ch-ua", "\"Google Chrome\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"" },
                            { "sec-ch-ua-mobile", "?0" },
                            { "sec-ch-ua-platform", "\"Windows\"" },
                            { "sec-fetch-dest", "empty" },
                            { "sec-fetch-mode", "cors" },
                            { "sec-fetch-site", "same-origin" },
                            { "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36" }
                        },
                        Params = new Dictionary<string, string>
                        {
                            { "update_version_code", "170400" },
                            { "pc_client_type", "1" },
                            { "pc_libra_divert", "Windows" },
                            { "support_h265", "1" },
                            { "support_dash", "1" },
                            { "cpu_core_num", "8" },
                            { "cookie_enabled", "true" },
                            { "screen_width", "1536" },
                            { "screen_height", "864" },
                            { "browser_language", "zh-CN" },
                            { "browser_platform", "Win32" },
                            { "browser_name", "Chrome" },
                            { "browser_version", "137.0.0.0" },
                            { "browser_online", "true" },
                            { "engine_name", "Blink" },
                            { "engine_version", "137.0.0.0" },
                            { "os_name", "Windows" },
                            { "os_version", "10" },
                            { "device_memory", "8" },
                            { "platform", "PC" }
                        }
                    }
                },
                {
                    "edge", new BrowserEnvironment
                    {
                        Headers = new Dictionary<string, string>
                        {
                            { "accept", "application/json, text/plain, */*" },
                            { "accept-language", "zh-CN,zh;q=0.9" },
                            { "cache-control", "no-cache" },
                            { "pragma", "no-cache" },
                            { "priority", "u=1, i" },
                            { "referer", "https://www.douyin.com/search/%E4%BA%A4%E4%B8%AA%E6%9C%8B%E5%8F%8B?type=user" },
                            { "sec-ch-ua", "\"Microsoft Edge\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"" },
                            { "sec-ch-ua-mobile", "?0" },
                            { "sec-ch-ua-platform", "\"Windows\"" },
                            { "sec-fetch-dest", "empty" },
                            { "sec-fetch-mode", "cors" },
                            { "sec-fetch-site", "same-origin" },
                            { "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0" }
                        },
                        Params = new Dictionary<string, string>
                        {
                            { "update_version_code", "170400" },
                            { "pc_client_type", "1" },
                            { "pc_libra_divert", "Windows" },
                            { "support_h265", "1" },
                            { "support_dash", "1" },
                            { "cpu_core_num", "12" },
                            { "cookie_enabled", "true" },
                            { "screen_width", "1920" },
                            { "screen_height", "1080" },
                            { "browser_language", "zh-CN" },
                            { "browser_platform", "Win32" },
                            { "browser_name", "Edge" },
                            { "browser_version", "137.0.0.0" },
                            { "browser_online", "true" },
                            { "engine_name", "Blink" },
                            { "engine_version", "137.0.0.0" },
                            { "os_name", "Windows" },
                            { "os_version", "10" },
                            { "device_memory", "8" },
                            { "platform", "PC" }
                        }
                    }
                }
            };
        } 


    }

    // 辅助类
    public class BrowserEnvironment
    {
        public Dictionary<string, string> Headers { get; set; }
        public Dictionary<string, string> Params { get; set; }
    }
}
