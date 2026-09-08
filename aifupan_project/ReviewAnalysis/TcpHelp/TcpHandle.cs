using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Sockets;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Model;
using System.Collections.Concurrent;
using ReviewAnalysis.DataCache;
using douyin.Utils;
using System.Data.Entity.Core.Common.CommandTrees;

namespace ReviewAnalysis.TcpHelp
{
    public class TcpHandle
    {
        private TcpClient client;
        private NetworkStream stream;
        private TcpListener listener;

        private Process bSoftwareProcess;

        private bool SendMessageIng = false;

        private static ConcurrentQueue<string> queue = new ConcurrentQueue<string>();

        public async Task InitializeServer()
        {
            // 创建一个 TCP 监听器
            listener = new TcpListener(IPAddress.Any, 6001);
            listener.Start();
            FileUtils.log("监听端口:6001...", "TcpHandle");
            StartClientApplication();
            while (true)
            {
                // 接受一个客户端连接
                TcpClient client = await listener.AcceptTcpClientAsync();
                stream = client.GetStream();
                // 接收数据
                byte[] buffer = new byte[1024];
                int bytesRead;
                bool isConnected = true;
                while (isConnected)
                {
                    try
                    {
                        bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length);
                        if (bytesRead > 0)
                        {
                            string receivedData = Encoding.UTF8.GetString(buffer, 0, bytesRead);
                            // 处理数据
                            HandleData(receivedData, stream);
                        }
                        else
                        {
                            // 如果 bytesRead 为 0，则表示客户端已经关闭连接 
                            isConnected = false;
                        }
                    }
                    catch (IOException ex)
                    {
                        // 如果出现 IOException，则表示客户端已经关闭连接
                        FileUtils.LogError($"Client disconnected: {ex.Message}", "TcpHandle");
                        isConnected = false;
                    }
                }
                // 关闭当前连接
                client.Close();
            }
        }


        private void HandleData(string receivedData, NetworkStream stream)
        {
            FileUtils.log("收到客户端: " + receivedData, "TcpHandle");
            TcpDto dto= JsonConvert.DeserializeObject<TcpDto>(receivedData);
            if (dto.ActionType == 0)
            {
                FileUtils.log("上线通知", "TcpHandle");
            }
            else if (dto.ActionType == 2) 
            {
                //业务返回
                if (dto.ActionStatus == 1)
                {
                    SetAnchorToCache(dto.ActionResult);
                    if (queue.TryDequeue(out string nextSecUid))
                    {
                        FileUtils.log($"客户端队列长度:{queue.Count}", "TcpHandle");
                        if (queue.Count > 0)
                        {
                            SendMessageIng = true;
                        }
                        else
                        {
                            SendMessageIng = false;
                        }
                        Thread.Sleep(2000);
                        SendMessage(nextSecUid);
                    }
                }
                else
                {
                    //采集失败
                    Dictionary<string, object> result = dto.ActionResult;
                    result.TryGetValue("SecUid", out object secuid);
                    string SecUid = Convert.ToString(secuid);
                    queue.Enqueue(SecUid);
                    //重新采集
                    if (queue.TryDequeue(out string nextSecUid2))
                    {
                        FileUtils.log($"客户端队列长度:{queue.Count}", "TcpHandle");
                        if (queue.Count > 0)
                        {
                            SendMessageIng = true;
                        }
                        else
                        {
                            SendMessageIng = false;
                        }
                        if (dto.ActionStatus == 2)
                        {
                            //等待验证 20秒
                            Thread.Sleep(20000);
                        }
                        else 
                        {
                            Thread.Sleep(2000);
                        }
                           
                        SendMessage(nextSecUid);
                    }
                }
            }
        }

        public bool GetSendMessageIng() 
        {
            return SendMessageIng;
        }

        public void StopSend() 
        {
            this.SendMessageIng = false;
        }


        // 创建一个锁对象
        object lockObject = new object();
        public void SendMessage(string secuid)
        {
            try
            {
                if (stream != null && stream.CanWrite)
                {
                    byte[] messageBuffer = Encoding.UTF8.GetBytes(secuid);
                    lock (lockObject)
                    {
                        stream.Write(messageBuffer, 0, messageBuffer.Length);
                    }
                    FileUtils.log("给客户端发送数据", "TcpHandle");
                }
                else
                {
                    CheckGather();
                    FileUtils.log("客户端链接不健康", "TcpHandle");
                }
            }
            catch (Exception ex)
            {
                CheckGather();
                FileUtils.LogError($"信息发送发生错误: {ex.Message}", "TcpHandle");
            }
        }

        public void SendMessage() 
        {
            if (queue.TryDequeue(out string firstSecUid)) 
            {
                SendMessage(firstSecUid);
            }
        }

        private void StartClientApplication()
        {
            string pathToBSoftware = @"Gather\Gather.exe";
            if (File.Exists(pathToBSoftware))
            {
                bSoftwareProcess = new Process();
                bSoftwareProcess.StartInfo.FileName = pathToBSoftware;
                bSoftwareProcess.Start();
            }
        }

        public void killGather() 
        {
            // 关闭B软件
            if (bSoftwareProcess != null && !bSoftwareProcess.HasExited)
            {
                bSoftwareProcess.Kill();
            }
        }

        public void SetQueue(string secuid) 
        {
            queue.Enqueue(secuid);
        }

        public int GetQueueCount() 
        {
            return queue.Count;
        }

        public void ClearQueue() 
        {
            while (queue.TryDequeue(out string secUid))
            {
                FileUtils.log("移除tcp发送队列", "TcpHandle");
            }
        }

        private void CheckGather()
        {
            if (bSoftwareProcess != null)
            {
                if (!bSoftwareProcess.HasExited)
                {
                    FileUtils.log("进程仍在运行", "TcpHandle");
                    //进程存在，但是发送TCP不成功，杀死客户端进程
                    killGather();
                    //重新启动客户端
                    StartClientApplication();
                }
                else
                {
                    //重新启动客户端
                    StartClientApplication();
                }
            }
            else
            {
                FileUtils.log("进程未启动", "TcpHandle");
            }
        }

        /// <summary>
        /// 将要写入数据的数据先写入缓存
        /// </summary>
        /// <param name="result"></param>
        private void SetAnchorToCache(Dictionary<string, object> result)
        {
          
            result.TryGetValue("SecUid", out object secuid);
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secuid.ToString());
            if (anchorInfo == null) 
            {
                anchorInfo = new AnchorInfo();
            }
            anchorInfo.SecUid = secuid.ToString();
            result.TryGetValue("liveUrl", out object liveUrl);
            if (liveUrl != null) 
            {
                anchorInfo.LiveUrl = liveUrl.ToString();
            }
            if (anchorInfo.Id < 1)
            {
                string baseUrl = "https://www.douyin.com/user/";
                string homeUrl = baseUrl + secuid;
                anchorInfo.HomeUrl = homeUrl;
                anchorInfo.LiveStatus = 0;
                anchorInfo.IsAutoRecord = 1;
                result.TryGetValue("avatarUrl", out object avatarUrl);
                anchorInfo.AnchorAvatar = avatarUrl.ToString();
                result.TryGetValue("nickname", out object nickname);
                anchorInfo.AnchorName = nickname.ToString();
                if (!string.IsNullOrEmpty(anchorInfo.LiveUrl) && anchorInfo.LiveUrl != "null")
                {
                    anchorInfo.AnchorPlatform = "DouYinLive";
                }
                else
                {
                    anchorInfo.AnchorPlatform = "DouYinHomeLive";
                }
            }
            else
            {
                if (!string.IsNullOrEmpty(anchorInfo.LiveUrl) && anchorInfo.LiveUrl != "null")
                {
                    anchorInfo.AnchorPlatform = "DouYinLive";
                    anchorInfo.LiveStatus = 2;
                }
                else
                {
                    anchorInfo.LiveStatus = 4;
                }
            }
            AnchorCacheManager.SetAnchorCache(anchorInfo);
            FileUtils.LogRecrd($"添加的主播对象信息:{JsonConvert.SerializeObject(anchorInfo)}", "主页Url添加主播信息");
            FileUtils.LogRecrd($"全部的主播缓存信息:{JsonConvert.SerializeObject(AnchorCacheManager.GetAllAnchors())}", "主页Url添加主播信息");
        }

        /// <summary>
        /// 将采集的数据写入数据库
        /// </summary>
        /// <param name="result">采集结果</param>
        private void SetAnchorToData(Dictionary<string, object> result) 
        {
            //Dictionary<string, object> result = dto.ActionResult;
            result.TryGetValue("SecUid", out object secuid);
            AnchorInfo anchorInfo = new AnchorInfo();
            anchorInfo.SecUid = secuid.ToString();
            anchorInfo.GetModelBySecuid();
            result.TryGetValue("liveUrl", out object liveUrl);
            anchorInfo.LiveUrl = liveUrl.ToString();
            if (anchorInfo.Id < 1)
            {
                string baseUrl = "https://www.douyin.com/user/";
                string homeUrl = baseUrl + secuid;
                anchorInfo.HomeUrl = homeUrl;
                anchorInfo.LiveStatus = 0;
                anchorInfo.IsAutoRecord = 1;
                result.TryGetValue("avatarUrl", out object avatarUrl);
                anchorInfo.AnchorAvatar = avatarUrl.ToString();
                result.TryGetValue("nickname", out object nickname);
                anchorInfo.AnchorName = nickname.ToString();
                if (!string.IsNullOrEmpty(anchorInfo.LiveUrl) && anchorInfo.LiveUrl != "null")
                {
                    anchorInfo.AnchorPlatform = "DouYinLive";
                }
                else
                {
                    anchorInfo.AnchorPlatform = "DouYinHomeLive";
                }
                anchorInfo.save();
            }
            else
            {
                if (!string.IsNullOrEmpty(anchorInfo.LiveUrl) && anchorInfo.LiveUrl != "null")
                {
                    anchorInfo.AnchorPlatform = "DouYinLive";
                    anchorInfo.LiveStatus = 2;
                }
                else
                {
                    anchorInfo.LiveStatus = 4;
                }
                anchorInfo.update();
            }
        }
    }
}
