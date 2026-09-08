using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.Dto;

namespace ReviewAnalysis.Ai
{
    public interface DiagnosisAskType
    {

        Task<AskRequestDto> getParams(AiAutoTimerDto dto);

    }
}
