package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 批量达人档案入参（按 author_id 直查公共爬取达人库，不分页，见接口文档 §6.22）。
 *
 * <p>用于「爆款选题上榜账号」这类场景：拿到一批 author_id 后一次取回档案（昵称/头像/认证/粉丝数等）。
 * 读的是全库爬取达人库 tb_video_influencer_info，<b>不做租户裁剪</b>，也不返回租户维度字段。</p>
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InfluencerDetailBatchBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 达人 id 集合（= tb_video_info.author_id 原值，与 §6.19 列表项 / §6.21 topAuthors[] 的 authorId 同口径）。
     *
     * <p>该列是字符串，语义上是 tb_video_influencer_info.id 的字符串形式；服务端按数字解析后直查主键，
     * 解析不了或库里查不到的 id 会原样出现在返回的 {@code notFoundAuthorIds} 里，不静默丢弃。</p>
     */
    @NotEmpty(message = "authorIdList 不能为空")
    @Size(max = 50, message = "authorIdList 最多 50 个")
    private List<String> authorIdList;
}
