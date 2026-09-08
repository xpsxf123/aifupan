using EmbedIO;
using ReviewAnalysis.Global;
using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using douyin.Utils;



namespace ReviewAnalysis.HttpServer
{
    public class HttpResourceFileServer
    {
        private string url;

        private string resourceFilePath;


        private WebServer _server;
        public HttpResourceFileServer(string _resourceFilePath)
        {
            url = "http://localhost:45001/";
            resourceFilePath = _resourceFilePath;
        }
        public HttpResourceFileServer()
        {
        }

        /// <summary>
        /// 开启端口监听
        /// </summary>
        /// <param name="serverPath">监听的地址,如：C:\\abc\\a\\</param>
        public async Task<string> StartResourceFileServer(string serverPath)
        {
            int port = Constant.protNum;
            int i = 10;
            while (i > 0)
            {
                port++;
                // 向即将要指向的文件夹地址写入测试文本
                using (StreamWriter writer = new StreamWriter(serverPath + "test.txt"))
                {
                    writer.Write("success");
                }
                // 监听端口
                string url = "http://localhost:" + port + "/";
                WebServer server = new WebServer(url)
                    .WithLocalSessionManager()
                    .WithStaticFolder("/", serverPath, true); // Serve static files from wwwroot folder

                server.RunAsync();
                // 访问测试文件，看服务是否成功启动
                Thread.Sleep(500);
                //bool result = await ReadTxt(url + "/test.txt");
                WebClient webClient = new WebClient();
                string html = webClient.GetHtml(url + "/test.txt");
                if (!string.IsNullOrEmpty(html))
                {
                    // 删除测试文件
                    if (File.Exists(serverPath + "test.txt"))
                    {
                        File.Delete(serverPath + "test.txt");
                    }

                    // 往缓存添加记录
                    Constant.portList.Add(serverPath, port + "");
                    Constant.protNum = port;
                    return port + "";
                }

            }

            return "8745";
        }
        //private CancellationTokenSource _cts = new CancellationTokenSource();
        public async Task<string> StartResourceFileServerAsync(string serverPath)
        {
            int port = Constant.protNum;
            int i = 10;
            while (i > 0)
            {
                port++;
                // 向即将要指向的文件夹地址写入测试文本
                using (StreamWriter writer = new StreamWriter(serverPath + "test.txt"))
                {
                    writer.Write("success");
                }
                // 监听端口
                string url = "http://localhost:" + port + "/";
 
                // 后台运行WebServer（脱离UI线程）
                new Task(async () =>
                {
                    WebServer server = new WebServer(url)
                        .WithLocalSessionManager()
                        .WithStaticFolder("/", serverPath, true); // Serve static files from wwwroot folder
                    // 运行WebServer，传入取消令牌支持优雅退出
                    await server.RunAsync();
                }, TaskCreationOptions.LongRunning).Start();
                // 访问测试文件，看服务是否成功启动
                //await Task.Delay(500);
                //bool result = await ReadTxt(url + "/test.txt");
                WebClient webClient = new WebClient();
                string html = webClient.GetHtml(url + "/test.txt");
                if (!string.IsNullOrEmpty(html))
                {
                    // 删除测试文件
                    if (File.Exists(serverPath + "test.txt"))
                    {
                        File.Delete(serverPath + "test.txt");
                    }

                    // 往缓存添加记录
                    Constant.portList.Add(serverPath, port + "");
                    Constant.protNum = port;
                    return await Task.FromResult(port + "");
                }

            }

            return await Task.FromResult("8745");
        }

        private async Task<bool> ReadTxt(string url)
        {

            try
            {
                // 创建 HttpClient 实例
                using (HttpClient client = new HttpClient())
                {
                    // 发送 GET 请求并获取响应内容
                    HttpResponseMessage response = await client.GetAsync(url);

                    // 检查响应状态码
                    if (response.IsSuccessStatusCode)
                    {
                        // 读取响应流中的内容
                        string content = await response.Content.ReadAsStringAsync();
                        //FileUtils.log("Content of the file:");
                        //FileUtils.log(content);
                        return true;
                    }
                    else
                    {
                        FileUtils.log($"Failed to retrieve the file. Status code: {response.StatusCode}");
                    }
                }
                return false;
            }
            catch (Exception ex)
            {
                FileUtils.log($"An error occurred: {ex.Message}");
                return false;
            }
        }

        public void StartResourceFileServer()
        {
            _server = new WebServer(url)
                .WithLocalSessionManager()
                .WithStaticFolder("/", resourceFilePath, true); // Serve static files from wwwroot folder

            _server.RunAsync();
        }

        private void StopServer()
        {
            if (_server != null)
            {
                _server.Dispose(); // 关闭服务器并释放资源
                _server = null; // 清除引用
                FileUtils.log("Server stopped.");
            }
        }

        /// <summary>
        /// 根据地址获取映射的端口
        /// </summary>
        /// <param name="serverPath">地址</param>
        /// <returns></returns>
        public string GetPort(string serverPath)
        {

            if(Constant.portList != null && Constant.portList.Count > 0)
            {
                if(Constant.portList.ContainsKey(serverPath))
                {
                    return Constant.portList[serverPath];
                }
            }

            return StartResourceFileServer(serverPath).Result;
        }
        public async Task<string> GetPortAsync(string serverPath)
        {

            if (Constant.portList != null && Constant.portList.Count > 0)
            {
                if (Constant.portList.ContainsKey(serverPath))
                {
                    return Constant.portList[serverPath];
                }
            }

            return await StartResourceFileServerAsync(serverPath);
        }
    }
}
