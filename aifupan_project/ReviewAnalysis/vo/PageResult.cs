using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo
{
    /// <summary>
    /// 服务端分页结果反序列化类，对应后端 <c>PageUtils&lt;T&gt;</c> 的 JSON 结构
    /// （字段名与后端 camelCase 序列化结果保持一致，供 <see cref="Newtonsoft.Json.JsonConvert"/> 直接反序列化）。
    /// </summary>
    /// <typeparam name="T">列表元素类型</typeparam>
    public class PageResult<T>
    {
        /// <summary>
        /// 总记录数
        /// </summary>
        public int totalCount { get; set; }

        /// <summary>
        /// 每页记录数
        /// </summary>
        public int pageSize { get; set; }

        /// <summary>
        /// 总页数
        /// </summary>
        public int totalPage { get; set; }

        /// <summary>
        /// 当前页数
        /// </summary>
        public int currPage { get; set; }

        /// <summary>
        /// 列表数据
        /// </summary>
        public List<T> list { get; set; }
    }
}
