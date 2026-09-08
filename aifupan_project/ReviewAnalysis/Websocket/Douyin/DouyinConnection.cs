using BarrageGrab.Entity.Protobuf.Douyin;
using douyin.Utils;
using Google.Protobuf;
using ReviewAnalysis.Websocket.Douyin.Helper;
using ReviewAnalysis.Websocket.Entity;
using Swan.Formatters;
using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.WebSockets;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.Websocket.Douyin
{
    /// <summary>
    /// 抖音的websocket链接
    /// </summary>
    public class DouyinConnection
    {
        private static int maxError = 5;

        private static int dataTimerMax = 1000 * 60 * 2;


        public static void addWebsocketLink(WebsocketEntity data)
        {
            if (data != null && data.websocketLinkStatus == 0 && !string.IsNullOrEmpty(data.ttwid) && !string.IsNullOrEmpty(data.wssUrl))
            {
                Task task = Task.Run(async() =>
                {
                    try
                    {
                        string taskId = Guid.NewGuid().ToString();
                        data.websocketTaskId = taskId;
                        ClientWebSocket clientWebSocket = null;

                        data.websocketLinkStatus = 1;
                        int errorNum = 0;
                        while (true)
                        {
                            clientWebSocket = new ClientWebSocket();
                            bool isContinue = false;
                            if (data.websocketTaskId != taskId) return;
                            clientWebSocket.Options.SetRequestHeader("cookie", $"ttwid={data.ttwid}");
                            Debug.WriteLine($"链接websocket：ttwid={data.ttwid}，wss={data.wssUrl}");
                            System.Timers.Timer heartbeatTimer = null;
                            System.Timers.Timer dataTimer = new System.Timers.Timer(dataTimerMax);
                            dataTimer.Elapsed += (sender, e) =>
                            {
                                FileUtils.log($"{Json.Serialize(data)}==true", "一分钟没有推数据过来");
                                isContinue = true;
                            };
                            dataTimer.Start();
                            if (errorNum >= maxError)
                            {
                                data.websocketLinkStatus = 3;
                                break;
                            }

                            try
                            {
                                await clientWebSocket.ConnectAsync(new Uri(data.wssUrl), CancellationToken.None);
                                if (clientWebSocket.State != WebSocketState.Open && clientWebSocket.State != WebSocketState.Connecting)
                                {
                                    //throw new Exception("连接服务器失败");
                                    data.websocketLinkStatus = 3;
                                    break;
                                }

                                #region 发送hb心跳
                                try
                                {
                                    byte[] heartbeat = new byte[] { 0x3a, 0x02, 0x68, 0x62 };
                                    await clientWebSocket.SendAsync(new ArraySegment<byte>(heartbeat), WebSocketMessageType.Binary, true, CancellationToken.None);

                                    heartbeatTimer = new System.Timers.Timer(10000);
                                    heartbeatTimer.Enabled = true;
                                    heartbeatTimer.Start();
                                    heartbeatTimer.Elapsed += async (sender, e) =>
                                    {
                                        if (clientWebSocket?.State == WebSocketState.Open)
                                        {
                                            clientWebSocket?.SendAsync(new ArraySegment<byte>(heartbeat), WebSocketMessageType.Binary, true, CancellationToken.None);
                                        }
                                        else
                                        {
                                            heartbeatTimer.Stop();
                                            isContinue = true;
                                        }
                                    };
                                }
                                catch (Exception ex)
                                {
                                    //do something
                                    return;
                                }
                                #endregion

                                if (data.websocketTaskId != taskId) return;
                                if (!isContinue)
                                {
                                    // 如果没有关闭，就一直接收
                                    while (data.websocketTaskId == taskId && clientWebSocket.State == WebSocketState.Open)
                                    {
                                        if (data.websocketTaskId != taskId) return;
                                        if (isContinue) break;
                                        // 缓冲写大一些，就不用while循环分多次取 - 1M
                                        byte[] buffer = new byte[1024 * 1024];
                                        WebSocketReceiveResult result = null;

                                        // 监听Socket信息，接收连接的套接字发来的数据
                                        try
                                        {
                                            result = await clientWebSocket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
                                        }
                                        catch (Exception)
                                        {
                                            FileUtils.log($"{Json.Serialize(data)}", "监听Socket信息，接收连接的套接字发来的数据 报错");
                                            if (isContinue)
                                            {
                                                break;
                                            }
                                        }
                                        if(result == null) continue;

                                        try
                                        {
                                            Array.Resize(ref buffer, result.Count);

                                            var package = PushFrame.Parser.ParseFrom(buffer);
                                            var response = Response.Parser.ParseFrom(DecompressHelper.Decompress(package.Payload.ToArray()));

                                            #region if NeedAck
                                            if (response.NeedAck)
                                            {
                                                PushFrame ack = new PushFrame()
                                                {
                                                    LogId = package.LogId,
                                                    PayloadType = "ack",
                                                    Payload = ByteString.CopyFromUtf8(response.InternalExt)
                                                };

                                                await clientWebSocket.SendAsync(new ArraySegment<byte>(ack.ToByteString().ToArray()), WebSocketMessageType.Binary, true, CancellationToken.None);
                                            }
                                            #endregion

                                            #region 处理消息数据
                                            data.websocketLinkStatus = 2;
                                            if (response.MessagesList != null && response.MessagesList.Count > 0)
                                            {

                                                errorNum = 0;
                                                foreach (var message in response.MessagesList)
                                                {
                                                    dataTimer.Interval = dataTimerMax;
                                                    switch (message.Method)
                                                    {
                                                        #region WebcastMemberMessage 进入
                                                        case "WebcastMemberMessage":
                                                            {
                                                                //MemberMessage memberMsg = MemberMessage.Parser.ParseFrom(message.Payload);
                                                                //FileUtils.AppendJsonToFile(Path.GetFullPath("webSocketData\\jinru.txt"), Json.Serialize(memberMsg));
                                                                //int level = (int)(memberMsg.User?.PayGrade?.Level ?? null);
                                                                //int fansLevel = (int)(memberMsg.User?.FansClub?.Data?.Level ?? null);
                                                                //string fansName = memberMsg.User?.FansClub?.Data?.ClubName;
                                                                //string name = memberMsg.User?.NickName ?? "";
                                                                //JinRu jinRu = new JinRu()
                                                                //{
                                                                //    type = "jinru",
                                                                //    msgId = $"{memberMsg?.Common?.MsgId}",
                                                                //    content = $"{name} 来了",
                                                                //    nickName = name,
                                                                //    level = level,
                                                                //    fansLevel = fansLevel,
                                                                //    fansName = fansName,
                                                                //};
                                                                //WebsocketDataHandle.Push("jinru", data, jinRu);
                                                                break;
                                                            }
                                                        #endregion
                                                        #region WebcastSocialMessage 关注 & 分享
                                                        case "WebcastSocialMessage":
                                                            {
                                                                SocialMessage socialMessage = SocialMessage.Parser.ParseFrom(message.Payload);

                                                                #region 分享
                                                                if (socialMessage.Action == 3)
                                                                {
                                                                    //    OpenBarrageMessage obm = new OpenBarrageMessage()
                                                                    //    {
                                                                    //        Type = MessageTypeEnum.Share,
                                                                    //        Data = new DouyinMsgShare()
                                                                    //        {
                                                                    //            MsgId = (long)socialMessage.Common.MsgId,
                                                                    //            Content = $"{socialMessage.User.NickName} 分享了直播间到{socialMessage.ShareTarget}",
                                                                    //            RoomId = (long)socialMessage.Common.RoomId,
                                                                    //            //ShareType = socialMessage.ShareTarget,
                                                                    //            User = GetUser(socialMessage.User)
                                                                    //        }
                                                                    //    };

                                                                    //    ApplicationRuntime.LocalWebSocketServer?.Broadcast(JsonConvert.SerializeObject(obm));

                                                                    //    ApplicationRuntime.MainForm?.PrintConsole($"[分享]{socialMessage.User.NickName} 分享了直播间到{socialMessage.ShareTarget}");
                                                                }
                                                                #endregion

                                                                #region 关注
                                                                else
                                                                {
                                                                    //FileUtils.AppendJsonToFile(Path.GetFullPath("webSocketData\\guanzhu.txt"), Json.Serialize(socialMessage));
                                                                    int level = (int)(socialMessage.User?.PayGrade?.Level ?? null);
                                                                    int fansLevel = (int)(socialMessage.User?.FansClub?.Data?.Level ?? null);
                                                                    string fansName = socialMessage.User?.FansClub?.Data?.ClubName;
                                                                    string name = socialMessage.User?.NickName ?? "";
                                                                    GuanZhu jinRu = new GuanZhu()
                                                                    {
                                                                        type = "guanzhu",
                                                                        msgId = $"{socialMessage?.Common?.MsgId}",
                                                                        content = $"{name} 关注了主播",
                                                                        nickName = name,
                                                                        level = level,
                                                                        fansLevel = fansLevel,
                                                                        fansName = fansName,
                                                                    };
                                                                    WebsocketDataHandle.Push("guanzhu", data, jinRu);
                                                                }
                                                                #endregion

                                                                break;
                                                            }
                                                        #endregion
                                                        #region WebcastChatMessage 弹幕
                                                        case "WebcastChatMessage":
                                                            {
                                                                ChatMessage chatMessage = ChatMessage.Parser.ParseFrom(message.Payload);
                                                                //FileUtils.AppendJsonToFile(Path.GetFullPath("webSocketData\\danmu.txt"), Json.Serialize(chatMessage));
                                                                int level = (int)(chatMessage.User?.PayGrade?.Level ?? 0);
                                                                int fansLevel = (int)(chatMessage.User?.FansClub?.Data?.Level ?? null);
                                                                string fansName = chatMessage.User?.FansClub?.Data?.ClubName;
                                                                string name = chatMessage.User?.NickName ?? "";
                                                                DanMu dnamu = new DanMu()
                                                                {
                                                                    type = "danmu",
                                                                    msgId = $"{chatMessage?.Common?.MsgId}",
                                                                    content = $"{chatMessage?.Content ?? ""}",
                                                                    nickName = name,
                                                                    level = level,
                                                                    fansLevel = fansLevel,
                                                                    fansName = fansName,
                                                                };
                                                                WebsocketDataHandle.Push("danmu", data, dnamu);
                                                                break;
                                                            }
                                                        #endregion
                                                        #region WebcastLikeMessage 点赞
                                                        case "WebcastLikeMessage":
                                                            {
                                                                LikeMessage likeMessage = LikeMessage.Parser.ParseFrom(message.Payload);
                                                                //FileUtils.AppendJsonToFile(Path.GetFullPath("webSocketData\\dianzan.txt"), Json.Serialize(likeMessage));
                                                                int level = (int)(likeMessage.User?.PayGrade?.Level ?? 0);
                                                                int fansLevel = (int)(likeMessage.User?.FansClub?.Data?.Level ?? null);
                                                                string fansName = likeMessage.User?.FansClub?.Data?.ClubName;
                                                                string name = likeMessage.User?.NickName ?? "";
                                                                DianZan dianzan = new DianZan()
                                                                {
                                                                    type = "dianzan",
                                                                    msgId = $"{likeMessage?.Common?.MsgId}",
                                                                    content = $"",
                                                                    nickName = name,
                                                                    level = level,
                                                                    fansLevel = fansLevel,
                                                                    fansName = fansName,
                                                                };
                                                                WebsocketDataHandle.Push("dianzan", data, dianzan);
                                                                break;
                                                            }
                                                        #endregion
                                                        #region WebcastGiftMessage 礼物
                                                        case "WebcastGiftMessage":
                                                            {
                                                                //GiftMessage giftMessage = GiftMessage.Parser.ParseFrom(message.Payload);
                                                                //WebsocketDataHandle.Push("liwu", data);
                                                                //FileUtils.AppendJsonToFile(Path.GetFullPath("webSocketData\\liwu.txt"), Json.Serialize(giftMessage));
                                                                break;
                                                            }
                                                        #endregion
                                                        #region WebcastFansclubMessage 粉丝团
                                                        case "WebcastFansclubMessage":
                                                            {
                                                                FansclubMessage fansclubMessage = FansclubMessage.Parser.ParseFrom(message.Payload);
                                                                //FileUtils.AppendJsonToFile(Path.GetFullPath("webSocketData\\fensi.txt"), Json.Serialize(fansclubMessage));
                                                                int level = (int)(fansclubMessage.User?.PayGrade?.Level ?? 0);
                                                                int fansLevel = (int)(fansclubMessage.User?.FansClub?.Data?.Level ?? null);
                                                                string fansName = fansclubMessage.User?.FansClub?.Data?.ClubName;
                                                                string name = fansclubMessage.User?.NickName ?? "";
                                                                FensTuan fensTuan = new FensTuan()
                                                                {
                                                                    type = "fensituan",
                                                                    msgId = $"{fansclubMessage?.CommonInfo?.MsgId}",
                                                                    content = $"{fansclubMessage?.Content ?? ""}",
                                                                    nickName = name,
                                                                    level = level,
                                                                    fansLevel = fansLevel,
                                                                    fansName = fansName,
                                                                };
                                                                WebsocketDataHandle.Push("fensituan", data, fensTuan);
                                                                break;
                                                            }
                                                        #endregion
                                                        #region WebcastRoomUserSeqMessage 统计
                                                        case "WebcastRoomUserSeqMessage":
                                                            {
                                                                RoomUserSeqMessage roomUserSeqMessage = RoomUserSeqMessage.Parser.ParseFrom(message.Payload);
                                                                //FileUtils.AppendJsonToFile(Path.GetFullPath("webSocketData\\tongji.txt"), Json.Serialize(roomUserSeqMessage));


                                                                RenShu renshu = new RenShu()
                                                                {
                                                                    type = "renshu",
                                                                    msgId = $"{roomUserSeqMessage?.Common?.MsgId}",
                                                                    content = $"{roomUserSeqMessage?.Total}",
                                                                };
                                                                WebsocketDataHandle.Push("renshu", data, renshu);

                                                                ChangGuan changGuan = new ChangGuan()
                                                                {
                                                                    type = "leijiguankanrenshu",
                                                                    msgId = $"{roomUserSeqMessage?.Common?.MsgId}",
                                                                    content = $"{roomUserSeqMessage?.TotalUser}",
                                                                };
                                                                WebsocketDataHandle.Push("leijiguankanrenshu", data, changGuan);
                                                                break;
                                                            }
                                                        #endregion
                                                        #region WebcastControlMessage 直播间状态变更
                                                        case "WebcastControlMessage":
                                                            ControlMessage controlMessage = ControlMessage.Parser.ParseFrom(message.Payload);
                                                            //WebsocketDataHandle.Push("WebcastControlMessage", data);
                                                                //FileUtils.AppendJsonToFile(Path.GetFullPath("webSocketData\\zhuantai.txt"), Json.Serialize(controlMessage));
                                                            break;
                                                        #endregion
                                                        #region 房间状态
                                                        case "WebcastRoomStatsMessage":
                                                            {
                                                                RoomStatsMessage msgRoomStats = RoomStatsMessage.Parser.ParseFrom(message.Payload);
                                                                //FileUtils.AppendJsonToFile(Path.GetFullPath("webSocketData\\fangjian.txt"), Json.Serialize(msgRoomStats));
                                                                
                                                                RenShu renshu = new RenShu()
                                                                {
                                                                    type = "renshu",
                                                                    msgId = $"{msgRoomStats?.Common?.MsgId}",
                                                                    content = $"{msgRoomStats?.Total}",
                                                                };
                                                                WebsocketDataHandle.Push("renshu", data, renshu);

                                                                if (msgRoomStats.DisplayType == 3)
                                                                {
                                                                    ChangGuan changGuan = new ChangGuan()
                                                                    {
                                                                        type = "leijiguankanrenshu",
                                                                        msgId = $"{msgRoomStats?.Common?.MsgId}",
                                                                        content = $"{msgRoomStats?.DisplayValue}",
                                                                    };
                                                                    WebsocketDataHandle.Push("leijiguankanrenshu", data, changGuan);
                                                                }
                                                                //WebsocketDataHandle.Push("WebcastRoomStatsMessage", data);
                                                                break;
                                                            }
                                                        #endregion
                                                        default:
                                                            break;
                                                    }
                                                }
                                            }
                                            #endregion

                                        }
                                        catch (Exception ex)
                                        {
                                            FileUtils.log($"{Json.Serialize(data)}", "解析websocket报错");

                                        }

                                    }
                                }
                                heartbeatTimer?.Stop();
                                heartbeatTimer?.Dispose();

                            }
                            catch (Exception ex)
                            {
                                FileUtils.log($"{Json.Serialize(data)}=={ex.Message}", "第二个while循环");
                            }
                            errorNum++;
                            data.websocketLinkStatus = 4;
                            Thread.Sleep(4000);
                        }

                        // 关闭websocket链接
                        try
                        {
                            clientWebSocket?.Abort();
                        }
                        catch (Exception ex)
                        {
                            // do something
                        }
                        try
                        {
                            clientWebSocket?.Dispose();
                            clientWebSocket = null;
                        }
                        catch (Exception)
                        {
                            // do something
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.log($"{Json.Serialize(data)}===={ex.ToString()}", "第一个while循环");
                    }
                });
            }
            

        }


    }
}
