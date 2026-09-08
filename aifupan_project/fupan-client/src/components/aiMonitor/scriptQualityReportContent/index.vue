<!--
@description 话术质检报告解析组件：兼容后端 reportContent 返回 HTML / JSON / Markdown(JSON) 三种格式，并渲染为可读的报告视图。
-->

<template>
    <div class="scriptQualityReportContent">
        <AiContentRenderer
            v-if="useCustomTagRenderer"
            :content="customTagText"
            :plainTextMode="false"
            renderMode="mdTag"
        />
        <div v-if="parsed.kind === 'empty'" class="sq-empty">暂无报告内容</div>

        <div v-else-if="parsed.kind === 'html'" v-html="parsed.html"></div>

        <div v-else-if="parsed.kind === 'json'">
            <div v-if="metaStatementHtmlSafe" class="sq-metaStatement" v-html="metaStatementHtmlSafe"></div>

            <div class="sq-summary" v-if="summaryView">
                <div class="sq-summaryItem">
                    <span class="sq-summaryLabel">崩盘话术：</span>{{ summaryView.crashCount }}
                </div>
                <div class="sq-summaryItem">
                    <span class="sq-summaryLabel">摸鱼话术：</span>{{ summaryView.slackCount }}
                </div>
                <div class="sq-summaryItem">
                    <span class="sq-summaryLabel">有损品牌：</span>{{ summaryView.brandDamageCount }}
                </div>
                <div class="sq-summaryItem">
                    <span class="sq-summaryLabel">增加售后：</span>{{ summaryView.afterSalesCount }}
                </div>
                <div class="sq-summaryItem" v-if="summaryView.totalNegativeCount !== null">
                    <span class="sq-summaryLabel">负面总数：</span>{{ summaryView.totalNegativeCount }}
                </div>
                <div class="sq-summaryItem" v-if="summaryView.totalSentencesAnalyzed !== null">
                    <span class="sq-summaryLabel">分析句数：</span>{{ summaryView.totalSentencesAnalyzed }}
                </div>
                <div class="sq-summaryItem" v-if="summaryView.overallScore !== null">
                    <span class="sq-summaryLabel">综合得分：</span>{{ summaryView.overallScore }}
                </div>
            </div>

            <div v-if="breakdownList.length" class="sq-breakdown">
                <div class="sq-breakdownItem" v-for="it in breakdownList" :key="it.key">
                    <span class="sq-breakdownLabel">{{ it.title }}：</span>
                    <span class="sq-breakdownText">{{ it.text }}</span>
                </div>
            </div>

            <div v-if="summaryHtmlSafe" class="sq-summaryHtml" v-html="summaryHtmlSafe"></div>
            <div v-if="improvementSuggestions.length" class="sq-suggestions">
                <div class="sq-suggestionsTitle">改进建议</div>
                <div class="sq-suggestion" v-for="(it, idx) in improvementSuggestions" :key="`s-${idx}`">{{ it }}</div>
            </div>

            <div class="sq-sections">
                <div class="sq-section" v-for="section in sections" :key="section.key">
                    <div class="sq-sectionTitle">
                        <div>
                            {{ section.title }}
                            <span class="sq-sectionHint">共 {{ section.items.length }} 条</span>
                        </div>
                    </div>

                    <div v-if="!section.items.length" class="sq-empty">暂无明细</div>
                    <div v-else class="sq-item" :class="it.itemClass" v-for="(it, idx) in section.items" :key="`${section.key}-${idx}`">
                        <div class="sq-itemMeta">
                            <span class="sq-tag">{{ it.timeRange || '-' }}</span>
                            <span class="sq-tag" :class="it.severityTagClass">{{ it.severityText }}</span>
                        </div>
                        <div v-if="it.issueHtml" class="sq-itemIssue" v-html="it.issueHtml"></div>
                        <div v-else class="sq-itemIssue">{{ it.issue || '-' }}</div>
                        <div v-if="it.originalHtml" class="sq-itemText" v-html="it.originalHtml"></div>
                        <div v-else class="sq-itemText">{{ it.originalText || '-' }}</div>
                        <div v-if="it.suggestionHtml" class="sq-itemSuggestion" v-html="it.suggestionHtml"></div>
                        <div v-else-if="it.suggestion" class="sq-itemSuggestion">{{ it.suggestion }}</div>
                    </div>
                </div>
            </div>
        </div>

        <AiContentRenderer
            v-else
            :content="parsed.rawText"
            :plainTextMode="true"
            renderMode="pureMd"
        />
    </div>
</template>

<script>
import { parseScriptQualityReportContent, safeParseJson, sanitizeHtml } from '@/utils/scriptQualityReportParser'
import { renderAiResponseContent } from '@/components/analysis/ai/common/pureMdRenderParser'
import './index.scss'

export default {
    components: {
        AiContentRenderer: () => import('@/components/aiContentRenderer/index.vue')
    },
    props: {
        report: {
            type: Object,
            default: () => null
        },
        content: {
            type: [String, Array, Object],
            default: ''
        },
        summaryJson: {
            type: String,
            default: ''
        }
    },
    computed: {
        rawContent() {
            return this.report?.reportContent ?? this.content
        },
        customTagText() {
            const c = this.rawContent
            if (c === null || c === undefined) return ''
            if (Array.isArray(c)) {
                return c.map((it) => (it === null || it === undefined ? '' : String(it))).join('')
            }
            return typeof c === 'string' ? c : ''
        },
        useCustomTagRenderer() {
            const text = (this.customTagText || '').trim()
            if (!text) return false
            return /<\s*aifupan-[^>\s/]+/i.test(text)
        },
        parsed() {
            if (this.useCustomTagRenderer) {
                return { kind: 'customTag' }
            }
            const summaryJson = this.report?.summaryJson ?? this.summaryJson ?? ''
            return parseScriptQualityReportContent(this.rawContent, summaryJson)
        },
        summaryView() {
            if (this.parsed.kind !== 'json') return null
            const summary = this.parsed.summary || this.parsed.data?.summary || safeParseJson(this.report?.summaryJson) || null
            if (!summary) return null
            return {
                crashCount: Number(summary.crashCount ?? 0),
                slackCount: Number(summary.slackCount ?? 0),
                brandDamageCount: Number(summary.brandDamageCount ?? 0),
                afterSalesCount: Number(summary.afterSalesCount ?? 0),
                totalNegativeCount: summary.totalNegativeCount === undefined ? null : Number(summary.totalNegativeCount ?? 0),
                totalSentencesAnalyzed: summary.totalSentencesAnalyzed === undefined ? null : Number(summary.totalSentencesAnalyzed ?? 0),
                overallScore: summary.overallScore === undefined ? null : Number(summary.overallScore ?? 0)
            }
        },
        sections() {
            if (this.parsed.kind !== 'json') return []
            const details = this.parsed.data?.details || {}
            const renderMd = (text) => {
                const raw = String(text ?? '').trim()
                if (!raw) return ''
                return renderAiResponseContent(raw)?.answerHtml || ''
            }
            const normalizeItem = (it = {}) => {
                const severity = String(it.severity || '').toLowerCase()
                const severityTextMap = { low: '低', medium: '中', high: '高' }
                const severityTagClassMap = {
                    high: 'sq-tag--high',
                    medium: 'sq-tag--medium',
                    low: 'sq-tag--low'
                }
                const itemClassMap = {
                    high: 'sq-item--high',
                    medium: 'sq-item--medium',
                    low: 'sq-item--low'
                }
                return {
                    timeRange: it.timeRange || '',
                    originalText: it.originalText || '',
                    originalHtml: renderMd(it.originalText),
                    issue: it.issue || '',
                    issueHtml: renderMd(it.issue),
                        suggestion: it.suggestion || '',
                        suggestionHtml: renderMd(it.suggestion),
                    severity,
                    severityText: severityTextMap[severity] || (it.severity ? String(it.severity) : '未知'),
                    severityTagClass: severityTagClassMap[severity] || '',
                    itemClass: itemClassMap[severity] || ''
                }
            }
            return [
                { key: 'crash', title: '崩盘话术', items: Array.isArray(details.crash) ? details.crash.map(normalizeItem) : [] },
                { key: 'slack', title: '摸鱼话术', items: Array.isArray(details.slack) ? details.slack.map(normalizeItem) : [] },
                { key: 'brandDamage', title: '有损品牌', items: Array.isArray(details.brandDamage) ? details.brandDamage.map(normalizeItem) : [] },
                { key: 'afterSales', title: '增加售后', items: Array.isArray(details.afterSales) ? details.afterSales.map(normalizeItem) : [] }
            ]
        },
        summaryHtmlSafe() {
            if (this.parsed.kind !== 'json') return ''
            const html = this.parsed.data?.summaryHtml
            if (!html) return ''
            return sanitizeHtml(html)
        },
        improvementSuggestions() {
            if (this.parsed.kind !== 'json') return []
            const list = this.parsed.data?.improvementSuggestions
            return Array.isArray(list) ? list.map((it) => String(it)).filter(Boolean) : []
        },
        metaStatementHtmlSafe() {
            if (this.parsed.kind !== 'json') return ''
            const html = this.parsed.data?.meta?.statement
            if (!html) return ''
            return sanitizeHtml(html)
        },
        breakdownList() {
            if (this.parsed.kind !== 'json') return []
            const b = this.parsed.data?.breakdown || {}
            const items = [
                { key: 'crash', title: '崩盘话术', text: b?.crash?.text },
                { key: 'slack', title: '摸鱼话术', text: b?.slack?.text },
                { key: 'brandDamage', title: '有损品牌', text: b?.brandDamage?.text },
                { key: 'afterSales', title: '增加售后', text: b?.afterSales?.text }
            ]
            return items
                .map((it) => ({ ...it, text: String(it.text || '').trim() }))
                .filter((it) => !!it.text)
        }
    }
}
</script>
