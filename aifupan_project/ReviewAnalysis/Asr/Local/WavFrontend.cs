using System;
using System.Diagnostics;
using System.IO;
using System.Text;

namespace ReviewAnalysis.Asr.Local
{
    /// <summary>
    /// Pure C# FBank feature extraction + LFR splicing for SenseVoiceSmall.
    /// Parameters: 16kHz, 25ms window, 10ms shift, 80 mel bins, Hamming window,
    /// LFR m=7 (stack 7 frames), n=6 (stride 6 frames) → 560-dim output.
    ///
    /// 性能优化（2026-05-21）：
    ///   - FFT 改为 float real/imag 双数组（去 System.Numerics.Complex 分配）+ 预计算 twiddle factors
    ///   - 每帧 FFT/power 缓冲提升为实例字段，避免每帧堆分配
    ///   - Mel filterbank 改用 sparse 表示（start/len/weights），跳过零值 bin
    /// 实例在 SenseVoiceSmallEngine 中单例使用，所有调用串行 await，缓冲复用安全。
    /// </summary>
    public class WavFrontend
    {
        private const int SampleRate = 16000;
        private const int FrameLength = 400;  // 25ms @ 16kHz
        private const int FrameShift = 160;   // 10ms @ 16kHz
        private const int FftSize = 512;
        private const int FftHalf = FftSize / 2 + 1;  // 257
        private const int NumMelBins = 80;
        private const double FreqLow = 20.0;
        private const double FreqHigh = 8000.0;
        private const int LfrM = 7;
        private const int LfrN = 6;

        private readonly float[] _hammingWindow;

        // Sparse mel filterbank: filter m 非零区间 = [_melStart[m], _melStart[m] + _melLen[m])
        // 对应权重保存在 _melWeights[m] 中
        private readonly int[] _melStart;
        private readonly int[] _melLen;
        private readonly float[][] _melWeights;

        // FFT 预计算 twiddle factors（cos/sin 表），按蝶形 level 分组
        private readonly float[] _twiddleCos;
        private readonly float[] _twiddleSin;

        // 帧级复用缓冲（实例字段，避免每帧 new）
        // 注意：这些缓冲在跨视频并发分析场景下会有竞争 — 因为 SenseVoiceSmallEngine 是
        // 单例（AsrEngineFactory._pool["svs"]），多个 AsrByDirectoryPath 调用共享同一
        // WavFrontend 实例。GetFbank 用 _fbankLock 串行化访问保护。
        private readonly float[] _fftReal = new float[FftSize];
        private readonly float[] _fftImag = new float[FftSize];
        private readonly float[] _powerSpec = new float[FftHalf];
        private readonly object _fbankLock = new object();

        public WavFrontend()
        {
            _hammingWindow = CreateHammingWindow(FrameLength);
            BuildSparseMelFilterbank(out _melStart, out _melLen, out _melWeights);
            BuildTwiddleTables(FftSize, out _twiddleCos, out _twiddleSin);
        }

        /// <summary>
        /// Decode audio file to 16kHz mono PCM float samples.
        /// WAV files are read directly; other formats are converted via FFmpeg first.
        /// </summary>
        public static float[] DecodeMp3(string mp3Path)
        {
            if (mp3Path.EndsWith(".wav", StringComparison.OrdinalIgnoreCase))
                return ReadWavFile(mp3Path);

            string tempWav = Path.Combine(Path.GetTempPath(), $"svs_{Guid.NewGuid():N}.wav");
            try
            {
                ConvertToWavWithFFmpeg(mp3Path, tempWav);
                return ReadWavFile(tempWav);
            }
            finally
            {
                try { if (File.Exists(tempWav)) File.Delete(tempWav); } catch { }
            }
        }

        private static void ConvertToWavWithFFmpeg(string inputPath, string outputPath)
        {
            string arguments = $"-i \"{inputPath}\" -ar 16000 -ac 1 -f wav -y \"{outputPath}\"";
            var startInfo = new ProcessStartInfo("tools\\ffmpeg", arguments)
            {
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true,
                StandardOutputEncoding = Encoding.UTF8,
                StandardErrorEncoding = Encoding.UTF8
            };
            using (var process = new Process { StartInfo = startInfo })
            {
                process.OutputDataReceived += (s, e) => { };
                process.ErrorDataReceived += (s, e) => { };
                process.Start();
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();
                bool exited = process.WaitForExit(60000);
                if (!exited)
                {
                    try { process.Kill(); } catch { }
                    throw new TimeoutException("FFmpeg转换WAV超时");
                }
                if (process.ExitCode != 0)
                    throw new Exception($"FFmpeg转换WAV失败，退出码: {process.ExitCode}");
            }
        }

        private static float[] ReadWavFile(string wavPath)
        {
            using (var fs = new FileStream(wavPath, FileMode.Open, FileAccess.Read))
            using (var br = new BinaryReader(fs))
            {
                br.ReadBytes(12); // RIFF + size + WAVE
                while (fs.Position < fs.Length - 8)
                {
                    string chunkId = new string(br.ReadChars(4));
                    int chunkSize = br.ReadInt32();
                    if (chunkId == "data")
                    {
                        int sampleCount = chunkSize / 2; // 16-bit PCM = 2 bytes/sample
                        float[] samples = new float[sampleCount];
                        for (int i = 0; i < sampleCount; i++)
                            samples[i] = br.ReadInt16() / 32768.0f;
                        return samples;
                    }
                    fs.Seek(chunkSize + (chunkSize % 2), SeekOrigin.Current);
                }
                throw new InvalidDataException("WAV文件缺少data块");
            }
        }

        /// <summary>
        /// Extract FBank features (80-dim) from PCM samples.
        /// Returns float[nFrames * 80] in row-major layout.
        ///
        /// 线程安全：用 _fbankLock 串行化 — 实例缓冲 _fftReal/_fftImag/_powerSpec 在多个
        /// 并发 ASR 调用下会数据竞争（SVS 引擎是 AsrEngineFactory 单例）。锁开销 ~µs，
        /// 相对 GetFbank 整体几百 ms 可忽略；单视频内 AsrUtils foreach-await 串行无竞争。
        /// </summary>
        public float[] GetFbank(float[] samples)
        {
            int numFrames = (samples.Length - FrameLength) / FrameShift + 1;
            if (numFrames <= 0)
                throw new ArgumentException("Audio too short for feature extraction");

            float[] fbanks = new float[numFrames * NumMelBins];

            lock (_fbankLock)
            {
                for (int f = 0; f < numFrames; f++)
                {
                    int offset = f * FrameShift;

                    // 准备 FFT 输入：复用 _fftReal / _fftImag 缓冲，windowed signal 写入实部，虚部清零
                    for (int i = 0; i < FrameLength; i++)
                    {
                        _fftReal[i] = samples[offset + i] * _hammingWindow[i];
                        _fftImag[i] = 0f;
                    }
                    for (int i = FrameLength; i < FftSize; i++)
                    {
                        _fftReal[i] = 0f;
                        _fftImag[i] = 0f;
                    }

                    Fft(_fftReal, _fftImag, _twiddleCos, _twiddleSin);

                    // 功率谱 = |X[k]|² = real² + imag²
                    for (int i = 0; i < FftHalf; i++)
                    {
                        float re = _fftReal[i];
                        float im = _fftImag[i];
                        _powerSpec[i] = re * re + im * im;
                    }

                    // Mel 滤波：稀疏路径，每个 filter 只跑非零区间
                    int fbankOffset = f * NumMelBins;
                    for (int m = 0; m < NumMelBins; m++)
                    {
                        int start = _melStart[m];
                        int len = _melLen[m];
                        float[] weights = _melWeights[m];
                        float sum = 0f;
                        for (int k = 0; k < len; k++)
                            sum += _powerSpec[start + k] * weights[k];
                        float logVal = (float)Math.Log(sum + 1e-10);
                        fbanks[fbankOffset + m] = logVal > -20f ? logVal : -20f;
                    }
                }
            }

            return fbanks;
        }

        /// <summary>
        /// Apply LFR (Low Frame Rate) splicing.
        /// Input: fbanks [nFrames * 80], output: [nLfrFrames * 560] (7*80).
        /// For edge frames, replicates the first/last frame.
        /// </summary>
        public float[] ApplyLfr(float[] fbanks)
        {
            int nFrames = fbanks.Length / NumMelBins;
            int tileSize = (LfrM - 1) / 2; // = 3
            int paddedFrames = nFrames + 2 * tileSize; // pad both sides
            int nLfrFrames = (paddedFrames - LfrM) / LfrN + 1;

            // Pad beginning: replicate first frame tileSize times
            float[] padded = new float[paddedFrames * NumMelBins];
            for (int i = 0; i < tileSize; i++)
                Array.Copy(fbanks, 0, padded, i * NumMelBins, NumMelBins);
            // Copy original data
            Array.Copy(fbanks, 0, padded, tileSize * NumMelBins, fbanks.Length);
            // Pad end: replicate last frame tileSize times
            int lastFrameOffset = (nFrames - 1) * NumMelBins;
            for (int i = 0; i < tileSize; i++)
                Array.Copy(fbanks, lastFrameOffset, padded, (tileSize + nFrames + i) * NumMelBins, NumMelBins);

            int lfrDim = LfrM * NumMelBins; // 560
            float[] lfrOutput = new float[nLfrFrames * lfrDim];

            for (int i = 0; i < nLfrFrames; i++)
            {
                int srcStart = i * LfrN * NumMelBins;
                Array.Copy(padded, srcStart, lfrOutput, i * lfrDim, lfrDim);
            }

            return lfrOutput;
        }

        public float[] ProcessFile(string mp3Path)
        {
            float[] pcm = DecodeMp3(mp3Path);
            float[] fbank = GetFbank(pcm);
            return ApplyLfr(fbank);
        }

        #region FFT

        /// <summary>
        /// 预计算 twiddle factors：对每个 level (half = 1, 2, 4, ..., n/2)，存 [cos(2πk/(2·half)), sin(...)]。
        /// 长度 = n - 1（所有 level 的 half 总和 = n/2 + n/4 + ... + 1 = n-1）。
        /// </summary>
        private static void BuildTwiddleTables(int n, out float[] cosTable, out float[] sinTable)
        {
            int totalSize = n - 1;
            cosTable = new float[totalSize];
            sinTable = new float[totalSize];
            int idx = 0;
            for (int len = 2; len <= n; len <<= 1)
            {
                int half = len >> 1;
                double baseAngle = -2.0 * Math.PI / len;
                for (int k = 0; k < half; k++)
                {
                    double angle = baseAngle * k;
                    cosTable[idx] = (float)Math.Cos(angle);
                    sinTable[idx] = (float)Math.Sin(angle);
                    idx++;
                }
            }
        }

        /// <summary>
        /// In-place iterative Cooley-Tukey FFT，用 float real/imag 双数组替代 Complex 结构，
        /// 配合预计算的 twiddle factors 避免每蝶形 sin/cos 调用与 Complex 实例化。
        /// </summary>
        private static void Fft(float[] real, float[] imag, float[] cosTable, float[] sinTable)
        {
            int n = real.Length;

            // bit-reversal permutation
            int j = 0;
            for (int i = 0; i < n; i++)
            {
                if (i < j)
                {
                    float tr = real[i]; real[i] = real[j]; real[j] = tr;
                    float ti = imag[i]; imag[i] = imag[j]; imag[j] = ti;
                }
                int m = n >> 1;
                while (m > 0 && (j & m) != 0)
                {
                    j ^= m;
                    m >>= 1;
                }
                j ^= m;
            }

            // Cooley-Tukey butterflies
            int twiddleIdx = 0;
            for (int len = 2; len <= n; len <<= 1)
            {
                int half = len >> 1;
                for (int i = 0; i < n; i += len)
                {
                    for (int k = 0; k < half; k++)
                    {
                        float wRe = cosTable[twiddleIdx + k];
                        float wIm = sinTable[twiddleIdx + k];
                        int idxA = i + k;
                        int idxB = idxA + half;

                        float vRe = real[idxB] * wRe - imag[idxB] * wIm;
                        float vIm = real[idxB] * wIm + imag[idxB] * wRe;

                        real[idxB] = real[idxA] - vRe;
                        imag[idxB] = imag[idxA] - vIm;
                        real[idxA] = real[idxA] + vRe;
                        imag[idxA] = imag[idxA] + vIm;
                    }
                }
                twiddleIdx += half;
            }
        }

        #endregion

        #region Windowing

        private static float[] CreateHammingWindow(int length)
        {
            float[] window = new float[length];
            for (int i = 0; i < length; i++)
                window[i] = (float)(0.54 - 0.46 * Math.Cos(2.0 * Math.PI * i / (length - 1)));
            return window;
        }

        #endregion

        #region Mel Filterbank

        private static double HzToMel(double hz)
        {
            return 1127.0 * Math.Log(1.0 + hz / 700.0);
        }

        private static double MelToHz(double mel)
        {
            return 700.0 * (Math.Exp(mel / 1127.0) - 1.0);
        }

        /// <summary>
        /// 构造稀疏 Mel filterbank：每个三角滤波器只在 [start, end) 区间有非零权重。
        /// 与稠密 float[][] 等价的输出，但内存与计算量减少约 95%（每个 filter 通常 5-15 个非零 bin）。
        /// </summary>
        private static void BuildSparseMelFilterbank(out int[] starts, out int[] lens, out float[][] weights)
        {
            double melLow = HzToMel(FreqLow);
            double melHigh = HzToMel(FreqHigh);
            int numBins = FftSize / 2 + 1; // 257

            // equally spaced points in Mel scale
            double[] melPoints = new double[NumMelBins + 2];
            for (int i = 0; i < NumMelBins + 2; i++)
                melPoints[i] = melLow + (melHigh - melLow) * i / (NumMelBins + 1);

            // convert to Hz → FFT bin indices
            int[] binIndices = new int[NumMelBins + 2];
            for (int i = 0; i < NumMelBins + 2; i++)
                binIndices[i] = (int)Math.Floor((FftSize + 1) * MelToHz(melPoints[i]) / SampleRate);

            starts = new int[NumMelBins];
            lens = new int[NumMelBins];
            weights = new float[NumMelBins][];

            for (int m = 0; m < NumMelBins; m++)
            {
                int leftBin = binIndices[m];
                int centerBin = binIndices[m + 1];
                int rightBin = binIndices[m + 2];
                int effectiveEnd = Math.Min(rightBin, numBins);

                int len = Math.Max(0, effectiveEnd - leftBin);
                starts[m] = leftBin;
                lens[m] = len;
                weights[m] = new float[len];

                // 左斜：从 leftBin 上升到 centerBin（权重 0 → 1）
                int leftBoundary = Math.Min(centerBin, effectiveEnd);
                for (int k = leftBin; k < leftBoundary; k++)
                    weights[m][k - leftBin] = (float)(k - leftBin) / (centerBin - leftBin);

                // 右斜：从 centerBin 下降到 rightBin（权重 1 → 0）
                for (int k = centerBin; k < effectiveEnd; k++)
                    weights[m][k - leftBin] = (float)(rightBin - k) / (rightBin - centerBin);
            }
        }

        #endregion
    }
}
