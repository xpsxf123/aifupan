using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using ReviewAnalysis.Ai.auto;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Bll;
using ReviewAnalysis.bo;
using ReviewAnalysis.bo.ai;
using ReviewAnalysis.Dto;
using ReviewAnalysis.vo.ai;

namespace ReviewAnalysis.Controller
{
    [RestController("关于AI相关的接口", "api/aiRelated")]
    public class AiRelatedController
    {
        public static AiRelatedBll aiRelatedBll = new AiRelatedBll();


        [HttpPost("AI问答", "/ask")]
        public AskResponseDto Ask(AskRequestDto dto)
        {
            return aiRelatedBll.Ask(dto);
        }

        [HttpPost("新增历史段落", "/addHistoryParagraph")]
        public HistoryParagraphVo addHistoryParagraph(HistoryParagraphBo history)
        {
            return aiRelatedBll.addHistoryParagraph(history);
        }

        [HttpPost("历史会话列表", "/historyParagraphList")]
        public List<HistoryParagraphVo> historyParagraphList(historyParagraphListBo bo)
        {
            return aiRelatedBll.historyParagraphList(bo.sourceId, bo.sourceType, bo.type);
        }

        [HttpPost("删除历史段落", "/deleteHistoryParagraph")]
        public void deleteHistoryParagraph(HistoryParagraphVo vo)
        {
            aiRelatedBll.deleteHistoryParagraph(vo);
        }


        [HttpPost("添加ai数据结构", "/addStructure")]
        public void addStructure(StructureReqBo bo)
        {
            aiRelatedBll.addStructure(bo);
        }

        
        [HttpPost("ai数据结构分页查询", "/structurePage")]
        public StructurePageVo structurePage(StructurePageBo bo)
        {
            if (bo.pageIndex == null) bo.pageIndex = 1;
            if (bo.pageSize == null) bo.pageSize = 30;

            return aiRelatedBll.structurePage(bo.type, bo.sourceId, bo.sourceType, bo.pageIndex, bo.pageSize);
        }

        /// <summary>
        /// 点赞
        /// </summary>
        /// <param name="likeBo"></param>
        [HttpPost("点赞", "/likes")]
        public void likes(StructureUpdateBo likeBo)
        {
            aiRelatedBll.likes(likeBo);
        }

        [HttpPost("获取使用ai的模型", "/getAiModel")]
        public int GetAiModel(AiModelVo vo)
        {
            return aiRelatedBll.GetAiModel(vo);
        }

        [HttpPost("分页查询ai问答记录数据", "/conversationPage")]
        public dynamic conversationPage(ConversationPageBo bo)
        {
            return aiRelatedBll.conversationPage(bo);
        }

        [HttpPost("获取ai推荐的行业", "/getAiRecommendTrade")]
        public AiRecommendTradeVo getAiRecommendTrade(AiRecommendTradeBo bo)
        {
            return aiRelatedBll.getAiRecommendTrade(bo);
        }

        [HttpGet("导出ai问答的配置", "/exportAiConfig")]
        public void exportAiConfig(string qaCodes)
        {
            aiRelatedBll.exportAiConfig(qaCodes);
        }

        [HttpGet("生成html", "/generateHtml")]
        public ConversationVo generateHtml(string id)
        {
            return HtmlAuto.add(id);
        }
        
        [HttpPost("获取html生成状态", "/getHtmlStatus")]
        public List<ConversationVo> getHtmlStatus(List<string> ids)
        {
            return aiRelatedBll.getHtmlStatus(ids);
        }
        
        [HttpGet("生成ai纠正内容", "/generateCorrectAiContent")]
        public ConversationVo generateCorrectAiContent(string id)
        {
            return CorrectAiContentAuto.add(id);
        }
    }
}
