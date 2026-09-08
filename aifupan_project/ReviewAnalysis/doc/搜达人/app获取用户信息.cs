

using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace UserProfileFetcher
{
    class Program
    {
        // 唯一的入口点
        static async Task Main(string[] args)
        {
            string secUserId = "MS4wLjABAAAAm_ltVqZsWCgxkJ5OIvv6E4qaSgqBOE9TLUnlNPbzvzYlNzayX7VmJOI3GOUiKaPl";
            await UserProfile(secUserId);
        }

        // 随机MAC地址生成
        static string GenerateMac()
        {
            Random random = new Random();
            byte[] macBytes = new byte[6];
            random.NextBytes(macBytes);
            return string.Join(":", macBytes.Select(b => b.ToString("X2")));
        }

        // 生成CDID (UUID)
        static string GenerateCdid()
        {
            return Guid.NewGuid().ToString();
        }

        // Luhn算法计算校验位
        static int LuhnCheckDigit(string number)
        {
            if (string.IsNullOrEmpty(number))
                throw new ArgumentException("输入的数字不能为空", nameof(number));
                
            List<int> digits = number.Select(c => int.Parse(c.ToString())).ToList();
            List<int> oddDigits = new List<int>();
            List<int> evenDigits = new List<int>();

            // 分离奇位和偶位数字
            for (int i = digits.Count - 1; i >= 0; i--)
            {
                if ((digits.Count - i) % 2 == 1)
                    oddDigits.Add(digits[i]);
                else
                    evenDigits.Add(digits[i]);
            }

            int checksum = oddDigits.Sum();
            
            foreach (int d in evenDigits)
            {
                checksum += (d * 2).ToString().Select(c => int.Parse(c.ToString())).Sum();
            }

            return (10 - (checksum % 10)) % 10;
        }

        // 生成随机IMEI
        static string GenerateImei()
        {
            Random random = new Random();
            // 生成14位随机数字
            long first14Digits = (long)(random.NextDouble() * 9e13 + 1e13);
            string first14Str = first14Digits.ToString("D14");
            // 计算校验位
            int checkDigit = LuhnCheckDigit(first14Str);
            return first14Str + checkDigit;
        }

        // 生成OpenUDID
        static string GenerateOpenUdid()
        {
            Random random = new Random();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++)
            {
                sb.Append(random.Next(1, 256).ToString("x2"));
            }
            return sb.ToString();
        }

        // 解析响应JSON
        static void ParseResponseJson(string? json)
        {
            if (string.IsNullOrEmpty(json))
            {
                Console.WriteLine("响应JSON为空");
                return;
            }

            try
            {
                using (JsonDocument doc = JsonDocument.Parse(json))
                {
                    JsonElement root = doc.RootElement;
                    
                    if (root.TryGetProperty("status_code", out JsonElement statusCode) && statusCode.GetInt32() == 0)
                    {
                        if (root.TryGetProperty("user", out JsonElement user))
                        {
                            Dictionary<string, object?> item = new Dictionary<string, object?>();
                            
                            // 基本用户信息
                            AddIfExists(item, user, "nickname", "nickname");
                            AddIfExists(item, user, "uid", "uid");
                            AddIfExists(item, user, "unique_id", "unique_id");
                            AddIfExists(item, user, "sec_uid", "sec_uid");
                            AddIfExists(item, user, "aweme_count", "aweme_count");
                            AddIfExists(item, user, "follower_count", "follower_count");
                            AddIfExists(item, user, "following_count", "following_count");
                            AddIfExists(item, user, "total_favorited", "total_favorited");
                            AddIfExists(item, user, "signature", "signature");
                            AddIfExists(item, user, "live_status", "live_status");
                            AddIfExists(item, user, "room_id_str", "room_id_str");

                            // 获取头像URL
                            if (user.TryGetProperty("avatar_larger", out JsonElement avatarLarger) &&
                                avatarLarger.TryGetProperty("url_list", out JsonElement urlList) &&
                                urlList.GetArrayLength() > 0)
                            {
                                item["avatar_larger"] = urlList[0].GetString();
                            }

                            // 处理房间数据
                            if (user.TryGetProperty("room_data", out JsonElement roomDataElem) && 
                                !roomDataElem.ValueKind.Equals(JsonValueKind.Null))
                            {
                                string? roomDataJson = roomDataElem.GetString();
                                if (!string.IsNullOrEmpty(roomDataJson))
                                {
                                    using (JsonDocument roomDoc = JsonDocument.Parse(roomDataJson))
                                    {
                                        JsonElement roomData = roomDoc.RootElement;
                                        AddIfExists(item, roomData, "user_count", "user_count");
                                        AddIfExists(item, roomData, "client_version", "client_version");
                                        AddIfExists(item, roomData, "stream_id_str", "stream_id_str");

                                        // 处理流URL
                                        if (roomData.TryGetProperty("stream_url", out JsonElement streamUrl))
                                        {
                                            Dictionary<string, object?> streamUrlDict = new Dictionary<string, object?>();
                                            AddIfExists(streamUrlDict, streamUrl, "resolution_name", "resolution_name");
                                            AddIfExists(streamUrlDict, streamUrl, "hls_pull_url", "hls_pull_url");
                                            
                                            if (streamUrl.TryGetProperty("hls_pull_url_map", out JsonElement hlsMap))
                                            {
                                                streamUrlDict["hls_pull_url_map"] = hlsMap.GetRawText();
                                            }
                                            
                                            item["stream_url"] = streamUrlDict;
                                        }
                                    }
                                }
                            }

                            // 打印结果
                            Console.WriteLine(JsonSerializer.Serialize(item, new JsonSerializerOptions { WriteIndented = true }));
                        }
                    }
                    else
                    {
                        Console.WriteLine("获取数据失败");
                        Console.WriteLine(json);
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"解析JSON错误: {ex.Message}");
            }
        }

        // 辅助方法：如果属性存在则添加到字典
        static void AddIfExists(Dictionary<string, object?> dict, JsonElement element, string propertyName, string key)
        {
            if (element.TryGetProperty(propertyName, out JsonElement prop))
            {
                dict[key] = GetJsonValue(prop);
            }
        }

        // 辅助方法：获取JSON元素的值
        static object? GetJsonValue(JsonElement element)
        {
            switch (element.ValueKind)
            {
                case JsonValueKind.String:
                    return element.GetString();
                case JsonValueKind.Number:
                    if (element.TryGetInt64(out long l)) return l;
                    if (element.TryGetDouble(out double d)) return d;
                    return element.GetRawText();
                case JsonValueKind.True:
                    return true;
                case JsonValueKind.False:
                    return false;
                case JsonValueKind.Object:
                case JsonValueKind.Array:
                    return element.GetRawText();
                case JsonValueKind.Null:
                    return null;
                default:
                    return null;
            }
        }

        // 用户信息接口调用
        static async Task UserProfile(string secUserId)
        {
            if (string.IsNullOrEmpty(secUserId))
            {
                Console.WriteLine("sec_user_id不能为空");
                return;
            }

            string macAddress = GenerateMac();
            string cdid = GenerateCdid();
            string uuid = GenerateImei();
            string openudid = GenerateOpenUdid();

            // 毫秒级时间戳（rticket）
            long rticket = (long)DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

            // 秒级时间戳（ts）
            long ts = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            string deviceId = "4484249592557674";
            string iid = "810786544184746";

            using (HttpClient client = new HttpClient())
            {
                // 设置请求头
                client.DefaultRequestHeaders.UserAgent.ParseAdd("okhttp/3.10.0.1");
                // client.DefaultRequestHeaders.AcceptEncoding.Add(new StringWithQualityHeaderValue("gzip"));
                client.DefaultRequestHeaders.Add("x-ss-req-ticket", rticket.ToString());
                client.DefaultRequestHeaders.Add("sdk-version", "1");
                client.DefaultRequestHeaders.Add("x-khronos", ts.ToString());

                // 构建查询参数
                Dictionary<string, string> parameters = new Dictionary<string, string>
                {
                    { "sec_user_id", secUserId },
                    { "address_book_access", "2" },
                    { "from", "0" },
                    { "publish_video_strategy_type", "2" },
                    { "manifest_version_code", "110501" },
                    { "_rticket", rticket.ToString() },
                    { "app_type", "normal" },
                    { "iid", iid },
                    { "channel", "gdt_growth14_big_yybwz" },
                    { "device_type", "V2307A" },
                    { "language", "zh" },
                    { "cpu_support64", "true" },
                    { "host_abi", "armeabi-v7a" },
                    { "uuid", uuid },
                    { "resolution", "900*1600" },
                    { "openudid", openudid },
                    { "update_version_code", "11509900" },
                    { "cdid", cdid },
                    { "os_api", "28" },
                    { "mac_address", macAddress },
                    { "dpi", "240" },
                    { "ac", "wifi" },
                    { "device_id", deviceId },
                    { "mcc_mnc", "46000" },
                    { "os_version", "9" },
                    { "version_code", "110500" },
                    { "app_name", "aweme" },
                    { "version_name", "11.5.0" },
                    { "device_brand", "vivo" },
                    { "ssmix", "a" },
                    { "device_platform", "android" },
                    { "aid", "1128" },
                    { "ts", ts.ToString() }
                };

                // 构建请求URL
                string queryString = string.Join("&", parameters.Select(p => $"{Uri.EscapeDataString(p.Key)}={Uri.EscapeDataString(p.Value)}"));
                string url = $"https://aweme.snssdk.com/aweme/v1/user/profile/other/?{queryString}";

                try
                {
                    HttpResponseMessage response = await client.GetAsync(url);
                    response.EnsureSuccessStatusCode();
                    
                    string responseBody = await response.Content.ReadAsStringAsync();
                    Console.WriteLine($"响应状态码: {response.StatusCode}");
                    
                    ParseResponseJson(responseBody);
                }
                catch (HttpRequestException ex)
                {
                    Console.WriteLine($"请求错误: {ex.Message}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"发生错误: {ex.Message}");
                }
            }
        }
    }
}






