using douyin.Utils;
using Microsoft.Playwright;
using OpenCvSharp;
using System.Net.Http;
using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using System.Linq;
using System.Threading;

namespace ReviewAnalysis.Utils.kuaishou
{
    /// <summary>
    /// 过验证码
    /// </summary>
    public class KuaiShouCaptchaUtils
    {
        /// <summary>
        /// 快手请求cookies的did
        /// </summary>
        public volatile static string did = "";

        static Random random = new Random();
        // 声明一个 SemaphoreSlim 作为异步锁
        private static readonly SemaphoreSlim _semaphore = new SemaphoreSlim(1, 1);

        /// <summary>
        /// 是否第二次过验证码
        /// </summary>
        private static volatile bool isTwoBrowserAuto = false;

        /// <summary>
        /// 清除did
        /// </summary>
        public static void clearDid()
        {
            did = "";
        }

        /// <summary>
        /// 获取did
        /// </summary>
        /// <returns></returns>
        public static async Task<string> getDid()
        {
            if(!string.IsNullOrEmpty(did))
            {
                return did;
            }

            // 获取
            await createDid();
            // 解封
            KuaiShouGetStreamClient instance = KuaiShouGetStreamClient.Instance;
            await instance.KuaishouUnblock(did);
            return did;
        }

        /// <summary>
        /// 获取did-过验证码
        /// </summary>
        /// <returns></returns>
        public static async Task<string> getDidCaptcha()
        {
            if (!string.IsNullOrEmpty(did))
            {
                return did;
            }

            // 获取
            await createDidCaptcha();

            return did;
        }

        /// <summary>
        /// 生成did
        /// </summary>
        /// <returns></returns>
        private static async Task createDid()
        {
            await _semaphore.WaitAsync(); // 异步等待获取锁


            try
            {
                int i = 0;
                while (true)
                {
                    i++;

                    isTwoBrowserAuto = false;
                    IBrowser browser = null;
                    IBrowserContext browserContext = null;
                    IPage page = null;

                    try
                    {
                        // 创建浏览器对象
                        browser = await createBrowser();
                        browserContext = await createBrowserContext(browser);
                        page = await createBrowserPage(browserContext);

                        // 浏览器自动化操作过验证码
                        did = await browserKuaishouHome(page, browserContext);

                        if (!string.IsNullOrEmpty(did) || i >= 3)
                        {
                            break;
                        }
                    }
                    finally
                    {
                        // 确保资源被释放
                        if (page != null)
                        {
                            await page.CloseAsync();
                            page = null;
                        }
                        if (browserContext != null)
                        {
                            await browserContext.CloseAsync();
                            browserContext = null;
                        }
                        if (browser != null)
                        {
                            await browser.CloseAsync();
                            browser = null;
                        }
                    }

                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"生成did发生异常");
            }
            finally
            {
                _semaphore.Release(); // 释放锁
            }
        }

        /// <summary>
        /// 生成did-过验证码
        /// </summary>
        /// <returns></returns>
        private static async Task createDidCaptcha()
        {

            await _semaphore.WaitAsync(); // 异步等待获取锁


            try
            {
                int i = 0;
                while(true)
                {
                    i++;

                    isTwoBrowserAuto = false;
                    IBrowser browser = null;
                    IBrowserContext browserContext = null;
                    IPage page = null;

                    try
                    {
                        // 创建浏览器对象
                        browser = await createBrowser();
                        browserContext = await createBrowserContext(browser);
                        page = await createBrowserPage(browserContext);

                        // 浏览器自动化操作过验证码
                        did = await browserAuto(page, browserContext);

                        if (!string.IsNullOrEmpty(did) || i >= 3)
                        {
                            break;
                        }
                    }
                    finally
                    {
                        // 确保资源被释放
                        if (page != null)
                        {
                            await page.CloseAsync();
                            page = null;
                        }
                        if (browserContext != null)
                        {
                            await browserContext.CloseAsync();
                            browserContext = null;
                        }
                        if (browser != null)
                        {
                            await browser.CloseAsync();
                            browser = null;
                        }
                    }
                    
                }
            }
            catch(Exception ex)
            {
                FileUtils.LogError($"{ex}", $"生成did发生异常");
            }
            finally
            {
                _semaphore.Release(); // 释放锁
            }
        }

        /// <summary>
        /// 加载快手首页获取did
        /// </summary>
        /// <param name="page"></param>
        /// <param name="context"></param>
        /// <returns></returns>
        private static async Task<string> browserKuaishouHome(IPage page, IBrowserContext context)
        {
            try
            {
                var url = "https://www.kuaishou.com/?isHome=1";
                await page.GotoAsync(url);
                return await getDidCookies(context);
            }
            catch(Exception ex)
            {
                FileUtils.LogError($"{ex}", $"加载快手首页获取did发生异常");
            }

            return null;
        }

        /// <summary>
        /// 浏览器自动化操作过验证码
        /// </summary>
        /// <returns></returns>
        private static async Task<string> browserAuto(IPage page, IBrowserContext context)
        {
            try
            {
                var url = "https://www.kuaishou.com/?isHome=1";

                if (!isTwoBrowserAuto)
                {
                    // 模拟真实用户行为：先访问其他页面
                    await page.GotoAsync("https://www.baidu.com");
                    await Task.Delay(TimeSpan.FromSeconds(random.NextDouble() * 2.0 + 1.0));

                    // 访问快手首页
                    await page.GotoAsync(url);
                }

                // 模拟真实用户浏览行为
                await Task.Delay(TimeSpan.FromSeconds(random.NextDouble() * 2.0 + 2.0));
                await page.Mouse.WheelAsync(0, random.Next(100, 300));
                await Task.Delay(TimeSpan.FromSeconds(random.NextDouble() * 1.0 + 0.5));
                // 模拟鼠标在页面上的随机移动
                for (int i = 0; i < random.Next(2, 5); i++)
                {
                    await page.Mouse.MoveAsync(
                        random.Next(200, 1720),
                        random.Next(200, 880)
                    );
                    await Task.Delay(TimeSpan.FromMilliseconds(random.NextDouble() * 300 + 100));
                }

                // 获取 iframe
                var iframeElement = await page.QuerySelectorAsync("iframe");
                //if(iframeElement == null)
                //{
                //    // 没有验证码弹窗，直接获取did
                //    await page.GotoAsync(url);
                //    return await getDidCookies(context);
                //}
                var iframe = await iframeElement.ContentFrameAsync();
                // 等待验证码元素加载
                await iframe.WaitForSelectorAsync(".bg-img", new FrameWaitForSelectorOptions { Timeout = 10000 });
                await iframe.WaitForSelectorAsync(".slider-img", new FrameWaitForSelectorOptions { Timeout = 10000 });
                await iframe.WaitForSelectorAsync(".slider-btn", new FrameWaitForSelectorOptions { Timeout = 10000 });
                // 获取背景图和滑块图
                var bgElement = await iframe.QuerySelectorAsync(".bg-img");
                var slideElement = await iframe.QuerySelectorAsync(".slider-img");
                var sliderBtn = await iframe.QuerySelectorAsync(".slider-btn");
                var bgBytes = await bgElement.GetAttributeAsync("src");
                var slideBytes = await slideElement.GetAttributeAsync("src");

                // 进行高精度滑块识别
                var distance = GetSliderDistance(bgBytes, slideBytes);
                distance = distance * 316 / 686 - 8;

                // 获取滑块按钮位置
                var sliderBtnLocation = await sliderBtn.BoundingBoxAsync();
                // 模拟随机延迟
                await Task.Delay(TimeSpan.FromSeconds(random.NextDouble() * 1.5 + 0.5));
                // 模拟真实用户发现并移动到滑块的过程
                var startX = sliderBtnLocation.X + sliderBtnLocation.Width / 2;
                var startY = sliderBtnLocation.Y + sliderBtnLocation.Height / 2;
                // 模拟用户寻找滑块的过程：先移动到附近区域
                var approachX = startX + random.Next(-50, 51);
                var approachY = startY + random.Next(-30, 31);
                await page.Mouse.MoveAsync(approachX, approachY);
                await Task.Delay(TimeSpan.FromMilliseconds(random.NextDouble() * 300 + 200));

                // 然后精确移动到滑块
                var initialX = startX + random.Next(-3, 4);
                var initialY = startY + random.Next(-3, 4);

                await page.Mouse.MoveAsync(initialX, initialY);
                await Task.Delay(TimeSpan.FromMilliseconds(random.NextDouble() * 400 + 300));

                // 模拟用户思考时间
                await Task.Delay(TimeSpan.FromMilliseconds(random.NextDouble() * 500 + 500));

                // 按住滑块并滑动
                await page.Mouse.DownAsync();
                await Task.Delay(TimeSpan.FromMilliseconds(random.NextDouble() * 200 + 100));

                // 生成自然滑动路径（相对偏移）
                var path = GenerateNaturalPath(distance);

                foreach (var (offsetX, offsetY) in path)
                {
                    // 计算绝对位置：起始位置 + 相对偏移
                    var targetX = startX + offsetX;
                    var targetY = startY + offsetY;

                    await page.Mouse.MoveAsync(targetX, targetY, new MouseMoveOptions
                    {
                        Steps = random.Next(3, 8) // 减少步数，使移动更流畅
                    });

                    // 随机暂停，模拟人类操作
                    if (random.NextDouble() < 0.3)
                        await Task.Delay(TimeSpan.FromMilliseconds(random.NextDouble() * 50 + 20));

                }

                await page.Mouse.UpAsync();

                // 等待验证结果
                await Task.Delay(TimeSpan.FromSeconds(random.NextDouble() * 1.0 + 3));

                await page.GotoAsync(url);

                await Task.Delay(TimeSpan.FromSeconds(random.NextDouble() * 1.0 + 1));

                // 检查验证是否成功（可以通过检查iframe是否消失或页面变化来判断）
                var iframeStillExists = await page.QuerySelectorAsync("iframe");

                if (iframeStillExists == null)
                {
                    // 从cookies中获取did的值
                    return await getDidCookies(context);
                }else if(!isTwoBrowserAuto)
                {
                    // 过第二次验证码
                    isTwoBrowserAuto = true;
                    return await browserAuto(page, context);
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"自动化操作过验证码发生异常");
            }

            return "";
        }

        /// <summary>
        /// 从cookies中获取did的值
        /// </summary>
        /// <param name="context"></param>
        /// <returns></returns>
        private static async Task<string> getDidCookies(IBrowserContext context)
        {
            var cookies = await context.CookiesAsync();

            // 按域名分组显示cookies
            var groupedCookies = cookies.GroupBy(c => c.Domain).OrderBy(g => g.Key);

            foreach (var domainGroup in groupedCookies)
            {
                if (domainGroup.Key.Contains("kuaishou.com"))
                {
                    foreach (var cookie in domainGroup.OrderBy(c => c.Name))
                    {
                        if (cookie.Name.Equals("did"))
                        {
                            return cookie.Value;
                        }
                    }
                }
            }

            return "";
        }

        /// <summary>
        /// 生成自然的滑动轨迹
        /// </summary>
        /// <param name="distance">距离</param>
        /// <param name="steps">步数</param>
        /// <returns></returns>
        private static (int, int)[] GenerateNaturalPath(int distance, int steps = 30)
        {
            var overshoot = distance * (random.NextDouble() * 0.1 + 0.1); // 超过目标10%-20%
            var totalDistance = distance + overshoot;

            int forwardSteps = (int)(steps * 0.7);
            int backwardSteps = steps - forwardSteps;

            var forwardPath = GeneratePathSegment(forwardSteps, totalDistance, overshoot);
            var backwardPath = GeneratePathSegment(backwardSteps, totalDistance, overshoot, true);


            var path = forwardPath.Concat(backwardPath).ToArray();

            // 添加随机抖动（合并垂直和横向扰动）
            for (int i = 0; i < path.Length; i++)
            {
                var horizontalJitter = random.Next(-1, 2); // 横向扰动
                var verticalJitter = random.Next(-2, 3);   // 垂直扰动
                path[i] = (path[i].Item1 + horizontalJitter, path[i].Item2 + verticalJitter);
            }

            return path;
        }

        /// <summary>
        /// 生成单段路径
        /// </summary>
        /// <param name="steps">步数</param>
        /// <param name="totalDistance">总距离</param>
        /// <param name="overshoot">超出的距离</param>
        /// <param name="reverse">是否是返回路径</param>
        /// <returns></returns>
        private static (int, int)[] GeneratePathSegment(int steps, double totalDistance, double overshoot, bool reverse = false)
        {
            var path = new (int, int)[steps];
            var timeSteps = Enumerable.Range(0, steps).Select(i => i / (double)steps).ToArray();

            for (int i = 0; i < steps; i++)
            {
                var t = timeSteps[i];
                double x = 0;
                if (reverse)
                {
                    x = overshoot * (1 - Math.Cos(t * Math.PI)) / 2;
                    x = totalDistance - x;
                    if (i == steps - 1)
                    {
                        x = totalDistance - overshoot;
                    }
                }
                else
                {
                    x = totalDistance * (1 - Math.Cos(t * Math.PI)) / 2;
                }
                path[i] = (Convert.ToInt32(x), random.Next(-2, 3)); // 随机垂直偏移
            }

            return path;
        }

        /// <summary>
        /// 使用高精度图像匹配识别滑块距离
        /// </summary>
        /// <param name="bgImageSrc">背景图片地址</param>
        /// <param name="slideImageSrc">滑块图片地址</param>
        /// <returns></returns>
        public static int GetSliderDistance(string bgImageSrc, string slideImageSrc)
        {
            try
            {
                // 创建调试目录
                string debugDir = Path.Combine(Directory.GetCurrentDirectory(), "DebugImages");
                Directory.CreateDirectory(debugDir);

                // 下载背景图和滑块图
                byte[] bgBytes = DownloadImage(bgImageSrc);
                byte[] slideBytes = DownloadImage(slideImageSrc);

                // 将字节数组转换为Mat对象，使用using确保资源释放
                using (Mat bgMat = Mat.FromImageData(bgBytes, ImreadModes.Color))
                using (Mat slideMat = Mat.FromImageData(slideBytes, ImreadModes.Color))
                {
                    // 保存原始图像用于调试
                    Cv2.ImWrite(Path.Combine(debugDir, "bg_original.png"), bgMat);
                    Cv2.ImWrite(Path.Combine(debugDir, "slide_original.png"), slideMat);

                    // 预处理图像以提高匹配精度
                    using (Mat bgProcessed = PreprocessImage(bgMat))
                    using (Mat slideProcessed = PreprocessImage(slideMat))
                    {
                        // 保存预处理后的图像
                        Cv2.ImWrite(Path.Combine(debugDir, "bg_processed.png"), bgProcessed);
                        Cv2.ImWrite(Path.Combine(debugDir, "slide_processed.png"), slideProcessed);

                        // 方法2: 改进的模板匹配（多尺度）
                        var multiScaleResult = PerformMultiScaleMatching(bgProcessed, slideProcessed);

                        return multiScaleResult.location.X;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"滑块识别失败: {ex.Message}");
                throw;
            }
        }

        /// <summary>
        /// 多尺度模板匹配
        /// </summary>
        /// <param name="background"></param>
        /// <param name="template"></param>
        /// <returns></returns>
        private static (Point location, double confidence) PerformMultiScaleMatching(Mat background, Mat template)
        {
            var bestResult = (location: new Point(0, 0), confidence: 0.0);

            // 尝试不同的缩放比例
            double[] scales = { 0.8, 0.9, 1.0, 1.1, 1.2 };

            foreach (var scale in scales)
            {
                if (scale == 1.0)
                {
                    // 原始尺寸匹配
                    var result = PerformTemplateMatching(background, template, TemplateMatchModes.CCoeffNormed);
                    if (result.confidence > bestResult.confidence)
                    {
                        bestResult = result;
                    }
                }
                else
                {
                    // 缩放模板
                    using (Mat scaledTemplate = new Mat())
                    {
                        var newSize = new Size((int)(template.Width * scale), (int)(template.Height * scale));
                        Cv2.Resize(template, scaledTemplate, newSize);

                        var result = PerformTemplateMatching(background, scaledTemplate, TemplateMatchModes.CCoeffNormed);
                        if (result.confidence > bestResult.confidence)
                        {
                            // 调整位置以补偿缩放
                            var adjustedX = (int)(result.location.X / scale);
                            var adjustedY = (int)(result.location.Y / scale);
                            bestResult = (new Point(adjustedX, adjustedY), result.confidence);
                        }
                    }
                }
            }

            return bestResult;
        }

        /// <summary>
        /// 执行模板匹配
        /// </summary>
        /// <param name="background"></param>
        /// <param name="template"></param>
        /// <param name="method"></param>
        /// <returns></returns>
        private static (Point location, double confidence) PerformTemplateMatching(Mat background, Mat template, TemplateMatchModes method)
        {
            using (Mat result = new Mat())
            {
                Cv2.MatchTemplate(background, template, result, method);

                double minVal, maxVal;
                Point minLoc, maxLoc;
                Cv2.MinMaxLoc(result, out minVal, out maxVal, out minLoc, out maxLoc);

                // 根据匹配方法选择合适的位置和置信度
                Point location;
                double confidence;

                if (method == TemplateMatchModes.SqDiff || method == TemplateMatchModes.SqDiffNormed)
                {
                    location = minLoc;
                    confidence = 1.0 - minVal; // 对于平方差，值越小越好
                }
                else
                {
                    location = maxLoc;
                    confidence = maxVal;
                }

                return (location, confidence);
            }
        }

        /// <summary>
        /// 针对滑块验证码优化的图像预处理
        /// </summary>
        /// <param name="image"></param>
        /// <returns></returns>
        private static Mat PreprocessImage(Mat image)
        {
            using (Mat processed = new Mat())
            using (var clahe = Cv2.CreateCLAHE(2.0, new Size(8, 8)))
            using (Mat enhanced = new Mat())
            using (Mat denoised = new Mat())
            using (Mat adaptive = new Mat())
            using (Mat kernel = Cv2.GetStructuringElement(MorphShapes.Rect, new Size(2, 2)))
            {
                // 转换为灰度图
                Cv2.CvtColor(image, processed, ColorConversionCodes.BGR2GRAY);

                // 自适应直方图均衡化（CLAHE）- 比普通直方图均衡化效果更好
                clahe.Apply(processed, enhanced);

                // 双边滤波去噪（保持边缘的同时去噪）
                Cv2.BilateralFilter(enhanced, denoised, 9, 75, 75);

                // 自适应阈值处理（增强缺口边缘）
                Cv2.AdaptiveThreshold(denoised, adaptive, 255, AdaptiveThresholdTypes.GaussianC, ThresholdTypes.Binary, 11, 2);

                // 形态学操作优化边缘
                Mat morphed = new Mat();
                Cv2.MorphologyEx(adaptive, morphed, MorphTypes.Close, kernel);

                return morphed; // 返回的Mat需要调用者负责释放
            }
        }

        /// <summary>
        /// 下载网络图片
        /// </summary>
        /// <param name="src">图片地址</param>
        /// <returns></returns>
        private static byte[] DownloadImage(string src)
        {
            if (src.StartsWith("data:image"))
            {
                // 处理base64编码的图片
                int commaIndex = src.IndexOf(',');
                if (commaIndex >= 0)
                {
                    string base64Data = src.Substring(commaIndex + 1);
                    return Convert.FromBase64String(base64Data);
                }
            }
            else if (src.StartsWith("http"))
            {
                // 下载网络图片
                using (HttpClient client = new HttpClient())
                {
                    return client.GetByteArrayAsync(src).Result;
                }
            }

            throw new ArgumentException("不支持的图片格式");
        }

        /// <summary>
        /// 创建浏览器page对象
        /// </summary>
        /// <returns></returns>
        private static async Task<IPage> createBrowserPage(IBrowserContext context)
        {
            var page = await context.NewPageAsync();

            // 注入更全面的反检测脚本
            await page.AddInitScriptAsync(@"
                // 移除webdriver属性
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined,
                });

                // 伪造chrome对象
                window.chrome = {
                    runtime: {},
                    loadTimes: function() {},
                    csi: function() {},
                    app: {}
                };

                // 伪造插件信息
                Object.defineProperty(navigator, 'plugins', {
                    get: () => [1, 2, 3, 4, 5],
                });

                // 伪造语言信息
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['zh-CN', 'zh', 'en'],
                });

                // 移除自动化检测标识
                delete navigator.__proto__.webdriver;

                // 伪造更多浏览器特征
                Object.defineProperty(navigator, 'hardwareConcurrency', {
                    get: () => 8,
                });

                Object.defineProperty(navigator, 'deviceMemory', {
                    get: () => 8,
                });

                // 伪造屏幕信息
                Object.defineProperty(screen, 'availWidth', {
                    get: () => 1920,
                });
                Object.defineProperty(screen, 'availHeight', {
                    get: () => 1040,
                });
            ");

            // 注入反检测脚本
            await page.AddInitScriptAsync(@"
                // 移除webdriver属性
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined,
                });

                // 伪造chrome对象
                window.chrome = {
                    runtime: {},
                    loadTimes: function() {},
                    csi: function() {},
                    app: {}
                };

                // 伪造插件信息
                Object.defineProperty(navigator, 'plugins', {
                    get: () => [1, 2, 3, 4, 5],
                });

                // 伪造语言信息
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['zh-CN', 'zh', 'en'],
                });

                // 移除自动化检测标识
                delete navigator.__proto__.webdriver;
            ");

            return page;
        }

        private static async Task<IBrowser> createBrowser()
        {
            // 设置playwright浏览器所在位置
            string customBrowserPath = Path.GetFullPath("ms-playwright");
            Environment.SetEnvironmentVariable("PLAYWRIGHT_BROWSERS_PATH", customBrowserPath);

            var browserType = await Playwright.CreateAsync();

            // 启动浏览器时添加反检测参数
            return await browserType.Chromium.LaunchAsync(new BrowserTypeLaunchOptions
            {
                Headless = true, // 设置为false以看到浏览器操作
                //Proxy = new Proxy
                //{
                //    Server = "http://114.80.161.92:55008", // 代理IP:端口
                //    Username = "s60b2e",          // 如果代理需要认证
                //    Password = "l496gb2e"
                //},
                Args = new[]
                {
                    "--no-sandbox",
                    "--disable-blink-features=AutomationControlled", // 禁用自动化控制标识
                    "--disable-dev-shm-usage",
                    "--disable-extensions-except=", // 允许扩展但不加载
                    "--disable-plugins-discovery",
                    "--disable-default-apps",
                    "--no-first-run",
                    "--no-default-browser-check",
                    "--disable-background-timer-throttling",
                    "--disable-backgrounding-occluded-windows",
                    "--disable-renderer-backgrounding",
                    "--disable-features=TranslateUI,BlinkGenPropertyTrees",
                    "--disable-ipc-flooding-protection"
                }
            });
        }

        private static async Task<IBrowserContext> createBrowserContext(IBrowser browser)
        {
            // 使用反检测配置创建上下文
            return await browser.NewContextAsync(new BrowserNewContextOptions
            {
                UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                Locale = "zh-CN",
                TimezoneId = "Asia/Shanghai",
                // 添加真实的浏览器特征
                ExtraHTTPHeaders = new Dictionary<string, string>
                {
                    ["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
                    ["Accept-Language"] = "zh-CN,zh;q=0.9,en;q=0.8",
                    ["Accept-Encoding"] = "gzip, deflate, br",
                    ["DNT"] = "1",
                    ["Connection"] = "keep-alive",
                    ["Upgrade-Insecure-Requests"] = "1",
                    ["Sec-Fetch-Dest"] = "document",
                    ["Sec-Fetch-Mode"] = "navigate",
                    ["Sec-Fetch-Site"] = "none",
                    ["Sec-Fetch-User"] = "?1"
                },
                // 设置权限
                Permissions = new[] { "geolocation", "notifications" },
                // 模拟真实的屏幕分辨率
                ScreenSize = new ScreenSize { Width = 1920, Height = 1080 },
                // 设置设备像素比
                DeviceScaleFactor = 1.0f
            });
        }

    }
}
