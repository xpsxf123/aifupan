using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Model;
using ReviewAnalysis.vo.socketCollectMessage;
using ReviewAnalysis.Websocket.Entity;
using Swan;
using static ReviewAnalysis.Websocket.Entity.WebsocketUtilsEntity;

namespace ReviewAnalysis.Websocket.utils
{
    public class WebsocketUtils
    {

        /// <summary>
        /// 根据开始结束时间获取弹幕内容
        /// </summary>
        /// <param name="batchNumber"></param>
        /// <param name="videoId"></param>
        /// <param name="startTime"></param>
        /// <param name="endTime"></param>
        /// <returns></returns>
        public static List<String> getDanMuData(string batchNumber, long userId, string videoId, string startTime, string endTime)
        {
            List<string> result = new List<string>();
            string path = $"{WebsocketDataHandle.savePath}\\{batchNumber}-{userId}\\danmu-{videoId}.txt";
            string path2 = $"{WebsocketDataHandle.savePath}\\{batchNumber}-{userId}\\danmu.txt";
            if (!File.Exists(path) && File.Exists(path2))
            {
                path = path2;
            }
            if (File.Exists(path))
            {
                DateTime startDateTime = DateTime.Parse(startTime);
                DateTime endDateTime = DateTime.Parse(endTime);

                // 使用 StreamReader 逐行读取
                using (var reader = new StreamReader(path))
                {
                    string line;
                    while ((line = reader.ReadLine()) != null)
                    {
                        if (!string.IsNullOrEmpty(line) && !line.Contains("下播") && !line.Contains("断掉") && !line.Contains("地址出错") && !line.Contains("无数据") && !line.Contains("暂停直播"))
                        {
                            string[] arrTime = Regex.Split(line, ">>>");
                            if (!string.IsNullOrEmpty(arrTime[0]))
                            {
                                DateTime temp = DateTime.Parse(arrTime[0]);
                                if (temp >= startDateTime && temp <= endDateTime)
                                {
                                    result.Add(line);
                                }
                                else if (temp > endDateTime)
                                {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }

        public static DataWebSocket FileStrToJson(string[] fileName, string videoId, string batchNumber, string startTime, string endTime, string secUid, long userId)
        {
            DateTime startDateTime = DateTime.Parse(startTime);
            DateTime endDateTime = DateTime.Parse(endTime);

            // 记录index
            Dictionary<string, int> indexObj = new Dictionary<string, int>();

            // 获取文件中的值
            SortedList<string, List<TimeWithData>> dataList = new SortedList<string, List<TimeWithData>>();
            if (fileName != null && fileName.Length > 0)
            {
                foreach (string fileStr in fileName)
                {
                    indexObj.Add(fileStr, 0);

                    string path = $"{WebsocketDataHandle.savePath}\\{batchNumber}-{userId}\\{fileStr}.txt";
                    if (File.Exists(path))
                    {
                        dataList.Add(fileStr, new List<TimeWithData>());
                        // 使用 StreamReader 逐行读取
                        using (var reader = new StreamReader(path))
                        {
                            string line;
                            while ((line = reader.ReadLine()) != null)
                            {
                                if (!string.IsNullOrEmpty(line) && !line.Contains("下播") && !line.Contains("断掉") && !line.Contains("地址出错") && !line.Contains("无数据") && !line.Contains("暂停直播"))
                                {
                                    string[] arrTime = Regex.Split(line, ">>>");
                                    if (!string.IsNullOrEmpty(arrTime[0]))
                                    {
                                        DateTime temp = DateTime.Parse(arrTime[0]);
                                        if (temp >= startDateTime && temp <= endDateTime)
                                        {
                                            TimeWithData timeWithData = new TimeWithData
                                            {
                                                dateTime = DateTime.Parse(arrTime[0]),
                                                data = arrTime[1]
                                            };
                                            if (dataList.TryGetValue(fileStr, out List<TimeWithData> list))
                                            {
                                                list.Add(timeWithData);
                                            }
                                        }
                                        else if(temp > endDateTime)
                                        {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            List<WebSocketEntity> datas = new List<WebSocketEntity>();

            // 每 30 秒生成一个时间点，并存入字典
            DateTime currentTime = startDateTime;
            while (currentTime < endDateTime)
            {
                // 增加 30 秒
                DateTime tempTime = currentTime.AddSeconds(30);
                if (tempTime <= endDateTime)
                {
                    WebSocketEntity item = new WebSocketEntity()
                    {
                        time = currentTime.ToString("yyyy-MM-dd HH:mm:ss"),
                        videoTime = ((currentTime - startDateTime).TotalSeconds).ToString()
                    };

                    foreach (string fileStr in fileName)
                    {
                        if (dataList.TryGetValue(fileStr, out List<TimeWithData> list))
                        {
                            if(indexObj.TryGetValue(fileStr, out int index) && index < list.Count)
                            {
                                // 为字段赋值,并且更新读取到的下标
                                indexObj[fileStr] = Handle(list, index, currentTime, tempTime, item, fileStr);
                            }
                        }
                    }
                    datas.Add(item);
                }
                currentTime = tempTime;
            }

            DataWebSocket result = new DataWebSocket();
            result.datas = datas;
            result.videoStartTime = startTime;
            result.videoEndTime = endTime;
            // 版本，有1.0   2.0
            result.version = "2.0";
            result.secUid = secUid;
            result.serviceStartTime = startTime;
            result.serviceEndTime = endTime;
            result.userId = userId.ToString();
            result.batchNumber = batchNumber;
            result.videoId = videoId.ToString();

            // 计算场观和累计观看人数和最小最大人数
            int minTotal = 0;
            int maxTotal = 0;
            int minRenShu = 0;
            int maxRenShu = 0;
            if (datas.Count > 0)
            {
                List<int> totalList = datas
                    .Where(v => !string.IsNullOrEmpty(v.leijiguankanrenshu))
                    .Select(v => int.TryParse(v.leijiguankanrenshu, out int temp) ? temp : 0)
                    .ToList();
                minTotal = totalList.DefaultIfEmpty(0).Min(v => v);
                maxTotal = totalList.DefaultIfEmpty(0).Max(v => v);
                List<int> renShuList = datas
                    .Where(v => !string.IsNullOrEmpty(v.renshu))
                    .Select(v => int.TryParse(v.renshu, out int temp) ? temp : 0)
                    .ToList();
                minRenShu = renShuList.DefaultIfEmpty(0).Min(v => v);
                maxRenShu = renShuList.DefaultIfEmpty(0).Max(v => v);
            }


            result.observationNum = (maxTotal - minTotal).ToString();
            result.totalOnlineNum = maxTotal.ToString();
            result.maxRenShu = maxRenShu.ToString();
            result.minRenShu = minRenShu.ToString();
            return result;
        }

        /// <summary>
        /// 为json中的对象属性赋值
        /// </summary>
        /// <param name="list"></param>
        /// <param name="index"></param>
        /// <param name="startTime"></param>
        /// <param name="endTime"></param>
        /// <param name="item"></param>
        /// <param name="dataType"></param>
        /// <returns></returns>
        public static int Handle(List<TimeWithData> list, int index, DateTime startTime, DateTime endTime, WebSocketEntity item, string dataType)
        {
            // 统计符合条件的数据，以时间范围搜索，[ ) 左闭右开
            List<TimeWithData> temp = new List<TimeWithData>();
            int tempIndex = index;
            int result = -1;
            for (int i = tempIndex; i < list.Count; i++)
            {
                if(startTime <= list[i].dateTime && list[i].dateTime < endTime)
                {
                    temp.Add(list[i]);
                    tempIndex = i;
                    result = i;
                }
                else
                {
                    // 如果没有找到数据，返回单前的下标
                    //result = index == tempIndex ? index : tempIndex + 1;
                    result = result == -1 ? index : tempIndex + 1;

                    break;
                }
            }

            // 赋值
            AssignmentHandle(item, temp, dataType);
            return result;
        }

        /// <summary>
        /// 每个类型对应为字段赋值
        /// </summary>
        /// <param name="data"></param>
        /// <param name="list"></param>
        /// <param name="dataType"></param>
        public static void AssignmentHandle(WebSocketEntity data, List<TimeWithData> list, string dataType)
        {
            if (list.Any())
            {
                if (dataType == "renshu")
                {
                    // 获取list中的最大值
                    data.renshu = list.Max(item => int.TryParse(item.data, out int value) ? value : 0).ToString();
                }
                else if (dataType == "guanzhu" || dataType == "fensituan")
                {
                    //int max = list.Max(item => int.TryParse(item.data, out int value) ? value : 0);
                    //int min = list.Min(item => int.TryParse(item.data, out int value) ? value : 0);
                    if (dataType == "guanzhu")
                    {
                        //data.guanzhu = (max - min).ToString();
                        data.guanzhu = list.Count.ToString();

                    }
                    else
                    {
                        //data.fensituan = (max - min).ToString();
                        data.fensituan = list.Count.ToString();
                    }
                }
                else if (dataType == "leijiguankanrenshu")
                {
                    //int max = list.DefaultIfEmpty(0).Max(item => int.TryParse(item.data, out int value) ? value : 0);
                    //int min = list.DefaultIfEmpty(0).Min(item => int.TryParse(item.data, out int value) ? value : 0);
                    data.leijiguankanrenshu = list[0].data;
                }
            }
        }

        /// <summary>
        /// 从cos服务器下载Websocket数据
        /// </summary>
        /// <param name="batchNumber"></param>
        /// <param name="videoId"></param>
        public static void downloadWebsocketData(string batchNumber, string userId, string videoId)
        {
            try
            {
                //如果本地没有，从cos服务器下载一份
                SocketCollectMessageVo socketMessage = WordApi.socketMessageInfoNotJson(null, videoId, batchNumber);
                if (socketMessage != null && !string.IsNullOrEmpty(socketMessage.cosKey))
                {
                    string cosKey = socketMessage.cosKey;
                    string tempPath = $"{WebsocketDataHandle.savePath}\\{batchNumber}-{userId}";
                    if (!Directory.Exists(tempPath))
                    {
                        Directory.CreateDirectory(tempPath);
                    }
                    TencentCosUtils.downloadWebcsocketJsonFile(cosKey, $"{WebsocketDataHandle.savePath}/{batchNumber}-{userId}", $"{videoId}.zip");
                }
            }
            catch (Exception e)
            {
                FileUtils.LogError($"websocket查询数据接口请求失败, msg = {e.Message}, StackTrace = {e.StackTrace}");
            }
        }
    }
}
