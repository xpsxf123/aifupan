using CefSharp;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang
{
    public class CustomResponseFilter : IResponseFilter
    {

        public List<byte> dataAll = new List<byte>();

        /// <summary>
        /// 处理响应的数据
        /// </summary>
        /// <param name="dataIn">原始响应数据流</param>
        /// <param name="dataInRead">从 dataIn 实际读取的字节数</param>
        /// <param name="dataOut">修改后的输出流（可替换为自定义数据）</param>
        /// <param name="dataOutWritten">向 dataOut 写入的字节数</param>
        /// <returns></returns>
        /// <exception cref="NotImplementedException"></exception>
        public FilterStatus Filter(Stream dataIn, out long dataInRead, Stream dataOut, out long dataOutWritten)
        {
            
            try
            {
                
                // 初始化输出参数
                dataInRead = 0;
                dataOutWritten = 0;

                if (dataIn == null || dataIn.Length == 0)
                {
                    // 返回Done（没有数据可处理）
                    return FilterStatus.Done;
                }

                dataInRead = dataIn.Length;
                dataOutWritten = Math.Min(dataInRead, dataOut.Length);

                dataIn.CopyTo(dataOut);
                dataIn.Seek(0, SeekOrigin.Begin);
                byte[] bs = new byte[dataIn.Length];
                dataIn.Read(bs, 0, bs.Length);
                dataAll.AddRange(bs);

                dataInRead = dataIn.Length;
                dataOutWritten = dataIn.Length;

                return FilterStatus.NeedMoreData;
            }
            catch (Exception ex)
            {
                dataInRead = dataIn.Length;
                dataOutWritten = dataIn.Length;

                return FilterStatus.Done;
            }
        }

        public void Dispose()
        {
            
        }

        public bool InitFilter()
        {
            return true;
        }
    }
}
