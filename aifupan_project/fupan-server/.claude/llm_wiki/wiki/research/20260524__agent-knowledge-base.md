# Agent 知识库管理与提示词治理（伴生 4）

**Parent:** 20260524__ai-agent-uplift-master.md（`20260524__ai-agent-uplift-master.md`，未建）
**Scope:** 把现有 1314 条 prompt（11 askType × tb_cue_words + 3 askType × systemKv + 1 硬编码）改造为 Agent 时代的知识库：萃取 → 清洗 → 结构化 → 索引 → 版本化 → 治理流程

---

## 1. 现状回顾（前置）

| 来源 | 条数 | 形态 |
|---|---|---|
| tb_cue_words | 1314 (跨 11 cue_type) | 关系行（cue_word 短标题 / problem prompt 主体 / sort 顺序 / trade_id 行业 / accountType 平台变体 / scope 直播 vs 短视频 / syncScene 1-3 / applyTo 0-1 / tenant_id 系统 0 vs 租户私有 ≠0） |
| systemKv 单值 | 3 条专属 | `check_ai_content_prompt` / `correct_ai_content_prompt` / 等 |
| 代码硬编码 | 1 条 | `AnchorUrlLogicImpl.java:978` "你是一个顶尖关键词内容提炼专家" |
| 死代码 | 1 条 (EXTRACT_VIDEO) | 0 引用 |

**关键挑战：**
- prompt 字段命名误导（`cue_word` 是短标题 / `problem` 才是 prompt 主体）
- 行业差异化：4 个 trade 各有变种但 sort 编号不稳定，跨 trade 不可移植
- 占位符未标准化（`#{trade}` `#{reason}` `#{platform}` 混用）
- 无版本管理 / 无评测 / 无灰度
- 散落多处存储（tb_cue_words + systemKv + 硬编码）

---

## 2. 萃取（DB → 结构化 YAML/JSON）

### 2.1 目标格式（Skill 定义级 YAML）

每个 Skill 对应一份 YAML：

```yaml
# skills/script_interaction/v1.yaml
skill:
  name: ScriptInteraction
  cue_type: 6
  sort: 3
  version: v1
  is_active: true
  created_at: 2026-05-24
  source: tb_cue_words id=4456181027774001152

metadata:
  description: "分析直播脚本中的互动话术"
  category: 内容生成
  recommended_model: doubao-pro
  estimated_tokens: { input: 3000, output: 800 }
  
applicable_to:
  trades: [1, 4324266648077860864, 4352179920231727104]  # 哪些行业适用
  platforms: [douyin]  # accountType=0 (推断为抖音)
  source_type: video  # scope=0 直播
  sync_scenes: [single]  # syncScene=1

prompt:
  role: |
    你是一个直播话术总结专家，深刻理解直播间的各种互动形式
    （如让用户在公屏打字、问用户问题、欢迎新用户）。
  
  task: |
    你将总结这篇直播原文的互动语句。
    这个直播间属于#{trade}行业。
  
  output_format: |
    输出 markdown 表格，列：
    - 互动话术原文
    - 互动类型（公屏打字 / 问问题 / 欢迎新用户 / 其他）
    - 出现时间（HH:mm:ss）
  
  constraints:
    - 不要以任何形式省略话术内容
    - 输出全部完整话术
    - 不要用省略号代替部分话术内容
    - 不要直接删掉部分话术内容

placeholders:
  - name: trade
    type: string
    source: ctx.tradeName
  - name: script
    type: text
    source: previous_skill_result.GetVideoScript
```

### 2.2 萃取脚本（Python，运行在 CI）

```python
# .claude/scripts/agent/extract_cue_words.py
import csv, yaml, re
from pathlib import Path

def extract(db_dump_csv: Path, output_dir: Path):
    with open(db_dump_csv) as f:
        rows = list(csv.DictReader(f))
    
    for row in rows:
        if row['cue_type'] not in TARGET_CUE_TYPES: continue
        
        skill_name = derive_skill_name(row)  # 从 cue_word 短标题推 PascalCase
        
        # 解析 problem 字段为结构化段（role / task / output_format / constraints）
        prompt_parts = parse_prompt_structure(row['problem'])
        
        yaml_doc = {
            'skill': {
                'name': skill_name,
                'cue_type': int(row['cue_type']),
                'sort': int(row['sort']),
                'version': 'v1',
                'source': f"tb_cue_words id={row['id']}",
            },
            'applicable_to': infer_applicability(row),
            'prompt': prompt_parts,
            'placeholders': extract_placeholders(row['problem']),
        }
        
        out_path = output_dir / f"{skill_name}/v1.yaml"
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with open(out_path, 'w') as f:
            yaml.dump(yaml_doc, f, allow_unicode=True, sort_keys=False)

def parse_prompt_structure(problem: str) -> dict:
    """
    把 problem 字段按段落语义切成 role / task / output_format / constraints
    现有 problem 多按"你是 ... 你将 ... 输出格式 ... 不要..."模式写
    """
    # 用正则 / 关键词找段落标记
    sections = {}
    if match := re.search(r'(你是?.*?[。\n])', problem):
        sections['role'] = match.group(1)
    if match := re.search(r'(你将.*?[。\n])', problem):
        sections['task'] = match.group(1)
    if match := re.search(r'输出.*?如下：?\s*([\s\S]+?)(?=不要|注意|$)', problem):
        sections['output_format'] = match.group(1).strip()
    constraints = re.findall(r'(不要.*?[\n。])', problem)
    if constraints:
        sections['constraints'] = [c.strip() for c in constraints]
    return sections
```

### 2.3 萃取产物目录

```
.claude/agent-kb/
├── skills/
│   ├── script_interaction/
│   │   ├── v1.yaml          ← trade=1 通用版
│   │   ├── v1-trade-861.yaml ← trade=4324...的差异版
│   │   └── v1-trade-104.yaml ← trade=4352...的差异版
│   ├── script_shaping/
│   ├── ... (12 个话术 + 其他 cue_type)
├── systemkv-prompts/
│   ├── check_ai_content.yaml
│   ├── correct_ai_content.yaml
├── hardcoded/
│   └── anchor_keyword.yaml   ← AnchorUrlLogicImpl:978
└── _metadata.yaml             ← 全局索引
```

代码运行时通过 ConfigurationProperties 加载 YAML（或 DB ALTER 后改回数据库；W1 阶段 YAML 文件即可，简化部署）。

---

## 3. 清洗（5 类规则）

### 3.1 去重

| 重复类型 | 检测 | 处理 |
|---|---|---|
| 完全重复（problem 文本 100% 一致） | hash 比对 | 仅保留 trade_id=1 的通用版，其他 trade 标记 `inherits_from` 指向通用版 |
| 高相似（编辑距离 > 90%） | difflib | 人工 review；保留差异更显著的 |
| sort 编号冲突（同 trade 同 sort 多条） | groupby (trade, sort) | 保留 update_date 最新一条 |

### 3.2 占位符标准化

现有占位符混用：`#{trade}` / `#{reason}` / `#{platform}` / `#{optimize}` / 还有不规范的 `{trade}` `${trade}` 等

标准化为统一规范：
```
${var.name}      ← 上下文变量（ctx.tradeName / ctx.userId）
${input.field}   ← Skill 输入参数（args.script / args.timeRange）
${prev.skill}    ← 上一步 Skill 结果（prev.GetVideoScript.result）
```

清洗脚本批量替换 + 失败项标记人工 review。

### 3.3 死内容剔除

- 检测：grep prompt 文本中提到的字段 / 接口名 / 模型 code → 在代码中找引用，0 引用即标记 dead
- 例：prompt 提到 `extract_video_data` 字段但代码无此字段 → 标记 dead
- 处理：dead → is_active=0 + 移到 `kb/_archived/`

### 3.4 行业归一

49 个 trade_id 中：
- trade=1 = 默认通用
- 其他 = 各行业（直播带货 / 教育 / 本地生活 等）

清洗策略：
- 找出"99% 内容相同"的 trade 变体 → 合并为基础版 + 注脚标"行业 X 微调"
- 找出"完全独立"的（如 trade=4352181997137821696 的情绪价值 / 刷礼物 / 回怼）→ 独立保留

### 3.5 失效检测（与代码 grep 联动）

| 检测项 | 工具 |
|---|---|
| Skill 是否被 SkillRegistry 注册 | Spring 启动时扫描 `@Component implements AgentSkill` |
| Skill 的 cue_type+sort 是否在 tb_cue_words 找得到 | CI 跑 SQL 比对 |
| 占位符变量是否在 ctx / args / prev 三个 namespace 中有定义 | PromptUtils 启动时校验 |

---

## 4. 结构化存储设计（含 DDL）

### 4.1 改造现有 tb_cue_words

```sql
-- 加版本管理 + 灰度 + 父版本继承
ALTER TABLE tb_cue_words 
  ADD COLUMN version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '版本号',
  ADD COLUMN is_active TINYINT NOT NULL DEFAULT 1 COMMENT '0=下线 1=在线 2=灰度',
  ADD COLUMN parent_id BIGINT NULL COMMENT '继承自哪个 prompt（迭代时填）',
  ADD COLUMN skill_name VARCHAR(100) NULL COMMENT '对应 Agent Skill 名（如 ScriptInteraction）',
  ADD COLUMN structured_yaml MEDIUMTEXT NULL COMMENT '结构化 prompt YAML（role/task/constraints）';

CREATE INDEX idx_cue_words_active ON tb_cue_words(cue_type, trade_id, is_active);
CREATE INDEX idx_cue_words_skill ON tb_cue_words(skill_name, is_active);
```

### 4.2 新增 Skill 注册表（可选 / W3+）

```sql
CREATE TABLE tb_agent_skill_def (
  id BIGINT PRIMARY KEY,
  skill_name VARCHAR(100) NOT NULL UNIQUE,
  cue_type TINYINT,
  description TEXT NOT NULL,
  input_schema JSON NOT NULL,
  model_hint JSON,
  is_active TINYINT DEFAULT 1,
  version VARCHAR(32) NOT NULL DEFAULT 'v1',
  tenant_id BIGINT DEFAULT 0,
  create_date DATETIME NOT NULL,
  update_date DATETIME NOT NULL,
  is_deleted TINYINT DEFAULT 0
);
```

**MVP 阶段不创建** — 先走代码 @Component 注册。Skill 数 > 30 再迁 DB。

### 4.3 RAG 向量索引（pgvector）

```sql
-- 在 RDS 上启用 pgvector 扩展（一次性）
CREATE EXTENSION IF NOT EXISTS vector;

-- 知识库向量表
CREATE TABLE agent_kb_embedding (
  id BIGSERIAL PRIMARY KEY,
  source_type VARCHAR(20) NOT NULL,  -- skill / doc / faq
  source_id VARCHAR(100) NOT NULL,   -- skill_name / doc_id
  chunk_text TEXT NOT NULL,
  embedding VECTOR(1024) NOT NULL,    -- bge-m3 1024 维
  metadata JSONB,                      -- {trade_id, askType, version, ...}
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- IVFFlat 索引（1024 维 + 千级数据足够）
CREATE INDEX idx_kb_embedding ON agent_kb_embedding 
  USING ivfflat (embedding vector_cosine_ops) WITH (lists=100);

-- metadata 过滤索引
CREATE INDEX idx_kb_metadata ON agent_kb_embedding USING GIN (metadata);
```

向量化触发：
- 手动：CLI 跑 `python scripts/agent/embed_skills.py --force`
- 自动：tb_cue_words 写入触发 trigger / 应用层 hook 异步入队（W3+）

---

## 5. RAG 检索（话术助手 MVP 用法）

### 5.1 检索场景

| 场景 | 触发 | 检索内容 |
|---|---|---|
| **Skill prompt 召回** | Planner 不知道有哪些 Skill 时 | 检索"互动 / 金句"等关键词，召回 top 3 Skill description |
| **行业相似 prompt 召回** | 当前 trade 无对应 prompt 时 | 检索同义 trade 的同 cue_type prompt |
| **历史相似问答召回** | 用户问题与历史相似时 | 检索过去 90 天 conversation Q 字段 top 3 |
| **失败案例避免** | 反思时 | 检索同类型失败案例（trace status=FAILED） |

### 5.2 检索流程（Spring AI VectorStore API）

```java
@Component
public class KnowledgeBaseRetriever {
    private final VectorStore vectorStore;  // PgVectorStore (Spring AI)

    public List<Document> retrieve(String query, Map<String, Object> filter, int topK) {
        SearchRequest req = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .filterExpression(buildFilter(filter))  // metadata 过滤
            .similarityThreshold(0.7)
            .build();
        return vectorStore.similaritySearch(req);
    }
    
    public List<AgentSkill> recallSkills(String userIntent, AgentContext ctx) {
        List<Document> docs = retrieve(userIntent, 
            Map.of("source_type", "skill", "trade_id", ctx.tradeId), 5);
        return docs.stream()
            .map(d -> skillRegistry.get(d.getMetadata().get("skill_name")))
            .filter(Objects::nonNull)
            .toList();
    }
}
```

### 5.3 RAG 与 Planner 配合

Planner 接受两种 Skill 候选来源：
1. **全部 Skill**（< 30 个时直接列）
2. **RAG 召回 Skill**（> 30 个时按 query 召回 top 10）

MVP 阶段 20 个 Skill 全列，不需要 RAG。RAG 在 W3+ 用于"历史相似问答召回 + 失败避免"。

---

## 6. 版本化与灰度发布流程

### 6.1 编辑 → 灰度 → 全量 流程

```
1. 编辑（DBA / 业务人员）
   ↓ 在 tb_cue_words 复制行 (parent_id=old.id, version='v2', is_active=2 灰度)
2. CI 自动触发
   ↓ 萃取 → 清洗 → 向量化 → 入 agent_kb_embedding
3. 评测（DeepEval）
   ↓ 跑 golden set，对比 v1 / v2 评分
4. 灰度发布
   ↓ 按 userId % 100 < N 决定走 v1 或 v2（N 从 10 → 30 → 100）
5. 评测达标
   ↓ v1 is_active=0 (下线), v2 is_active=1 (在线)
6. 老版本保留 3 个月
   ↓ 完整 trace + 评测记录，支持回滚
```

### 6.2 回滚机制

```sql
-- 一键回滚
UPDATE tb_cue_words SET is_active = 0 WHERE id = <v2_id>;
UPDATE tb_cue_words SET is_active = 1 WHERE id = <v1_id>;
-- 应用层缓存清除（pub/sub）
```

回滚 5 分钟内生效。

---

## 7. 治理流程图

```
┌──────────────────────────────────────────────────────────────┐
│ DBA / 业务人员                                                │
│ 在 tb_cue_words 编辑（新增 / 改 / 创建灰度版本）              │
└─────────────────────┬────────────────────────────────────────┘
                      │ git push / DB 写入触发
┌─────────────────────▼────────────────────────────────────────┐
│ CI Pipeline                                                  │
│  1. python extract_cue_words.py → YAML 产物                  │
│  2. python clean_prompts.py     → 去重 / 占位符 / 死内容     │
│  3. python validate_schema.py   → YAML 结构校验              │
│  4. python embed_skills.py      → bge-m3 向量化 → pgvector   │
│  5. pytest deepeval/            → 评测 golden set            │
│     │ FAIL → 阻断发布                                         │
│     │ PASS → 进入灰度                                         │
└─────────────────────┬────────────────────────────────────────┘
                      │ 灰度策略生效
┌─────────────────────▼────────────────────────────────────────┐
│ 生产 Agent                                                    │
│  Planner 按 userId % 100 选 v1/v2 prompt                     │
│  对比 first-pass / 反思 / 用户满意度                          │
└─────────────────────┬────────────────────────────────────────┘
                      │ 7 天观察后
┌─────────────────────▼────────────────────────────────────────┐
│ 评测复盘 + 全量发布                                            │
│  v1 is_active=0 / v2 is_active=1 / 老版本保留 3 月            │
└──────────────────────────────────────────────────────────────┘
```

---

## 8. 知识库维护责任分工

| 角色 | 职责 |
|---|---|
| **PM** | 决定哪些 Skill 上线 / 哪些子能力优先 / 行业差异化策略 |
| **运营 / 业务专家** | 撰写 / 调优 prompt 文本（直接编辑 tb_cue_words） |
| **后端开发** | 维护 SkillRegistry / Planner / Subagent 代码 |
| **数据团队** | 维护萃取 / 清洗 / 向量化脚本 |
| **测试** | 维护 golden set + 评测 metric 阈值 |
| **DBA** | tb_cue_words DDL + pgvector 扩展运维 |
| **SRE** | Langfuse / DeepEval 容器运维 |

---

## 9. 治理早期反模式（已知风险）

| 反模式 | 缓解 |
|---|---|
| 业务人员直接改 prompt 不走灰度 → 全量受影响 | DDL 加 trigger 强制 is_active=2（灰度态）；改 v1 需走流程 |
| Skill description 写得模糊 → Planner 选错 Skill | description 必须含 "什么时候用 / 输入 / 输出" 三段；CI 校验长度 ≥ 50 字 |
| 占位符变量未定义就用 → 运行时 NPE | PromptUtils 启动时校验所有 placeholder 在 namespace 中存在 |
| YAML 结构频繁改 → 评测集失效 | structured_yaml schema 加版本号，不兼容时强制 golden set 也升级 |
| RAG 召回错 Skill → Agent 一直选错 | similarity_threshold ≥ 0.7 + Planner 仍有最终决策权（不强制听 RAG）|

---

## 10. 知识库迭代规划

| 阶段 | 工作 |
|---|---|
| **W1** | 萃取 1 个 Skill（ScriptInteraction）→ YAML；手写不走 CI |
| **W2** | extract / clean / validate 脚本上线；萃取 5 个核心 Skill |
| **W3** | 全部 12 个话术 Skill 萃取；ALTER tb_cue_words 加 version 字段；向量化入 pgvector |
| **W4** | 灰度流程跑通（拿 1 个 Skill 真灰度 1 周） |
| **W5** | 治理流程文档化；运营 / PM 培训；全 askType 覆盖路线图 |

详 Skill 设计 → 伴 3（`20260524__agent-skills-and-dispatch.md`，未建）
详技术选型 → 伴 2（`20260524__agent-tech-stack.md`，未建）
详可观察评测 → 伴 5（`20260524__agent-obs-eval-rollout.md`，未建）
