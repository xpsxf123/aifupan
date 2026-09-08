using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Dto;

namespace ReviewAnalysis.Ai
{
    public interface Ask
    {

        /// <summary>
        /// 问答接口
        /// </summary>
        /// <param name="dto"></param>
        /// <param name="request"></param>
        /// <param name="response"></param>
        /// <returns></returns>
        Task<AskResponseDto> AskStream(AskRequestDto dto, HttpListenerRequest request, HttpListenerResponse response);

    }
}
