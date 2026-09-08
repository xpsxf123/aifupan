namespace ReviewAnalysis.Bll
{
    /// <summary>
    /// Word 文件的处理类
    /// </summary>
    public static class WordDocumentBll
    {
        /// <summary>
        /// 获得 Word 文件的全部文字
        /// </summary>
        /// <param name="filePath">文件路径</param>
        public static string GetTextByWord(string filePath)
        {
            // 使用 SpireDoc 读取 doc 文件，OpenXml 不支持 doc 文件。
            // 但免费的 SpireDoc 有最多 500 段跟 25 张表格的限制，超过任意一项后将无法继续读取。
            return GetWordTextBySpireDoc(filePath);
        }

        /// <summary>
        /// 使用 SpireDoc 类库获得 Word 文件的全部文字
        /// </summary>
        private static string GetWordTextBySpireDoc(string filePath)
        {
            Spire.Doc.Document doc = new Spire.Doc.Document(filePath);
            doc.LoadFromFile(filePath);

            return doc.GetText();
        }
    }
}
