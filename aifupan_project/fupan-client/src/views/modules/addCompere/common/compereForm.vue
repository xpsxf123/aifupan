<!--
@description 主播录制配置表单：用于添加主播、编辑主播基础配置，并承接 AI 监控能力开关与保存参数映射。
注意：
1) 本表单被添加主播与主播列表编辑弹窗复用，开关显隐调整会同步影响两个入口；
2) AI 监控开关提交时需分别映射到 `isScriptQualityInspection`、`isScriptFidelityMonitor`、`isInteractionPatrol`。
-->
<template>
    <div class="compereForm ">
        <div class="bodyContainer pd-16 main-bg flex-1">
            <el-form ref="ruleForm" :model="compereForm" :rules="rules" label-width="140px" :label-position="labelPosition" size="default"
                     :validate-on-rule-change="false">
                <slot name="addUrl">
                    <div class="flex-form" style="align-items: center" v-if="!isSettingChange && isDouYin && isUrls">
                        <el-form-item :label="isDouYinNum?'抖音号':'粘贴链接'" prop="broadcastUrls">
                            <el-input
                                @blur="getByAnchorNumber"
                                style="width: 435px;"
                                :placeholder="isDouYinNum?'ctr1+v粘贴直播间账号,为保证网络安全，一次仅能添加一个直播间':'请将账号的电脑端直播地址粘贴到这里'"
                                v-model="compereForm.broadcastUrls"/>
                        </el-form-item>
                        <span class="other" @click="changeAction">
                            <i class="el-icon-sort"
                            style="transform: rotate(90deg)"></i> 其他方式</span>
                    </div>
                    <div v-else-if="!isSettingChange && isKuaiShou && isUrls">
                         <el-form-item label="快手ID" prop="ksUrls">
                            <el-input
                                @blur="getByAnchorNumber"
                                style="width: 435px;"
                                placeholder="ctr1+v粘贴快手ID,一次添加一个直播间，格式为数字、字母、下划线或减号"
                                v-model="compereForm.ksUrls"/>
                        </el-form-item>
                    </div>
                    <div v-else-if="!isSettingChange && isShipinhao">
                        <el-form-item label="视频号" prop="authorizerInfoId">
                            <slot name="sphAuthorization"></slot>
                        </el-form-item>
                    </div>
                </slot>

                <el-form-item label="直播间名称" prop="remarksName" v-if="isSettingChange">
                    <el-input
                        placeholder="直播间名称"
                        v-model="compereForm.remarksName"/>
                </el-form-item>
                <el-form-item label="行业选择" prop="tradeId">
                    <tradeId v-model="compereForm.tradeId" v-removeAriaHidden :options="tradeTreeList"
                             style="width: 435px;"></tradeId>
                </el-form-item>
                <el-form-item label="账号归属" prop="accountType">
                    <el-radio-group v-model="compereForm.accountType">
                        <el-radio :label="0" class="radio-as-checkbox">自有账号</el-radio>
                        <el-radio :label="1" v-if="!isShipinhao" class="radio-as-checkbox">同行业账号</el-radio>
                    </el-radio-group>
                    <span class="tips" v-if="!isSettingChange && isShipinhao">视频号目前仅能录制自有账号</span>
                </el-form-item>
                <div class="flex-form" v-if="showSubscribeShortVideo">
                    <el-form-item label="订阅短视频" prop="subscribeShortVideo" label-width="112px" class="flex-form-item">
                        <el-radio-group v-model="compereForm.subscribeShortVideo">
                            <el-radio :label="1" class="radio-as-checkbox">是</el-radio>
                            <el-radio :label="0" class="radio-as-checkbox">否</el-radio>
                        </el-radio-group>
                    </el-form-item>
                </div>
                <el-form-item label="直播间模式" prop="livingMode" style="max-width: 100%" v-if="!isPureDyOrKs">
                    <el-radio-group v-model="compereForm.livingMode">
                        <el-radio v-for="item in accountLivingModeList" :key="item.value" :label="item.value"
                                  class="radio-as-checkbox">
                            {{ item.label }}
                        </el-radio>
                    </el-radio-group>
                </el-form-item>


                <div v-if="compereForm.accountType===0 && !isPure && !isShipinhao" class="accountInfo">
                    <!-- <div class="notRequired">以下内容部分非必填：</div> -->
                    <el-form-item v-if="isSettingChange" label="A.账号阶段" prop="accountStage" style="max-width: 100%">
                        <el-radio-group v-model="compereForm.accountStage">
                            <el-radio v-for="item in accountStageList" :key="item.value" :label="item.value"
                                      class="radio-as-checkbox">
                                {{ item.label }}
                            </el-radio>
                        </el-radio-group>
                    </el-form-item>

                    <div class="flex-form">
                        <el-form-item :label="accountIssueLabel" prop="anchorSituation" class="flex-form-item">
                            <el-input v-model="compereForm.anchorSituation" type="textarea" :rows="5" resize="none"
                                      maxlength="500"
                                      style="width: 435px;"
                                      show-word-limit
                                      placeholder="AI越了解您的需求和困惑，提供的建议更有效，请把账号目前遇到的问题和您的需求详细的描述出来，例如：最近在线一直往下掉、曝光量越来越低，投放ROI不高，怎么提升在线，怎么提升转化"/>
                        </el-form-item>
                        <span class="tips" v-if="!isSettingChange" style="margin-left: 65px">*有助于AI分析的准确性</span>
                    </div>

                    <div v-if="isSettingChange" class="flex-form">
                        <el-form-item label="C.自动生成AI数据报告" prop="isAutoDiagnosis" label-width="180px"
                                      class="flex-form-item">
                            <el-switch
                                class="themeAsSwitch"
                                v-model="compereForm.isAutoDiagnosis"
                                inactive-text="否"
                                :active-value="1"
                                @change="onIsAiReportChange"
                                :disabled="!compereForm.tradeId"
                                :inactive-value="0"
                                active-text="是"
                                active-color="#13ce66"
                                inactive-color="#ff4949">
                            </el-switch>
                        </el-form-item>
                        <span class="tips edit" @click="()=>onIsAiReportChange(compereForm.isAutoDiagnosis,'edit')"
                              v-if="compereForm.isAutoDiagnosis===1">修改</span>
                        <span class="tips" style="color: red">*自动生成报告AI算力消耗非常巨大！<span
                            style="color: var(--color-main)">非土豪</span> 不要使用！</span>
                    </div>
                    <!-- 按排班录制 -->
                    <div class="flex-form" v-if="isDouYin">
                        <UpgradeTooltip v-slot="{ largeEnterprises }">
                            <div class="isScheduleRecord-form-item-wrap" :class="{'isScheduleRecord-form-item-wrap-disabled': !largeEnterprises}">
                                <el-form-item  :label="scheduleRecordLabel" prop="isScheduleRecord" style="max-width: 100%" label-width="126px">
                                    <el-radio-group v-model="compereForm.isScheduleRecord" :disabled="!largeEnterprises" @change="isScheduleRecordChange">
                                        <el-radio :label="1" class="radio-as-checkbox">
                                            是
                                        </el-radio>
                                        <el-radio :label="0" class="radio-as-checkbox">
                                            否
                                        </el-radio>
                                    </el-radio-group>
                                </el-form-item>
                            </div>
                        </UpgradeTooltip>
                        <span class="tips" v-if="!isSettingChange">*分段优先级：1、按排班录制分段  2、按时间点分段  3、按时长分段</span>
                    </div>
                    <!-- E.是否统计业绩： -->
                    <div class="flex-form" v-if="compereForm.isScheduleRecord === 1">
                        <el-form-item  :label="statisticsPerformanceLabel" prop="isStatisticsPerformance" style="max-width: 100%" label-width="126px">
                            <el-radio-group v-model="compereForm.isStatisticsPerformance">
                                <el-radio :label="1" class="radio-as-checkbox">
                                    是
                                </el-radio>
                                <el-radio :label="0" class="radio-as-checkbox">
                                    否
                                </el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <span class="tips" v-if="!isSettingChange">*为保证数据的统一性，同一个企业账号下，只能有一个爱复盘子账号能开启一个直播间的业绩统计功能</span>
                    </div>

                    <div v-if="isSettingChange" class="aiMonitorBox">
                        <div class="aiMonitorTitle">AI自动监控直播间话术/弹幕互动项<span class="text-colorErr font-s12">(所有功能都需消耗算力！)</span></div>
                        <div class="aiMonitorRow" :class="{ aiMonitorRowDisabled: !workshopOrAbove }">
                            <div class="aiMonitorLabel">
                                <el-tooltip effect="dark" placement="bottom-start" popper-class="aiMonitor-tooltip-popper">
                                    <div slot="content" class="aiMonitor-tooltip-content">
                                        <div style="text-decoration: underline;">什么是负面话术质检？</div>
                                        <div>负面话术质检会在每场直播间下播后，自动检测直播间话术中的以下四类话术：</div>
                                        <div>
                                            <span style="color: #f56c6c;">(1)</span>
                                            <span>崩盘话术</span>、
                                            <span style="color: #f56c6c;">(2)</span>
                                            <span>摸鱼话术</span>、
                                            <span style="color: #f56c6c;">(3)</span>
                                            <span>有损品牌的话术</span>、
                                            <span style="color: #f56c6c;">(4)</span>
                                            <span>增加售后成本的话术</span>。
                                        </div>
                                        <div style="font-weight: 700;">质检规则可以通过<span class="text-colorTheme">定制话术质检智能体</span>来定制。</div>
                                    </div>
                                    <i class="el-icon-question aiMonitorHelpIcon"></i>
                                </el-tooltip>
                                <span>AI负面话术质检：</span>
                            </div>
                            <UpgradeTooltip v-slot="{ largeEnterprises }" :largeEnterprises="workshopOrAbove" :tag-img-src="workshopTagImgSrc">
                                <el-radio-group
                                    v-model="compereForm.aiReplyScriptRestore"
                                    :disabled="!largeEnterprises"
                                    @change="onAiReplyScriptRestoreChange">
                                    <el-radio :label="1" class="radio-as-checkbox">是</el-radio>
                                    <el-radio :label="0" class="radio-as-checkbox">否</el-radio>
                                </el-radio-group>
                            </UpgradeTooltip>
                            <span class="font-s12 mg-l10">共享余量（<span :class="isAiReplyQuotaFull ? 'text-colorErr' : ''">{{ aiReplyQuotaRemain }}</span>/{{ aiReplyQuotaTotal }}）</span>
                        </div>
                        <div class="aiMonitorRow" :class="{ aiMonitorRowDisabled: !workshopOrAbove }">
                            <div class="aiMonitorLabel">
                                <el-tooltip effect="dark" placement="bottom-start" popper-class="aiMonitor-tooltip-popper">
                                    <div slot="content" class="aiMonitor-tooltip-content">
                                        <div>不知道直播间有没有按照标准直播话术脚本来直播？</div>
                                        <div>不知道直播间是不是随意发挥？</div>
                                        <div>监控直播间整场直播与标准直播话术脚本的对比还原情况，防止直播间随意发挥。</div>
                                        <div>还原度规则可以通过定制还原度监控智能体来定制。</div>
                                    </div>
                                    <i class="el-icon-question aiMonitorHelpIcon"></i>
                                </el-tooltip>
                                <span>话术还原度监控：</span>
                            </div>
                            <UpgradeTooltip v-slot="{ largeEnterprises }" :largeEnterprises="workshopOrAbove" :tag-img-src="workshopTagImgSrc">
                                <el-radio-group
                                    v-model="compereForm.scriptRestoreMonitor"
                                    :disabled="!largeEnterprises"
                                    @change="onScriptRestoreMonitorChange">
                                    <el-radio :label="1" class="radio-as-checkbox">是</el-radio>
                                    <el-radio :label="0" class="radio-as-checkbox">否</el-radio>
                                </el-radio-group>
                            </UpgradeTooltip>
                            <span class="font-s12 mg-l10">共享余量（<span :class="isScriptRestoreQuotaFull ? 'text-colorErr' : ''">{{ scriptRestoreQuotaRemain }}</span>/{{ scriptRestoreQuotaTotal }}）</span>
                            <el-button
                                v-if="Number(compereForm.scriptRestoreMonitor) === 1 && (isSettingChange || (Array.isArray(compereForm.timeAxisScript) && compereForm.timeAxisScript.length))"
                                type="text"
                                class="aiMonitorLink mg-l10"
                                @click="openScriptRestoreEditor"
                            >编辑还原度</el-button>
                        </div>
                        <div class="aiMonitorRow" :class="{ aiMonitorRowDisabled: !workshopOrAbove }">
                            <div class="aiMonitorLabel">
                                <el-tooltip effect="dark" placement="bottom-start" popper-class="aiMonitor-tooltip-popper">
                                    <div slot="content" class="aiMonitor-tooltip-content">
                                        <div>互动巡检文案：</div>
                                        <div>不知道直播间有没有及时回复高潜用户的疑问？</div>
                                        <div>不知道直播间的互动是不是随意发挥？</div>
                                        <div>巡检整场直播间用户互动和直播间的回复情况，防止直播间随意发挥。</div>
                                        <div>互动巡检规则可以通过定制互动巡检智能体来定制。</div>
                                    </div>
                                    <i class="el-icon-question aiMonitorHelpIcon"></i>
                                </el-tooltip>
                                <span>AI弹幕互动巡检：</span>
                            </div>
                            <UpgradeTooltip v-slot="{ largeEnterprises }" :largeEnterprises="workshopOrAbove" :tag-img-src="workshopTagImgSrc">
                                <el-radio-group
                                    v-model="compereForm.bulletInteractMonitor"
                                    :disabled="!largeEnterprises"
                                    @change="onBulletInteractMonitorChange">
                                    <el-radio :label="1" class="radio-as-checkbox">是</el-radio>
                                    <el-radio :label="0" class="radio-as-checkbox">否</el-radio>
                                </el-radio-group>
                            </UpgradeTooltip>
                            <span class="font-s12 mg-l10">共享余量（<span :class="isBulletInteractQuotaFull ? 'text-colorErr' : ''">{{ bulletInteractQuotaRemain }}</span>/{{ bulletInteractQuotaTotal }}）</span>
                            <span class="tips" v-if="!isSettingChange">*需要开启弹幕监控才会生效！</span>
                        </div>
                    </div>
                    <!-- <div class="flex-form">
                        <el-form-item label="D.AI负面话术质检" label-width="145px" prop="NegativeInspection" class="flex-form-item">
                            <el-switch
                                class="themeAsSwitch"
                                v-model="compereForm.NegativeInspection"
                                inactive-text="否"
                                :active-value="1"
                                @change="onIsAiReportChange"
                                :inactive-value="0"
                                active-text="是"
                                active-color="#13ce66"
                                inactive-color="#ff4949">
                            </el-switch>
                            <span class="font-s12 mg-l10">共享余量（<span class="text-colorErr">2</span>/3）</span>
                        </el-form-item>
                        <span class="tips" v-if="!isSettingChange">*您目前使用的是通用版负面话术质检智能体，需要更加精准的分析请<span class="text-colorTheme">定制质检智能体</span></span>
                    </div> -->
                </div>

                <el-form-item label="账号水平" prop="accountWaterLevel" v-if="isSettingChange && !isPureDyOrKs">
                    <el-radio-group v-model="compereForm.accountWaterLevel">
                        <el-radio v-for="item in accountWaterLevelList" :key="item.value" :label="item.value"
                                  class="radio-as-checkbox">
                            {{ item.label }}
                        </el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="流量结构" prop="accountFlow" style="max-width: 100%" v-if="!isPureDyOrKs">
                    <el-radio-group v-model="compereForm.accountFlow">
                        <el-radio
                            v-for="item in accountFlowList"
                            :key="item.value" :label="item.value"
                            class="radio-as-checkbox">
                            {{ item.label }}
                        </el-radio>
                    </el-radio-group>
                </el-form-item>
                <div class="flex-form" v-if="showRoiAccuracy">
                    <el-form-item label="ROI观察精度" prop="roiAccuracy" style="max-width: 100%" class="flex-form-item">
                        <el-radio-group v-model="compereForm.roiAccuracy">
                            <el-radio
                                v-for="item in roiAccuracyList"
                                :key="item.value"
                                :label="item.value"
                                class="radio-as-checkbox">
                                {{ item.label }}
                            </el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <span class="tips">*与您的循环话术和转化节奏有关</span>
                </div>
                <div class="flex-form" v-if="showPremiereDate">
                    <el-form-item label="首播日期" prop="premiereDate" label-width="112px" class="flex-form-item">
                        <el-date-picker
                            v-model="compereForm.premiereDate"
                            type="date"
                            format="yyyy-MM-dd"
                            value-format="yyyy-MM-dd HH:mm:ss"
                            :picker-options="premiereDatePickerOptions"
                            placeholder="请选择首播日期"
                            style="width: 220px;" />
                    </el-form-item>
                    <span class="tips" v-if="!isSettingChange">*用于补充 AI 分析背景信息</span>
                </div>
                <div class="flex-form-collapse" v-if="showMoreAiAnalysisConfig">
                    <el-collapse v-model="moreAiAnalysisActiveNames">
                        <el-collapse-item title="更多AI分析配置" name="moreAiAnalysis">
                            <div class="collapse-item-container ai-analysis-config-panel">
                                <el-form-item
                                    v-for="item in visibleAiAnalysisConfigList"
                                    :key="item.value"
                                    :label="item.label"
                                    :prop="item.value"
                                    label-width="120px"
                                    :class="['ai-analysis-config-item']">
                                    <el-radio-group
                                        v-if="item.selectType === 'single'"
                                        v-model="compereForm[item.value]">
                                        <el-radio
                                            v-for="option in item.config"
                                            :key="option.value"
                                            :label="option.value"
                                            class="radio-as-checkbox">
                                            {{ option.label }}
                                        </el-radio>
                                    </el-radio-group>
                                    <el-checkbox-group v-else v-model="compereForm[item.value]">
                                        <el-checkbox
                                            v-for="option in item.config"
                                            :key="option.value"
                                            :label="option.value"
                                            class="radio-as-checkbox">
                                            {{ option.label }}
                                        </el-checkbox>
                                    </el-checkbox-group>
                                </el-form-item>
                            </div>
                        </el-collapse-item>
                    </el-collapse>
                </div>
                <!-- TODO: 已迁移至统一取消授权入口 cancelAuthDialog -->
                <!-- <el-form-item label="巨量百应" prop="juliangAuthStatus"
                              v-if="isSettingChange&&localBuyInAuthStatus===1 && !isShipinhao">
                    <el-checkbox-group v-model="localBuyInAuthStatus" class="buy_in_selected">
                        <el-checkbox-button :label="1" :key="1" @change="cancelAuthorizeJuliang">取消授权
                        </el-checkbox-button>
                    </el-checkbox-group>
                </el-form-item> -->
                <div class="flex-form" v-if="!isPureDyOrKs && (isKuaiShou || (isDouYin && isSettingChange))">
                    <el-form-item label="开播时间" prop="recordTime" class="flex-form-item">
                        <template v-if="isKuaiShou">
                            <el-time-picker
                                v-model="compereForm.recordTime[0]"
                                value-format="HH:mm"
                                format="HH:mm"
                                placeholder="任意开始时间点"
                                style="width: 160px;"
                                :picker-options="{
                                }"
                                >
                            </el-time-picker>
                            <span class="mg-l6 mg-r6">至</span>
                            <el-time-picker
                                v-model="compereForm.recordTime[1]"
                                value-format="HH:mm"
                                format="HH:mm"
                                placeholder="任意结束时间点"
                                style="width: 160px;"
                                :picker-options="{
                                }"
                                >
                            </el-time-picker>
                        </template>
                        <template v-else-if="isSettingChange && isDouYin">
                                <el-time-picker
                                is-range
                                v-model="compereForm.recordTime"
                                range-separator="至"
                                format="HH:mm"
                                value-format="HH:mm"
                                :default-value="getDefaultTimeValue"
                                :picker-options="pickerOptions"
                                start-placeholder="开始时间"
                                end-placeholder="结束时间"
                                placeholder="选择时间范围"
                                @change="onTimeChange">
                            </el-time-picker>
                        </template>
                    </el-form-item>
                    <span class="tips" v-if="isSettingChange">*{{isKuaiShou?'默认监控全天直播，可按需调整时间段':'仅监控录制该时间段内的直播，时间越准，电脑消耗越低'}}</span>
                </div>
                <div class="flex-form" v-if="isShipinhao">

                    <el-form-item label="开播时间" prop="recordTimes" class="flex-form-item">
                        <FormItemAry v-model="compereForm.recordTimes" dataType="array" :min="1" :max="shipinhaoTimesMax" >
                            <template #default="{index}">
                                <TimePickerRange v-model="compereForm.recordTimes[index]" :key="index" :maxTimeInterval="shipinhaoTimeInterval" :index="index"  :disabledTimes="compereForm.recordTimes"></TimePickerRange>
                            </template>
                        </FormItemAry>
                        <div class="tips" style="margin: 0;">仅监控时间段内的直播，<span>间隔{{shipinhaoTimeInterval}}个小时以内</span>，<span>可同时设置{{shipinhaoTimesMax}}个监控时间段</span></div>
                    </el-form-item>
                </div>
                
                <div class="flex-form" v-if="isShipinhao">
                    <el-form-item label="按排班录制" prop="isScheduleRecord" label-width="112px" class="flex-form-item">
                        <el-switch
                            class="themeAsSwitch"
                            v-model="compereForm.isScheduleRecord"
                            inactive-text="否"
                            :active-value="1"
                            :inactive-value="0"
                            active-text="是"
                            active-color="#13ce66"
                            @change="isScheduleRecordChange"
                            inactive-color="#ff4949">
                        </el-switch>
                    </el-form-item>
                    <span class="tips" style="margin-bottom:18px" v-if="!isSettingChange">*分段优先级：1、按排班录制分段  2、按时间点分段  3、按时长分段</span>
                </div>
                <div class="flex-form" v-if="isShipinhao && compereForm.isScheduleRecord === 1">
                    <el-form-item label="是否统计业绩" prop="isStatisticsPerformance" label-width="112px" class="flex-form-item">
                        <el-switch
                            class="themeAsSwitch"
                            v-model="compereForm.isStatisticsPerformance"
                            inactive-text="否"
                            :active-value="1"
                            :inactive-value="0"
                            active-text="是"
                            active-color="#13ce66"
                            inactive-color="#ff4949">
                        </el-switch>
                    </el-form-item>
                </div>
                <div class="aiMonitorBox" v-if="isShipinhao">
                    <div class="aiMonitorTitle">AI自动监控直播间话术/弹幕互动项<span class="text-colorErr font-s12">(所有功能都需消耗算力！)</span></div>
                    <div class="aiMonitorRow" :class="{ aiMonitorRowDisabled: !workshopOrAbove }">
                        <div class="aiMonitorLabel">
                            <el-tooltip effect="dark" placement="bottom-start" popper-class="aiMonitor-tooltip-popper">
                                <div slot="content" class="aiMonitor-tooltip-content">
                                    <div style="text-decoration: underline;">什么是负面话术质检？</div>
                                    <div>负面话术质检会在每场直播间下播后，自动检测直播间话术中的以下四类话术：</div>
                                    <div>
                                        <span style="color: #f56c6c;">(1)</span>
                                        <span>崩盘话术</span>、
                                        <span style="color: #f56c6c;">(2)</span>
                                        <span>摸鱼话术</span>、
                                        <span style="color: #f56c6c;">(3)</span>
                                        <span>有损品牌的话术</span>、
                                        <span style="color: #f56c6c;">(4)</span>
                                        <span>增加售后成本的话术</span>。
                                    </div>
                                    <div style="font-weight: 700;">质检规则可以通过<span class="text-colorTheme">定制话术质检智能体</span>来定制。</div>
                                </div>
                                <i class="el-icon-question aiMonitorHelpIcon"></i>
                            </el-tooltip>
                            <span>AI负面话术质检：</span>
                        </div>
                        <UpgradeTooltip v-slot="{ largeEnterprises }" :largeEnterprises="workshopOrAbove" :tag-img-src="workshopTagImgSrc">
                            <el-radio-group
                                v-model="compereForm.aiReplyScriptRestore"
                                :disabled="!largeEnterprises"
                                @change="onAiReplyScriptRestoreChange">
                                <el-radio :label="1" class="radio-as-checkbox">是</el-radio>
                                <el-radio :label="0" class="radio-as-checkbox">否</el-radio>
                            </el-radio-group>
                        </UpgradeTooltip>
                        <span class="font-s12 mg-l10">共享余量（<span :class="isAiReplyQuotaFull ? 'text-colorErr' : ''">{{ aiReplyQuotaRemain }}</span>/{{ aiReplyQuotaTotal }}）</span>
                    </div>
                    <div class="aiMonitorRow" :class="{ aiMonitorRowDisabled: !workshopOrAbove }">
                        <div class="aiMonitorLabel">
                            <el-tooltip effect="dark" placement="bottom-start" popper-class="aiMonitor-tooltip-popper">
                                <div slot="content" class="aiMonitor-tooltip-content">
                                    <div>不知道直播间有没有按照标准直播话术脚本来直播？</div>
                                    <div>不知道直播间是不是随意发挥？</div>
                                    <div>监控直播间整场直播与标准直播话术脚本的对比还原情况，防止直播间随意发挥。</div>
                                    <div>还原度规则可以通过定制还原度监控智能体来定制。</div>
                                </div>
                                <i class="el-icon-question aiMonitorHelpIcon"></i>
                            </el-tooltip>
                            <span>话术还原度监控：</span>
                        </div>
                        <UpgradeTooltip v-slot="{ largeEnterprises }" :largeEnterprises="workshopOrAbove" :tag-img-src="workshopTagImgSrc">
                            <el-radio-group
                                v-model="compereForm.scriptRestoreMonitor"
                                :disabled="!largeEnterprises"
                                @change="onScriptRestoreMonitorChange">
                                <el-radio :label="1" class="radio-as-checkbox">是</el-radio>
                                <el-radio :label="0" class="radio-as-checkbox">否</el-radio>
                            </el-radio-group>
                        </UpgradeTooltip>
                        <span class="font-s12 mg-l10">共享余量（<span :class="isScriptRestoreQuotaFull ? 'text-colorErr' : ''">{{ scriptRestoreQuotaRemain }}</span>/{{ scriptRestoreQuotaTotal }}）</span>
                        <el-button
                            v-if="Number(compereForm.scriptRestoreMonitor) === 1 && (isSettingChange || (Array.isArray(compereForm.timeAxisScript) && compereForm.timeAxisScript.length))"
                            type="text"
                            class="aiMonitorLink mg-l10"
                            @click="openScriptRestoreEditor"
                        >编辑还原度</el-button>
                    </div>
                    <div class="aiMonitorRow" :class="{ aiMonitorRowDisabled: !workshopOrAbove }">
                        <div class="aiMonitorLabel">
                            <el-tooltip effect="dark" placement="bottom-start" popper-class="aiMonitor-tooltip-popper">
                                <div slot="content" class="aiMonitor-tooltip-content">
                                    <div>互动巡检文案：</div>
                                    <div>不知道直播间有没有及时回复高潜用户的疑问？</div>
                                    <div>不知道直播间的互动是不是随意发挥？</div>
                                    <div>巡检整场直播间用户互动和直播间的回复情况，防止直播间随意发挥。</div>
                                    <div>互动巡检规则可以通过定制互动巡检智能体来定制。</div>
                                </div>
                                <i class="el-icon-question aiMonitorHelpIcon"></i>
                            </el-tooltip>
                            <span>AI弹幕互动巡检：</span>
                        </div>
                        <UpgradeTooltip v-slot="{ largeEnterprises }" :largeEnterprises="workshopOrAbove" :tag-img-src="workshopTagImgSrc">
                            <el-radio-group
                                v-model="compereForm.bulletInteractMonitor"
                                :disabled="!largeEnterprises"
                                @change="onBulletInteractMonitorChange">
                                <el-radio :label="1" class="radio-as-checkbox">是</el-radio>
                                <el-radio :label="0" class="radio-as-checkbox">否</el-radio>
                            </el-radio-group>
                        </UpgradeTooltip>
                        <span class="font-s12 mg-l10">共享余量（<span :class="isBulletInteractQuotaFull ? 'text-colorErr' : ''">{{ bulletInteractQuotaRemain }}</span>/{{ bulletInteractQuotaTotal }}）</span>
                        <span class="tips" v-if="!isSettingChange">*需要开启弹幕监控才会生效！</span>
                    </div>
                </div>
                <div class="flex-form" v-if="!isPureDyOrKs">
                    <el-form-item label="开播/下播提醒" prop="smsTip" label-width="112px" class="flex-form-item">
                        <el-switch
                            class="themeAsSwitch"
                            v-model="compereForm.smsTip"
                            inactive-text="否"
                            :active-value="3"
                            :inactive-value="0"
                            active-text="是"
                            active-color="#13ce66"
                            inactive-color="#ff4949">
                        </el-switch>
                    </el-form-item>
                    <span class="tips" style="margin-bottom:18px" v-if="!isSettingChange">*短信提醒：短信提醒会消耗短信次数</span>
                </div>
                <div class="flex-form" v-if="!isPureDyOrKs">
                    <el-form-item label="默认识别语言" prop="engSerViceType" label-width="112px" class="flex-form-item">
                        <el-select v-model="compereForm.engSerViceType" placeholder="请选择识别语言">
                            <el-option v-for="item in engSerViceTypeList" :key="item.value" :label="item.label" :value="item.value"></el-option>
                        </el-select>
                    </el-form-item>
                </div>
                <!-- PURE模式：更多个性化录制配置项直接展示，与账号归属同级 -->
                <RecordConfig v-if="isPureDyOrKs" v-model="compereForm"
                              :isSettingChange="isSettingChange" :showAutoAnalysis="false"/>
                <!-- 非PURE模式：保持原有折叠面板 -->
                <div class="flex-form-collapse" v-if="!isPureDyOrKs">
                    <el-collapse>
                        <el-collapse-item title="更多个性化录制配置" name="1">
                            <div class="collapse-item-container">
                                <RecordConfig v-model="compereForm"
                                              :isSettingChange="isSettingChange" :showAutoAnalysis="true"/>
                            </div>
                        </el-collapse-item>
                    </el-collapse>
                </div>

                <el-form-item label-width="0px" v-if="!isSettingChange">
                    <afp-button type="primary" :plain="false" size="default" style="margin-top: 16px;" @click="addCompereForm"
                               :loading="loading">添加到直播间列表
                    </afp-button>
                    <div v-if="isShipinhao" class="font-s12 text-colorErr">请用微信视频号超级管理员扫码授权，且授权时，所有授权项需全部勾选</div>
                </el-form-item>
                <el-form-item label-width="0" style="text-align: left" v-else>
                    <afp-button size="default"  @click="handleCancel">取消
                    </afp-button>
                    <afp-button type="primary" :plain="false" size="default"  @click="addCompereForm">保存
                    </afp-button>
                </el-form-item>
            </el-form>
        </div>

        <ScriptRestorationDrawer
            :visible.sync="scriptRestoreDrawerVisible"
            :form="compereForm"
            scene="anchorConfig" />

        <QrCodeDialog ref="qrCodeDialog" />
    </div>
</template>

<script>
/**
 * @description 主播录制配置表单脚本：负责表单初始化、AI 监控开关校验、额度校验与最终保存参数组装。
 * 注意：互动巡检开关与话术质检共用该表单，但提交字段需要单独映射为 `isInteractionPatrol`。
 */
import tradeId from '@/components/tradeId/index.vue'
import AddCompereStatusDialog from './addCompereStatusDialog.vue'
import {cloneDeep,omit} from "lodash";
import FormItemAry from '@/components/formItemAry/index.vue'
import RecordConfig from './recordConfig.vue'
import TimePickerRange from '@/components/formItemAry/time-picker-range.vue'
import { PLATFORM_ENUM, VERSION_TYPE } from '@/enum'
import myUtils from "@/utils/utils";
import ScriptRestorationDrawer from '@/components/scriptRestoration/ScriptRestorationDrawer.vue'
import UpgradeTooltip from '@/components/upgradeTooltip/index.vue'
import QrCodeDialog from '@/components/qrCodeGuard/QrCodeDialog/index.vue'
import { QR_CODE_DIALOG_TYPE } from '@/enum/qrCodeDialogEnum'

/**
 * 主播录制配置表单
 */
export default {
    inject: ['appVnode'],
    components: {
        tradeId, 
        AddCompereStatusDialog,
        FormItemAry,
        TimePickerRange,
        RecordConfig,
        ScriptRestorationDrawer,
        UpgradeTooltip,
        QrCodeDialog
    },
    props: {
        tradeTreeList: {
            type: Array,
            default: () => []
        },
        isSettingChange: {
            type: Boolean,
            default: false
        },
        resultData: {
            type: Object,
            default: () => {
            }
        },
        rulesConfig:{
            type: Object,
            default: () => {return {}}
        },
        tabName:{
            type: String,
            default: ''
        },
        notUrls:{
            type: Boolean,
            default: false
        },
        labelPosition:{
            type: String,
            default: 'right'
        },
        defaultTredeId: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            workshopTagImgSrc: require('@/assets/imgs/version-gzs.png'),
            compereForm: {
                broadcastUrls: '',
                remarksName: '',
                tradeId: '',
                accountType: null,
                subscribeShortVideo: 1,
                livingMode:'',
                accountStage: '',
                roster:'',
                accountFlow: '',
                roiAccuracy: '',
                accountWaterLevel: '',
                anchorSituation: '',
                premiereDate: '',
                livingTarget: '',
                marketing: '',
                optimizeDirection: [],
                learning: [],
                recordTime: ((this.tabName === 'kuaishou') || (this.resultData?.platform === PLATFORM_ENUM.kuaishou)) ? ['00:00', '23:59'] : ['', ''],
                recordTimes: [],
                smsTip: 0,
                isScheduleRecord: 0,
                isStatisticsPerformance: 0,
                engSerViceType:'',
                segmentTimePoints: [],
                recordDefinition: -1,
                recordLimitValue: 0,
                recordDuration: 30,
                recordLimitType: -1,
                isAutoAnalysis: 1,
                autoDeleteTime: '-1',
                deleteContent: '',

                ksUrls: '',
                minTime: '',
                maxTime: '',
                authorizerInfoId:'',

                aiReplyScriptRestore: 0,
                scriptRestoreMonitor: 0,
                bulletInteractMonitor: 0,
                scriptRestoreMode: '',
                scriptRestoreLoopDuration: 0,
                scriptRestoreTalkSpeed: 280,
                scriptRestoreReferenceScript: '',
                standardScriptId: null,
                timeAxisScript: []
            },
            aiMonitorOrigin: {
                aiReplyScriptRestore: null,
                scriptRestoreMonitor: null,
                bulletInteractMonitor: null
            },
            aiMonitorTouched: {
                aiReplyScriptRestore: false,
                scriptRestoreMonitor: false,
                bulletInteractMonitor: false
            },
            // 视频号开播时间配置
            shipinhaoTimesMax: 6,
            shipinhaoTimeInterval: 4,
            loading: false,
            accountStageList: [],
            accountLivingModeList: [],// 直播间 模式列表
            accountWaterLevelList: [],
            accountFlowList: [],
            livingTargetList: [],
            marketingList: [],
            optimizeDirectionList: [],
            learningList: [],
            roiAccuracyList: [],
            engSerViceTypeList:[],
            isDouYinNum: true,
            localBuyInAuthStatus: 0,
            // 用于存储上一次的时间选择，用于判断是开始时间还是结束时间发生变化
            previousRecordTime: ['', ''],
            // 防抖控制变量，用于控制警告弹窗的触发频率
            messageDebounceTimer: null,
            scheduleRecordPrev: 0,
            scheduleRecordInternalChange: false,
            // 时间间隔配置（小时）
            maxTimeInterval: 16,
            // 最小时间间隔配置（分钟）
            minTimeInterval: 10,
            moreAiAnalysisActiveNames: [],
            scriptRestoreDrawerVisible: false,
            aiReplyScriptRestoreInternalChange: false,
            scriptRestoreMonitorInternalChange: false,
            bulletInteractMonitorInternalChange: false,
            aiMonitorQuota: {
                aiNegativeInspection: { used: 0, total: 0 },
                scriptRestoreMonitor: { used: 0, total: 0 },
                bulletInteractMonitor: { used: 0, total: 0 }
            }
        }
    },
    computed: {
        workshopOrAbove() {
            const level = Number(this.$store?.getters?.getPackageLevel ?? 0)
            if (level === -1) return true
            return level >= 15
        },
        aiReplyQuotaUsed() {
            return Number((this.aiMonitorQuota.aiNegativeInspection || {}).used || 0)
        },
        aiReplyQuotaRemain() {
            const {used = 0, total = 0} = this.aiMonitorQuota.aiNegativeInspection || {}
            return Math.max(0, total - used)
        },
        aiReplyQuotaTotal() {
            return (this.aiMonitorQuota.aiNegativeInspection || {}).total || 0
        },
        isAiReplyQuotaFull() {
            return this.aiReplyQuotaRemain <= 0
        },
        scriptRestoreQuotaUsed() {
            return Number((this.aiMonitorQuota.scriptRestoreMonitor || {}).used || 0)
        },
        scriptRestoreQuotaRemain() {
            const {used = 0, total = 0} = this.aiMonitorQuota.scriptRestoreMonitor || {}
            return Math.max(0, total - used)
        },
        scriptRestoreQuotaTotal() {
            return (this.aiMonitorQuota.scriptRestoreMonitor || {}).total || 0
        },
        isScriptRestoreQuotaFull() {
            return this.scriptRestoreQuotaRemain <= 0
        },
        bulletInteractQuotaUsed() {
            return Number((this.aiMonitorQuota.bulletInteractMonitor || {}).used || 0)
        },
        bulletInteractQuotaRemain() {
            const {used = 0, total = 0} = this.aiMonitorQuota.bulletInteractMonitor || {}
            return Math.max(0, total - used)
        },
        bulletInteractQuotaTotal() {
            return (this.aiMonitorQuota.bulletInteractMonitor || {}).total || 0
        },
        isBulletInteractQuotaFull() {
            return this.bulletInteractQuotaRemain <= 0
        },
        showSubscribeShortVideo() {
            // TODO: 后续版本开放，当前版本暂时注释
            return false
            // return !this.isSettingChange && !this.isShipinhao
        },
        showPremiereDate() {
            return true
        },
        showMoreAiAnalysisConfig() {
            return true
        },
        accountIssueLabel() {
            return this.isSettingChange ? 'B.账号问题和困惑' : '账号问题和困惑'
        },
        scheduleRecordLabel() {
            return this.isSettingChange ? 'D.按排班录制' : '按排班录制'
        },
        statisticsPerformanceLabel() {
            return this.isSettingChange ? 'E.是否统计业绩' : '是否统计业绩'
        },
        premiereDatePickerOptions() {
            return {
                disabledDate(time) {
                    return time.getTime() > Date.now()
                }
            }
        },
        /**
         * @description 根据账号归属切换“更多AI分析配置”的展示项，自有账号展示优化方向，同行账号展示学习方向。
         * @returns {Array<{label: string, value: string, selectType: string, config: Array}>}
         */
        visibleAiAnalysisConfigList() {
            const accountType = Number(this.compereForm?.accountType ?? 0)
            const baseList = [
                {
                    label: '直播目标',
                    value: 'livingTarget',
                    selectType: 'single',
                    config: this.livingTargetList
                },
                {
                    label: '营销组件',
                    value: 'marketing',
                    selectType: 'single',
                    config: this.marketingList
                }
            ]
            if (accountType === 1) {
                baseList.push({
                    label: '学习方向（多选）',
                    value: 'learning',
                    selectType: 'multiple',
                    hint: true,
                    config: this.learningList
                })
            } else {
                baseList.push({
                    label: '优化方向\n(多选)',
                    value: 'optimizeDirection',
                    selectType: 'multiple',
                    // hint: true,
                    config: this.optimizeDirectionList
                })
            }
            return baseList.filter(item => Array.isArray(item.config) && item.config.length)
        },
        rules() {
            return {
                broadcastUrls: [
                    {
                        required: true,
                        message: this.isDouYinNum ? '请填写抖音号' : '请填写/粘贴电脑端直播地址',
                        trigger: 'change'
                    },
                    {
                        pattern: this.isDouYinNum ? /^[a-zA-Z0-9_.]{3,23}$/ : /^(https?:\/\/)?([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})(:[0-9]{1,5})?(\/[^\s]*)?$/,
                        message: this.isDouYinNum ? '抖音号格式不正确' : '直播间地址不正确',
                        trigger: ['change', 'blur']
                    }
                ],
                ksUrls:[
                    {
                        required: true,
                        message:'请填写快手ID',
                        trigger: 'change'
                    },
                    {
                        pattern: /^[a-zA-Z0-9_-]+$/,
                        message: '快手ID格式不正确',
                        trigger: ['change', 'blur']
                    }
                ],
                authorizerInfoId:[
                    {
                        required: true,
                        validator: (rule, value, callback) => {
                            if(!this.compereForm.authorizerInfoId){
                                return callback(new Error('请授权视频号'));
                            }
                            callback();
                        },
                        trigger: ['change','blur']
                    }
                ],
                recordTime:this.isKuaiShou?[{
                    type: 'array',
                    required: false,
                    validator: (rule, value, callback) => {
                        const timeValue = Array.isArray(value) ? value : ['', '']
                        const hasAnyTime = timeValue.some(item => !!item)
                        if (!hasAnyTime) {
                            callback();
                            return;
                        }
                        if(timeValue.some(item=>!item)){
                            callback(new Error('请完整选择监控时间'));
                            return;
                        }
                        const [startTime, endTime] = timeValue;
                        if (!startTime || !endTime || startTime.trim() === '' || endTime.trim() === '') {
                            callback();
                            return;
                        }
                        callback();
                    },
                    trigger: 'change'
                }]:[],
                anchorSituation:{
                    required: true,
                    message:'账号问题是必选项',
                    trigger: 'change'
                },
                premiereDate: [
            {
                required: false,
                message: '请选择首播日期',
                trigger: 'change'
            }
        ],
                tradeId: [{required: !this.isPureDyOrKs, message: '请选择直播间行业', trigger: 'change'}],
                accountType: [{required: true, message: '请选择账号归属', trigger: 'change'}],
                accountFlow: [{required: true, message: '请选择流量结构', trigger: 'change'}],
                roiAccuracy: [{
                    validator: (rule, value, callback) => {
                        if (!this.showRoiAccuracy) {
                            callback()
                            return
                        }
                        if (value === '' || value === null || value === undefined) {
                            callback(new Error('请选择ROI观察精度'))
                            return
                        }
                        callback()
                    },
                    trigger: 'change'
                }],
                recordTimes: [
                    {required: true, message: '请选择监控时间段', trigger: 'change', validator: (rule, value, callback) => {
                        const isComplete = Array.isArray(value) && value.length > 0 && value.every(times => {
                            return Array.isArray(times) && times.length === 2 && times.every(item => !!item);
                        });
                        if (isComplete) {
                            callback();
                        } else {
                            callback(new Error('请选择监控时间段'));
                        }
                    }}      
                ],
                segmentTimePoints: [
                    {
                        validator: this.validateTimeSegmentation,
                        trigger: 'change'
                    }
                ],
                ...this.rulesConfig,
            }
        },
        isDouYin () {
            return this.tabName === 'douyin' || this.resultData?.platform === PLATFORM_ENUM.douyin
        },
        isKuaiShou () {
            return this.tabName === 'kuaishou' || this.resultData?.platform === PLATFORM_ENUM.kuaishou
        },
        isShipinhao(){
            return this.tabName === 'shipinhao' || this.resultData?.platform === PLATFORM_ENUM.shipinhao
        },
        getDefaultTimeValue(){
            return this.isKuaiShou ? [new Date(2025, 5, 20, 10, 0), new Date(2025, 5, 20, 12, 0)] : [new Date(2025, 5, 20, 6, 0), new Date(2025, 5, 20, 22, 0)];
        },
        isUrls(){
            return !this.notUrls
        },
        isPure() {
            return this.$store.getters.getVersionType === VERSION_TYPE.PURE
        },
        isPurePackage() {
            return this.$store.getters.isPure
        },
        isPureDyOrKs() {
            return this.isPure && (this.isDouYin || this.isKuaiShou)
        },
        selectedAccountFlowOption() {
            return this.accountFlowList.find(item => String(item?.value) === String(this.compereForm.accountFlow))
        },
        showRoiAccuracy() {
            if (this.isPureDyOrKs) return false
            if (Number(this.compereForm.accountType) !== 0) return false
            const label = String(this.selectedAccountFlowOption?.label || '').replace(/\s+/g, '')
            if (!label) return false
            return !label.includes('纯自然流')
        },
        pickerOptions() {
            if(this.isKuaiShou){
                return {};
            }
            return {
                selectableRange: '00:00:00 - 23:59:59',
                format: 'HH:mm',
            };
        }
    },
    created() {
        this.loading = false
    },
    mounted() {
        this.$nextTick(()=>{
            if(this.isShipinhao){
                this.$set(this.compereForm, 'accountType', 0)
            }
            if(this.isPureDyOrKs){
                this.$set(this.compereForm, 'smsTip', 0)
                if(!this.compereForm.tradeId){
                    this.$set(this.compereForm, 'tradeId', '1')
                }
            }
        })
    },
    watch: {
        defaultTredeId: {
            handler(newVal, oldVal) {
                if (newVal !== oldVal) {
                    this.compereForm.tradeId = newVal
                }
            },
            immediate: true
        },
        '$route'(to, from) {
            this.$nextTick(() => {
                this.initForm()
            })
        },
        resultData: {
            handler(val) {
                if (!this.isSettingChange) this.existAnchor()
                const codes = ['account_stage', 'account_water_level', 'account_flow', 'living_mode', 'eng_ser_vice_type', 'roi_accuracy', 'living_target', 'marketing', 'optimize_direction', 'learning']
                this.getDictDataListByCodes(codes.toString())

                if (this.isPure) {
                    this.compereForm.isAutoAnalysis = 0
                }

                if (val) {
                    const aiReplyScriptRestore = val.aiReplyScriptRestore ?? val.isScriptQualityInspection
                    const scriptRestoreMonitor = val.scriptRestoreMonitor ?? val.isScriptFidelityMonitor
                    const bulletInteractMonitor = val.bulletInteractMonitor ?? val.isInteractionPatrol
                    const roiAccuracy = this.normalizeRoiAccuracyValue(val.roiAccuracy)
                    this.compereForm = {
                        ...this.compereForm,
                        ...val,
                        ...(val.subscribeShortVideo === undefined ? {} : { subscribeShortVideo: Number(val.subscribeShortVideo) }),
                        ...(val.premiereDate === undefined ? {} : { premiereDate: val.premiereDate }),
                        ...(val.livingTarget === undefined ? {} : { livingTarget: val.livingTarget }),
                        ...(val.marketing === undefined ? {} : { marketing: val.marketing }),
                        ...(val.optimizeDirection === undefined ? {} : { optimizeDirection: myUtils.normalizeVal(val.optimizeDirection) }),
                        ...(val.learning === undefined ? {} : { learning: myUtils.normalizeVal(val.learning) }),
                        ...(aiReplyScriptRestore === undefined || aiReplyScriptRestore === null ? {} : { aiReplyScriptRestore }),
                        ...(scriptRestoreMonitor === undefined || scriptRestoreMonitor === null ? {} : { scriptRestoreMonitor }),
                        ...(bulletInteractMonitor === undefined || bulletInteractMonitor === null ? {} : { bulletInteractMonitor }),
                        ...(val.roiAccuracy === undefined ? {} : { roiAccuracy }),
                        recordLimitValue: val.recordLimitValue > 0 ? 1 : 0,
                        recordDuration: val.recordLimitValue > 0 ? val.recordLimitValue : 30,
                    }
                    this.aiMonitorOrigin = {
                        aiReplyScriptRestore: (aiReplyScriptRestore === undefined || aiReplyScriptRestore === null) ? null : Number(aiReplyScriptRestore),
                        scriptRestoreMonitor: (scriptRestoreMonitor === undefined || scriptRestoreMonitor === null) ? null : Number(scriptRestoreMonitor),
                        bulletInteractMonitor: (bulletInteractMonitor === undefined || bulletInteractMonitor === null) ? null : Number(bulletInteractMonitor)
                    }
                    this.aiMonitorTouched = {
                        aiReplyScriptRestore: false,
                        scriptRestoreMonitor: false,
                        bulletInteractMonitor: false
                    }
                    if (this.isPure) {
                        this.compereForm.accountStage = ''
                        this.compereForm.isScheduleRecord=''
                        this.compereForm.anchorSituation = ''
                    }
                    this.localBuyInAuthStatus = val.juliangAuthStatus
                }
                this.scheduleRecordPrev = Number(this.compereForm?.isScheduleRecord || 0)
                this.loadAiMonitorQuota()
                const secUid = this.resolveCurrentSecUid(val)
                if (this.isSettingChange && secUid) {
                    this.loadAnchorBasicConfig({ id: secUid, secUid })
                }
            },
            deep: true,
            immediate: true
        },
        'compereForm.tradeId': {
            async handler(newVal, oldVal) {
                if ([null, undefined].includes(newVal)) {
                    await this.$refs.ruleForm.validateField('tradeId')
                }
                if (newVal !== oldVal) {
                }
            },
            deep: true,
        },
        'compereForm.accountType': {
            handler(newVal, oldVal) {
                if (newVal !== oldVal && newVal === 0 && !this.isPure) {
                    this.compereForm = {
                        ...this.compereForm,
                        isAutoAnalysis: 1,
                        learning: []
                    }
                } else if (newVal !== oldVal && newVal === 1) {
                    this.compereForm = {
                        ...this.compereForm,
                        optimizeDirection: []
                    }
                }
                if (Number(newVal) !== 0 && this.compereForm.roiAccuracy !== '') {
                    this.compereForm = {
                        ...this.compereForm,
                        roiAccuracy: ''
                    }
                }
                this.$nextTick(() => {
                    this.$refs.ruleForm?.clearValidate('roiAccuracy')
                })
            },
            deep: true,
        },
        'compereForm.accountFlow': {
            handler() {
                if (!this.showRoiAccuracy && this.compereForm.roiAccuracy !== '') {
                    this.compereForm = {
                        ...this.compereForm,
                        roiAccuracy: ''
                    }
                }
                this.$nextTick(() => {
                    this.$refs.ruleForm?.clearValidate('roiAccuracy')
                })
            },
            deep: true,
        },
        tabName() {
            this.initForm()
        },
        /**
         * 监听录制时间变化，进行时间范围检查和自动纠正
         * @param {Array} newVal - 新的时间值
         * @param {Array} oldVal - 旧的时间值
         */
        'compereForm.recordTime': {
            handler(newVal, oldVal) {
                if (this.isKuaiShou) {
                    return
                }
                // 如果新值存在且是数组且长度为2
                if (newVal && Array.isArray(newVal) && newVal.length === 2) {
                    const [startTime, endTime] = newVal;
                    // 如果开始时间和结束时间都存在，进行检查
                    if (startTime && endTime && startTime.trim() !== '' && endTime.trim() !== '') {
                        this.checkAndCorrectTimeRange(startTime, endTime);
                    }
                }
            },
            deep: true
        },
        'compereForm.segmentTimePoints': {
            handler() {
                this.$nextTick(() => {
                    this.$refs.ruleForm?.validateField('segmentTimePoints');
                });
            },
            deep: true
        }
    },
    methods: {
        resolveCurrentSecUid(input = {}) {
            const candidates = [
                input?.secUid,
                input?.SecUid,
                input?.anchorUrlSecUid,
                input?.anchorUrlSecuid,
                input?.anchorInfo?.secUid,
                input?.anchorInfo?.SecUid,
                this.compereForm?.secUid,
                this.resultData?.secUid,
                this.resultData?.SecUid,
                this.resultData?.anchorUrlSecUid,
                this.resultData?.anchorUrlSecuid
            ]
            for (const item of candidates) {
                if (item !== undefined && item !== null && String(item).trim()) {
                    return String(item).trim()
                }
            }
            return ''
        },
        /**
         * @description 统一将 ROI 观察精度转换为字符串，避免单选框因数值类型不一致导致回显异常。
         * @param {*} value ROI 观察精度原始值
         * @returns {string}
         */
        normalizeRoiAccuracyValue(value) {
            if (value === undefined || value === null || value === '') return ''
            return String(value)
        },
        /**
         * @description 统一清洗直播稿文本，兼容接口里可能返回的转义换行。
         * @param {string} text 直播稿文本
         * @returns {string}
         */
        normalizeScriptRestoreText(text) {
            if (text === null || text === undefined) return ''
            return String(text)
                .replace(/\\r\\n/g, '\n')
                .replace(/\\n/g, '\n')
                .replace(/\r\n/g, '\n')
        },
        /**
         * @description 获取开播时间默认值，快手默认展示全天时间段，其余平台保持空值。
         * @returns {string[]}
         */
        getDefaultRecordTime() {
            return this.isKuaiShou ? ['00:00', '23:59'] : ['', '']
        },
        /**
         * @description 归一化标准直播稿时间轴，保证提交与回显结构一致。
         * @param {Array<Object>} list 标准直播稿时间轴
         * @returns {Array<Object>}
         */
        normalizeTimeAxisScript(list = []) {
            return (Array.isArray(list) ? list : [])
                .filter(item => item && (item.timeRange || item.title || item.content))
                .map((item) => ({
                    timeRange: item?.timeRange || '',
                    title: this.normalizeScriptRestoreText(item?.title || ''),
                    content: this.normalizeScriptRestoreText(item?.content || '')
                }))
        },
        /**
         * @description 将标准直播稿详情接口返回的数据完整回填到表单，供基础设置保存时复用。
         * @param {Object} data 标准直播稿详情
         * @returns {void}
         */
        applyStandardScriptDetailToForm(data = {}) {
            const speechMode = Number(data?.speechMode)
            const scriptRestoreMode = speechMode === 1 ? 1 : speechMode === 0 ? 2 : ''
            const timeAxisScript = this.normalizeTimeAxisScript(data?.timeAxisScript || [])
            const standardScriptId = data?.standardScriptId ?? data?.scriptId ?? this.compereForm.standardScriptId ?? null
            this.compereForm = {
                ...this.compereForm,
                scriptRestoreMode,
                scriptRestoreLoopDuration: Number(data?.cycleDurationMinutes || 0),
                scriptRestoreTalkSpeed: Number(data?.speechSpeed || 0),
                scriptRestoreReferenceScript: this.normalizeScriptRestoreText(data?.referenceScript || ''),
                standardScriptId,
                timeAxisScript
            }
        },
        /**
         * @description 编辑直播间时，补拉已保存的标准直播稿详情，并沉淀到外层表单。
         * @param {Object} params 查询参数
         * @returns {Promise<void>}
         */
        async loadStandardScriptDetail(params = {}) {
            if (!this.$httpBack?.scriptMonitor?.getStandardScript) return
            const secUid = this.resolveCurrentSecUid(params)
            const rawAnchorUrlUserId = params?.anchorUrlUserId
                ?? this.resultData?.anchorUrlUserId
                ?? this.resultData?.anchor_url_user_id
                ?? this.compereForm?.anchorUrlUserId
                ?? this.compereForm?.anchor_id
            const anchorUrlUserId = Number(rawAnchorUrlUserId || 0)
            if (!secUid && !anchorUrlUserId) return
            try {
                const res = await this.$httpBack.scriptMonitor.getStandardScript({
                    ...(secUid ? { secUid } : {}),
                    ...(anchorUrlUserId > 0 ? { anchorUrlUserId } : {})
                })
                if (res?.code !== 0) return
                const data = res?.data || {}
                if (!data?.hasScript) return
                this.applyStandardScriptDetailToForm(data)
            } catch (e) {
            }
        },
        /**
         * @description 组装基础设置保存接口需要的直播稿字段。
         * @returns {Object}
         */
        buildScriptRestoreSubmitPayload() {
            const scriptType = Number(this.compereForm.scriptRestoreMode || 0)
            const speechMode = scriptType === 1 ? 1 : scriptType === 2 ? 0 : null
            const speechSpeed = Number(this.compereForm.scriptRestoreTalkSpeed || 0)
            const cycleDurationMinutes = Number(this.compereForm.scriptRestoreLoopDuration || 0)
            const referenceScript = this.normalizeScriptRestoreText(this.compereForm.scriptRestoreReferenceScript || '')
            const payload = {
                scriptRestoreMode: this.compereForm.scriptRestoreMode,
                scriptRestoreLoopDuration: this.compereForm.scriptRestoreLoopDuration,
                scriptRestoreTalkSpeed: this.compereForm.scriptRestoreTalkSpeed,
                scriptRestoreReferenceScript: referenceScript,
                speechMode,
                speechSpeed,
                referenceScript
            }
            if (speechMode === 1) {
                payload.cycleDurationMinutes = cycleDurationMinutes
            }
            if (this.compereForm.standardScriptId) {
                payload.standardScriptId = this.compereForm.standardScriptId
            }
            const timeAxisScript = this.normalizeTimeAxisScript(this.compereForm.timeAxisScript || [])
            if (timeAxisScript.length) {
                payload.timeAxisScript = timeAxisScript
            }
            return payload
        },
        openQrCodeDialog(type) {
            this.$refs.qrCodeDialog?.show?.({ type })
        },
        openScriptRestoreEditor() {
            if (Number(this.compereForm.scriptRestoreMonitor) !== 1) return
            const secUid = this.resolveCurrentSecUid()
            if (secUid) {
                this.$set(this.compereForm, 'secUid', secUid)
            }
            this.scriptRestoreDrawerVisible = true
        },
        async onAiReplyScriptRestoreChange(val) {
            if (this.aiReplyScriptRestoreInternalChange) return
            this.aiMonitorTouched.aiReplyScriptRestore = true
            if (val !== 1) return
            if (!this.workshopOrAbove) {
                this.aiReplyScriptRestoreInternalChange = true
                this.$set(this.compereForm, 'aiReplyScriptRestore', 0)
                this.$nextTick(() => {
                    this.aiReplyScriptRestoreInternalChange = false
                })
                return
            }
            await this.loadAiMonitorQuota()
            const guardType = (this.aiReplyQuotaTotal <= 0 || this.isAiReplyQuotaFull)
                ? QR_CODE_DIALOG_TYPE.AI_SPEECH_QC_QUOTA
                : false
            if (!guardType) return
            this.openQrCodeDialog(guardType)
            this.aiReplyScriptRestoreInternalChange = true
            this.$set(this.compereForm, 'aiReplyScriptRestore', 0)
            this.$nextTick(() => {
                this.aiReplyScriptRestoreInternalChange = false
            })
        },
        async onScriptRestoreMonitorChange(val) {
            if (this.scriptRestoreMonitorInternalChange) return
            this.aiMonitorTouched.scriptRestoreMonitor = true
            if (val === 1) {
                if (!this.workshopOrAbove) {
                    this.scriptRestoreMonitorInternalChange = true
                    this.$set(this.compereForm, 'scriptRestoreMonitor', 0)
                    this.$nextTick(() => {
                        this.scriptRestoreMonitorInternalChange = false
                    })
                    return
                }
                await this.loadAiMonitorQuota()
                const guardType = (this.scriptRestoreQuotaTotal <= 0 || this.isScriptRestoreQuotaFull)
                    ? QR_CODE_DIALOG_TYPE.AI_SCRIPT_RESTORE_QUOTA
                    : false
                if (guardType) {
                    this.openQrCodeDialog(guardType)
                    this.scriptRestoreMonitorInternalChange = true
                    this.$set(this.compereForm, 'scriptRestoreMonitor', 0)
                    this.$nextTick(() => {
                        this.scriptRestoreMonitorInternalChange = false
                    })
                    return
                }
                this.scriptRestoreDrawerVisible = true;
            } else {
                this.scriptRestoreDrawerVisible = false;
                this.$set(this.compereForm, 'scriptRestoreMode', '');
                this.$set(this.compereForm, 'scriptRestoreLoopDuration', 0);
                this.$set(this.compereForm, 'scriptRestoreTalkSpeed', 280);
                this.$set(this.compereForm, 'scriptRestoreReferenceScript', '');
                this.$set(this.compereForm, 'standardScriptId', null);
                this.$set(this.compereForm, 'timeAxisScript', []);
            }
        },
        async onBulletInteractMonitorChange(val) {
            if (this.bulletInteractMonitorInternalChange) return
            this.aiMonitorTouched.bulletInteractMonitor = true
            if (val !== 1) return
            if (!this.workshopOrAbove) {
                this.bulletInteractMonitorInternalChange = true
                this.$set(this.compereForm, 'bulletInteractMonitor', 0)
                this.$nextTick(() => {
                    this.bulletInteractMonitorInternalChange = false
                })
                return
            }
            await this.loadAiMonitorQuota()
            const guardType = (this.bulletInteractQuotaTotal <= 0 || this.isBulletInteractQuotaFull)
                ? QR_CODE_DIALOG_TYPE.AI_INTERACTION_INSPECT_QUOTA
                : false
            if (!guardType) return
            this.openQrCodeDialog(guardType)
            this.bulletInteractMonitorInternalChange = true
            this.$set(this.compereForm, 'bulletInteractMonitor', 0)
            this.$nextTick(() => {
                this.bulletInteractMonitorInternalChange = false
            })
        },
        generateStandardScript() {
            if (this.compereForm.scriptRestoreMonitor !== 1) return;
            if (!this.compereForm.scriptRestoreMode) {
                return this.$message.warning('请选择话术模式');
            }
            if (!this.compereForm.scriptRestoreReferenceScript?.trim()) {
                return this.$message.warning('请录入参考直播脚本');
            }
            if (this.compereForm.scriptRestoreMode === 1 && !this.compereForm.scriptRestoreLoopDuration) {
                return this.$message.warning('请填写循环话术预计时长');
            }
            this.$emit('generateStandardScript', {
                mode: this.compereForm.scriptRestoreMode,
                loopDuration: this.compereForm.scriptRestoreLoopDuration,
                talkSpeed: this.compereForm.scriptRestoreTalkSpeed,
                referenceScript: this.compereForm.scriptRestoreReferenceScript
            });
        },
        isScheduleRecordChange(val) {
            const nextVal = Number(val || 0)
            if (this.scheduleRecordInternalChange) {
                this.scheduleRecordInternalChange = false
                return
            }

            if (!nextVal) {
                this.scheduleRecordPrev = 0
                this.compereForm.isStatisticsPerformance = 0
                return
            }

            this.scheduleRecordInternalChange = true
            this.$set(this.compereForm, 'isScheduleRecord', Number(this.scheduleRecordPrev || 0))
            this.$nextTick(() => {
                this.scheduleRecordInternalChange = false
            })

            this.$confirm('开启了按排班录制后，<span style="color: #f56c6c;">不排班就不会录制</span>，是否确认按排班录制？', '提示', {
                confirmButtonText: '确认',
                cancelButtonText: '取消',
                type: 'warning',
                dangerouslyUseHTMLString: true,
                closeOnClickModal: false,
                closeOnPressEscape: false
            }).then(() => {
                this.scheduleRecordInternalChange = true
                this.$set(this.compereForm, 'isScheduleRecord', 1)
                this.scheduleRecordPrev = 1
                this.$nextTick(() => {
                    this.scheduleRecordInternalChange = false
                })
            }).catch(() => {
                this.scheduleRecordInternalChange = true
                this.$set(this.compereForm, 'isScheduleRecord', Number(this.scheduleRecordPrev || 0))
                if (!Number(this.scheduleRecordPrev || 0)) this.compereForm.isStatisticsPerformance = 0
                this.$nextTick(() => {
                    this.scheduleRecordInternalChange = false
                })
            })
        },
        /**
         * 校验分段时间点是否重复
         * @param {Object} rule 校验规则对象
         * @param {Array<string>} value 分段时间点数组
         * @param {Function} callback 校验回调
         * @returns {void}
         */
        validateTimeSegmentation(rule, value, callback) {
            const selectedTimes = (Array.isArray(value) ? value : [])
                .filter(item => !!item);

            if (!selectedTimes.length) {
                callback();
                return;
            }

            const uniqueTimes = new Set(selectedTimes);
            if (uniqueTimes.size !== selectedTimes.length) {
                callback(new Error('分段时间点不能重复'));
                return;
            }

            callback();
        },
        setAuthorizerInfoId(id){
            this.compereForm.authorizerInfoId = id
            this.$nextTick(()=>{
                this.$refs.ruleForm?.validateField('authorizerInfoId');
            })
        },
        async existAnchor() {
           try {
               const result = await this.$httpClient.compere.existAnchor()
               if (result.code !== 0) return
               this.compereForm = {
                   ...this.compereForm,
                   smsTip: (this.isPure && this.isDouYin) ? 0 : (result.data ? 0 : 3)
               }
           }catch (e) {}
        },
        async getByAnchorNumber() {
            if (this.compereForm.broadcastUrls.trim() === '' && this.compereForm.ksUrls.trim() === '') return
            try {
                if (!this.isSettingChange || ((this.isDouYin && this.isDouYinNum) || this.isKuaiShou)) {
                    const result = await this.$httpBack.words.getByAnchorNumber({
                        anchorNumber: this.compereForm.broadcastUrls.trim()||this.compereForm.ksUrls.trim(),
                    })
                    if (result.code === 0) {
                        const {systemTradeId} = result.data||{}
                        if (systemTradeId !== undefined) this.$set(this.compereForm, 'tradeId', systemTradeId);
                    }
                }
            } catch (e) {
            }
        },
        changeAction() {
            this.isDouYinNum = !this.isDouYinNum
            this.$nextTick(async () => {
                if (this.compereForm.broadcastUrls) {
                    await this.$refs.ruleForm.validateField('broadcastUrls')
                }
            })
        },
        // TODO: 已迁移至统一取消授权入口 cancelAuthDialog
        // async cancelAuthorizeJuliang() {
        //     try {
        //         const result = await this.$httpClient.buyIn.cancelAuthorizeJuliang({
        //             secUid: this.resultData?.secUid,
        //         })
        //         if (result.code === 0) {
        //             this.localBuyInAuthStatus = 0
        //             this.$message.success('巨量百应授权已解约')
        //         }
        //     } catch (e) {
        //         this.localBuyInAuthStatus = 1
        //     }
        // },
        loadingStatus(status, msg) {
            this.loading = status
            if (msg) {
                const deep = cloneDeep(this.compereForm) || {}
                this.initForm('loadingStatus')
                this.compereForm = {
                    ...this.compereForm,
                    recordTimes: deep.recordTimes || [],
                    accountType: deep.accountType,
                    tradeId: deep.tradeId,
                }
            }
        },
        initForm(type) {
            this.compereForm = {
                broadcastUrls: '',
                remarksName: '',
                tradeId: '',
                accountType: null,
                subscribeShortVideo: 1,
                livingMode:'',
                accountStage: '',
                roster:'',
                accountFlow: '',
                roiAccuracy: '',
                accountWaterLevel: '',
                anchorSituation: '',
                premiereDate: '',
                livingTarget: '',
                marketing: '',
                optimizeDirection: [],
                learning: [],
                recordTime: this.getDefaultRecordTime(),
                // recordTimes: [],
                smsTip: 0,
                isScheduleRecord: 0,
                isStatisticsPerformance: 0,
                engSerViceType:'',
                segmentTimePoints: [],
                recordDefinition: -1,
                recordLimitValue: 0,
                recordDuration: 30,
                recordLimitType: -1,
                isAutoAnalysis: 1,
                autoDeleteTime: '-1',
                deleteContent: '',

                ksUrls: '',
                minTime: '',
                maxTime: '',
                authorizerInfoId:'',

                aiReplyScriptRestore: 0,
                scriptRestoreMonitor: 0,
                bulletInteractMonitor: 0,
                scriptRestoreMode: '',
                scriptRestoreLoopDuration: 0,
                scriptRestoreTalkSpeed: 280,
                scriptRestoreReferenceScript: '',
                standardScriptId: null,
                timeAxisScript: []
            }
            this.aiMonitorOrigin = {
                aiReplyScriptRestore: null,
                scriptRestoreMonitor: null,
                bulletInteractMonitor: null
            }
            this.aiMonitorTouched = {
                aiReplyScriptRestore: false,
                scriptRestoreMonitor: false,
                bulletInteractMonitor: false
            }
            this.scheduleRecordPrev = 0
            this.scheduleRecordInternalChange = false
            this.moreAiAnalysisActiveNames = []
            if (this.isPureDyOrKs) {
                this.compereForm.isAutoAnalysis = 0
                this.compereForm.tradeId = '1'
            }
            if (this.isPure && this.isDouYin) {
                this.compereForm.smsTip = 0
            }
            this.scheduleRecordPrev = Number(this.compereForm?.isScheduleRecord || 0)
            this.resetFields();
            this.$nextTick(() => {
                this.$refs?.ruleForm?.clearValidate();
            })
        },
        setFormData(data) {
            this.compereForm = {
                ...this.compereForm,
                ...data
            }
            if (data && data.isScheduleRecord !== undefined && data.isScheduleRecord !== null) {
                this.scheduleRecordPrev = Number(data.isScheduleRecord || 0)
            }
        },
        async loadAiMonitorQuota() {
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

                const next = { ...(this.aiMonitorQuota || {}) }
                if (qc) next.aiNegativeInspection = { used: Number(qc.useQuantity || 0), total: Number(qc.totalQuantity || 0) }
                if (fidelity) next.scriptRestoreMonitor = { used: Number(fidelity.useQuantity || 0), total: Number(fidelity.totalQuantity || 0) }
                if (inspect) next.bulletInteractMonitor = { used: Number(inspect.useQuantity || 0), total: Number(inspect.totalQuantity || 0) }
                this.aiMonitorQuota = next
            } catch (e) {}
        },
        async loadAnchorBasicConfig(params) {
            if (!this.$httpBack?.scriptMonitor?.getAnchorBasicConfig) return
            try {
                const input = (typeof params === 'string')
                    ? { id: String(params || '') }
                    : (params || {})
                const id = String(input?.id || '')
                const secUid = this.resolveCurrentSecUid(input)
                const safeSecUid = secUid || id
                if (!safeSecUid) return
                const res = await this.$httpBack.scriptMonitor.getAnchorBasicConfig({ secUid: safeSecUid })
                if (res?.code !== 0) return
                const data = res?.data || {}
                const platform = Number(this.resultData?.platform ?? this.compereForm?.platform ?? 0)
                const rawRecordTime = String(data.recordTime || '')
                let nextRecordTime = this.compereForm.recordTime
                let nextRecordTimes = this.compereForm.recordTimes
                if (platform === 2) {
                    nextRecordTimes = rawRecordTime
                        ? rawRecordTime.split(',').filter((it) => it).map((item) => item.split('-'))
                        : []
                    nextRecordTime = ['', '']
                } else {
                    nextRecordTime = rawRecordTime ? rawRecordTime.split('-') : this.getDefaultRecordTime()
                    nextRecordTimes = []
                }
                const recordLimitValue = Number(data.recordLimitValue)
                const nextRecordLimitValue = recordLimitValue > 0 ? 1 : 0
                const nextRecordDuration = recordLimitValue > 0 ? recordLimitValue : 30
                const autoDeleteTime = data?.autoDeleteTime ?? data?.AutoDeleteTime ?? '-1'
                const deleteContent = data?.deleteContent ?? data?.DeleteContent ?? ''
                const aiReplyScriptRestore = data.isScriptQualityInspection
                const scriptRestoreMonitor = data.isScriptFidelityMonitor
                const bulletInteractMonitor = data.isInteractionPatrol
                const roiAccuracy = this.normalizeRoiAccuracyValue(data.roiAccuracy)
                const anchorInfo = data.anchorInfo || {}
                this.compereForm = {
                    ...this.compereForm,
                    ...(safeSecUid ? { secUid: safeSecUid } : {}),
                    ...(!anchorInfo?.anchorName ? {} : { anchorName: anchorInfo.anchorName }),
                    ...(!anchorInfo?.platform && anchorInfo?.platform !== 0 ? {} : { platform: anchorInfo.platform }),
                    ...(data.tradeId === undefined ? {} : { tradeId: String(data.tradeId || '') }),
                    ...(data.accountType === undefined ? {} : { accountType: data.accountType }),
                    ...(data.subscribeShortVideo === undefined ? {} : { subscribeShortVideo: Number(data.subscribeShortVideo) }),
                    ...(data.livingMode === undefined ? {} : { livingMode: data.livingMode }),
                    ...(data.accountWaterLevel === undefined ? {} : { accountWaterLevel: data.accountWaterLevel }),
                    ...(data.accountFlow === undefined ? {} : { accountFlow: data.accountFlow }),
                    ...(data.premiereDate === undefined ? {} : { premiereDate: data.premiereDate }),
                    ...(data.livingTarget === undefined ? {} : { livingTarget: data.livingTarget }),
                    ...(data.marketing === undefined ? {} : { marketing: data.marketing }),
                    ...(data.optimizeDirection === undefined ? {} : { optimizeDirection: myUtils.normalizeVal(data.optimizeDirection) }),
                    ...(data.learning === undefined ? {} : { learning: myUtils.normalizeVal(data.learning) }),
                    ...(data.roiAccuracy === undefined ? {} : { roiAccuracy }),
                    ...(data.smsTip === undefined ? {} : { smsTip: data.smsTip }),
                    ...(data.engSerViceType === undefined ? {} : { engSerViceType: data.engSerViceType }),
                    ...(data.recordDefinition === undefined ? {} : { recordDefinition: data.recordDefinition }),
                    ...(data.recordLimitType === undefined ? {} : { recordLimitType: data.recordLimitType }),
                    ...(data.recordLimitValue === undefined ? {} : { recordLimitValue: nextRecordLimitValue, recordDuration: nextRecordDuration }),
                    ...(data.isAutoAnalysis === undefined ? {} : { isAutoAnalysis: data.isAutoAnalysis }),
                    ...(autoDeleteTime === undefined || autoDeleteTime === null ? {} : { autoDeleteTime: String(autoDeleteTime) }),
                    ...(deleteContent === undefined || deleteContent === null ? {} : { deleteContent: String(deleteContent) }),
                    ...(data.isAutoUploadCloud === undefined ? {} : { isAutoUploadCloud: data.isAutoUploadCloud }),
                    ...(data.recordTime === undefined ? {} : { recordTime: nextRecordTime, recordTimes: nextRecordTimes }),
                    ...(aiReplyScriptRestore === undefined || aiReplyScriptRestore === null ? {} : { aiReplyScriptRestore }),
                    ...(scriptRestoreMonitor === undefined || scriptRestoreMonitor === null ? {} : { scriptRestoreMonitor }),
                    ...(bulletInteractMonitor === undefined || bulletInteractMonitor === null ? {} : { bulletInteractMonitor }),
                    ...(data.standardScriptId === undefined ? {} : { standardScriptId: data.standardScriptId })
                }
                this.aiMonitorOrigin = {
                    aiReplyScriptRestore: (aiReplyScriptRestore === undefined || aiReplyScriptRestore === null) ? this.aiMonitorOrigin.aiReplyScriptRestore : Number(aiReplyScriptRestore),
                    scriptRestoreMonitor: (scriptRestoreMonitor === undefined || scriptRestoreMonitor === null) ? this.aiMonitorOrigin.scriptRestoreMonitor : Number(scriptRestoreMonitor),
                    bulletInteractMonitor: (bulletInteractMonitor === undefined || bulletInteractMonitor === null) ? this.aiMonitorOrigin.bulletInteractMonitor : Number(bulletInteractMonitor)
                }
                this.aiMonitorTouched = {
                    aiReplyScriptRestore: false,
                    scriptRestoreMonitor: false,
                    bulletInteractMonitor: false
                }
                const needLoadStandardScript = Number(scriptRestoreMonitor) === 1 || !!data.standardScriptId
                if (needLoadStandardScript) {
                    await this.loadStandardScriptDetail({
                        secUid: safeSecUid,
                        anchorUrlUserId: data.anchorUrlUserId
                    })
                }
            } catch (e) {}
        },
        onIsAiReportChange(value, action) {
            if (!this.compereForm.tradeId) {
                this.compereForm.isAutoDiagnosis = 0
                return this.$message.warning('请先选择行业')
            }
            this.$emit('onIsAiReportChange', {
                isAutoDiagnosis: value,
                tradeId: this.compereForm.tradeId,
                sourceId: this.resultData?.secUid,
                action
            })
        },
        isCompleteTime(timeStr) {
            const regex = /^(2[0-3]|[01]?\d):([0-5]?\d):([0-5]?\d)$/
            return regex.test(timeStr)
        },
        async addCompereForm() {
            try {
                const shouldSendAiMonitorField = (originVal, currentVal, touched) => {
                    const originIsNumber = originVal === 0 || originVal === 1
                    if (originIsNumber) return Number(currentVal) !== Number(originVal)
                    return touched === true && Number(currentVal) === 1
                }
                const shouldSendFidelity = shouldSendAiMonitorField(
                    this.aiMonitorOrigin.scriptRestoreMonitor,
                    this.compereForm.scriptRestoreMonitor,
                    this.aiMonitorTouched.scriptRestoreMonitor
                )
                const shouldCarryScriptRestoreConfig = Number(this.compereForm.scriptRestoreMonitor) === 1 && (
                    this.isSettingChange
                    || shouldSendFidelity
                    || !!this.compereForm.standardScriptId
                    || (Array.isArray(this.compereForm.timeAxisScript) && this.compereForm.timeAxisScript.length > 0)
                )

                if (shouldCarryScriptRestoreConfig) {
                    if (!Number(this.compereForm.scriptRestoreMode) || !this.compereForm.scriptRestoreReferenceScript?.trim()) {
                        this.scriptRestoreDrawerVisible = true;
                        this.$message.warning('请完善话术还原度监控配置');
                        return;
                    }
                    if (Number(this.compereForm.scriptRestoreMode) === 1 && !Number(this.compereForm.scriptRestoreLoopDuration)) {
                        this.scriptRestoreDrawerVisible = true;
                        this.$message.warning('请填写循环话术预计时长');
                        return;
                    }
                    if (!Array.isArray(this.compereForm.timeAxisScript) || !this.compereForm.timeAxisScript.length) {
                        this.scriptRestoreDrawerVisible = true;
                        this.$message.warning('请先确认标准直播稿');
                        return;
                    }
                }
                const response = await this.$refs.ruleForm.validate()
                if (response) {
                    const {recordTime} = this.compereForm
                    let finalTime = []
                    if (recordTime?.length > 0 && recordTime?.every(d=>d)) {
                        const startTime = recordTime[0], endTime = recordTime[1]
                        finalTime = startTime && endTime ? [this.isCompleteTime(startTime) ? startTime : `${startTime}:00`, this.isCompleteTime(endTime) ? endTime : `${endTime}:00`] : ''
                    } else {
                        finalTime = ''
                    }
                    const payload = {
                        ...omit(this.compereForm, [
                            'aiReplyScriptRestore',
                            'scriptRestoreMonitor',
                            'bulletInteractMonitor',
                            'scriptRestoreMode',
                            'scriptRestoreLoopDuration',
                            'scriptRestoreTalkSpeed',
                            'scriptRestoreReferenceScript',
                            'standardScriptId',
                            'timeAxisScript'
                        ]),
                        recordTime: finalTime,
                        optimizeDirection: Array.isArray(this.compereForm.optimizeDirection) ? this.compereForm.optimizeDirection.join(',') : this.compereForm.optimizeDirection,
                        learning: Array.isArray(this.compereForm.learning) ? this.compereForm.learning.join(',') : this.compereForm.learning,
                        urlType: this.isDouYinNum ? 0 : 1
                    }
                    // 自动删除：不删除(-1)时不透传字段，否则透传 autoDeleteTime / deleteContent
                    if (String(this.compereForm.autoDeleteTime) === '-1') {
                        delete payload.autoDeleteTime
                        delete payload.deleteContent
                    }
                    if (Number(this.compereForm.accountType) === 1) {
                        delete payload.optimizeDirection
                    } else {
                        delete payload.learning
                    }
                    if (this.isSettingChange) {
                        delete payload.subscribeShortVideo
                    }
                    if (this.isShipinhao) {
                        delete payload.subscribeShortVideo
                    }
                    if (payload.roiAccuracy !== undefined) {
                        payload.roiAccuracy = this.normalizeRoiAccuracyValue(payload.roiAccuracy)
                    }

                    const shouldSendQc = shouldSendAiMonitorField(
                        this.aiMonitorOrigin.aiReplyScriptRestore,
                        this.compereForm.aiReplyScriptRestore,
                        this.aiMonitorTouched.aiReplyScriptRestore
                    )
                    if (shouldSendQc) {
                        payload.isScriptQualityInspection = this.compereForm.aiReplyScriptRestore
                    }

                    const shouldSendInspect = shouldSendAiMonitorField(
                        this.aiMonitorOrigin.bulletInteractMonitor,
                        this.compereForm.bulletInteractMonitor,
                        this.aiMonitorTouched.bulletInteractMonitor
                    )
                    if (shouldSendInspect) {
                        payload.isInteractionPatrol = this.compereForm.bulletInteractMonitor
                    }

                    if (shouldSendFidelity || shouldCarryScriptRestoreConfig) {
                        payload.isScriptFidelityMonitor = this.compereForm.scriptRestoreMonitor
                    }
                    if (shouldCarryScriptRestoreConfig) {
                        Object.assign(payload, this.buildScriptRestoreSubmitPayload())
                    }

                    this.$emit('addCompere', payload)
                }
            } catch (error) {
            }
        },
        async getDictDataListByCodes(codes) {
            try {
                const result = await this.$httpBack.dictdata.dictDataListByCodes({codes})
                if (result.code === 0) {
                    const ENUM_OBJ = {
                        accountStageList: 'account_stage',
                        accountWaterLevelList: 'account_water_level',
                        accountFlowList: 'account_flow',
                        roiAccuracyList: 'roi_accuracy',
                        accountLivingModeList: 'living_mode',
                        livingTargetList: 'living_target',
                        marketingList: 'marketing',
                        optimizeDirectionList: 'optimize_direction',
                        learningList: 'learning',
                        engSerViceTypeList: 'eng_ser_vice_type',
                    }
                    const accountEnum = myUtils.createEnumHelper(ENUM_OBJ)
                    for (let key in result.data || {}) {
                        const _list = result.data[key]
                        _list.forEach(item => {
                            item.value = key === 'roi_accuracy'
                                ? this.normalizeRoiAccuracyValue(item.value)
                                : this.toNumberOrOriginal(item.value)
                        })
                        this[accountEnum.getKey(key)] = _list
                    }
                    this.compereForm = {
                        ...this.compereForm,
                        engSerViceType: this.compereForm.engSerViceType || this.engSerViceTypeList[0]?.value
                    }
                }
            } catch (e) {

            }
        },
        toNumberOrOriginal(val) {
            const num = Number(val);
            return Number.isFinite(num) ? num : val;
        },
        /**
         * 检查并自动纠正时间范围，确保不超过2小时
         * @param {string} startTime - 开始时间 HH:mm 格式
         * @param {string} endTime - 结束时间 HH:mm 格式
         */
        /**
         * 检查并自动调整时间范围，支持跨天情况，确保时间间隔在允许范围内（10分钟-12小时）
         * @param {string} startTime - 开始时间 HH:mm 格式
         * @param {string} endTime - 结束时间 HH:mm 格式
         */
        checkAndCorrectTimeRange(startTime, endTime) {
            // 如果开始时间或结束时间为空，不进行检查
            if (!startTime || !endTime || startTime.trim() === '' || endTime.trim() === '') {
                return;
            }

            try {
                // 解析时间
                const [startHour, startMinute] = startTime.split(':').map(Number);
                const [endHour, endMinute] = endTime.split(':').map(Number);

                // 转换为分钟数进行计算
                let startTotalMinutes = startHour * 60 + startMinute;
                let endTotalMinutes = endHour * 60 + endMinute;
                const maxIntervalMinutes = this.maxTimeInterval * 60;
                const minIntervalMinutes = this.minTimeInterval;

                // 判断是开始时间还是结束时间发生变化
                const previousTime = this.previousRecordTime || ['', ''];
                const [prevStartTime, prevEndTime] = previousTime;
                const isStartTimeChanged = startTime !== prevStartTime;
                const isEndTimeChanged = endTime !== prevEndTime;

                let correctedTime = null;
                let warningMessage = '';

                // 计算时间差（支持跨天情况）
                let timeDifference;
                if (endTotalMinutes >= startTotalMinutes) {
                    // 正常情况：结束时间大于等于开始时间
                    timeDifference = endTotalMinutes - startTotalMinutes;
                } else {
                    // 跨天情况：结束时间小于开始时间
                    timeDifference = (24 * 60) - startTotalMinutes + endTotalMinutes;
                }

                // 检查时间间隔是否超出允许范围
                if (timeDifference < minIntervalMinutes) {
                    // 时间间隔小于最小值，调整为最小间隔
                    if (isStartTimeChanged) {
                        // 开始时间变化，调整结束时间
                        const correctedEndTotalMinutes = (startTotalMinutes + minIntervalMinutes) % (24 * 60);
                        const correctedEndHour = Math.floor(correctedEndTotalMinutes / 60);
                        const correctedEndMinute = correctedEndTotalMinutes % 60;
                        const correctedEndTime = `${correctedEndHour.toString().padStart(2, '0')}:${correctedEndMinute.toString().padStart(2, '0')}`;

                        correctedTime = [startTime, correctedEndTime];
                        warningMessage = `时间间隔不能少于${this.minTimeInterval}分钟，已自动调整`;
                    } else if (isEndTimeChanged) {
                        // 结束时间变化，调整开始时间
                        let correctedStartTotalMinutes = endTotalMinutes - minIntervalMinutes;
                        if (correctedStartTotalMinutes < 0) {
                            correctedStartTotalMinutes += 24 * 60;
                        }

                        const correctedStartHour = Math.floor(correctedStartTotalMinutes / 60);
                        const correctedStartMinute = correctedStartTotalMinutes % 60;
                        const correctedStartTime = `${correctedStartHour.toString().padStart(2, '0')}:${correctedStartMinute.toString().padStart(2, '0')}`;

                        correctedTime = [correctedStartTime, endTime];
                        warningMessage = `时间间隔不能少于${this.minTimeInterval}分钟，已自动调整`;
                    }
                } else if (timeDifference > maxIntervalMinutes) {
                    // 时间间隔大于最大值，调整为最大间隔
                    if (isStartTimeChanged) {
                        // 开始时间变化，调整结束时间
                        const correctedEndTotalMinutes = (startTotalMinutes + maxIntervalMinutes) % (24 * 60);
                        const correctedEndHour = Math.floor(correctedEndTotalMinutes / 60);
                        const correctedEndMinute = correctedEndTotalMinutes % 60;
                        const correctedEndTime = `${correctedEndHour.toString().padStart(2, '0')}:${correctedEndMinute.toString().padStart(2, '0')}`;

                        correctedTime = [startTime, correctedEndTime];
                        warningMessage = `时间间隔不能超过${this.maxTimeInterval}小时，已自动调整`;
                    } else if (isEndTimeChanged) {
                        // 结束时间变化，调整开始时间
                        let correctedStartTotalMinutes = endTotalMinutes - maxIntervalMinutes;
                        if (correctedStartTotalMinutes < 0) {
                            correctedStartTotalMinutes += 24 * 60;
                        }

                        const correctedStartHour = Math.floor(correctedStartTotalMinutes / 60);
                        const correctedStartMinute = correctedStartTotalMinutes % 60;
                        const correctedStartTime = `${correctedStartHour.toString().padStart(2, '0')}:${correctedStartMinute.toString().padStart(2, '0')}`;

                        correctedTime = [correctedStartTime, endTime];
                        warningMessage = `时间间隔不能超过${this.maxTimeInterval}小时，已自动调整`;
                    }
                }

                // 更新时间并显示提示
                if (correctedTime) {
                    this.$nextTick(() => {
                        this.compereForm.recordTime = correctedTime;
                    });

                    // 防抖处理：3秒钟内只能触发一次弹窗
                    if (warningMessage && !this.messageDebounceTimer) {
                        this.$message.info(warningMessage);
                        this.messageDebounceTimer = setTimeout(() => {
                            this.messageDebounceTimer = null;
                        }, 3000);
                    }

                    // 保存调整后的时间作为下次比较的基准
                    this.previousRecordTime = correctedTime;
                    return;
                }

                // 保存当前时间作为下次比较的基准
                this.previousRecordTime = [startTime, endTime];

            } catch (error) {
                console.error('时间范围检查出错:', error);
            }
        },
        /**
         * 时间选择变化时的处理
         * @param {Array} value - 选择的时间范围
         */
        onTimeChange(value) {
            // 时间范围限制已通过 pickerOptions 的 disabledHours 和 disabledMinutes 实现
            // 这里可以添加其他业务逻辑处理
            if(this.isKuaiShou){return;}
            // 如果有选择的时间值，进行时间范围检查
            if (value && Array.isArray(value) && value.length >= 2) {
                const [startTime, endTime] = value;
                if (startTime && endTime) {
                    this.checkAndCorrectTimeRange(startTime, endTime);
                }
            }
        },
        resetFields() {
            this.$refs.ruleForm?.resetFields()
        },
        handleCancel() {
            this.resetFields()
            this.$emit('closeDialog')
        }
    },
    beforeDestroy() {
        this.initForm();
        // 清理防抖定时器，避免内存泄漏
        if (this.messageDebounceTimer) {
            clearTimeout(this.messageDebounceTimer);
            this.messageDebounceTimer = null;
        }
    }, //生命周期 - 销毁之前
}
</script>
<style lang="scss" scoped>
::v-deep(.flex-form-collapse) {
    padding: 0 10px;

    .collapse-item-container{
        background-color: #F7F7F7;
        border-radius: 12px;
        padding: 16px 16px 0 0;
    }

    .el-collapse, .el-collapse-item__header, .el-collapse-item__wrap {
        border: none !important;
    }

    .el-collapse-item__header {
        color: var(--color-main);
    }

    .el-collapse-item__arrow {
        margin: 2px 6px 0 10px;
        font-weight: bold;
        transform: rotate(90deg);
    }

    .el-collapse-item__arrow.is-active {
        transform: rotate(-90deg);
    }
}

.compereForm {
    .isScheduleRecord-form-item-wrap {
        display: inline-block;
    }

    .isScheduleRecord-form-item-wrap-disabled {
        cursor: not-allowed;

        ::v-deep(.el-radio__input),
        ::v-deep(.el-radio__label) {
            cursor: not-allowed;
        }
    }

    .buy_in_selected {
        ::v-deep(.el-checkbox-button__inner) {
            color: #28BD6C;
            border-color: #28BD6C !important;
            background-color: rgba(40, 189, 108, 0.05);
            border-radius: 5px;
        }
    }

    .ai-analysis-config-panel {
        padding-top: 8px;

        .ai-analysis-config-item {
            max-width: none;

            ::v-deep .el-form-item__label {
                line-height: 38px;
            }

            &.has-hint ::v-deep .el-form-item__label {
                white-space: pre-line;
                line-height: 1.3;
                font-size: 12px;
            }
        }
    }

    .notRequired {
        color: var(--color-main);
        font-size: 14px;
        text-decoration: underline;
        padding: 0 0 5px 10px;
    }

    .accountInfoAdd {
        background: rgba(230, 230, 230, 0.3);
        border-radius: 6px;
        padding: 6px;
        margin-bottom: 12px;
    }

    .aiMonitorBox{
        border: 1px solid rgba(68, 77, 255, 0.35);
        border-radius: 6px;
        padding: 12px;
        margin: 10px 10px 8px 10px;
        background: rgba(255, 255, 255, 0.7);
    }

    .aiMonitorTitle{
        font-size: 14px;
        font-weight: 600;
        color: #3D43FF;
        margin-bottom: 10px;
    }

    .aiMonitorRow{
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;
        margin-bottom: 8px;
    }

    .aiMonitorRowDisabled{
        cursor: not-allowed;
        ::v-deep(.el-radio__input),
        ::v-deep(.el-radio__label) {
            cursor: not-allowed;
        }
    }

    .aiMonitorLabel{
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        color: var(--color-text, #333);
        min-width: 140px;
    }

    .aiMonitorHelpIcon{
        font-size: 14px;
        color: #999;
        cursor: pointer;
    }

    .flex-form {
        display: flex;
        align-items: center;
        justify-content: flex-start;

        .flex-form-item {
            width: auto;
        }

        .flex-form-item-set {
            margin-bottom: 2px;
        }

        .other {
            font-size: 12px;
            margin-left: 68px;
            margin-bottom: 18px;
            cursor: pointer;
            color: var(--color-main);
        }

        .edit {
            color: #0B7CFF;
            margin-left: 5px;
            cursor: pointer
        }
    }

    .tips {
        color: #F4BE34;
        font-size: 12px;
        margin-left: 16px;
    }

    ::v-deep(.el-form-item) {
        max-width: 600px;
    }
}

::v-deep(.aiMonitor-tooltip-popper) {
    background: rgba(51, 51, 51, 0.96) !important;
    border: none !important;
    padding: 10px 14px !important;
    border-radius: 6px !important;
    max-width: 380px;
}

::v-deep(.aiMonitor-tooltip-popper[x-placement^='top'] .popper__arrow::after) {
    border-top-color: rgba(51, 51, 51, 0.96) !important;
}

::v-deep(.aiMonitor-tooltip-popper[x-placement^='bottom'] .popper__arrow::after) {
    border-bottom-color: rgba(51, 51, 51, 0.96) !important;
}

.aiMonitor-tooltip-content{
    color: #fff;
    font-size: 12px;
    line-height: 18px;
}
</style>
