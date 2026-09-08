<template>
    <div class="inspection-empty-page">
        <div class="inspection-ai-script">
            <div class="inspection-content">
                <div class="inspection-title">{{ config.title }}</div>
                <div class="inspection-desc">
                    <p v-for="(line, index) in config.lines" :key="index">
                        <span
                            v-for="(part, partIndex) in line"
                            :key="partIndex"
                            :class="{
                                'inspection-purple': part.type === 'purple',
                                'inspection-blue': part.type === 'blue'
                            }"
                        >{{ part.text }}</span>
                    </p>
                </div>
                <div class="inspection-actions" :class="{ 'single-action': !config.secondaryText || !secondaryVisible }">
                    <afp-button v-if="config.secondaryText && secondaryVisible" class="inspection-btn" size="default"
                                @click="secondaryClick">
                        {{ config.secondaryText }}
                    </afp-button>

                    <afp-button class="inspection-btn" size="default" :plain="false" type="primary"
                                @click="$emit('primary-click')">
                        {{ config.primaryText }}
                    </afp-button>
                </div>
            </div>
        </div>
        <QrCodeDialog ref="qrDialog"/>
    </div>
</template>

<script>
import QrCodeDialog from "@/components/qrCodeGuard/QrCodeDialog/index.vue";
import {QR_CODE_DIALOG_TYPE} from "@/enum/qrCodeDialogEnum";

const CONFIG_MAP = {
    aiSharding: {
        title: '什么是AI脚本拆解？',
        lines: [
            [
                {text: 'AI脚本拆解是爱复盘投喂了'},
                {text: '千万级场次数据进行AI训练', type: 'blue'},
                {text: '后沉淀出的能力。'}
            ],
            [{text: '帮助你快速拆解优秀直播间的话术结构、卖点顺序和关键承接动作。'}],
            [{text: '既能提升运营复盘效率，也能辅助主播学习和还原优质直播脚本。'}],
            [{text: '通过大量训练，可完成同行优质话术提取、分类与针对性复盘。'}]
        ],
        primaryText: '点我获取AI脚本'
    },
    aiDiagnose: {
        title: '什么是AI数据诊断？',
        lines: [
            [
                {text: '爱复盘通用'},
                {text: 'AI数据诊断智能体', type: 'purple'},
                {text: '，3分钟快速生成数据诊断报告'}
            ],
            [{text: '品牌、MCN、代运营公司首选提效赋能工具'}],
            [{text: '需要更个性化的数据诊断报告？'}],
            [
                {text: '请联系爱复盘产品顾问，'},
                {text: '定制数据诊断智能体', type: 'blue'}
            ]
        ],
        primaryText: '点我生成诊断报告'
    },
    scriptQuality: {
        title: '什么是负面话术质检？',
        lines: [
            [{text: '负面话术质检会在每场下播后，自动检测主播话术中的以下四类话术：'}],
            [{
                text: '1、崩盘话术        2、摸鱼话术        3、有损品牌的话术        4、增加售后成本的话术',
                type: 'purple'
            }],
            [
                {text: '质检规则可以通过'},
                {text: '定制话术质检智能体', type: 'blue'},
                {text: '来定制'}
            ]
        ],
        secondaryText: '点我开启自动话术质检',
        primaryText: '生成本场话术质检报告'
    },
    interactionInspection: {
        title: '什么是弹幕互动有效性巡检？',
        lines: [
            [
                {text: '不知道主播有没有及时'},
                {text: '回复高潜用户', type: 'purple'},
                {text: '的疑问？'}
            ],
            [
                {text: '不知道主播的互动是不是'},
                {text: '随意发挥', type: 'purple'},
                {text: '？'}
            ],
            [
                {text: '巡检整场直播用户互动和主播的回复情况，'},
                {text: '防止主播随意发挥', type: 'purple'}
            ],
            [
                {text: '互动巡检规则可以通过'},
                {text: '定制互动巡检智能体', type: 'blue'},
                {text: '来定制'}
            ]
        ],
        secondaryText: '点我开启自动互动巡检',
        primaryText: '生成本场互动巡检报告'
    },
    scriptRestoration: {
        title: '什么是话术还原监控？',
        lines: [
            [{text: '不知道主播有没有按照标准直播话术脚本来直播？'}],
            [
                {text: '不知道主播是不是'},
                {text: '随意发挥', type: 'purple'},
                {text: '？'}
            ],
            [
                {text: '监控主播整场直播与标准直播话术脚本的对比还原情况，'},
                {text: '防止主播随意发挥', type: 'purple'}
            ],
            [
                {text: '还原度规则可以通过'},
                {text: '定制还原度监控智能体', type: 'blue'},
                {text: '来定制'}
            ]
        ],
        secondaryText: '点我开启话术还原度监控',
        primaryText: '生成本场话术还原度报告'
    }
}

export default {
    name: 'InspectionEmptyPage',
    components: {QrCodeDialog},
    props: {
        type: {
            type: String,
            default: 'interactionInspection'
        },
        secondaryVisible: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return {
            aiMonitorQuota: {
                qc: {used: 0, total: 0},
                fidelity: {used: 0, total: 0},
                inspect: {used: 0, total: 0}
            }
        }
    },
    computed: {
        config() {
            return CONFIG_MAP[this.type] || CONFIG_MAP.interactionInspection
        }
    },
    methods: {
        async statusJudgment(key) {
            await this.getResourcesInfo()
            const level = Number(this.$store?.getters?.getPackageLevel ?? 0)
            const workshopOrAbove = level === -1 ? true : level >= 15
            if (!workshopOrAbove) return QR_CODE_DIALOG_TYPE.NO_PERMISSION_FREE_VERSION

            const map = {
                scriptQuality: {key: 'qc', type: QR_CODE_DIALOG_TYPE.AI_SPEECH_QC_QUOTA},
                scriptRestoration: {key: 'fidelity', type: QR_CODE_DIALOG_TYPE.AI_SCRIPT_RESTORE_QUOTA},
                interactionInspection: {key: 'inspect', type: QR_CODE_DIALOG_TYPE.AI_INTERACTION_INSPECT_QUOTA}
            }
            const item = map[key]
            if (!item) return false
            const {used = 0, total = 0} = this.aiMonitorQuota?.[item.key] || {}
            const totalNum = Number(total || 0)
            const usedNum = Number(used || 0)
            if (totalNum <= 0 || totalNum - usedNum <= 0) {
                await this.$refs?.qrDialog?.show({type: item.type})
                return false
            }
            return true
        },
        async getResourcesInfo() {
            if (!this.$httpBack?.scriptMonitor?.monitorPositionStatistics) return
            try {
                const res = await this.$httpBack.scriptMonitor.monitorPositionStatistics({})
                if (res?.code !== 0) return
                const list = Array.isArray(res?.data?.monitorPositions)
                    ? res.data.monitorPositions
                    : (Array.isArray(res?.data) ? res.data : [])
                const getItem = (codes = []) => list.find((it) => codes.some((code) => String(it?.code) === String(code)))
                const qc = getItem(['scriptQualityInspectionNum', 'scriptQualityNum'])
                const fidelity = getItem(['scriptFidelityMonitorNum', 'scriptFidelityNum'])
                const inspect = getItem(['interactionPatrolNum'])
                this.aiMonitorQuota = {
                    qc: {used: Number(qc?.useQuantity || 0), total: Number(qc?.totalQuantity || 0)},
                    fidelity: {used: Number(fidelity?.useQuantity || 0), total: Number(fidelity?.totalQuantity || 0)},
                    inspect: {used: Number(inspect?.useQuantity || 0), total: Number(inspect?.totalQuantity || 0)}
                }
            } catch (e) {
            }
        },
        async secondaryClick() {
            if (this.type === 'aiDiagnose') {
                this.$emit('secondary-click')
                return
            }
            const blocked = await this.statusJudgment(this.type)
            if (blocked) this.$emit('secondary-click')
        },
    }
}
</script>

<style scoped lang="scss">

.inspection-empty-page {
    height: 100%;
    box-sizing: border-box;
    overflow: auto;

    .inspection-ai-script {
        position: relative;
        width: calc(100% - 80px);
        min-width: 820px;
        height: 385px;
        margin: 12px 40px 0;
        background: url("~@/assets/imgs/2_5_8/ai_script.png") no-repeat center center;
        background-size: 100% 100%;
        box-sizing: border-box;
    }

    .inspection-content {
        position: absolute;
        top: 78px;
        left: 20.6%;
        width: 549px;
        color: #151719;
        font-size: 14px;
        line-height: 22px;
    }

    .inspection-title {
        display: inline-block;
        font-size: 20px;
        font-weight: 600;
        line-height: 28px;
        background: linear-gradient(172.77deg, #09CFFF 4.95%, #942DFE 91.45%);
        -webkit-background-clip: text;
        background-clip: text;
        color: transparent;
        white-space: nowrap;
    }

    .inspection-desc {
        display: flex;
        flex-direction: column;
        gap: 16px;
        margin-top: 24px;

        p {
            margin: 0;
            white-space: pre-wrap;
        }
    }

    .inspection-purple {
        color: #942dfe;
        font-weight: 500;
    }

    .inspection-blue {
        color: #444dff;
        font-weight: 500;
    }

    .inspection-actions {
        display: flex;
        align-items: center;
        gap: 20px;
        margin-top: 26px;

        &.single-action {
            justify-content: flex-start;
        }
    }

    .inspection-btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 34px;
        padding: 0 10px;
        border-radius: 55px;
        border: 1px solid #444dff;
        font-size: 14px;
        font-weight: 500;
        line-height: 22px;
        white-space: nowrap;
        cursor: pointer;
        box-sizing: border-box;
        font-family: inherit;
    }
}
</style>
