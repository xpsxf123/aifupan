using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Text;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.system;
using ReviewAnalysis.enums;
using ReviewAnalysis.Global;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.config;

namespace ReviewAnalysis.Bll
{
    public class ConfigBll
    {

        //public static ConcurrentDictionary<string, Config> configCahce = new ConcurrentDictionary<string, Config>();

        private static readonly Object lockObject = new Object();
        /// <summary>
        /// 系统配置文件路径
        /// </summary>
        private static string configPath = Path.GetFullPath(@"Config/systemConfig.txt");
        
        /// <summary>
        /// 获取用户配置的路径
        /// </summary>
        /// <exception cref="CustomException"></exception>
        private static string userConfigPath
        {
            get
            {
                if (string.IsNullOrEmpty(ReplayHttpUtils.UserId))
                {
                    throw new CustomException("请登录");
                }
                return Path.GetFullPath($"Config\\{ReplayHttpUtils.UserId}.config");
            }
        }

        /// <summary>
        /// 缓存中的系统配置对象
        /// </summary>
        private static Config configCahce = null;

        /// <summary>
        /// 获取配置
        /// </summary>
        /// <returns></returns>
        public Config GetModel() 
        {
            if (configCahce != null)
            {
                return configCahce;
            }

            lock (lockObject)
            {
                try
                {

                    if(File.Exists(configPath))
                    {
                        // 本地有配置文件，直接返回
                        string configStr = File.ReadAllText(configPath);
                        try
                        {
                            configCahce = JsonConvert.DeserializeObject<Config>(configStr);
                            configCahce.SerialNumber = Constant.VERSION;
                        }
                        catch (Exception ex)
                        {
                            // 读取配置文件序列化失败
                            FileUtils.LogError($"{ex}", $"读取配置文件序列化失败，尝试从备份文件读取");
                            try
                            {
                                configStr = File.ReadAllText(configPath + ".bak");
                                configCahce = JsonConvert.DeserializeObject<Config>(configStr);
                                configCahce.SerialNumber = Constant.VERSION;

                                SafeWriteConfigToFile(configCahce);
                            }
                            catch (Exception e)
                            {
                                FileUtils.LogError($"{ex}", $"备份配置文件不存在，或从备份文件读取失败，创建新的配置文件");
                                createConfig();
                                return configCahce;
                            }
                        }

                        // 补救bug
                        bool needUpdate = false;
                        if(configCahce.LimitType == 0 && configCahce.LimitValue > 0)
                        {
                            configCahce.LimitType = 2;
                            needUpdate = true;
                        }

                        // 限制时长改成时长分段
                        if(configCahce.LimitType == 1)
                        {
                            configCahce.LimitType = 2;
                            needUpdate = true;
                        }

                        if(configCahce.LimitValue < 30)
                        {
                            configCahce.LimitValue = 30;
                            needUpdate = true;
                        }

                        // 如果有更新，统一写入一次
                        if(needUpdate)
                        {
                            SafeWriteConfigToFile(configCahce);
                        }

                        return configCahce;
                    }

                    // 没有配置文件，创建
                    createConfig();

                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", $"获取系统配置文件发生异常");
                }
            }

            return configCahce;
        }

        /// <summary>
        /// 创建系统配置
        /// </summary>
        /// <returns></returns>
        private void createConfig()
        {
            try
            {
                Config config = new Config()
                {
                    Id = 1,
                    SerialNumber = Constant.VERSION,
                    DetectionFre = 2000,
                    SavePath = Path.GetFullPath(@"download"),
                    LiveSource = 0,
                    LimitType = 2,
                    LimitValue = 240,
                    IsRocord = 0,
                    IsSubection = 0,
                    HideDuration = 0,
                    HideSize = 0,
                    LiveNotice = 0,
                    OnlineNumber = 1,
                };

                SafeWriteConfigToFile(config);

                configCahce = config;

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"创建系统配置文件发生异常");
            }

        }

        /// <summary>
        /// 启动客户端时初始化配置信息
        /// </summary>
        public void startClientInitConfig()
        {
            // 获取配置信息
            Config config = GetModel();

            string root = Path.GetPathRoot(config.SavePath);
            if (string.Equals(config.SavePath, root, StringComparison.OrdinalIgnoreCase))
            {
                // 根目录，加一层目录
                config.SavePath = config.SavePath + "aifupanVideo";
                updateModel(config);
            }

            if (!Directory.Exists(config.SavePath))
            {
                // 视频文件夹不存在，创建
                try
                {
                    Directory.CreateDirectory(config.SavePath);
                }
                catch (Exception ex)
                {
                    FileUtils.log($"{config.SavePath}", $"启动客户端时初始化配置信息-创建视频存储文件夹失败");
                }
            }

            if(config.IsRocord != 0)
            {
                config.IsRocord = 0;
                // 更新配置信息
                updateModel(config);
            }

        }

        /// <summary>
        /// 更新配置
        /// </summary>
        /// <param name="config"></param>
        public void updateModel(Config newConfig)
        {

            lock (lockObject)
            {
                Config config = GetModel();
                config.SerialNumber = newConfig.SerialNumber;
                config.DetectionFre = newConfig.DetectionFre;
                if (!string.IsNullOrEmpty(newConfig.SavePath))
                {
                    string decode = WebUtility.UrlDecode(newConfig.SavePath);
                    config.SavePath = decode.Replace("\\\\", "\\");

                    string root = Path.GetPathRoot(config.SavePath);
                    if (string.Equals(config.SavePath, root, StringComparison.OrdinalIgnoreCase))
                    {
                        // 根目录，加一层目录
                        config.SavePath = config.SavePath + "aifupanVideo";
                    }
                }
                config.LiveSource = newConfig.LiveSource;
                config.LimitType = newConfig.LimitType;
                config.LimitValue = newConfig.LimitValue;
                config.IsRocord = newConfig.IsRocord;
                config.IsSubection = newConfig.IsSubection;
                config.HideDuration = newConfig.HideDuration;
                config.HideSize = newConfig.HideSize;
                config.LiveNotice = newConfig.LiveNotice;
                config.OnlineNumber = newConfig.OnlineNumber;
                config.autoDeleteTime = newConfig.autoDeleteTime;
                config.deleteContent = newConfig.deleteContent;

                // 安全写入到本地文件
                SafeWriteConfigToFile(config);

                // 存到缓存
                configCahce = config;

            }
        }

        /// <summary>
        /// 修改配置信息的录制状态
        /// </summary>
        /// <param name="recordStatus">录制状态 0否 1是</param>
        public void updateConfigRecordStatus(int recordStatus)
        {
            lock (lockObject)
            {
                Config config = GetModel();
                config.IsRocord = recordStatus;

                // 安全写入到本地文件
                SafeWriteConfigToFile(config);

                // 存到缓存
                configCahce = config;
            }
        }

        public void setClientVersion()
        {
            try
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", ReplayHttpUtils.Token);
                request.RequestUri = new Uri(ReplayHttpUtils.BaseUrl + "/openapi/clientupdate/setClientVersion");
                var client = new Dictionary<string, string>
                {
                    { "clientCpuid", ReplayHttpUtils.cpuid},
                    { "clientUuid", ReplayHttpUtils.uuid },
                    { "clientVersion", Constant.VERSION }
                };
                request.Content = new StringContent(JsonConvert.SerializeObject(client), Encoding.UTF8, "application/json");
                using(HttpClient Client = new HttpClient())
                {
                    HttpResponseMessage result = Client.SendAsync(request).Result;
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"设置客户端版本报错={ex.Message}");
            }
        }

        public AccountPasswordVo getUserObject()
        {
            try
            {
                string FilePath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Aifupan", "accounts");
                if (File.Exists(FilePath))
                {
                    string str = File.ReadAllText(FilePath);
                    if (!string.IsNullOrEmpty(str))
                    {
                        AccountPasswordVo vo = JsonConvert.DeserializeObject<AccountPasswordVo>(str);

                        if (!string.IsNullOrEmpty(vo.password))
                        {
                            byte[] bytes = Convert.FromBase64String(vo.password);
                            vo.password = Encoding.UTF8.GetString(bytes);
                        }

                        return vo;
                    }
                }
                else
                {
                    return null;
                }
            }
            catch (Exception ex)
            {
                FileUtils.log(ex.Message, "保存账号密码报错");
            }
            return null;
        }

        public void putUserObject(AccountPasswordVo vo)
        {
            try
            {
                if (vo != null)
                {
                    if (!string.IsNullOrEmpty(vo.password))
                    {
                        byte[] bytes = Encoding.UTF8.GetBytes(vo.password); // 转为字节数组 
                        string base64String = Convert.ToBase64String(bytes);   // 编码为Base64 
                        vo.password = base64String;
                    }
                    string FilePath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),"Aifupan","accounts");

                    Directory.CreateDirectory(Path.GetDirectoryName(FilePath));

                    File.WriteAllText(FilePath, JsonConvert.SerializeObject(vo));
                }
            }
            catch (Exception ex)
            {
                FileUtils.log(ex.Message, "保存账号密码报错");
            }

        }

        /// <summary>
        /// 安全写入配置文件，防止文件损坏
        /// </summary>
        /// <param name="config">配置对象</param>
        private void SafeWriteConfigToFile(Config config)
        {
            try
            {
                if (config == null)
                {
                    FileUtils.LogError("配置对象为空", "SafeWriteConfigToFile");
                    return;
                }

                // 序列化配置对象
                string jsonContent = JsonConvert.SerializeObject(config, Formatting.Indented);

                // 验证序列化结果
                if (string.IsNullOrEmpty(jsonContent) || jsonContent.Trim() == "{}")
                {
                    FileUtils.LogError($"配置序列化结果异常: {jsonContent}", "SafeWriteConfigToFile");
                    return;
                }

                // 验证反序列化是否正常
                try
                {
                    var testConfig = JsonConvert.DeserializeObject<Config>(jsonContent);
                    if (testConfig == null || testConfig.Id != config.Id)
                    {
                        FileUtils.LogError("配置序列化验证失败", "SafeWriteConfigToFile");
                        return;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"配置序列化验证异常: {ex.Message}", "SafeWriteConfigToFile");
                    return;
                }

                // 确保目录存在
                string directory = Path.GetDirectoryName(configPath);
                if (!Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                // 使用临时文件写入，然后原子性替换
                string tempPath = configPath + ".tmp";

                // 写入临时文件
                File.WriteAllText(tempPath, jsonContent, Encoding.UTF8);

                // 验证临时文件写入是否成功
                if (File.Exists(tempPath))
                {
                    string writtenContent = File.ReadAllText(tempPath, Encoding.UTF8);
                    if (writtenContent == jsonContent)
                    {
                        // 使用原子性替换
                        if (File.Exists(configPath))
                        {
                            // 使用 File.Replace 进行原子性替换，自动处理备份
                            string backupPath = configPath + ".bak";
                            File.Replace(tempPath, configPath, backupPath);
                        }
                        else
                        {
                            // 目标文件不存在，直接移动
                            File.Move(tempPath, configPath);
                        }

                        FileUtils.log($"配置文件写入成功: {configPath}", "SafeWriteConfigToFile");
                    }
                    else
                    {
                        FileUtils.LogError("临时文件内容验证失败", "SafeWriteConfigToFile");
                        if (File.Exists(tempPath))
                        {
                            File.Delete(tempPath);
                        }
                    }
                }
                else
                {
                    FileUtils.LogError("临时文件创建失败", "SafeWriteConfigToFile");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"安全写入配置文件异常: {ex.Message}\n{ex.StackTrace}", "SafeWriteConfigToFile");

                // 清理临时文件
                string tempPath = configPath + ".tmp";
                if (File.Exists(tempPath))
                {
                    try
                    {
                        File.Delete(tempPath);
                    }
                    catch { }
                }
            }
        }

        /// <summary>
        /// 将SQLite的配置信息写到配置文件
        /// </summary>
        public void readSQLiteConfigWriteFile()
        {
            if (File.Exists(configPath))
            {
                return;
            }

            // 本地没有配置信息，从SQLite读取写入
            string dbPath = Path.GetFullPath(@"DbFile/review_analysis.db");
            if (!File.Exists(dbPath))
            {
                return;
            }

            // SQLite读取配置信息
            Config config = new Config();
            config.GetModel();
            if (config != null && config.Id == 1)
            {
                lock (lockObject)
                {
                    int limitValue = config.LimitValue;
                    int LimitType = config.LimitType;
                    if (limitValue == 0)
                    {
                        // 无限制改成600分钟
                        limitValue = 600;
                        if (LimitType == 0)
                        {
                            LimitType = 2;
                        }
                    }
                    else if (limitValue < 30)
                    {
                        limitValue = 30;
                    }

                    // 限制时长改成时长分段
                    if(LimitType == 1)
                    {
                        LimitType = 2;
                    }

                    string savePath = string.IsNullOrEmpty(config.SavePath) ? Path.GetFullPath(@"download") : config.SavePath;

                    Config newConfig = new Config()
                    {
                        Id = 1,
                        SerialNumber = Constant.VERSION,
                        DetectionFre = 2000,
                        SavePath = savePath,
                        LiveSource = config.LiveSource,
                        LimitType = LimitType,
                        LimitValue = limitValue,
                        IsRocord = 0,
                        IsSubection = 0,
                        HideDuration = 0,
                        HideSize = 0,
                        LiveNotice = 0,
                        OnlineNumber = 1,
                    };

                    // 安全写入到本地文件
                    SafeWriteConfigToFile(newConfig);

                    // 存到缓存
                    configCahce = newConfig;
                }

            }

        }

        /// <summary>
        /// 纯录制版/复盘按切换
        /// </summary>
        /// <param name="vo"></param>
        public void setClientVersionConfig(ClientVersionConfigVo vo)
        {
            if (vo == null || string.IsNullOrEmpty(vo.clientVersion))
            {
                throw new CustomException("参数丢失");
            }

            if (!ClientVersion.Replay.Equals(vo.clientVersion) && !ClientVersion.Record.Equals(vo.clientVersion))
            {
                throw new CustomException("版本未知");
            }
            
            // 调用接口，切换版本
            OrderApi.setClientVersionConfig(vo.clientVersion);
        }
        
        /// <summary>
        /// 用户用户配置
        /// </summary>
        /// <returns></returns>
        public UserConfig getUserConfig()
        {
            var userConfig = new UserConfig();
            if (!File.Exists(userConfigPath))
            {
                return userConfig;
            }
            String str = File.ReadAllText(userConfigPath, Encoding.UTF8);
            if (string.IsNullOrEmpty(str.Trim()))
            {
                return userConfig;
            }

            return JsonConvert.DeserializeObject<UserConfig>(str.Trim());
        }

        /// <summary>
        /// 设置用户配置
        /// </summary>
        /// <param name="userConfig"></param>
        public void setUserConfig(UserConfig userConfig)
        {
            if (userConfig == null)
            {
                userConfig = new UserConfig();
            }
            File.WriteAllText(userConfigPath, JsonConvert.SerializeObject(userConfig));
        }
    }
}
