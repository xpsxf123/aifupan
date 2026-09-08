package com.jiuyu.replay.system.bo;

import java.io.Serializable;

/**
 * 一份待导入的 Markdown 文件（内存副本）。
 *
 * <p><b>为什么要复制而不是直接传 MultipartFile</b>：MultipartFile 背后是请求的临时文件，
 * HTTP 请求一结束容器就会清理它。导入走异步，异步线程真正读取时请求早已返回，
 * 拿到的是空流或 FileNotFoundException，而且这个错只在文件较大（落磁盘）时才出现，
 * 小文件（留在内存）测试时看不出来——是那种上线才炸的写法。
 *
 * <p><b>为什么带 rejectReason</b>：扩展名不对、单文件超限这类问题在 Controller 就能发现，
 * 但不能因此让整批失败——openspec §5.4 把「文件过大」列为单篇失败原因，
 * 一个拼错扩展名的文件不该让另外 49 篇正常文件一起作废。
 * 因此这里把「已经知道会失败」的文件也放进批次，由导入执行体产出对应的逐篇结果。
 *
 * @param fileName     原始文件名，用于导入结果逐篇展示
 * @param content      文件全文（已按 UTF-8 解码）；被拒文件为 null
 * @param rejectReason 提交阶段就判定失败的原因；正常文件为 null
 *
 * @author claude
 * @date 2026-08-13
 */
public record SeoImportFileBo(String fileName, String content, String rejectReason) implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 正常文件 */
    public static SeoImportFileBo of(String fileName, String content) {
        return new SeoImportFileBo(fileName, content, null);
    }

    /** 提交阶段即判定失败的文件，仍进批次以便产出逐篇结果 */
    public static SeoImportFileBo rejected(String fileName, String reason) {
        return new SeoImportFileBo(fileName, null, reason);
    }
}
