using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;

namespace ReviewAnalysis.ShortVideo
{
    /// <summary>
    /// 移动端UA随机获取工具类（静态版，无需实例化）
    /// 适配抖音/短视频爬虫场景，直接通过类名调用方法
    /// </summary>
    public static class MobileUaHelper
    {
        // 静态UA列表（内置常见移动端UA，支持动态扩展）
        private static readonly List<string> _mobileUaList = new List<string>
    {
        "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Android 14; Mobile; rv:109.0) Gecko/109.0 Firefox/117.0",
        "Mozilla/5.0 (Linux; Android 13; SM-G9980 Build/TP1A.220624.014; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/115.0.5790.166 Mobile Safari/537.36 ByteDanceWeibo/1338000000",
        "Mozilla/5.0 (Android 12; Xiaomi Redmi K60; Build/SKQ1.221222.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_7_8 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Douyin/25.8.0",
        "Mozilla/5.0 (Linux; Android 11; vivo X90 Pro+ Build/RP1A.200720.011) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/103.0.5060.129 Mobile Safari/537.36",
        "Mozilla/5.0 (iPad; CPU OS 17.0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 WeChat/8.0.41",
        "Mozilla/5.0 (Linux; Android 10; HUAWEI Mate 60 Pro Build/HUAWEIMate60Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36",
    #region Android 通用版（覆盖Android 8.0-14，Chrome内核）
    "Mozilla/5.0 (Linux; Android 8.0.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 9; Pixel 3 Build/PQ3A.190801.002) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; Pixel 4 Build/QQ3A.200805.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; Pixel 5 Build/RQ3A.210805.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SQ3A.220705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; Pixel 7 Build/TQ3A.230705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 7.1.2; Redmi Note 5 Build/N2G47H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36",
    #endregion

    #region Android 华为机型（Mate/P系列，鸿蒙兼容）
    "Mozilla/5.0 (Linux; Android 10; HUAWEI Mate 30 Pro Build/HUAWEIMate30Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; HUAWEI P40 Pro Build/ANP-AN00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; HUAWEI Mate 40 Pro Build/NOH-AN00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; HarmonyOS 2.0; HUAWEI Mate 50 Pro Build/HMOS2.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; HarmonyOS 3.0; HUAWEI P60 Pro Build/HMOS3.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; HUAWEI Nova 11 Build/CHA-AL80) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
    #endregion

    #region Android 小米机型（Redmi/小米数字系列）
    "Mozilla/5.0 (Linux; Android 12; Xiaomi 12 Pro Build/SKQ1.220213.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; Xiaomi 13 Ultra Build/TQ3A.230705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; Redmi K50 Build/RKQ1.200825.002) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; Redmi Note 10 Pro Build/QP1A.190711.020) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 14; Xiaomi 14 Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
    #endregion

    #region Android vivo/OPPO 机型
    "Mozilla/5.0 (Linux; Android 12; vivo X90 Pro Build/TP1A.220829.005) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; vivo X100 Pro Build/TQ3A.230705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; OPPO Find X5 Pro Build/PEPM00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 14; OPPO Find X6 Pro Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 12; realme GT Neo5 Build/SKQ1.220213.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
    #endregion

    #region Android 三星机型
    "Mozilla/5.0 (Linux; Android 12; SM-S22 Ultra Build/SP2A.220305.013) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 13; SM-S23 Ultra Build/TQ3A.230705.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 14; SM-S24 Ultra Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 11; Galaxy S21 Ultra Build/RP1A.200720.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
    #endregion

    #region iOS iPhone 机型（iOS 15-18，不同机型）
    "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 15_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/360.0.743255906 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/363.0.743255906 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone 12; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone 13 Pro; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0.1 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone 14 Pro Max; CPU iPhone OS 18_1_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1.1 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPhone 15 Ultra; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1",
    #endregion

    #region iOS iPad 移动端模式
    "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPad; CPU OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (iPad Pro; CPU OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1",
    #endregion

    #region 微信内置浏览器（Android/iOS）
    "Mozilla/5.0 (Linux; Android 12; SM-G9980 Build/SP2A.220305.013; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.0.0 Mobile Safari/537.36 MicroMessenger/8.0.500.1000 WeChat/arm64",
    "Mozilla/5.0 (Linux; Android 13; Pixel 7 Build/TQ3A.230705.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/134.0.0.0 Mobile Safari/537.36 MicroMessenger/8.0.510.1000 WeChat/arm64",
    //"Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.500(0x18003229) NetType/WIFI MiniProgramEnv/ios",
    //"Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.510(0x18003329) NetType/5G MiniProgramEnv/ios",
    #endregion

    #region QQ浏览器（Android/iOS）
    "Mozilla/5.0 (Linux; Android 12; Redmi K60 Build/SKQ1.220213.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36 MQQBrowser/14.8.0",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1 MQQBrowser/14.8.0",
    #endregion

    #region UC浏览器（Android）
    "Mozilla/5.0 (Linux; Android 11; vivo X80 Build/TP1A.220829.005) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36 UCBrowser/15.5.0.1000",
    "Mozilla/5.0 (Linux; Android 13; HUAWEI Mate 50 Pro Build/HMOS2.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36 UCBrowser/15.6.0.1000",
    #endregion

    #region 低版本兼容（适配老旧移动端）
    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.4936.1160 Mobile Safari/537.36",
    "Mozilla/5.0 (Linux; Android 7.0; SM-G930F Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Mobile Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 14_8 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1",
    #endregion
        
  };

        // 线程安全的随机数生成器（静态类多线程调用时避免重复值）
        private static readonly ThreadLocal<Random> _random = new ThreadLocal<Random>(
            () => new Random(Environment.TickCount ^ Thread.CurrentThread.ManagedThreadId)
        );

        // 线程锁（保证动态添加UA时的线程安全）
        private static readonly object _lockObj = new object();

        /// <summary>
        /// 【核心方法】直接调用，随机获取一个移动端UA值
        /// </summary>
        /// <returns>随机UA字符串</returns>
        /// <exception cref="InvalidOperationException">UA列表为空时抛出</exception>
        public static string GetRandomMobileUa()
        {
            // 空列表校验：避免索引越界
            if (_mobileUaList == null || _mobileUaList.Count == 0)
            {
                throw new InvalidOperationException("移动端UA列表为空，请先添加UA值");
            }

            // 每个线程使用独立的Random实例，保证随机性
            int randomIndex = _random.Value.Next(0, _mobileUaList.Count);
            return _mobileUaList[randomIndex];
        }

        /// <summary>
        /// 动态添加自定义UA到列表（支持多线程调用）
        /// </summary>
        /// <param name="ua">要添加的UA字符串</param>
        /// <exception cref="ArgumentNullException">UA为空时抛出</exception>
        public static void AddMobileUa(string ua)
        {
            if (string.IsNullOrWhiteSpace(ua))
            {
                throw new ArgumentNullException(nameof(ua), "添加的UA不能为空或空白字符");
            }

            // 加锁保证多线程下添加UA不冲突
            lock (_lockObj)
            {
                // 避免重复添加相同UA
                if (!_mobileUaList.Contains(ua))
                {
                    _mobileUaList.Add(ua);
                }
            }
        }

        /// <summary>
        /// 获取当前UA列表的长度（用于调试/校验）
        /// </summary>
        public static int GetUaListCount()
        {
            lock (_lockObj) // 加锁保证计数准确
            {
                return _mobileUaList?.Count ?? 0;
            }
        }

        // 浏览器版本列表
        private static readonly List<string> devIOSReleases = new List<string>
{
    "146.0.7633.1", "145.0.7620.1", "145.0.7582.5", "145.0.7582.2", "145.0.7572.1", "145.0.7561.1", "144.0.7532.1", "144.0.7524.6", "144.0.7510.2", "144.0.7500.1", "143.0.7488.1", "143.0.7473.1", "143.0.7459.1", "143.0.7445.1", "142.0.7432.2", "142.0.7418.4", "142.0.7405.1", "142.0.7391.1", "141.0.7378.2", "141.0.7367.1", "141.0.7354.1", "141.0.7340.1", "140.0.7327.3", "140.0.7313.1", "140.0.7299.1", "140.0.7259.1", "139.0.7247.1", "139.0.7233.1", "139.0.7219.1", "139.0.7205.2", "138.0.7193.1", "138.0.7179.1", "138.0.7168.1", "138.0.7154.1", "137.0.7143.1", "137.0.7129.1", "137.0.7119.1", "137.0.7112.1", "137.0.7106.1", "136.0.7093.1", "136.0.7079.1", "136.0.7066.1", "136.0.7052.1", "135.0.7039.1", "135.0.7025.1", "135.0.7015.1", "135.0.7001.1", "134.0.6988.1", "134.0.6974.1", "134.0.6960.1", "134.0.6946.1", "133.0.6905.3", "133.0.6905.2", "133.0.6891.1", "133.0.6878.1", "133.0.6850.1", "133.0.6836.1", "132.0.6824.1", "132.0.6809.1", "132.0.6794.1", "132.0.6781.1", "131.0.6767.1", "131.0.6754.1", "131.0.6740.1", "131.0.6726.1", "130.0.6713.3", "130.0.6699.1", "130.0.6685.1", "130.0.6671.1", "129.0.6658.1", "129.0.6644.1", "129.0.6630.1", "129.0.6616.1", "128.0.6603.1", "128.0.6561.1", "128.0.6549.1", "128.0.6536.1", "127.0.6523.1", "127.0.6512.1", "127.0.6496.1", "127.0.6490.1", "126.0.6468.1", "126.0.6453.1", "126.0.6439.1", "126.0.6425.1", "125.0.6412.1", "125.0.6398.1", "125.0.6384.1", "125.0.6370.1", "124.0.6357.1", "124.0.6343.1", "124.0.6329.1", "124.0.6315.1", "123.0.6302.1", "123.0.6288.1", "123.0.6274.1", "123.0.6264.1", "122.0.6254.1", "122.0.6239.1", "122.0.6228.1", "122.0.6184.1", "122.0.6168.1", "121.0.6157.1", "121.0.6131.1", "121.0.6116.3", "121.0.6102.1", "120.0.6090.1", "120.0.6076.3", "120.0.6061.2", "120.0.6051.1", "120.0.6048.1", "119.0.6033.1", "119.0.6019.1", "119.0.6007.1", "118.0.5993.4", "118.0.5980.1", "118.0.5967.1", "118.0.5952.1", "117.0.5938.3", "117.0.5927.1", "117.0.5913.1", "117.0.5899.1", "116.0.5845.34", "116.0.5845.13", "116.0.5845.1", "116.0.5832.1", "116.0.5819.1", "116.0.5805.1", "115.0.5790.4", "115.0.5778.1", "115.0.5764.1", "115.0.5750.1", "114.0.5735.5", "114.0.5718.1", "114.0.5707.1", "114.0.5693.1", "114.0.5680.1", "113.0.5665.1", "113.0.5651.1", "113.0.5636.1", "113.0.5622.1", "112.0.5609.1", "112.0.5596.1", "112.0.5581.0", "112.0.5570.0", "111.0.5557.0", "111.0.5544.0", "111.0.5530.0", "111.0.5517.0", "110.0.5475.2", "110.0.5461.0", "110.0.5445.0", "110.0.5420.0", "109.0.5407.0", "109.0.5394.0", "109.0.5380.2", "109.0.5380.0", "109.0.5364.0", "108.0.5353.0", "108.0.5339.0", "108.0.5325.0", "108.0.5313.0", "108.0.5311.0", "107.0.5298.0", "107.0.5284.0", "107.0.5270.0", "107.0.5257.0", "106.0.5243.0", "106.0.5228.0", "106.0.5214.0", "106.0.5202.0", "105.0.5189.0", "105.0.5147.0", "105.0.5133.0", "105.0.5119.0", "104.0.5106.0", "104.0.5094.0", "104.0.5081.0", "104.0.5068.0", "103.0.5055.0", "103.0.5041.0", "103.0.5027.0", "103.0.5013.0", "102.0.4999.0", "102.0.4985.0", "102.0.4971.0", "102.0.4957.0", "101.0.4945.0", "101.0.4932.0", "101.0.4917.0", "101.0.4903.0", "100.0.4889.0", "100.0.4876.0", "100.0.4863.0", "100.0.4851.0", "99.0.4837.0", "99.0.4819.0", "99.0.4806.0", "99.0.4765.0", "98.0.4752.0"

};

        private static readonly List<string> betaIOSReleases = new List<string>
{
    "145.0.7632.3", "144.0.7559.54", "144.0.7559.41", "144.0.7559.31", "144.0.7559.19", "144.0.7559.2", "143.0.7499.39", "143.0.7499.24", "143.0.7499.12", "143.0.7499.2", "142.0.7444.47", "142.0.7444.31", "142.0.7444.22", "142.0.7444.5", "141.0.7390.42", "141.0.7390.28", "141.0.7390.21", "141.0.7390.14", "141.0.7390.4", "140.0.7339.40", "140.0.7339.26", "140.0.7339.14", "140.0.7339.7", "140.0.7339.3", "139.0.7258.61", "139.0.7258.51", "139.0.7258.40", "139.0.7258.30", "139.0.7258.7", "138.0.7204.34", "138.0.7204.24", "138.0.7204.13", "138.0.7204.2", "137.0.7151.35", "137.0.7151.24", "137.0.7151.13", "137.0.7151.4", "136.0.7103.43", "136.0.7103.34", "136.0.7103.24", "136.0.7103.5", "135.0.7049.36", "135.0.7049.26", "135.0.7049.17", "135.0.7049.3", "134.0.6998.34", "134.0.6998.22", "134.0.6998.12", "134.0.6998.2", "133.0.6943.34", "133.0.6943.24", "133.0.6943.14", "133.0.6943.2", "132.0.6834.54", "132.0.6834.44", "132.0.6834.31", "132.0.6834.14", "132.0.6834.4", "131.0.6778.32", "131.0.6778.22", "131.0.6778.12", "131.0.6778.2", "130.0.6723.38", "130.0.6723.29", "130.0.6723.16", "130.0.6723.10", "129.0.6668.47", "129.0.6668.31", "129.0.6668.21", "129.0.6668.11", "128.0.6613.35", "128.0.6613.27", "128.0.6613.16", "128.0.6613.5", "127.0.6533.58", "127.0.6533.40", "127.0.6533.24", "127.0.6533.16", "127.0.6533.3", "126.0.6478.34", "126.0.6478.27", "126.0.6478.17", "126.0.6478.8", "125.0.6422.32", "125.0.6422.21", "125.0.6422.14", "125.0.6422.3", "124.0.6367.38", "124.0.6367.26", "124.0.6367.18", "124.0.6367.5", "123.0.6312.51", "123.0.6312.38", "123.0.6312.30", "123.0.6312.17", "123.0.6312.2", "122.0.6261.50", "122.0.6261.47", "122.0.6261.26", "122.0.6261.20", "122.0.6261.3", "121.0.6167.65", "121.0.6167.56", "121.0.6167.48", "121.0.6167.18", "121.0.6167.5", "120.0.6099.47", "120.0.6099.28", "120.0.6099.16", "120.0.6099.6", "119.0.6045.40", "119.0.6045.30", "119.0.6045.24", "119.0.6045.18", "119.0.6045.5", "118.0.5993.29", "118.0.5993.21", "118.0.5993.13", "117.0.5938.79", "117.0.5938.54", "117.0.5938.36", "117.0.5938.24", "117.0.5938.22", "117.0.5938.12", "116.0.5845.86", "116.0.5845.60", "116.0.5845.52", "116.0.5845.43", "115.0.5790.83", "115.0.5790.71", "115.0.5790.55", "115.0.5790.40", "115.0.5790.32", "115.0.5790.24", "115.0.5790.13", "114.0.5735.49", "114.0.5735.35", "114.0.5735.26", "114.0.5735.16", "113.0.5672.67", "113.0.5672.54", "113.0.5672.33", "113.0.5672.25", "113.0.5672.9", "112.0.5615.40", "112.0.5615.37", "112.0.5615.29", "112.0.5615.20", "112.0.5615.9", "111.0.5563.39", "111.0.5563.28", "111.0.5563.19", "111.0.5563.8", "110.0.5481.52", "110.0.5481.41", "110.0.5481.32", "110.0.5481.22", "109.0.5414.46", "109.0.5414.33", "109.0.5414.25", "109.0.5414.7", "108.0.5359.40", "108.0.5359.30", "108.0.5359.20", "108.0.5359.12", "107.0.5304.37", "107.0.5304.26", "107.0.5304.18", "107.0.5304.7", "106.0.5249.41", "106.0.5249.30", "106.0.5249.21", "106.0.5249.10", "105.0.5195.37", "105.0.5195.28", "105.0.5195.19", "105.0.5195.7", "104.0.5112.54", "104.0.5112.48", "104.0.5112.29", "104.0.5112.22", "104.0.5112.8", "103.0.5060.42", "103.0.5060.34", "103.0.5060.25", "103.0.5060.10", "102.0.5005.50", "102.0.5005.42", "102.0.5005.40", "102.0.5005.23", "102.0.5005.7", "101.0.4951.34", "101.0.4951.26", "101.0.4951.17", "101.0.4951.8", "100.0.4896.46", "100.0.4896.28", "100.0.4896.20", "100.0.4896.10", "99.0.4844.36"

};

        private static readonly List<string> stableIOSReleases = new List<string>
{
    "144.0.7559.85", "144.0.7559.53", "143.0.7499.151", "143.0.7499.108", "143.0.7499.92", "143.0.7499.38", "142.0.7444.148", "142.0.7444.128", "142.0.7444.77", "142.0.7444.46", "141.0.7390.96", "141.0.7390.69", "141.0.7390.41", "141.0.7390.26", "140.0.7339.122", "140.0.7339.101", "140.0.7339.95", "140.0.7339.39", "139.0.7258.76", "139.0.7258.60", "138.0.7204.156", "138.0.7204.119", "138.0.7204.56", "138.0.7204.53", "138.0.7204.33", "137.0.7151.107", "137.0.7151.79", "137.0.7151.51", "137.0.7151.34", "136.0.7103.91", "136.0.7103.56", "136.0.7103.42", "135.0.7049.83", "135.0.7049.53", "135.0.7049.35", "134.0.6998.99", "134.0.6998.33", "133.0.6943.120", "133.0.6943.84", "133.0.6943.33", "132.0.6834.100", "132.0.6834.78", "131.0.6778.154", "131.0.6778.134", "131.0.6778.103", "131.0.6778.73", "131.0.6778.31", "130.0.6723.90", "130.0.6723.78", "130.0.6723.37", "129.0.6668.69", "129.0.6668.46", "128.0.6613.98", "128.0.6613.92", "128.0.6613.34", "127.0.6533.107", "127.0.6533.77", "127.0.6533.56", "126.0.6478.190", "126.0.6478.153", "126.0.6478.108", "126.0.6478.54", "126.0.6478.35", "125.0.6422.145", "125.0.6422.80", "125.0.6422.51", "125.0.6422.33", "124.0.6367.111", "124.0.6367.88", "124.0.6367.71", "124.0.6367.68", "123.0.6312.52", "122.0.6261.89", "122.0.6261.62", "122.0.6261.51", "122.0.6261.48", "121.0.6167.171", "121.0.6167.138", "121.0.6167.66", "120.0.6099.119", "120.0.6099.101", "120.0.6099.50", "119.0.6045.169", "119.0.6045.109", "119.0.6045.41", "118.0.5993.92", "118.0.5993.69", "118.0.5993.58", "117.0.5938.117", "117.0.5938.108", "117.0.5938.104", "117.0.5938.82", "116.0.5845.177", "116.0.5845.146", "116.0.5845.118", "116.0.5845.103", "116.0.5845.90", "115.0.5790.160", "115.0.5790.130", "115.0.5790.84", "114.0.5735.124", "114.0.5735.99", "114.0.5735.50", "113.0.5672.121", "113.0.5672.109", "113.0.5672.69", "112.0.5615.167", "112.0.5615.70", "112.0.5615.69", "112.0.5615.46", "111.0.5563.101", "111.0.5563.72", "111.0.5563.54", "110.0.5481.114", "110.0.5481.83", "109.0.5414.112", "109.0.5414.83", "108.0.5359.112", "108.0.5359.52", "107.0.5304.101", "107.0.5304.66", "106.0.5249.92", "106.0.5249.75", "106.0.5249.70", "106.0.5249.60", "105.0.5195.147", "105.0.5195.129", "105.0.5195.100", "105.0.5195.98", "105.0.5195.69", "104.0.5112.99", "104.0.5112.88", "104.0.5112.71", "103.0.5060.63", "103.0.5060.54", "102.0.5005.87", "102.0.5005.67", "101.0.4951.58", "101.0.4951.44", "100.0.4896.85", "100.0.4896.77", "100.0.4896.56", "99.0.4844.59", "99.0.4844.47", "98.0.4758.97", "98.0.4758.85", "97.0.4692.84", "97.0.4692.72", "96.0.4664.116", "96.0.4664.101", "96.0.4664.94", "96.0.4664.53", "96.0.4664.36", "95.0.4638.50", "94.0.4606.76", "94.0.4606.52", "93.0.4577.78", "93.0.4577.39", "92.0.4515.90", "91.0.4472.80", "90.0.4430.216", "90.0.4430.78", "87.0.4280.163", "87.0.4280.77", "87.0.4280.60", "86.0.4240.93", "86.0.4240.77", "86.0.4240.65", "85.0.4183.109", "85.0.4183.92", "85.0.4183.72", "84.0.4147.122", "84.0.4147.71", "83.0.4103.88", "83.0.4103.63", "81.0.4044.124", "81.0.4044.62", "80.0.3987.95", "80.0.3987.88", "79.0.3945.73", "78.0.3904.84", "78.0.3904.67", "77.0.3865.103", "77.0.3865.93", "77.0.3865.69", "76.0.3809.123", "76.0.3809.81", "75.0.3770.103", "75.0.3770.85", "75.0.3770.70", "74.0.3729.155", "74.0.3729.121", "73.0.3683.68", "72.0.3626.101", "72.0.3626.74", "71.0.3578.89", "71.0.3578.77", "70.0.3538.75", "70.0.3538.60", "69.0.3497.105"

};

        // iOS 版本数据
        private static readonly List<iOSVersionData> ios12 = new List<iOSVersionData>
{
    new iOSVersionData("12.0", "16A5288q", "Developer beta 1", "2018年6月5日"),
    new iOSVersionData("12.0", "16A5308e", "Developer beta 2", "2018年6月20日"),
    new iOSVersionData("12.0", "16A5308e", "Public beta 1", "2018年6月27日"),
    new iOSVersionData("12.0", "16A5318d", "Developer beta 3", "2018年7月4日"),
    new iOSVersionData("12.0", "16A5318d", "Public beta 2", "2018年7月4日"),
    new iOSVersionData("12.0", "16A5327f", "Developer beta 4", "2018年7月18日"),
    new iOSVersionData("12.0", "16A5327f", "Public beta 3", "2018年7月19日"),
    new iOSVersionData("12.0", "16A5339e", "Developer beta 5", "2018年7月31日"),
    new iOSVersionData("12.0", "16A5339e", "Public beta 4", "2018年8月1日"),
    new iOSVersionData("12.0", "16A5345f", "Developer beta 6", "2018年8月7日"),
    new iOSVersionData("12.0", "16A5345f", "Public beta 5", "2018年8月7日"),
    new iOSVersionData("12.0", "16A5354b", "Developer beta 7", "2018年8月14日"),
    new iOSVersionData("12.0", "16A5357b", "Developer beta 8", "2018年8月16日"),
    new iOSVersionData("12.0", "16A5357b", "Public beta 6", "2018年8月16日"),
    new iOSVersionData("12.0", "16A5362a", "Developer beta 9", "2018年8月21日"),
    new iOSVersionData("12.0", "16A5362a", "Public beta 7", "2018年8月21日"),
    new iOSVersionData("12.0", "16A5364a", "Developer beta 10", "2018年8月24日"),
    new iOSVersionData("12.0", "16A5364a", "Public beta 8", "2018年8月24日"),
    new iOSVersionData("12.0", "16A5365b", "Developer beta 11", "2018年8月28日"),
    new iOSVersionData("12.0", "16A5365b", "Public beta 9", "2018年8月28日"),
    new iOSVersionData("12.0", "16A5366a", "Developer beta 12", "2018年9月1日"),
    new iOSVersionData("12.0", "16A5366a", "Public Beta 10", "2018年9月1日"),
    new iOSVersionData("12.0", "16A366", "Golden Master", "2018年9月13日"),
    new iOSVersionData("12.0", "16A366", "正式版", "2018年9月18日"),
    new iOSVersionData("12.0.1", "16A404", "正式版", "2018年10月9日"),
    new iOSVersionData("12.1", "16B5059d", "Developer beta 1", "2018年9月18日"),
    new iOSVersionData("12.1", "16B5059d", "Public Beta 1", "2018年9月20日"),
    new iOSVersionData("12.1", "16B5068i", "Developer beta 2", "2018年10月2日"),
    new iOSVersionData("12.1", "16B5068i", "Public Beta 2", "2018年10月2日"),
    new iOSVersionData("12.1", "16B5077c", "Developer beta 3", "2018年10月10日"),
    new iOSVersionData("12.1", "16B5084a", "Developer beta 4", "2018年10月15日"),
    new iOSVersionData("12.1", "16B5089b", "Developer beta 5", "2018年10月22日"),
    new iOSVersionData("12.1", "16B92", "正式版", "2018年10月30日"),
    new iOSVersionData("12.1", "16B93", "正式版", "2018年10月30日"),
    new iOSVersionData("12.1", "16B94", "正式版", "2018年10月30日"),
    new iOSVersionData("12.1.1", "16C5036c", "Developer beta 1", "2018年11月1日"),
    new iOSVersionData("12.1.1", "16C5043b", "Developer beta 2", "2018年11月8日"),
    new iOSVersionData("12.1.1", "16C5050a", "Developer beta 3", "2018年11月16日"),
    new iOSVersionData("12.1.1", "16C50", "正式版", "2018年12月6日"),
    new iOSVersionData("12.1.2", "16D5024a", "Developer beta 1", "2018年12月11日"),
    new iOSVersionData("12.1.2", "16C101", "正式版", "2018年12月18日"),
    new iOSVersionData("12.1.2", "16C104", "正式版", "2018年12月21日"),
    new iOSVersionData("12.1.3", "16D5032a", "Developer beta 2", "2018年12月20日"),
    new iOSVersionData("12.1.3", "16D5037a", "Developer beta 3", "2019年1月8日"),
    new iOSVersionData("12.1.3", "16D5039a", "Developer beta 4", "2019年1月11日"),
    new iOSVersionData("12.1.3", "16D39", "正式版", "2019年1月23日"),
    new iOSVersionData("12.1.3", "16D40", "正式版", "2019年1月23日"),
    new iOSVersionData("12.2", "16E5181f", "Developer beta 1", "2019年1月25日"),
    new iOSVersionData("12.2", "16E5191d", "Developer beta 2", "2019年2月5日"),
    new iOSVersionData("12.2", "16E5201e", "Developer beta 3", "2019年2月19日"),
    new iOSVersionData("12.2", "16E5212f", "Developer beta 4", "2019年3月5日"),
    new iOSVersionData("12.2", "16E5223a", "Developer beta 5", "2019年3月12日"),
    new iOSVersionData("12.2", "16E5227a", "Developer beta 6", "2019年3月19日"),
    new iOSVersionData("12.1.4", "16D57", "正式版", "2019年2月8日"),
    new iOSVersionData("12.2", "16E227", "正式版", "2019年3月26日"),
    new iOSVersionData("12.3", "16F5117h", "Developer beta 1", "2019年3月28日"),
    new iOSVersionData("12.3", "16F5129d", "Developer beta 2", "2019年4月9日"),
    new iOSVersionData("12.3", "16F5139e", "Developer beta 3", "2019年4月22日"),
    new iOSVersionData("12.3", "16F5148a", "Developer beta 4", "2019年4月29日"),
    new iOSVersionData("12.3", "16F5155a", "Developer beta 5", "2019年5月7日"),
    new iOSVersionData("12.3", "16F5156a", "Developer beta 6", "2019年5月10日"),
    new iOSVersionData("12.3", "16F156", "正式版", "2019年5月13日"),
    new iOSVersionData("12.3.1", "16F203", "正式版", "2019年5月25日"),
    new iOSVersionData("12.4", "16G5027g", "Developer beta 1", "2019年5月15日"),
    new iOSVersionData("12.4", "16G5027i", "Developer beta 2", "2019年5月21日"),
    new iOSVersionData("12.4", "16G5038d", "Developer beta 3", "2019年5月29日"),
    new iOSVersionData("12.4", "16G5046d", "Developer beta 4", "2019年6月11日"),
    new iOSVersionData("12.4", "16G5056d", "Developer beta 5", "2019年6月25日"),
    new iOSVersionData("12.4", "16G5069a", "Developer beta 6", "2019年7月10日"),
    new iOSVersionData("12.4", "16G5077a", "Developer beta 7", "2019年7月17日"),
    new iOSVersionData("12.3.2", "16F250", "正式版", "2019年6月10日"),
    new iOSVersionData("12.4", "16G77", "正式版", "2019年7月23日"),
    new iOSVersionData("12.4.1", "16G102", "正式版", "2019年8月27日"),
    new iOSVersionData("12.4.2", "16G114", "正式版", "2019年9月27日"),
    new iOSVersionData("12.4.3", "16G130", "正式版", "2019年10月28日"),
    new iOSVersionData("12.4.4", "16G140", "正式版", "2019年12月10日"),
    new iOSVersionData("12.4.5", "16G161", "正式版", "2020年1月29日"),
    new iOSVersionData("12.4.6", "16G183", "正式版", "2020年3月25日"),
    new iOSVersionData("12.4.7", "16G192", "正式版", "2020年5月21日"),
    new iOSVersionData("12.4.8", "16G201", "正式版", "2020年7月16日"),
    new iOSVersionData("12.4.9", "16H5", "正式版", "2020年11月6日"),
    new iOSVersionData("12.5", "16H20", "正式版", "2020年12月15日"),
    new iOSVersionData("12.5.1", "16H22", "正式版", "2021年1月12日"),
    new iOSVersionData("12.5.2", "16H30", "正式版", "2021年3月27日"),
    new iOSVersionData("12.5.3", "16H41", "正式版", "2021年5月4日"),
    new iOSVersionData("12.5.4", "16H50", "正式版", "2021年6月15日"),
    new iOSVersionData("12.5.5", "16H62", "正式版", "2021年9月23日"),
    new iOSVersionData("12.5.6", "16H71", "正式版", "2022年8月31日"),
    new iOSVersionData("12.5.7", "16H81", "正式版", "2023年1月23日")
};

        private static readonly List<iOSVersionData> ios13 = new List<iOSVersionData>
{
    new iOSVersionData("13.0", "17A5492t", "Developer beta 1", "2019年6月4日"),
    new iOSVersionData("13.0", "17A5508m", "Developer beta 2", "2019年6月18日"),
    new iOSVersionData("13.0", "17A5508m", "Public beta 1", "2019年6月23日"),
    new iOSVersionData("13.0", "17A5522f", "Developer beta 3", "2019年7月3日"),
    new iOSVersionData("13.0", "17A5522g", "Developer beta 3", "2019年7月9日"),
    new iOSVersionData("13.0", "17A5522g", "Public beta 2", "2019年7月9日"),
    new iOSVersionData("13.0", "17A5534f", "Developer beta 4", "2019年7月18日"),
    new iOSVersionData("13.0", "17A5534f", "Public beta 3", "2019年7月19日"),
    new iOSVersionData("13.0", "17A5547d", "Developer beta 5", "2019年7月30日"),
    new iOSVersionData("13.0", "17A5547d", "Public beta 4", "2019年7月31日"),
    new iOSVersionData("13.0", "17A5556d", "Developer beta 6", "2019年8月8日"),
    new iOSVersionData("13.0", "17A5556d", "Public beta 5", "2019年8月9日"),
    new iOSVersionData("13.0", "17A5565b", "Developer beta 7", "2019年8月16日"),
    new iOSVersionData("13.0", "17A5565b", "Public beta 6", "2019年8月16日"),
    new iOSVersionData("13.0", "17A5572a", "Developer beta 8", "2019年8月22日"),
    new iOSVersionData("13.0", "17A5572a", "Public beta 7", "2019年8月22日"),
    new iOSVersionData("13.0", "17A577", "Golden Master", "2019年9月11日"),
    new iOSVersionData("13.0", "17A577", "正式版", "2019年9月20日"),
    new iOSVersionData("13.1", "17A5821e", "Developer beta 1", "2019年8月28日"),
    new iOSVersionData("13.1", "17A5821e", "Public beta 1", "2019年8月29日"),
    new iOSVersionData("13.1", "17A5831c", "Developer beta 2", "2019年9月5日"),
    new iOSVersionData("13.1", "17A5831c", "Public beta 2", "2019年9月5日"),
    new iOSVersionData("13.1", "17A5837a", "Developer beta 3", "2019年9月11日"),
    new iOSVersionData("13.1", "17A5844a", "Developer beta 4", "2019年9月19日"),
    new iOSVersionData("13.1", "17A844", "正式版", "2019年9月25日"),
    new iOSVersionData("13.1.1", "17A854", "正式版", "2019年9月28日"),
    new iOSVersionData("13.1.2", "17A860", "正式版", "2019年10月1日"),
    new iOSVersionData("13.1.2", "17A861", "正式版", "2019年10月1日"),
    new iOSVersionData("13.2", "17B5059g", "Developer beta 1", "2019年10月3日"),
    new iOSVersionData("13.2", "17B5068e", "Developer beta 2", "2019年10月11日"),
    new iOSVersionData("13.2", "17B5077a", "Developer Beta 3", "2019年10月16日"),
    new iOSVersionData("13.2", "17B5084a", "Developer Beta 4", "2019年10月24日"),
    new iOSVersionData("13.1.3", "17A878", "正式版", "2019年10月16日"),
    new iOSVersionData("13.2", "17B84", "正式版", "2019年10月29日"),
    new iOSVersionData("13.2.1", "17B90", "正式版", "2019年10月31日"),
    new iOSVersionData("13.2.2", "17B102", "正式版", "2019年11月8日"),
    new iOSVersionData("13.2.3", "17B111", "正式版", "2019年11月19日"),
    new iOSVersionData("13.3", "17C5032d", "Developer beta 1", "2019年11月6日"),
    new iOSVersionData("13.3", "17C5038a", "Developer beta 2", "2019年11月13日"),
    new iOSVersionData("13.3", "17C5046a", "Developer Beta 3", "2019年11月21日"),
    new iOSVersionData("13.3", "17C5053a", "Developer Beta 4", "2019年12月6日"),
    new iOSVersionData("13.3", "17C54", "正式版", "2019年12月10日"),
    new iOSVersionData("13.3.1", "17D5026c", "Developer beta 1", "2019年12月18日"),
    new iOSVersionData("13.3.1", "17D5044a", "Developer beta 2", "2020年1月15日"),
    new iOSVersionData("13.3.1", "17D5050a", "Developer beta 3", "2020年1月24日"),
    new iOSVersionData("13.3.1", "17D50", "正式版", "2020年1月29日"),
    new iOSVersionData("13.4", "17E5223h", "Developer beta 1", "2020年2月6日"),
    new iOSVersionData("13.4", "17E5233g", "Developer beta 2", "2020年2月20日"),
    new iOSVersionData("13.4", "17E5241d", "Developer beta 3", "2020年2月27日"),
    new iOSVersionData("13.4", "17E5249a", "Developer beta 4", "2020年3月4日"),
    new iOSVersionData("13.4", "17E5255a", "Developer beta 5", "2020年3月11日"),
    new iOSVersionData("13.4", "17E255", "Developer beta 6/GM", "2020年3月18日"),
    new iOSVersionData("13.4", "17E255", "正式版", "2020年3月25日"),
    new iOSVersionData("13.4.1", "17E262", "正式版", "2020年4月8日"),
    new iOSVersionData("13.4.5", "17F5034c", "Developer beta 1", "2020年4月1日"),
    new iOSVersionData("13.4.5", "17F5044d", "Developer beta 2", "2020年4月16日"),
    new iOSVersionData("13.5", "17F5054h", "Developer beta 3", "2020年4月30日"),
    new iOSVersionData("13.5", "17F5065a", "Developer beta 4", "2020年5月7日"),
    new iOSVersionData("13.5", "17F75", "Developer beta 5/GM", "2020年5月19日"),
    new iOSVersionData("13.5", "17F75", "正式版", "2020年5月26日"),
    new iOSVersionData("13.5.1", "17F80", "正式版", "2020年6月2日"),
    new iOSVersionData("13.5.5", "17G5035d", "Developer beta 1", "2020年6月2日"),
    new iOSVersionData("13.6", "17G5045c", "Developer beta 2", "2020年6月10日"),
    new iOSVersionData("13.6", "17G5059c", "Developer beta 3", "2020年7月1日"),
    new iOSVersionData("13.6", "17G68", "Developer beta 4/GM", "2020年7月10日"),
    new iOSVersionData("13.6", "17G68", "正式版", "2020年7月16日"),
    new iOSVersionData("13.6.1", "17G80", "正式版", "2020年8月13日"),
    new iOSVersionData("13.7", "17H33", "Developer beta", "2020年8月27日"),
    new iOSVersionData("13.7", "17H35", "正式版", "2020年9月2日")
};

        private static readonly List<iOSVersionData> ios14 = new List<iOSVersionData>
{
    new iOSVersionData("14.0", "18A5301v", "Developer beta 1", "2020年6月23日"),
    new iOSVersionData("14.0", "18A5319i", "Developer beta 2", "2020年7月8日"),
    new iOSVersionData("14.0", "18A5319i", "Public beta 2", "2020年7月8日"),
    new iOSVersionData("14.0", "18A5332f", "Developer beta 3", "2020年7月23日"),
    new iOSVersionData("14.0", "18A5332f", "Public beta 3", "2020年7月23日"),
    new iOSVersionData("14.0", "18A5342e", "Developer beta 4", "2020年8月5日"),
    new iOSVersionData("14.0", "18A5342e", "Public beta 4", "2020年8月5日"),
    new iOSVersionData("14.0", "18A5351d", "Developer beta 5", "2020年8月19日"),
    new iOSVersionData("14.0", "18A5351d", "Public beta 5", "2020年8月19日"),
    new iOSVersionData("14.0", "18A5357e", "Developer beta 6", "2020年8月26日"),
    new iOSVersionData("14.0", "18A5357e", "Public beta 6", "2020年8月26日"),
    new iOSVersionData("14.0", "18A5369b", "Developer beta 7", "2020年9月4日"),
    new iOSVersionData("14.0", "18A5369b", "Public beta 7", "2020年9月4日"),
    new iOSVersionData("14.0", "18A5373a", "Developer beta 8", "2020年9月10日"),
    new iOSVersionData("14.0", "18A5373a", "Public beta 8", "2020年9月10日"),
    new iOSVersionData("14.0", "18A373", "Golden Master", "2020年9月16日"),
    new iOSVersionData("14.0", "18A373", "正式版", "2020年9月17日"),
    new iOSVersionData("14.0.1", "18A393", "正式版", "2020年9月24日"),
    new iOSVersionData("14.1", "18A8395", "Golden Master", "2020年10月14日"),
    new iOSVersionData("14.1", "18A8395", "正式版", "2020年10月21日"),
    new iOSVersionData("14.2", "18B5052h", "Developer beta 1", "2020年9月18日"),
    new iOSVersionData("14.2", "18B5052h", "Public beta 1", "2020年9月18日"),
    new iOSVersionData("14.2", "18B5061e", "Developer beta 2", "2020年9月30日"),
    new iOSVersionData("14.2", "18B5061e", "Public beta 2", "2020年9月30日"),
    new iOSVersionData("14.2", "18B5072f", "Developer beta 3", "2020年10月14日"),
    new iOSVersionData("14.2", "18B5072f", "Public beta 3", "2020年10月14日"),
    new iOSVersionData("14.2", "18B5083a", "Developer beta 4", "2020年10月21日"),
    new iOSVersionData("14.2", "18B5083a", "Public beta 4", "2020年10月21日"),
    new iOSVersionData("14.2", "18B91", "Golden Master", "2020年10月31日"),
    new iOSVersionData("14.2", "18B92", "正式版", "2020年11月6日"),
    new iOSVersionData("14.2.1", "18B121", "正式版", "2020年11月20日"),
    new iOSVersionData("14.3", "18C5044f", "Developer beta 1", "2020年11月13日"),
    new iOSVersionData("14.3", "18C5044f", "Public beta 1", "2020年11月13日"),
    new iOSVersionData("14.3", "18C5054c", "Developer beta 2", "2020年11月18日"),
    new iOSVersionData("14.3", "18C5054c", "Public beta 2", "2020年11月18日"),
    new iOSVersionData("14.3", "18C5061a", "Developer beta 3", "2020年12月3日"),
    new iOSVersionData("14.3", "18C5061a", "Public beta 3", "2020年12月3日"),
    new iOSVersionData("14.3", "18C65", "Release Candidate", "2020年12月9日"),
    new iOSVersionData("14.3", "18C66", "Release Candidate 2", "2020年12月11日"),
    new iOSVersionData("14.3", "18C66", "正式版", "2020年12月15日"),
    new iOSVersionData("14.4", "18D5030e", "Developer beta 1", "2020年12月17日"),
    new iOSVersionData("14.4", "18D5030e", "Public beta 1", "2020年12月17日"),
    new iOSVersionData("14.4", "18D5043d", "Developer beta 2", "2021年1月14日"),
    new iOSVersionData("14.4", "18D5043d", "Public beta 2", "2021年1月14日"),
    new iOSVersionData("14.4", "18D52", "Release Candidate", "2021年1月22日"),
    new iOSVersionData("14.4", "18D52", "正式版", "2021年1月27日"),
    new iOSVersionData("14.4.1", "18D61", "正式版", "2021年3月9日"),
    new iOSVersionData("14.4.2", "18D70", "正式版", "2021年3月27日"),
    new iOSVersionData("14.5", "18E5140j", "Developer beta 1", "2021年2月2日"),
    new iOSVersionData("14.5", "18E5140j", "Public beta 1", "2021年2月2日"),
    new iOSVersionData("14.5", "18E5154f", "Developer beta 2", "2021年2月17日"),
    new iOSVersionData("14.5", "18E5154f", "Public beta 2", "2021年2月17日"),
    new iOSVersionData("14.5", "18E5164h", "Developer beta 3", "2021年3月3日"),
    new iOSVersionData("14.5", "18E5164h", "Public beta 3", "2021年3月3日"),
    new iOSVersionData("14.5", "18E5178a", "Developer beta 4", "2021年3月16日"),
    new iOSVersionData("14.5", "18E5178a", "Public beta 4", "2021年3月16日"),
    new iOSVersionData("14.5", "18E5186a", "Developer beta 5", "2021年3月24日"),
    new iOSVersionData("14.5", "18E5186a", "Public beta 5", "2021年3月24日"),
    new iOSVersionData("14.5", "18E5194a", "Developer beta 6", "2021年4月1日"),
    new iOSVersionData("14.5", "18E5194a", "Public beta 6", "2021年4月1日"),
    new iOSVersionData("14.5", "18E5198a", "Developer beta 7", "2021年4月8日"),
    new iOSVersionData("14.5", "18E5198a", "Public beta 7", "2021年4月8日"),
    new iOSVersionData("14.5", "18E5199a", "Developer beta 8", "2021年4月14日"),
    new iOSVersionData("14.5", "18E5199a", "Public beta 8", "2021年4月14日"),
    new iOSVersionData("14.5", "18E199", "Release Candidate", "2021年4月21日"),
    new iOSVersionData("14.5", "18E199", "正式版", "2021年4月27日"),
    new iOSVersionData("14.5.1", "18E212", "正式版", "2021年5月4日"),
    new iOSVersionData("14.6", "18F5046f", "Developer beta 1", "2021年4月23日"),
    new iOSVersionData("14.6", "18F5046f", "Public beta 1", "2021年4月23日"),
    new iOSVersionData("14.6", "18F5055b", "Developer beta 2", "2021年5月1日"),
    new iOSVersionData("14.6", "18F5055b", "Public beta 2", "2021年5月1日"),
    new iOSVersionData("14.6", "18F5065a", "Developer beta 3", "2021年5月11日"),
    new iOSVersionData("14.6", "18F5065a", "Public beta 3", "2021年5月11日"),
    new iOSVersionData("14.6", "18F71", "Release Candidate", "2021年5月18日"),
    new iOSVersionData("14.6", "18F72", "Release Candidate 2", "2021年5月22日"),
    new iOSVersionData("14.6", "18F72", "正式版", "2021年5月25日"),
    new iOSVersionData("14.7", "18G5023c", "Developer beta 1", "2021年5月20日"),
    new iOSVersionData("14.7", "18G5023c", "Public beta 1", "2021年5月20日"),
    new iOSVersionData("14.7", "18G5033e", "Developer beta 2", "2021年6月3日"),
    new iOSVersionData("14.7", "18G5033e", "Public beta 2", "2021年6月3日"),
    new iOSVersionData("14.7", "18G5042c", "Developer beta 3", "2021年6月15日"),
    new iOSVersionData("14.7", "18G5042c", "Public beta 3", "2021年6月15日"),
    new iOSVersionData("14.7", "18G5052d", "Developer beta 4", "2021年6月30日"),
    new iOSVersionData("14.7", "18G5052d", "Public beta 4", "2021年6月30日"),
    new iOSVersionData("14.7", "18G5063a", "Developer beta 5", "2021年7月9日"),
    new iOSVersionData("14.7", "18G5063a", "Public beta 5", "2021年7月9日"),
    new iOSVersionData("14.7", "18G68", "Release Candidate", "2021年7月14日"),
    new iOSVersionData("14.7", "18G69", "正式版", "2021年7月20日"),
    new iOSVersionData("14.7.1", "18G82", "正式版", "2021年7月27日"),
    new iOSVersionData("14.8", "18H17", "正式版", "2021年9月14日"),
    new iOSVersionData("14.8.1", "18H107", "正式版", "2021年10月27日")
};

        private static readonly List<iOSVersionData> ios15 = new List<iOSVersionData>
{
    new iOSVersionData("15.0", "19A5261w", "Developer Beta 1", "2021年6月8日"),
    new iOSVersionData("15.0", "19A5281h", "Developer Beta 2", "2021年6月25日"),
    new iOSVersionData("15.0", "19A5281j", "Re-release Developer Beta 2", "2021年7月1日"),
    new iOSVersionData("15.0", "19A5281j", "Public Beta 2", "2021年7月1日"),
    new iOSVersionData("15.0", "19A5297e", "Developer Beta 3", "2021年7月15日"),
    new iOSVersionData("15.0", "19A5297e", "Public Beta 3", "2021年7月17日"),
    new iOSVersionData("15.0", "19A5307g", "Developer Beta 4", "2021年7月28日"),
    new iOSVersionData("15.0", "19A5307g", "Public Beta 4", "2021年7月29日"),
    new iOSVersionData("15.0", "19A5318f", "Developer Beta 5", "2021年8月11日"),
    new iOSVersionData("15.0", "19A5318f", "Public Beta 5", "2021年8月12日"),
    new iOSVersionData("15.0", "19A5325f", "Developer Beta 6", "2021年8月18日"),
    new iOSVersionData("15.0", "19A5325f", "Public Beta 6", "2021年8月19日"),
    new iOSVersionData("15.0", "19A5337a", "Developer Beta 7", "2021年8月26日"),
    new iOSVersionData("15.0", "19A5337a", "Public Beta 7", "2021年8月27日"),
    new iOSVersionData("15.0", "19A5340a", "Developer Beta 8", "2021年8月31日"),
    new iOSVersionData("15.0", "19A5340a", "Public Beta 8", "2021年9月1日"),
    new iOSVersionData("15.0", "19A344", "Release Candidate", "2021年9月15日"),
    new iOSVersionData("15.0", "19A346", "正式版", "2021年9月21日"),
    new iOSVersionData("15.0.1", "19A348", "正式版", "2021年10月2日"),
    new iOSVersionData("15.0.2", "19A404", "正式版", "2021年10月12日"),
    new iOSVersionData("15.1", "19B5042h", "Developer Beta 1", "2021年9月22日"),
    new iOSVersionData("15.1", "19B5042h", "Public Beta 1", "2021年9月23日"),
    new iOSVersionData("15.1", "19B5052f", "Developer Beta 2", "2021年9月29日"),
    new iOSVersionData("15.1", "19B5052f", "Public Beta 2", "2021年9月30日"),
    new iOSVersionData("15.1", "19B5060d", "Developer Beta 3", "2021年10月7日"),
    new iOSVersionData("15.1", "19B5060d", "Public Beta 3", "2021年10月7日"),
    new iOSVersionData("15.1", "19B5068a", "Developer Beta 4", "2021年10月14日"),
    new iOSVersionData("15.1", "19B5068a", "Public Beta 4", "2021年10月14日"),
    new iOSVersionData("15.1", "19B74", "Release Candidate", "2021年10月19日"),
    new iOSVersionData("15.1", "19B74", "正式版", "2021年10月26日"),
    new iOSVersionData("15.1.1", "19B81", "正式版", "2021年11月18日"),
    new iOSVersionData("15.2", "19C5026i", "Developer Beta 1", "2021年10月28日"),
    new iOSVersionData("15.2", "19C5026i", "Public Beta 1", "2021年10月29日"),
    new iOSVersionData("15.2", "19C5036e", "Developer Beta 2", "2021年11月10日"),
    new iOSVersionData("15.2", "19C5036e", "Public Beta 2", "2021年11月11日"),
    new iOSVersionData("15.2", "19C5044b", "Developer Beta 3", "2021年11月17日"),
    new iOSVersionData("15.2", "19C5044b", "Public Beta 3", "2021年11月18日"),
    new iOSVersionData("15.2", "19C5050b", "Developer Beta 4", "2021年12月3日"),
    new iOSVersionData("15.2", "19C5050b", "Public Beta 4", "2021年12月3日"),
    new iOSVersionData("15.2", "19C56", "Release Candidate", "2021年12月8日"),
    new iOSVersionData("15.2", "19C57", "Release Candidate 2", "2021年12月11日"),
    new iOSVersionData("15.2", "19C56 19C57", "正式版", "2021年12月14日"),
    new iOSVersionData("15.2.1", "19C63", "正式版", "2022年1月13日"),
    new iOSVersionData("15.3", "19D5026g", "Developer Beta 1", "2021年12月18日"),
    new iOSVersionData("15.3", "19D5026g", "Public Beta 1", "2021年12月21日"),
    new iOSVersionData("15.3", "19D5040e", "Developer Beta 2", "2022年1月13日"),
    new iOSVersionData("15.3", "19D5040e", "Public Beta 2", "2022年1月14日"),
    new iOSVersionData("15.3", "19D49", "Release Candidate", "2022年1月21日"),
    new iOSVersionData("15.3", "19D50", "正式版", "2022年1月29日"),
    new iOSVersionData("15.3.1", "19D52", "正式版", "2022年2月11日"),
    new iOSVersionData("15.4", "19E5209h", "Developer Beta 1", "2022年1月28日"),
    new iOSVersionData("15.4", "19E5209h", "Public Beta 1", "2022年1月29日"),
    new iOSVersionData("15.4", "19E5219e", "Developer Beta 2", "2022年2月9日"),
    new iOSVersionData("15.4", "19E5219e", "Public Beta 2", "2022年2月10日"),
    new iOSVersionData("15.4", "19E5225g", "Developer Beta 3", "2022年2月16日"),
    new iOSVersionData("15.4", "19E5225g", "Public Beta 3", "2022年2月17日"),
    new iOSVersionData("15.4", "19E5235a", "Developer Beta 4", "2022年2月23日"),
    new iOSVersionData("15.4", "19E5235a", "Public Beta 4", "2022年2月23日"),
    new iOSVersionData("15.4", "19E5241a", "Developer Beta 5", "2022年3月2日"),
    new iOSVersionData("15.4", "19E5241a", "Public Beta 5", "2022年3月2日"),
    new iOSVersionData("15.4", "19E241", "Release Candidate", "2022年3月9日"),
    new iOSVersionData("15.4", "19E241", "正式版", "2022年3月15日"),
    new iOSVersionData("15.4.1", "19E258", "正式版", "2022年4月1日"),
    new iOSVersionData("15.5", "19F5047e", "Developer Beta 1", "2022年4月6日"),
    new iOSVersionData("15.5", "19F5047e", "Public Beta 1", "2022年4月7日"),
    new iOSVersionData("15.5", "19F5057e", "Developer Beta 2", "2022年4月20日"),
    new iOSVersionData("15.5", "19F5057e", "Public Beta 2", "2022年4月21日"),
    new iOSVersionData("15.5", "19F5062g", "Developer Beta 3", "2022年4月27日"),
    new iOSVersionData("15.5", "19F5062g", "Public Beta 3", "2022年4月27日"),
    new iOSVersionData("15.5", "19F5070b", "Developer Beta 4", "2022年5月4日"),
    new iOSVersionData("15.5", "19F5070b", "Public Beta 4", "2022年5月4日"),
    new iOSVersionData("15.5", "19F77", "Release Candidate", "2022年5月13日"),
    new iOSVersionData("15.5", "19F77", "正式版", "2022年5月17日"),
    new iOSVersionData("15.6", "19G5027e", "Developer Beta 1", "2022年5月19日"),
    new iOSVersionData("15.6", "19G5027e", "Public Beta 1", "2022年5月20日"),
    new iOSVersionData("15.6", "19G5037d", "Developer Beta 2", "2022年6月1日"),
    new iOSVersionData("15.6", "19G5037d", "Public Beta 2", "2022年6月2日"),
    new iOSVersionData("15.6", "19G5046d", "Developer Beta 3", "2022年6月15日"),
    new iOSVersionData("15.6", "19G5046d", "Public Beta 3", "2022年6月16日"),
    new iOSVersionData("15.6", "19G5056c", "Developer Beta 4", "2022年6月29日"),
    new iOSVersionData("15.6", "19G5056c", "Public Beta 4", "2022年6月30日"),
    new iOSVersionData("15.6", "19G5063a", "Developer Beta 5", "2022年7月6日"),
    new iOSVersionData("15.6", "19G5063a", "Public Beta 5", "2022年7月7日"),
    new iOSVersionData("15.6", "19G69", "Release Candidate", "2022年7月13日"),
    new iOSVersionData("15.6", "19G71", "Release Candidate 2", "2022年7月16日"),
    new iOSVersionData("15.6", "19G71", "正式版", "2022年7月21日"),
    new iOSVersionData("15.6.1", "19G82", "正式版", "2022年8月18日"),
    new iOSVersionData("15.7", "19H12", "Release Candidate", "2022年9月8日"),
    new iOSVersionData("15.7", "19H12", "正式版", "2022年9月13日"),
    new iOSVersionData("15.7.1", "19H115", "Release Candidate", "2022年10月19日"),
    new iOSVersionData("15.7.1", "19H117", "正式版", "2022年10月28日"),
    new iOSVersionData("15.7.2", "19H218", "Release Candidate", "2022年12月8日"),
    new iOSVersionData("15.7.2", "19H218", "正式版", "2022年12月14日"),
    new iOSVersionData("15.7.3", "19H307", "Release Candidate", "2023年1月19日"),
    new iOSVersionData("15.7.3", "19H307", "正式版", "2023年1月24日"),
    new iOSVersionData("15.7.4", "19H321", "Release Candidate", "2023年3月22日"),
    new iOSVersionData("15.7.4", "19H321", "正式版", "2023年3月28日"),
    new iOSVersionData("15.7.5", "19H332", "正式版", "2023年4月11日"),
    new iOSVersionData("15.7.6", "19H349", "Release Candidate", "2023年5月10日"),
    new iOSVersionData("15.7.6", "19H349", "正式版", "2023年5月20日"),
    new iOSVersionData("15.7.7", "19H357", "正式版", "2023年6月22日"),
    new iOSVersionData("15.7.8", "19H364", "Release Candidate", "2023年7月19日"),
    new iOSVersionData("15.7.8", "19H364", "正式版", "2023年7月25日"),
    new iOSVersionData("15.7.9", "19H365", "正式版", "2023年9月12日"),
    new iOSVersionData("15.8", "19H370", "Release Candidate", "2023年10月19日"),
    new iOSVersionData("15.8", "19H370", "正式版", "2023年10月26日"),
    new iOSVersionData("15.8.1", "19H380", "Release Candidate", "2024年1月18日"),
    new iOSVersionData("15.8.1", "19H380", "正式版", "2024年1月24日"),
    new iOSVersionData("15.8.2", "19H384", "Release Candidate", "2024年2月28日"),
    new iOSVersionData("15.8.2", "19H384", "正式版", "2024年3月9日"),
    new iOSVersionData("15.8.3", "19H386", "Release Candidate", "2024年7月24日"),
    new iOSVersionData("15.8.3", "19H386", "正式版", "2024年7月30日"),
    new iOSVersionData("15.8.4", "19H390", "正式版", "2025年4月1日"),
    new iOSVersionData("15.8.5", "19H394", "正式版", "2025年9月10日")
};

        private static readonly List<iOSVersionData> ios16 = new List<iOSVersionData>
{
    new iOSVersionData("16.0", "20A5283p", "Developer Beta 1", "2022年6月7日"),
    new iOSVersionData("16.0", "20A5303i", "Developer Beta 2", "2022年6月23日"),
    new iOSVersionData("16.0", "20A5312g", "Developer Beta 3", "2022年7月7日"),
    new iOSVersionData("16.0", "20A5312j", "Re-release Developer Beta 3", "2022年7月12日"),
    new iOSVersionData("16.0", "20A5312j", "Public Beta 1", "2022年7月12日"),
    new iOSVersionData("16.0", "20A5328h", "Developer Beta 4", "2022年7月28日"),
    new iOSVersionData("16.0", "20A5328h", "Public Beta 2", "2022年7月29日"),
    new iOSVersionData("16.0", "20A5339d", "Developer Beta 5", "2022年8月9日"),
    new iOSVersionData("16.0", "20A5339d", "Public Beta 3", "2022年8月10日"),
    new iOSVersionData("16.0", "20A5349b", "Developer Beta 6", "2022年8月16日"),
    new iOSVersionData("16.0", "20A5349b", "Public Beta 4", "2022年8月16日"),
    new iOSVersionData("16.0", "20A5356a", "Developer Beta 7", "2022年8月24日"),
    new iOSVersionData("16.0", "20A5356a", "Public Beta 5", "2022年8月25日"),
    new iOSVersionData("16.0", "20A5358a", "Developer Beta 8", "2022年8月30日"),
    new iOSVersionData("16.0", "20A5358a", "Public Beta 6", "2022年8月30日"),
    new iOSVersionData("16.0", "20A362", "Release Candidate", "2022年9月8日"),
    new iOSVersionData("16.0", "20A362", "正式版", "2022年9月13日"),
    new iOSVersionData("16.0.1", "20A371", "正式版", "2022年9月15日"),
    new iOSVersionData("16.0.2", "20A380", "正式版", "2022年9月23日"),
    new iOSVersionData("16.0.3", "20A392", "正式版", "2022年10月11日"),
    new iOSVersionData("16.1", "20B5045d", "Developer Beta 1", "2022年9月15日"),
    new iOSVersionData("16.1", "20B5045d", "Public Beta 1", "2022年9月16日"),
    new iOSVersionData("16.1", "20B5050f", "Developer Beta 2", "2022年9月21日"),
    new iOSVersionData("16.1", "20B5050f", "Public Beta 2", "2022年9月22日"),
    new iOSVersionData("16.1", "20B5056e", "Developer Beta 3", "2022年9月28日"),
    new iOSVersionData("16.1", "20B5056e", "Public Beta 3", "2022年9月29日"),
    new iOSVersionData("16.1", "20B5064c", "Developer Beta 4", "2022年10月5日"),
    new iOSVersionData("16.1", "20B5064c", "Public Beta 4", "2022年10月6日"),
    new iOSVersionData("16.1", "20B5072b", "Developer Beta 5", "2022年10月12日"),
    new iOSVersionData("16.1", "20B5072b", "Public Beta 5", "2022年10月12日"),
    new iOSVersionData("16.1", "20B79", "Release Candidate", "2022年10月19日"),
    new iOSVersionData("16.1", "20B82", "正式版", "2022年10月25日"),
    new iOSVersionData("16.1.1", "20B101", "正式版", "2022年11月10日"),
    new iOSVersionData("16.1.2", "20B110", "正式版", "2022年12月1日"),
    new iOSVersionData("16.2", "20C5032e", "Developer Beta 1", "2022年10月26日"),
    new iOSVersionData("16.2", "20C5032e", "Public Beta 1", "2022年10月28日"),
    new iOSVersionData("16.2", "20C5043e", "Developer Beta 2", "2022年11月9日"),
    new iOSVersionData("16.2", "20C5043e", "Public Beta 2", "2022年11月10日"),
    new iOSVersionData("16.2", "20C5049e", "Developer Beta 3", "2022年11月16日"),
    new iOSVersionData("16.2", "20C5049e", "Public Beta 3", "2022年11月17日"),
    new iOSVersionData("16.2", "20C5058d", "Developer Beta 4", "2022年12月2日"),
    new iOSVersionData("16.2", "20C5058d", "Public Beta 4", "2022年12月2日"),
    new iOSVersionData("16.2", "20C65", "Release Candidate", "2022年12月8日"),
    new iOSVersionData("16.2", "20C65", "正式版", "2022年12月14日"),
    new iOSVersionData("16.3", "20D5024e", "Developer Beta 1", "2022年12月15日"),
    new iOSVersionData("16.3", "20D5024e", "Public Beta 1", "2022年12月16日"),
    new iOSVersionData("16.3", "20D5035i", "Developer Beta 2", "2023年1月11日"),
    new iOSVersionData("16.3", "20D5035i", "Public Beta 2", "2023年1月12日"),
    new iOSVersionData("16.3", "20D47", "Release Candidate", "2023年1月19日"),
    new iOSVersionData("16.3", "20D47", "正式版", "2023年1月24日"),
    new iOSVersionData("16.3.1", "20D67", "正式版", "2023年2月14日"),
    new iOSVersionData("16.4", "20E5212f", "Developer Beta 1", "2023年2月17日"),
    new iOSVersionData("16.4", "20E5212f", "Public Beta 1", "2023年2月18日"),
    new iOSVersionData("16.4", "20E5223e", "Developer Beta 2", "2023年3月1日"),
    new iOSVersionData("16.4", "20E5223e", "Public Beta 2", "2023年3月2日"),
    new iOSVersionData("16.4", "20E5229e", "Developer Beta 3", "2023年3月8日"),
    new iOSVersionData("16.4", "20E5229e", "Public Beta 3", "2023年3月9日"),
    new iOSVersionData("16.4", "20E5239b", "Developer Beta 4", "2023年3月16日"),
    new iOSVersionData("16.4", "20E5239b", "Public Beta 4", "2023年3月16日"),
    new iOSVersionData("16.4", "20E246", "Release Candidate", "2023年3月22日"),
    new iOSVersionData("16.4", "20E247", "正式版", "2023年3月28日"),
    new iOSVersionData("16.4.1", "20E252", "正式版", "2023年4月8日"),
    new iOSVersionData("16.4.1 (a)", "20E772520a", "正式版", "2023年5月2日"),
    new iOSVersionData("16.5", "20F5028e", "Developer Beta 1", "2023年3月29日"),
    new iOSVersionData("16.5", "20F5028e", "Public Beta 1", "2023年3月31日"),
    new iOSVersionData("16.5", "20F5039e", "Developer Beta 2", "2023年4月11日"),
    new iOSVersionData("16.5", "20F5039e", "Public Beta 2", "2023年4月12日"),
    new iOSVersionData("16.5", "20F5050f", "Developer Beta 3", "2023年4月26日"),
    new iOSVersionData("16.5", "20F5050f", "Public Beta 3", "2023年4月27日"),
    new iOSVersionData("16.5", "20F5059a", "Developer Beta 4", "2023年5月3日"),
    new iOSVersionData("16.5", "20F5059a", "Public Beta 4", "2023年5月3日"),
    new iOSVersionData("16.5", "20F65", "Release Candidate", "2023年5月10日"),
    new iOSVersionData("16.5", "20F66", "Release Candidate 2", "2023年5月16日"),
    new iOSVersionData("16.5", "20F66", "正式版", "2023年5月19日"),
    new iOSVersionData("16.5.1", "20F75", "正式版", "2023年6月22日"),
    new iOSVersionData("16.5.1 (a)", "20F770750b", "正式版", "2023年7月11日"),
    new iOSVersionData("16.5.1 (c)", "20F770750d", "正式版", "2023年7月13日"),
    new iOSVersionData("16.6", "20G5026e", "Developer Beta 1", "2023年5月20日"),
    new iOSVersionData("16.6", "20G5026e", "Public Beta 1", "2023年5月23日"),
    new iOSVersionData("16.6", "20G5037d", "Developer Beta 2", "2023年6月1日"),
    new iOSVersionData("16.6", "20G5037d", "Public Beta 2", "2023年6月2日"),
    new iOSVersionData("16.6", "20G5047d", "Developer Beta 3", "2023年6月16日"),
    new iOSVersionData("16.6", "20G5047d", "Public Beta 3", "2023年6月17日"),
    new iOSVersionData("16.6", "20G5058d", "Developer Beta 4", "2023年6月28日"),
    new iOSVersionData("16.6", "20G5058d", "Public Beta 4", "2023年6月29日"),
    new iOSVersionData("16.6", "20G5070a", "Developer Beta 5", "2023年7月11日"),
    new iOSVersionData("16.6", "20G5070a", "Public Beta 5", "2023年7月11日"),
    new iOSVersionData("16.6", "20G75", "Release Candidate", "2023年7月19日"),
    new iOSVersionData("16.6", "20G75", "正式版", "2023年7月25日"),
    new iOSVersionData("16.6.1", "20G81", "正式版", "2023年9月8日"),
    new iOSVersionData("16.7", "20H18", "Release Candidate", "2023年9月13日"),
    new iOSVersionData("16.7", "20H19", "正式版", "2023年9月22日"),
    new iOSVersionData("16.7.1", "20H30", "正式版", "2023年10月11日"),
    new iOSVersionData("16.7.2", "20H115", "Release Candidate", "2023年10月18日"),
    new iOSVersionData("16.7.2", "20H115", "正式版", "2023年10月26日"),
    new iOSVersionData("16.7.3", "20H232", "Release Candidate", "2023年12月6日"),
    new iOSVersionData("16.7.3", "20H232", "正式版", "2023年12月12日"),
    new iOSVersionData("16.7.4", "20H240", "正式版", "2023年12月20日"),
    new iOSVersionData("16.7.5", "20H307", "Release Candidate", "2024年1月18日"),
    new iOSVersionData("16.7.5", "20H307", "正式版", "2024年1月23日"),
    new iOSVersionData("16.7.6", "20H320", "Release Candidate", "2024年2月28日"),
    new iOSVersionData("16.7.6", "20H320", "正式版", "2024年3月6日"),
    new iOSVersionData("16.7.7", "20H330", "正式版", "2024年3月22日"),
    new iOSVersionData("16.7.8", "20H343", "Release Candidate", "2024年5月8日"),
    new iOSVersionData("16.7.8", "20H343", "正式版", "2024年5月14日"),
    new iOSVersionData("16.7.9", "20H348", "Release Candidate", "2024年7月24日"),
    new iOSVersionData("16.7.9", "20H348", "正式版", "2024年7月30日"),
    new iOSVersionData("16.7.10", "20H350", "正式版", "2024年8月8日"),
    new iOSVersionData("16.7.11", "20H360", "正式版", "2025年4月1日"),
    new iOSVersionData("16.7.12", "20H364", "正式版", "2025年9月10日")
};

        private static readonly List<iOSVersionData> ios17 = new List<iOSVersionData>
{
    new iOSVersionData("17.0", "21A5248v", "Developer Beta 1", "2023年6月6日"),
    new iOSVersionData("17.0", "21A5268h", "Developer Beta 2", "2023年6月22日"),
    new iOSVersionData("17.0", "21A5277h", "Developer Beta 3", "2023年7月6日"),
    new iOSVersionData("17.0", "21A5277j", "Re-release Developer Beta 3", "2023年7月12日"),
    new iOSVersionData("17.0", "21A5277j", "Public Beta 1", "2023年7月13日"),
    new iOSVersionData("17.0", "21A5291h", "Developer Beta 4", "2023年7月26日"),
    new iOSVersionData("17.0", "21A5291j", "Re-release Developer Beta 4", "2023年8月1日"),
    new iOSVersionData("17.0", "21A5291j", "Public Beta 2", "2023年8月1日"),
    new iOSVersionData("17.0", "21A5303d", "Developer Beta 5", "2023年8月9日"),
    new iOSVersionData("17.0", "21A5303d", "Public Beta 3", "2023年8月10日"),
    new iOSVersionData("17.0", "21A5312c", "Developer Beta 6", "2023年8月16日"),
    new iOSVersionData("17.0", "21A5312c", "Public Beta 4", "2023年8月16日"),
    new iOSVersionData("17.0", "21A5319a", "Developer Beta 7", "2023年8月23日"),
    new iOSVersionData("17.0", "21A5319a", "Public Beta 5", "2023年8月23日"),
    new iOSVersionData("17.0", "21A5326a", "Developer Beta 8", "2023年8月30日"),
    new iOSVersionData("17.0", "21A5326a", "Public Beta 6", "2023年8月30日"),
    new iOSVersionData("17.0", "21A329", "Release Candidate", "2023年9月13日"),
    new iOSVersionData("17.0", "21A329", "正式版", "2023年9月19日"),
    new iOSVersionData("17.0.1", "21A340", "正式版", "2023年9月22日"),
    new iOSVersionData("17.0.2", "21A350", "正式版", "2023年9月22日"),
    new iOSVersionData("17.0.2", "21A351", "正式版", "2023年9月27日"),
    new iOSVersionData("17.0.3", "21A360", "正式版", "2023年10月5日"),
    new iOSVersionData("17.1", "21B5045h", "Developer Beta 1", "2023年9月28日"),
    new iOSVersionData("17.1", "21B5045h", "Public Beta 1", "2023年9月29日"),
    new iOSVersionData("17.1", "21B5056e", "Developer Beta 2", "2023年10月4日"),
    new iOSVersionData("17.1", "21B5056e", "Public Beta 2", "2023年10月5日"),
    new iOSVersionData("17.1", "21B5066a", "Developer Beta 3", "2023年10月11日"),
    new iOSVersionData("17.1", "21B5066a", "Public Beta 3", "2023年10月11日"),
    new iOSVersionData("17.1", "21B74", "Release Candidate", "2023年10月18日"),
    new iOSVersionData("17.1", "21B77", "Release Candidate 2", "2023年10月21日"),
    new iOSVersionData("17.1", "21B74", "正式版", "2023年10月26日"),
    new iOSVersionData("17.1", "21B80", "正式版", "2023年10月26日"),
    new iOSVersionData("17.1.1", "21B91", "正式版", "2023年11月8日"),
    new iOSVersionData("17.1.2", "21B101", "正式版", "2023年12月1日"),
    new iOSVersionData("17.2", "21C5029g", "Developer Beta 1", "2023年10月27日"),
    new iOSVersionData("17.2", "21C5029g", "Public Beta 1", "2023年10月28日"),
    new iOSVersionData("17.2", "21C5040g", "Developer Beta 2", "2023年11月10日"),
    new iOSVersionData("17.2", "21C5040g", "Public Beta 2", "2023年11月11日"),
    new iOSVersionData("17.2", "21C5046c", "Developer Beta 3", "2023年11月15日"),
    new iOSVersionData("17.2", "21C5046c", "Public Beta 3", "2023年11月16日"),
    new iOSVersionData("17.2", "21C5054b", "Developer Beta 4", "2023年11月29日"),
    new iOSVersionData("17.2", "21C5054b", "Public Beta 4", "2023年11月29日"),
    new iOSVersionData("17.2", "21C62", "Release Candidate", "2023年12月6日"),
    new iOSVersionData("17.2", "21C62", "正式版", "2023年12月12日"),
    new iOSVersionData("17.2.1", "21C66", "正式版", "2023年12月20日"),
    new iOSVersionData("17.3", "21D5026f", "Developer Beta 1", "2023年12月13日"),
    new iOSVersionData("17.3", "21D5026f", "Public Beta 1", "2023年12月15日"),
    new iOSVersionData("17.3", "21D5036c", "Developer Beta 2", "2024年1月4日"),
    new iOSVersionData("17.3", "21D5044a", "Developer Beta 3", "2024年1月10日"),
    new iOSVersionData("17.3", "21D5044a", "Public Beta 3", "2024年1月11日"),
    new iOSVersionData("17.3", "21D50", "Release Candidate", "2024年1月18日"),
    new iOSVersionData("17.3", "21D50", "正式版", "2024年1月23日"),
    new iOSVersionData("17.3.1", "21D61", "正式版", "2024年2月9日"),
    new iOSVersionData("17.4", "21E5184i", "Developer Beta 1", "2024年1月26日"),
    new iOSVersionData("17.4", "21E5184k", "Re-release Developer Beta 1", "2024年1月31日"),
    new iOSVersionData("17.4", "21E5184k", "Public Beta 1", "2024年1月31日"),
    new iOSVersionData("17.4", "21E5195e", "Developer Beta 2", "2024年2月7日"),
    new iOSVersionData("17.4", "21E5195e", "Public Beta 2", "2024年2月8日"),
    new iOSVersionData("17.4", "21E5200d", "Developer Beta 3", "2024年2月14日"),
    new iOSVersionData("17.4", "21E5200d", "Public Beta 3", "2024年2月15日"),
    new iOSVersionData("17.4", "21E5209b", "Developer Beta 4", "2024年2月21日"),
    new iOSVersionData("17.4", "21E5209b", "Public Beta 4", "2024年2月21日"),
    new iOSVersionData("17.4", "21E217", "Release Candidate", "2024年2月28日"),
    new iOSVersionData("17.4", "21E219", "正式版", "2024年3月6日"),
    new iOSVersionData("17.4.1", "21E236", "正式版", "2024年3月22日"),
    new iOSVersionData("17.4.1", "21E237", "正式版", "2024年3月27日"),
    new iOSVersionData("17.5", "21F5048f", "Developer Beta 1", "2024年4月3日"),
    new iOSVersionData("17.5", "21F5048f", "Public Beta 1", "2024年4月5日"),
    new iOSVersionData("17.5", "21F5058e", "Developer Beta 2", "2024年4月17日"),
    new iOSVersionData("17.5", "21F5058e", "Public Beta 2", "2024年4月18日"),
    new iOSVersionData("17.5", "21F5063f", "Developer Beta 3", "2024年4月24日"),
    new iOSVersionData("17.5", "21F5063f", "Public Beta 3", "2024年4月25日"),
    new iOSVersionData("17.5", "21F5073b", "Developer Beta 4", "2024年5月1日"),
    new iOSVersionData("17.5", "21F5073b", "Public Beta 4", "2024年5月1日"),
    new iOSVersionData("17.5", "21F79", "Release Candidate", "2024年5月8日"),
    new iOSVersionData("17.5", "21F79", "正式版", "2024年5月14日"),
    new iOSVersionData("17.5.1", "21F90", "正式版", "2024年5月21日"),
    new iOSVersionData("17.6", "21G5052e", "Developer Beta 1", "2024年6月18日"),
    new iOSVersionData("17.6", "21G5052e", "Public Beta 1", "2024年6月21日"),
    new iOSVersionData("17.6", "21G5061c", "Developer Beta 2", "2024年7月2日"),
    new iOSVersionData("17.6", "21G5061c", "Public Beta 2", "2024年7月3日"),
    new iOSVersionData("17.6", "21G5066d", "Developer Beta 3", "2024年7月10日"),
    new iOSVersionData("17.6", "21G5066d", "Public Beta 3", "2024年7月11日"),
    new iOSVersionData("17.6", "21G5075a", "Developer Beta 4", "2024年7月17日"),
    new iOSVersionData("17.6", "21G5075a", "Public Beta 4", "2024年7月17日"),
    new iOSVersionData("17.6", "21G79", "Release Candidate", "2024年7月24日"),
    new iOSVersionData("17.6", "21G80", "正式版", "2024年7月30日"),
    new iOSVersionData("17.6.1", "21G93", "正式版", "2024年8月8日"),
    new iOSVersionData("17.6.1", "21G101", "正式版", "2024年8月20日"),
    new iOSVersionData("17.7", "21H16", "Release Candidate", "2024年9月10日"),
    new iOSVersionData("17.7", "21H16", "正式版", "2024年9月17日"),
    new iOSVersionData("17.7.1", "21H216", "Release Candidate", "2024年10月22日"),
    new iOSVersionData("17.7.1", "21H216", "正式版", "2024年10月29日"),
    new iOSVersionData("17.7.2", "21H221", "正式版", "2024年11月20日")
};

        private static readonly List<iOSVersionData> ios18 = new List<iOSVersionData>
{
    new iOSVersionData("18.0", "22A5282m", "Developer Beta 1", "2024年6月11日"),
    new iOSVersionData("18.0", "22A5297f", "Developer Beta 2", "2024年6月25日"),
    new iOSVersionData("18.0", "22A5307f", "Developer Beta 3", "2024年7月9日"),
    new iOSVersionData("18.0", "22A5307i", "Re-release Developer Beta 3", "2024年7月16日"),
    new iOSVersionData("18.0", "22A5307i", "Public Beta 1", "2024年7月16日"),
    new iOSVersionData("18.0", "22A5316j", "Developer Beta 4", "2024年7月24日"),
    new iOSVersionData("18.0", "22A5316k", "Re-release Developer Beta 4", "2024年7月27日"),
    new iOSVersionData("18.0", "22A5316k", "Public Beta 2", "2024年7月30日"),
    new iOSVersionData("18.0", "22A5326f", "Developer Beta 5", "2024年8月6日"),
    new iOSVersionData("18.0", "22A5326f", "Public Beta 3", "2024年8月7日"),
    new iOSVersionData("18.0", "22A5338b", "Developer Beta 6", "2024年8月13日"),
    new iOSVersionData("18.0", "22A5338b", "Public Beta 4", "2024年8月13日"),
    new iOSVersionData("18.0", "22A5346a", "Developer Beta 7", "2024年8月21日"),
    new iOSVersionData("18.0", "22A5346a", "Public Beta 5", "2024年8月21日"),
    new iOSVersionData("18.0", "22A5350a", "Developer Beta 8", "2024年8月29日"),
    new iOSVersionData("18.0", "22A5350a", "Public Beta 6", "2024年8月29日"),
    new iOSVersionData("18.0", "22A3354", "Release Candidate", "2024年9月10日"),
    new iOSVersionData("18.0", "22A3354", "正式版", "2024年9月17日"),
    new iOSVersionData("18.0.1", "22A3370", "正式版", "2024年10月4日"),
    new iOSVersionData("18.1", "22B5007p", "Developer Beta 1", "2024年7月30日"),
    new iOSVersionData("18.1", "22B5023e", "Developer Beta 2", "2024年8月13日"),
    new iOSVersionData("18.1", "22B5034e", "Developer Beta 3", "2024年8月29日"),
    new iOSVersionData("18.1", "22B5034o", "Developer Beta 3", "2024年9月12日"),
    new iOSVersionData("18.1", "22B5045g", "Developer Beta 4", "2024年9月18日"),
    new iOSVersionData("18.1", "22B5045h", "Developer Beta 4", "2024年9月18日"),
    new iOSVersionData("18.1", "22B5045g", "Public Beta 1", "2024年9月20日"),
    new iOSVersionData("18.1", "22B5045h", "Public Beta 1", "2024年9月20日"),
    new iOSVersionData("18.1", "22B5054e", "Developer Beta 5", "2024年9月24日"),
    new iOSVersionData("18.1", "22B5054e", "Public Beta 2", "2024年9月25日"),
    new iOSVersionData("18.1", "22B5069a", "Developer Beta 6", "2024年10月8日"),
    new iOSVersionData("18.1", "22B5069a", "Public Beta 3", "2024年10月8日"),
    new iOSVersionData("18.1", "22B5075a", "Developer Beta 7", "2024年10月15日"),
    new iOSVersionData("18.1", "22B5075a", "Public Beta 4", "2024年10月15日"),
    new iOSVersionData("18.1", "22B82", "Release Candidate", "2024年10月22日"),
    new iOSVersionData("18.1", "22B83", "正式版", "2024年10月29日"),
    new iOSVersionData("18.1.1", "22B91", "正式版", "2024年11月20日"),
    new iOSVersionData("18.2", "22C5109p", "Developer Beta 1", "2024年10月24日"),
    new iOSVersionData("18.2", "22C5125e", "Developer Beta 2", "2024年11月5日"),
    new iOSVersionData("18.2", "22C5125e", "Public Beta 1", "2024年11月7日"),
    new iOSVersionData("18.2", "22C5131e", "Developer Beta 3", "2024年11月12日"),
    new iOSVersionData("18.2", "22C5131e", "Public Beta 2", "2024年11月13日"),
    new iOSVersionData("18.2", "22C5142a", "Developer Beta 4", "2024年11月21日"),
    new iOSVersionData("18.2", "22C5142a", "Public Beta 3", "2024年11月21日"),
    new iOSVersionData("18.2", "22C150", "Release Candidate", "2024年12月6日"),
    new iOSVersionData("18.2", "22C151", "Release Candidate 2", "2024年12月10日"),
    new iOSVersionData("18.2", "22C152", "正式版", "2024年12月12日"),
    new iOSVersionData("18.2.1", "22C161", "正式版", "2025年1月7日"),
    new iOSVersionData("18.3", "22D5034e", "Developer Beta 1", "2024年12月17日"),
    new iOSVersionData("18.3", "22D5034e", "Public Beta 1", "2024年12月19日"),
    new iOSVersionData("18.3", "22D5040d", "Developer Beta 2", "2025年1月8日"),
    new iOSVersionData("18.3", "22D5040d", "Public Beta 2", "2025年1月9日"),
    new iOSVersionData("18.3", "22D5055b", "Developer Beta 3", "2025年1月17日"),
    new iOSVersionData("18.3", "22D5055b", "Public Beta 3", "2025年1月17日"),
    new iOSVersionData("18.3", "22D60", "Release Candidate", "2025年1月22日"),
    new iOSVersionData("18.3", "22D63", "正式版", "2025年1月28日"),
    new iOSVersionData("18.3", "22D64", "正式版", "2025年2月4日"),
    new iOSVersionData("18.3.1", "22D72", "正式版", "2025年2月11日"),
    new iOSVersionData("18.3.2", "22D82", "正式版", "2025年3月12日"),
    new iOSVersionData("18.4", "22E5200s", "Developer Beta 1", "2025年2月22日"),
    new iOSVersionData("18.4", "22E5200s", "Public Beta 1", "2025年2月25日"),
    new iOSVersionData("18.4", "22E5216h", "Developer Beta 2", "2025年3月4日"),
    new iOSVersionData("18.4", "22E5216h", "Public Beta 2", "2025年3月5日"),
    new iOSVersionData("18.4", "22E5222f", "Developer Beta 3", "2025年3月11日"),
    new iOSVersionData("18.4", "22E5222f", "Public Beta 3", "2025年3月12日"),
    new iOSVersionData("18.4", "22E5232a", "Developer Beta 4", "2025年3月18日"),
    new iOSVersionData("18.4", "22E5232a", "Public Beta 4", "2025年3月18日"),
    new iOSVersionData("18.4", "22E239", "Release Candidate", "2025年3月25日"),
    new iOSVersionData("18.4", "22E240", "Release Candidate 2", "2025年3月29日"),
    new iOSVersionData("18.4", "22E240", "正式版", "2025年4月1日"),
    new iOSVersionData("18.4.1", "22E252", "正式版", "2025年4月17日"),
    new iOSVersionData("18.5", "22F5042g", "Developer Beta 1", "2025年4月3日"),
    new iOSVersionData("18.5", "22F5053f", "Developer Beta 2", "2025年4月15日"),
    new iOSVersionData("18.5", "22F5053f", "Public Beta 1", "2025年4月16日"),
    new iOSVersionData("18.5", "22F5053j", "Developer Beta 3", "2025年4月22日"),
    new iOSVersionData("18.5", "22F5053j", "Public Beta 2", "2025年4月23日"),
    new iOSVersionData("18.5", "22F5068a", "Developer Beta 4", "2025年4月29日"),
    new iOSVersionData("18.5", "22F5068a", "Public Beta 3", "2025年4月29日"),
    new iOSVersionData("18.5", "22F75", "Release Candidate", "2025年5月7日"),
    new iOSVersionData("18.5", "22F76", "正式版", "2025年5月13日"),
    new iOSVersionData("18.6", "22G5054d", "Developer Beta 1", "2025年6月17日"),
    new iOSVersionData("18.6", "22G5054d", "Public Beta 1", "2025年6月19日"),
    new iOSVersionData("18.6", "22G5064d", "Developer Beta 2", "2025年7月1日"),
    new iOSVersionData("18.6", "22G5064d", "Public Beta 2", "2025年7月2日"),
    new iOSVersionData("18.6", "22G5073b", "Developer Beta 3", "2025年7月15日"),
    new iOSVersionData("18.6", "22G5073b", "Public Beta 3", "2025年7月16日"),
    new iOSVersionData("18.6", "22G84", "Release Candidate", "2025年7月22日"),
    new iOSVersionData("18.6", "22G86", "正式版", "2025年7月30日"),
    new iOSVersionData("18.6.1", "22G90", "正式版", "2025年8月15日"),
    new iOSVersionData("18.6.2", "22G100", "正式版", "2025年8月21日"),
    new iOSVersionData("18.7", "22H20", "Release Candidate", "2025年9月10日"),
    new iOSVersionData("18.7", "22H20", "正式版", "2025年9月16日"),
    new iOSVersionData("18.7.1", "22H31", "正式版", "2025年9月30日"),
    new iOSVersionData("18.7.2", "22H123", "Release Candidate", "2025年10月29日"),
    new iOSVersionData("18.7.2", "22H124", "正式版", "2025年11月6日"),
    new iOSVersionData("18.7.3", "22H217", "Release Candidate", "2025年12月4日"),
    new iOSVersionData("18.7.3", "22H217", "正式版", "2025年12月13日")
};

        /// <summary>
        /// 构建 iOS 手机 User-Agent
        /// </summary>
        /// <param name="versionName">可以指定版本 12 13 14 15 16 17 18，传入 null 表示所有版本</param>
        /// <param name="releasesName">谷歌浏览器版本选择 Stable（稳定版） Beta（测试版） Dev（开发版)</param>
        /// <param name="isOfficial">是否选择 "正式版"</param>
        /// <returns>iOS User-Agent 字符串</returns>
        public static string GetRandomIOSUA(int? versionName = null, string releasesName = null, bool isOfficial = false)
        {
            var random = new Random();

            // 根据版本名称获取对应的数据源
            List<iOSVersionData> targetData;

            switch (versionName)
            {
                case 12:
                    targetData = ios12;
                    break;
                case 13:
                    targetData = ios13;
                    break;
                case 14:
                    targetData = ios14;
                    break;
                case 15:
                    targetData = ios15;
                    break;
                case 16:
                    targetData = ios16;
                    break;
                case 17:
                    targetData = ios17;
                    break;
                case 18:
                    targetData = ios18;
                    break;
                default:
                    // 默认使用所有版本
                    targetData = new List<iOSVersionData>();
                    targetData.AddRange(ios12);
                    targetData.AddRange(ios13);
                    targetData.AddRange(ios14);
                    targetData.AddRange(ios15);
                    targetData.AddRange(ios16);
                    targetData.AddRange(ios17);
                    targetData.AddRange(ios18);
                    break;
            }

            // 使用 HashSet 去重
            HashSet<(string Version, string BuildNumber)> iosSet = new HashSet<(string, string)>();

            // 遍历数据源，筛选符合条件的版本
            foreach (var itemData in targetData)
            {
                // 仅当非官方 或 官方且是正式版时，才添加
                if (!isOfficial || (isOfficial && itemData.VersionDetail == "正式版"))
                {
                    iosSet.Add((itemData.Version, itemData.BuildNumber));
                }
            }

            // 转换为列表并随机选择
            var iosVersionList = iosSet.ToList();
            if (iosVersionList.Count == 0)
            {
                // 如果没有符合条件的版本，使用默认选择
                var fallbackData = targetData.FirstOrDefault();
                if (fallbackData == null)
                    fallbackData = ios18.First();

                iosVersionList.Add((fallbackData.Version, fallbackData.BuildNumber));
            }

            var selectedIOS = iosVersionList[random.Next(iosVersionList.Count)];
            FileUtils.log($"选择的 iOS 版本：{selectedIOS.Version}, Build: {selectedIOS.BuildNumber}");

            // 处理 iOS 版本号
            string iosVersionCode = selectedIOS.Version.Replace('.', '_');
            string iosVersionBuild = selectedIOS.BuildNumber;

            // 选择浏览器版本
            string iosReleases;
            switch (releasesName)
            {
                case "Stable":
                    iosReleases = stableIOSReleases[random.Next(stableIOSReleases.Count)];
                    break;
                case "Beta":
                    iosReleases = betaIOSReleases[random.Next(betaIOSReleases.Count)];
                    break;
                case "Dev":
                    iosReleases = devIOSReleases[random.Next(devIOSReleases.Count)];
                    break;
                default:
                    var allReleases = stableIOSReleases.Concat(betaIOSReleases).Concat(devIOSReleases).ToList();
                    iosReleases = allReleases[random.Next(allReleases.Count)];
                    break;
            }

            FileUtils.log($"选择的浏览器版本：{iosReleases}");

            // 构建 User-Agent 字符串
            string ua = $"Mozilla/5.0 (iPhone; CPU iPhone OS {iosVersionCode} like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/{iosReleases} Mobile/{iosVersionBuild} Safari/604.1";
            return ua;
        }


    }

    public class iOSVersionData
    {
        public string Version { get; set; }
        public string BuildNumber { get; set; }
        public string VersionDetail { get; set; }
        public string PublishTime { get; set; }

        public iOSVersionData(string version, string buildNumber, string versionDetail, string publishTime)
        {
            Version = version;
            BuildNumber = buildNumber;
            VersionDetail = versionDetail;
            PublishTime = publishTime;
        }
    }

}
