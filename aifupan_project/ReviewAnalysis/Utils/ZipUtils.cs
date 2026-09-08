using System;
using System.IO.Compression;
using System.IO;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public class ZipUtils
    {
        /// <summary>
        /// 将压缩包里面的analysis.txt读取成字符串返回
        /// </summary>
        /// <param name="zipPath">zip压缩包路径</param>
        /// <returns></returns>
        public static string ReadFileFromZip(string zipPath)
        {
            using (ZipArchive archive = ZipFile.OpenRead(zipPath))
            {
                ZipArchiveEntry entry = archive.GetEntry("analysis.txt");

                using (Stream stream = entry.Open())
                using (StreamReader reader = new StreamReader(stream))
                {
                    return reader.ReadToEnd();
                }
            }
        }
        /// <summary>
        /// 异步从ZIP压缩包中读取指定文件内容（analysis.txt）
        /// </summary>
        /// <param name="zipPath">ZIP文件完整路径</param>
        /// <returns>文件内容字符串</returns>
        /// <exception cref="FileNotFoundException">ZIP文件不存在时抛出</exception>
        /// <exception cref="InvalidDataException">ZIP包中无analysis.txt文件时抛出</exception>
        /// <exception cref="IOException">文件读取失败时抛出</exception>
        public static async Task<string> ReadFileFromZipAsync(string zipPath)
        {
            // 1. 校验ZIP文件路径
            if (!File.Exists(zipPath))
            {
                throw new FileNotFoundException("ZIP文件不存在", zipPath);
            }

            // 2. 打开ZIP包（ZipFile.OpenRead无异步版本，属IO密集型但无异步替代，保留同步打开）
            using (ZipArchive archive = ZipFile.OpenRead(zipPath))
            {
                // 3. 获取目标文件条目
                ZipArchiveEntry entry = archive.GetEntry("analysis.txt");
                if (entry == null)
                {
                    throw new InvalidDataException($"ZIP包 [{zipPath}] 中未找到 analysis.txt 文件");
                }

                // 4. 异步打开流 + 异步读取内容（核心异步改造点）
                using (Stream stream = entry.Open())
                using (StreamReader reader = new StreamReader(stream))
                {
                    // 替换同步ReadToEnd为异步ReadToEndAsync，避免阻塞线程
                    return await reader.ReadToEndAsync().ConfigureAwait(false);
                }
            }
        }
    }
}
