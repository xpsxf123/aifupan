using ReviewAnalysis.Ai.Model;

namespace ReviewAnalysis.Dto
{
    public class PromptAssemblyResultDto
    {
        public string assembledPrompt { get; set; }
        public string identity { get; set; }
        public string contextId { get; set; }
        public int useModelWay { get; set; }
        public AiTempTokenDto modelConfig { get; set; }
        public string systemPrompt { get; set; }
    }
}
