using douyin.Utils;
using Newtonsoft.Json.Linq;
using System;
using System.IO;
using System.Management;
using System.Runtime.InteropServices;

namespace ReviewAnalysis.Asr.Local
{
    /// <summary>
    /// 进程级硬件能力探测（一次性计算 + 缓存）。两类用途：
    ///   ① CPU 物理核数 → ONNX IntraOpNumThreads 推荐值（见 <see cref="GetRecommendedIntraOpThreads"/>）。
    ///   ② GPU 专用显存（DXGI DedicatedVideoMemory）→ DirectML 准入判定（见 <see cref="GetMaxDedicatedVideoMemoryMB"/>）。
    ///
    /// 设计原则：
    ///   - 所有原生/WMI 调用全程 try/catch；探测失败返回保守哨兵值（核数回退 ProcessorCount、显存回退 -1），
    ///     绝不让硬件探测异常冒泡打断推理初始化。
    ///   - 结果缓存为静态字段，整进程只探测一次。
    /// </summary>
    public static class HardwareProbe
    {
        // DirectML 准入默认阈值：专用显存 ≥ 2048MB 才认为是「值得用」的独显；
        // 集显（Intel UHD/HD、AMD APU）的 DedicatedVideoMemory 通常为 0~512MB（共享内存不计入），
        // 在其上跑 234M transformer 通常比纯 CPU 更慢，故默认强制 CPU。可经 svs_model_config.json 覆盖。
        private const int DefaultDirectMlMinVramMB = 2048;
        private const int DXGI_ADAPTER_FLAG_SOFTWARE = 2;

        private static readonly object _lock = new object();
        private static int _physicalCores = -1;          // -1 = 未探测
        private static long _maxDedicatedVramMB = -2;     // -2 = 未探测; -1 = 探测失败/无独显
        private static int _maxVramAdapterIndex = -2;     // -2 = 未探测; -1 = 探测失败/无独显; ≥0 = 独显的 DXGI 枚举索引
        private static int _directMlMinVramMB = -1;       // -1 = 未读取

        // 线程上限：再多核也不超过此值，避免高核机上 ORT 线程调度开销 + 防止意外吃满。
        private const int MaxIntraOpThreads = 8;

        /// <summary>
        /// 推荐 ONNX IntraOpNumThreads —— 本机还要并行做直播录制、用户也可能开其他软件，
        /// 按核数分档，绝不把 CPU 吃满卡死：
        ///   - ≤ 4 物理核（低端机）：保守取半核 —— 2核→1, 3核→1, 4核→2。
        ///   - ≥ 5 物理核：激进些，只留 2 核给录制/UI/其他软件 —— 5核→3, 6核→4, 8核→6。
        ///   - 上限 <see cref="MaxIntraOpThreads"/>（超高核机封顶，如 16核→8 仍留 8 核）。
        ///   - 至少 1 线程。
        /// 基于**物理核数**（非 Environment.ProcessorCount 的逻辑核），避免 HT 超订导致缓存争抢。
        /// </summary>
        public static int GetRecommendedIntraOpThreads()
        {
            int cores = GetPhysicalCoreCount();
            int threads = cores <= 4 ? cores / 2 : cores - 2;
            threads = Math.Min(threads, MaxIntraOpThreads);
            return Math.Max(1, threads);
        }

        /// <summary>CPU 物理核数（WMI Win32_Processor.NumberOfCores 求和）；失败回退 Environment.ProcessorCount。</summary>
        public static int GetPhysicalCoreCount()
        {
            if (_physicalCores > 0)
                return _physicalCores;

            lock (_lock)
            {
                if (_physicalCores > 0)
                    return _physicalCores;

                int cores = 0;
                try
                {
                    using (var searcher = new ManagementObjectSearcher(
                        "SELECT NumberOfCores FROM Win32_Processor"))
                    using (var results = searcher.Get())
                    {
                        foreach (ManagementBaseObject item in results)
                        {
                            using (item)
                            {
                                object val = item["NumberOfCores"];
                                if (val != null)
                                    cores += Convert.ToInt32(val);
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"[HardwareProbe] WMI 物理核探测失败，回退 ProcessorCount: {ex.Message}");
                }

                if (cores <= 0)
                    cores = Math.Max(1, Environment.ProcessorCount);

                _physicalCores = cores;
                FileUtils.LogAnalysis($"[HardwareProbe] 物理核={_physicalCores}, 逻辑核={Environment.ProcessorCount}");
                return _physicalCores;
            }
        }

        /// <summary>
        /// 全部硬件适配器中最大的专用显存（MB），用于 DirectML 准入判定。
        /// 跳过软件适配器（WARP / Basic Render Driver）。失败或无独显返回 -1。
        /// </summary>
        public static long GetMaxDedicatedVideoMemoryMB()
        {
            EnsureVramProbed();
            return _maxDedicatedVramMB;
        }

        /// <summary>
        /// 专用显存最大的非软件适配器的 DXGI 枚举索引（= DirectML device_id），用于把 DML 绑定到独显而非核显。
        /// 失败或无独显返回 -1。索引与 <see cref="GetMaxDedicatedVideoMemoryMB"/> 来自同一次枚举，保证对应同一张卡。
        /// </summary>
        public static int GetMaxVramAdapterIndex()
        {
            EnsureVramProbed();
            return _maxVramAdapterIndex;
        }

        /// <summary>一次性枚举 DXGI 适配器，同时填充最大专用显存(MB)与其适配器索引，整进程只探测一次。</summary>
        private static void EnsureVramProbed()
        {
            if (_maxDedicatedVramMB != -2)
                return;

            lock (_lock)
            {
                if (_maxDedicatedVramMB != -2)
                    return;

                ProbeMaxVramAdapter();
            }
        }

        /// <summary>DirectML 准入的最小专用显存阈值（MB）。可经 svs_model_config.json 的 directMLMinDedicatedVramMB 覆盖。</summary>
        public static int GetDirectMlMinVramMB()
        {
            if (_directMlMinVramMB > 0)
                return _directMlMinVramMB;

            lock (_lock)
            {
                if (_directMlMinVramMB > 0)
                    return _directMlMinVramMB;

                int threshold = DefaultDirectMlMinVramMB;
                try
                {
                    string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                    string configPath = Path.Combine(localAppData, "ReviewAnalysis", "Config", "svs_model_config.json");
                    if (File.Exists(configPath))
                    {
                        var cfg = JObject.Parse(File.ReadAllText(configPath));
                        int? configured = cfg["directMLMinDedicatedVramMB"]?.Value<int?>();
                        if (configured.HasValue && configured.Value >= 0)
                            threshold = configured.Value;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"[HardwareProbe] 读取 DirectML 显存阈值配置失败，用默认 {DefaultDirectMlMinVramMB}MB: {ex.Message}");
                }

                _directMlMinVramMB = threshold;
                return _directMlMinVramMB;
            }
        }

        // ── DXGI native interop ──────────────────────────────────────────────

        // 枚举 DXGI 适配器，找出专用显存最大的非软件适配器，记录其显存(MB)与 DXGI 枚举索引。
        // 索引 == DirectML device_id（两者均按 IDXGIFactory 枚举顺序），用于把 DML 精确绑定到独显。
        // 失败或无（非软件）独显时，两字段均置 -1。
        private static void ProbeMaxVramAdapter()
        {
            IntPtr pFactory = IntPtr.Zero;
            try
            {
                Guid iid = typeof(IDXGIFactory1).GUID;
                int hr = CreateDXGIFactory1(ref iid, out pFactory);
                if (hr != 0 || pFactory == IntPtr.Zero)
                {
                    FileUtils.LogAnalysis($"[HardwareProbe] CreateDXGIFactory1 失败 hr=0x{hr:X8}，显存未知");
                    _maxDedicatedVramMB = -1;
                    _maxVramAdapterIndex = -1;
                    return;
                }

                var factory = (IDXGIFactory1)Marshal.GetObjectForIUnknown(pFactory);
                try
                {
                    long maxBytes = -1;
                    int maxIndex = -1;
                    uint i = 0;
                    while (true)
                    {
                        IntPtr pAdapter;
                        int enumHr = factory.EnumAdapters1(i, out pAdapter);
                        if (enumHr != 0 || pAdapter == IntPtr.Zero)
                            break; // DXGI_ERROR_NOT_FOUND (0x887A0002) → 枚举结束

                        var adapter = (IDXGIAdapter1)Marshal.GetObjectForIUnknown(pAdapter);
                        try
                        {
                            DXGI_ADAPTER_DESC1 desc;
                            if (adapter.GetDesc1(out desc) == 0
                                && (desc.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) == 0)
                            {
                                long bytes = (long)desc.DedicatedVideoMemory.ToUInt64();
                                if (bytes > maxBytes)
                                {
                                    maxBytes = bytes;
                                    maxIndex = (int)i; // 该独显在 DXGI 枚举中的索引 → DML device_id
                                }
                            }
                        }
                        finally
                        {
                            Marshal.ReleaseComObject(adapter);
                            Marshal.Release(pAdapter);
                        }
                        i++;
                    }

                    _maxDedicatedVramMB = maxBytes < 0 ? -1 : maxBytes / (1024 * 1024);
                    _maxVramAdapterIndex = maxIndex;
                    FileUtils.LogAnalysis($"[HardwareProbe] 最大专用显存={_maxDedicatedVramMB}MB, 适配器#{maxIndex} (适配器数={i})");
                }
                finally
                {
                    Marshal.ReleaseComObject(factory);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[HardwareProbe] DXGI 显存探测异常，显存未知: {ex.Message}");
                _maxDedicatedVramMB = -1;
                _maxVramAdapterIndex = -1;
            }
            finally
            {
                if (pFactory != IntPtr.Zero)
                    Marshal.Release(pFactory);
            }
        }

        [DllImport("dxgi.dll")]
        private static extern int CreateDXGIFactory1([In] ref Guid riid, out IntPtr ppFactory);

        // COM 互操作：仅声明到 EnumAdapters1 / GetDesc1 为止，前置方法用占位符占满 vtable 槽位
        // （每个声明的方法 = 一个槽位；占位符签名无关紧要，永不调用）。

        [ComImport, InterfaceType(ComInterfaceType.InterfaceIsIUnknown),
         Guid("770aae78-f26f-4dba-a829-253c83d1b387")]
        private interface IDXGIFactory1
        {
            // IDXGIObject
            [PreserveSig] int _SetPrivateData();
            [PreserveSig] int _SetPrivateDataInterface();
            [PreserveSig] int _GetPrivateData();
            [PreserveSig] int _GetParent();
            // IDXGIFactory
            [PreserveSig] int _EnumAdapters();
            [PreserveSig] int _MakeWindowAssociation();
            [PreserveSig] int _GetWindowAssociation();
            [PreserveSig] int _CreateSwapChain();
            [PreserveSig] int _CreateSoftwareAdapter();
            // IDXGIFactory1
            [PreserveSig] int EnumAdapters1(uint Adapter, out IntPtr ppAdapter);
            [PreserveSig] int _IsCurrent();
        }

        [ComImport, InterfaceType(ComInterfaceType.InterfaceIsIUnknown),
         Guid("29038f61-3839-4626-91fd-086879011a05")]
        private interface IDXGIAdapter1
        {
            // IDXGIObject
            [PreserveSig] int _SetPrivateData();
            [PreserveSig] int _SetPrivateDataInterface();
            [PreserveSig] int _GetPrivateData();
            [PreserveSig] int _GetParent();
            // IDXGIAdapter
            [PreserveSig] int _EnumOutputs();
            [PreserveSig] int _GetDesc();
            [PreserveSig] int _CheckInterfaceSupport();
            // IDXGIAdapter1
            [PreserveSig] int GetDesc1(out DXGI_ADAPTER_DESC1 pDesc);
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct DXGI_ADAPTER_DESC1
        {
            [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 128)]
            public string Description;
            public uint VendorId;
            public uint DeviceId;
            public uint SubSysId;
            public uint Revision;
            public UIntPtr DedicatedVideoMemory;
            public UIntPtr DedicatedSystemMemory;
            public UIntPtr SharedSystemMemory;
            public long AdapterLuid;
            public uint Flags;
        }
    }
}
