

using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.ShortVideo.DouYin;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.hotSearch;
using ReviewAnalysis.vo.system;
using static DouYinSession;

/// <summary>
/// 网络爬虫类，用于处理账户登录和会话管理
/// 主要功能包括账户登录、回调处理和SessionId获取
/// </summary>
public class BlessBagSessionId : DouYinDeveloperOpen
{


    /// <summary>
    /// 本地文件保存的位置
    /// </summary>
    public static string sessionPath = Path.GetFullPath("dataCollect\\session\\blessBagConfig.json");
    /// <summary>
    /// SessionId过期时间（天数）
    /// </summary>
    private const int SESSION_EXPIRE_DAYS = 15;

    /// <summary>
    /// 获取本地的sessionId
    /// </summary>
    /// <returns></returns>
    private SessionData getLoadAccount()
    {
        try
        {
            if (!File.Exists(sessionPath))
            {
                return null;
            }

            string jsonContent = File.ReadAllText(sessionPath);
            if (string.IsNullOrEmpty(jsonContent))
            {
                return null;
            }

            var sessionData = JsonConvert.DeserializeObject<SessionData>(jsonContent);
            return sessionData;
        }
        catch (Exception ex)
        {
            FileUtils.LogError($"读取SessionId文件异常: {ex.Message}\n异常详情: {ex.ToString()}", "getLoadAccount");
            return null;
        }
    }

    /// <summary>
    /// 检查SessionId是否有效且未过期
    /// </summary>
    /// <param name="sessionData"></param>
    /// <returns></returns>
    private string checkLoadAccount(SessionData sessionData)
    {
        int value = KvHelper.GetIntKvByKey("blessBag_sessionId_expire_days", 15);
        // 检查SessionId是否有效且未过期
        if (sessionData != null && !string.IsNullOrEmpty(sessionData.SessionId) && DateTime.Now < sessionData.ExpireTime 
            && (DateTime.Now - sessionData.CreateTime).TotalDays <= value)
        {
            //FileUtils.LogAnalysis($"使用缓存的SessionId: {sessionData.SessionId}", "GetValidSessionId");
            return sessionData.SessionId;
        }

        return null;
    }

    /// <summary>
    /// 获取账号密码
    /// </summary>
    /// <returns></returns>
    private async Task<(string accountId, string account, string pass)> getServerEmail()
    {
        string accountId = null, account = null, pass = null;
        //取账号密码
        HotSearchEmailAccountVo emailAccount = HotSearchApi.GetHotSearchEmailAccount(1);

        if (emailAccount == null || string.IsNullOrEmpty(emailAccount.email) || string.IsNullOrEmpty(emailAccount.emailPassword))
        {
            var param = new ReportFailureVo()
            {
                accountId = Convert.ToInt64(emailAccount.accountId),
                failureReason = "取邮箱账号失败！",
                email = emailAccount.email,
                errorMessage = "取邮箱账号失败！",
                failureType = 5 //其他
            };
            await HotSearchApi.HotSearchReportFailure(param);
            FileUtils.LogError($"{emailAccount.email},取邮箱密码解密失败！", "福袋");
            return (accountId, account, pass);
        }

        accountId = emailAccount.accountId;
        account = emailAccount.email;

        // 解密（使用相同email）
        string decryptedPassword = AesCbcUtils.DecryptEmailPassword(emailAccount.emailPassword, emailAccount.email);
        if (string.IsNullOrWhiteSpace(decryptedPassword))
        {
            var param = new ReportFailureVo()
            {
                accountId = Convert.ToInt64(emailAccount.accountId),
                failureReason = "邮箱密码解密错误",
                email = emailAccount.email,
                errorMessage = "邮箱账号登录错误",
                failureType = 1 //密码错误
            };
            await HotSearchApi.HotSearchReportFailure(param);
            FileUtils.LogError($"{emailAccount.email},取邮箱密码解密失败！", "福袋");
            return (accountId, account, pass);
        }

        pass = decryptedPassword;

        return (accountId, account, pass);
    }

    /// <summary>
    /// 获取sessionId
    /// </summary>
    /// <param name="accountId"></param>
    /// <param name="account"></param>
    /// <param name="pass"></param>
    /// <returns></returns>
    private async Task<string> accountLogin(string accountId, string account, string pass)
    {
        if (string.IsNullOrEmpty(accountId) || string.IsNullOrEmpty(account) || string.IsNullOrEmpty(pass))
        {
            FileUtils.LogError($"string.IsNullOrEmpty(accountId) || string.IsNullOrEmpty(account) || string.IsNullOrEmpty(pass)", "福袋");
            return null;
        }
        //取sessionid，若异常回调异常
        var (sessionid, reason, message, type) = await AccountLoginByEmail(account, pass);
        if (string.IsNullOrEmpty(sessionid))
        {
            var param = new ReportFailureVo()
            {
                accountId = Convert.ToInt64(accountId),
                failureReason = string.IsNullOrEmpty(reason) ? "邮箱登录错误" : reason,
                email = account,
                errorMessage = string.IsNullOrEmpty(message) ? "邮箱账号登录错误" : message,
                failureType = type
            };
            if (!(param.errorMessage.Contains("发送请求时出错") || param.errorMessage.Contains("已取消一个任务")))
                await HotSearchApi.HotSearchReportFailure(param);
            FileUtils.LogError($"{account}邮箱登录错误!" + reason + message, "福袋");
            return null;
        }
        FileUtils.log($"{account}邮箱登录成功!" + reason + message, "福袋");
        return sessionid;
    }

    /// <summary>
    /// 保存SessionId数据到文件
    /// </summary>
    /// <param name="sessionId">要保存的SessionId</param>
    private void SaveSessionToFile(string sessionId)
    {
        try
        {
            // 确保目录存在
            string directory = Path.GetDirectoryName(sessionPath);
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }

            int value = KvHelper.GetIntKvByKey("blessBag_sessionId_expire_days", 15);

            // 创建SessionId数据
            var sessionData = new SessionData
            {
                SessionId = sessionId,
                CreateTime = DateTime.Now,
                ExpireTime = DateTime.Now.AddDays(value)
            };

            // 序列化并保存到文件
            string jsonContent = JsonConvert.SerializeObject(sessionData, Formatting.Indented);
            File.WriteAllText(sessionPath, jsonContent);

            FileUtils.log($"SessionId已保存到文件: {sessionPath}", "SaveSessionToFile-福袋");
        }
        catch (Exception ex)
        {
            FileUtils.LogError($"保存SessionId文件异常: {ex.Message}\n异常详情: {ex.ToString()}", "SaveSessionToFile-福袋");
        }
    }

    /// <summary>
    /// 获取福袋的sessionId
    /// </summary>
    /// <returns></returns>
    public async Task<string> getBlessBagSessionId()
    {
        // 查询本地sessionId
        string sessionId = checkLoadAccount(getLoadAccount());
        if (!string.IsNullOrEmpty(sessionId))
        {
            return sessionId;
        }

        // 重新获取-服务器的账号密码
        var (accountId, account, pass) = await getServerEmail();

        // 调用接口
        sessionId = await accountLogin(accountId, account, pass);

        // 保存到本地
        SaveSessionToFile(sessionId);

        return sessionId;
    }
}
