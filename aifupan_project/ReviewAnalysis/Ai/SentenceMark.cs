using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Dto;

namespace ReviewAnalysis.Ai
{
    public interface SentenceMark
    {
        SentenceMarkDto GetMarkDto();

        string GetContent();

        string GetTextParamsContent();

        string GetAllContent();

        string GetTradeId();

        string GetTradeName();

        string GetPlatform();

        string GetDataScreenshot();

        string GetBoard();

        void setSingleMaxNum(int num);

        string setAskQuestion(AskRequestDto dto);

        void setOtherParams(Dictionary<string, Object> otherParams);

        int? getSpeed();

        void setSpeed(AskRequestDto dto);
    }
}
