<!--
@description 段落内容容器：负责详情页/AI 助手的文本段落、段落指标、AI 报告与导出能力渲染。
-->
<template>
    <div v-loading="!refresh" class="discernSearchContainerContent main-bg pd-l12 pd-r12 pd-b12" ref="discernSearchContainerContent">
        <!-- 搜索分类功能 -->
        <Toolbar v-if="!monitorOnlyMode || !notSearch" ref="toolbar" :readonly="readonly" :notTarde="notTarde" :notSearch="notSearch" :notExport="notExport" :treeData="treeData"
            :keyWordCount="keyWordCount" :loading.sync="keyWordLoad" :isCompare="isCompare" :ai="isAi"
            :textData="textData"
            :notes="notes"
            :wordsInfo="wordsInfo"
            :targetType="targetType"
            :enableSwappedTabs="enableSwappedTabs"
            :textTypeConfig="{aiOptimalStatus, aiShardingStatus, aiDiagnoseStatus, scriptQualityStatus, interactionInspectionStatus, scriptRestorationStatus, scriptQualityUnread, interactionInspectionUnread, scriptRestorationUnread}"
            @createText="createText"
            :isVideo="isVideo"
            :isRecording="isRecording"
            :selectIndex="selectWordIndex" :isSelectTrade="isSelectTrade" :playUrl="getPlayUrl" @keyWord="keyWord"
            @prev="selectKeyWordIndex(selectWordIndex - 1)" @next="selectKeyWordIndex(selectWordIndex + 1)" @tabsClick="setTextType"
            @exportWord="exportWord"
            @exportTxt="$emit('exportTxt')" @copyText="copyText" @treeChange="treeChange" @treeLoad="treeLoad"
            @pace-click="$emit('pace-click', $event)"
            @markClick="$emit('markClick', $event)"
            @bulletscreen-click="$emit('bulletscreen-click', $event)"
            @deal-click="$emit('deal-click', $event)"
            @interactionRate-click="$emit('interactionRate-click', $event)"
            @dealRate-click="$emit('dealRate-click', $event)"
            @sales-click="$emit('sales-click', $event)"
            @uv-click="$emit('uv-click', $event)"
            @qianchuanCost-click="$emit('qianchuanCost-click', $event)">
            <template #toolbar-left>
                <slot name="toolbar-left"></slot>
            </template>
            <template #toolbar-right-before>
                <slot name="toolbar-right-before"></slot>
            </template>
            <template #toolbar-right-after>
                <slot name="toolbar-right-after"></slot>
            </template>
            <template #word-control>
                <slot name="word-control"></slot>
            </template>
        </Toolbar>
        <el-tooltip  effect="dark" :content="sliceRemarks" placement="top-start">
            <div slot="content" style="max-width: 600px; white-space: normal;">
               {{sliceRemarks}}
            </div>
            <div class="aiSliceRemarks text-clamp1 text-sm cursor-pointer" v-if="sliceRemarks">备注：{{ sliceRemarks }}</div>
        </el-tooltip>
        <div v-if="refresh && isProductData" class="productDataScrollWrapper">
            <ProductDataTab :textData="textData" />
        </div>
        <!-- 音频/视频文件的文字段落 -->
        <div @mousedown="mousedownHandle" @mouseup="handleSelectText" @contextmenu.prevent="rightTickContextMenu" ref="wordsBodyContainer"
            class="wordsBodyContainer" :class="{'pd-10': isAnnotation, 'pd-t6':isFileText}" v-else-if="isVideo && refresh && isText">
            <div v-for="(item, index) in getList" :key="index" class="wordsBodyItemContainer" ref="dom">
                <AvatarImg v-if="!isAnnotation" class="not-select-text avatar-img-box" :replayType="getReplayType" :avatar="getAnchorInfo?.AnchorAvatar"></AvatarImg>
                <div class="wordsBodyContentContainer">
                    <div class="flex-jc-sb pd-r6 pd-b6 words-other-info">
                        <!-- ai详情页展示内容 -->
                        <div :class="{'not-select-text':isAnnotation||notes,'flex-ai-c': !isAi&& !isCompare}">
                            <!-- 开始时间 -->
                            <SpeedTime v-if="isShowOther(1)" :startTime="item?.startTime" :label="'段落'+(index + 1)+'开始时间：'" :isAi="isAi" class="mg-r4 not-select-text"></SpeedTime>
<!--                           老板说 纯录制版只展示开始时间 其它一律不展示-->
                            <template v-if="canUseAgentDisplay">
                                <!-- 弹幕 -->
                                <bulletScreen v-if="item.isBulletScreen && isShowBulletScreen && isShowOther(4)"
                                              :class="{'not-select-text':isAnnotation||notes,'not-word':true}"
                                              :countNum="item?.bulletScreenNum"
                                              :hideIcon="monitorOnlyMode"
                                              :item="{...item,index}"
                                              :textData="textData"></bulletScreen>
                                <!-- 在线人数 -->
                                <OnlineNum v-if="((isVideo && item?.onlinePeopleObj?.show) || isAi) && isShowOther(3)"
                                           :class="{'mg-l6': !isAi&& !isCompare}"
                                           class="not-select-text not-word"
                                           :isAi="isAi" :item="item" :label="'段落'+(index + 1)+'在线人数：'"></OnlineNum>
                                <!-- 销售额 -->
                                <SectionSales v-if="isAi&&isBuyInData&&isVideo&&!isNilData(item.salesCount)&&isShowOther(9)" class="item-font not-select-text not-word" notLabel :item="item"/>
                                <!-- UV价值 -->
                                <SectionUV v-if="isAi&&isBuyInData&&isVideo&&!isNilData(item.uv)&&isShowOther(10)" class="item-font not-select-text not-word" notLabel :item="item"/>
                                <!-- 成交量 -->
                                <SectionDeal v-if="isAi&&isBuyInData&&isVideo&&!isNilData(item.dealCount)&&isShowOther(5)" class="item-font not-select-text not-word" notLabel :item="item"/>
                                <!-- 互动率 -->
                                <SectionInteractRate v-if="isAi&&isVideo&&item.isBulletScreen&&isShowOther(6)" class="item-font not-select-text not-word" notLabel :item="item" :label="'段落'+(index + 1)+'互动率：'"/>
                                <!-- 成交率 -->
                                <SectionDealRate v-if="isAi&&isBuyInData&&isVideo&&!isNilData(item.dealCount)&&isShowOther(7)" class="item-font not-select-text not-word" notLabel :item="item"/>
                                <!-- 投放消耗 -->
                                <SectionRoiValue
                                    v-if="isAi && $store.getters.largeEnterprises && hasRoiValue(item?.qianchuanCost) && isShowOther(11)"
                                    class="item-font not-select-text not-word"
                                    label="投放消耗"
                                    :value="item.qianchuanCost" />
                            </template>
                        </div>
                        <!-- 详情页展示内容 -->
                        <div class="not-select-text pd-r6" :class="!isAi && !isCompare ? ['flex-ai-c']: []" v-if="canUseAgentDisplay">
                            <!-- 销售额 -->
                            <SectionSales v-if="salesVisible&&isBuyInData && !isAi" class="item-font text-right not-select-text not-word" notLabel :item="item"/>
                            <!-- UV价值 -->
                            <SectionUV v-if="uvVisible&&isBuyInData && !isAi" class="item-font text-right not-select-text not-word" notLabel :item="item"/>
                            <!-- 成交量 -->
                            <SectionDeal v-if="dealVisible&&isBuyInData && !isAi" class="item-font text-right not-select-text not-word" notLabel :item="item"/>
                            <!-- 互动率 -->
                            <SectionInteractRate v-if="interaction&&item.isBulletScreen  && !isAi" class="item-font text-right not-select-text not-word" notLabel :item="item" :label="'段落'+(index + 1)+'互动率：'"/>
                            <!-- 成交率 -->
                            <SectionDealRate v-if="dealRateVisible&&isBuyInData && !isAi" class="item-font text-right not-select-text not-word" notLabel :item="item"/>
                           <!-- 自然时间 -->
                            <NaturalTime v-if="item.naturalTime && isShowOther(2)"
                            :class="{'mg-l6': !isAi&& !isCompare}"
                            class="not-select-text not-word"
                            :style="(!isAi&& !isCompare)?{order: 2} : {}"
                            :naturalTime="item.naturalTime"></NaturalTime>
                            <!-- ROI 数据只在云空间视频详情页展示，不在录制版、AI 助手与文件详情中复用 -->
                            <SectionRoiValue
                                v-if="canShowParagraphRoi && qianchuanCostVisible && $store.getters.largeEnterprises && hasRoiValue(item?.qianchuanCost)"
                                class="item-font text-right not-select-text not-word"
                                label="投放消耗"
                                :value="item.qianchuanCost" />
                            <!-- 语速 -->
                            <TalkSpeed v-if="analysisChar && isShowOther(8)" class="text-right not-select-text not-word" notLabel :charNumSecond="item.charNumSecond"></TalkSpeed>
                        </div>
                    </div>
                    <TextParagraph :id="`textParagraph-${index + 1}-dom`"
                        class="textParagraph-content"
                        ref="textParagraph"
                        :class="[`${name}textParagraph-${index}`,`${isAnnotation?'is-annotation':''}`]"
                        :sentenceMark="item" :currentTime="videoCurrentTime" @playerRead='playerReadied'
                        :paragraphIndex="index" @updateData="updateData" :keyWord="keyWordText"
                        @annotation-mark-click="annotationMarkClick"
                        @updateKeyWord="updateKeyWord" :notWordsType="notWordsType" :selectMark="selectMark"
                        :name="name" :currentParagraphIndex="currentParagraphIndex" :annotation="annotationMap[index]"
                        :isText="isAnnotation ? true : (currentParagraphIndex !== index)" :textShow="textShow" />
                    <slot name="textParagraph-after" v-bind="{item,index}"></slot>
                </div>
            </div>
        </div>
        <!-- 文本文件的文字段落 -->
        <div v-else-if="refresh && isText" @mousedown="mousedownHandle" @mouseup="handleSelectText" @contextmenu.prevent="rightTickContextMenu"
            class="wordsBodyContainer" :class="{'pd-10': isAnnotation, 'pd-t6':isFileText}" ref="wordsBodyContainer">
            <div v-for="(item, index) in getList" class="wordsBodyItemContainer" ref="dom" :key="index">
                <AvatarImg :avatar="getAnchorInfo?.AnchorAvatar"></AvatarImg>
                <div class="wordsBodyContentContainer">
                    <TextParagraph :sentenceMark="item"  :name="name" 
                        class="textParagraph-content"  ref="textParagraph"
                        :class="[`${name}textParagraph-${index}`,`${isAnnotation?'is-annotation':''}`]" :currentTime="videoCurrentTime"
                        @playerRead='playerReadied' :paragraphIndex="index" @updateData="updateData"
                        :keyWord="keyWordText" @updateKeyWord="updateKeyWord" :notWordsType="notWordsType"
                        :annotation="annotationMap[index]" @annotation-mark-click="annotationMarkClick"
                        :selectMark="selectMark" :oldSelectData="oldSelectData" :isText="true" textType="text" />
                    <slot name="textParagraph-after" v-bind="{item,index}"></slot>
                </div>
            </div>
        </div>
        <div v-else-if="isAiSharding" class="wordsBodyContainer">
            <div class="font-s14 text-color2 pd-14 brs-10" style="background: rgba(68, 77, 255, 0.05)" v-if="(isAiShardingStatus || $isAifupan)">
                <b>注意：</b>经过AI整理，基于<span style="color: var(--color-main)">千万级场次数据训练</span>，按照一线直播间优质话术类型归类，对直播稿<span style="color: var(--color-main)">进行功能性话术拆解</span>，方便运营和主播快速复盘。
            </div>
            <template v-if="aiShardingStatus == 2">
                <div ref="aiSharding" class="font-s14 text-colorMain md-format-box" :class="{'md-format-box-left':!isAi}" @contextmenu.prevent="rightContextMenuHanlder">
                    <AiContentRenderer
                        :content="normalizeAiMd(aiShardings)"
                        renderMode="mdTag"
                        :option="{ forceStyle: true, upgradePlainTable: true }"
                    />
                </div>
                <afp-button @click="getText" size="medium" style="float: right;margin:12px">重新生成</afp-button>
            </template>
            <div v-else-if="aiShardingStatus == 1">
                <div class="pd-t40">
                    <letterSpacing text="AI脚本拆解正在重新生成中，请五分钟后再查看..." :position="-3"></letterSpacing>
                </div>
            </div>
<!--            class="pd-t20 flex-jc-c"-->
            <div v-else>
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">
                    暂未生成AI脚本拆解...
                </div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    请前往客户端获取AI脚本拆解...
                </div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    没有获取AI脚本拆解权限...
                </div>
                <InspectionEmptyPage v-else type="aiSharding" @primary-click="getText" />
            </div>
        </div>

        <div v-else-if="isAiDiagnose" class="wordsBodyContainer ai-diagnose ai-content-box" :style="{ padding: aiDiagnoseStatus==2?'0 12px':'0'}">
            <div v-if="aiDiagnoseStatus==2" class="ai-diagnose-content">
                <div ref="aiDiagnose">
                    <AiContentRenderer
                        :content="normalizeAiMd(aiDiagnosis)"
                        renderMode="mdTag"
                        :option="{ forceStyle: true, upgradePlainTable: true }"
                    />
                </div>

                <afp-button size="medium" style="float: right;margin:12px" :disabled="!isAuthenticated" @click="aiDataReport">重新生成诊断报告
                </afp-button>
            </div>

            <div v-else-if="[0,1].includes(aiDiagnoseStatus)">
                <div class="pd-t40">
                    <letterSpacing text="AI数据诊断正在获取中，请五分钟后再查看..." :position="-3"></letterSpacing>
                </div>
            </div>
            <div v-else-if="[3].includes(aiDiagnoseStatus)">
                <div class="font-s12 text-colorc2 text-center">
                    诊断报告生成失败...
                </div>
                <afp-button size="medium" style="float: right;margin:12px" :disabled="!isAuthenticated" @click="aiDataReport">重新生成诊断报告
                </afp-button>
            </div>
            <template v-else>
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">
                    暂未生成AI数据诊断报告...
                </div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    请前往客户端获取AI数据诊断报告...
                </div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    没有生成AI数据诊断报告权限...
                </div>
                <InspectionEmptyPage
                    v-else
                    type="aiDiagnose"
                    @primary-click="aiDataReport"
                    @secondary-click="viewExample"
                />
            </template>
        </div>

        <div v-else-if="isAiOptimal" class="wordsBodyContainer" >
            <div class="font-s14 text-color2  pd-14 brs-10"  style="background: rgba(68, 77, 255, 0.05)" v-if="(isAiOptimalStatus || $isAifupan)">
                <b>注意：</b>优化原文是经过AI整理，在原文基础上进行了一定的话术优化，作为您优化稿件的参考，推荐自有账号使用。（大健康、情感等高风控行业请勿直接使用）
            </div>
            <div v-if="aiOptimalStatus  == 2" ref="aiOptimal" class="font-s14 text-colorMain md-format-box" :class="{'md-format-box-left':!isAi}"  @contextmenu.prevent="rightContextMenuHanlder">
                <AiContentRenderer
                    :content="normalizeAiMd(aiOptimals)"
                    renderMode="mdTag"
                    :option="{ forceStyle: true, upgradePlainTable: true }"
                />
            </div>
            <div v-else-if="aiOptimalStatus == 1" class="h100">
                <div class="pd-t40">
                    <letterSpacing text="优化原文正在获取中,请五分钟后再查看..." :position="-3"></letterSpacing>
                </div>
            </div>
            <div v-else class="pd-t20 flex-jc-c">
                <div v-if="readonly" class="font-s12 text-colorc2">
                    暂未生成优化原文...
                </div>
                <div v-else-if="$isWeb"  class="font-s12 text-colorc3">
                    请前往客户端获取优化原文...
                </div>
                <div v-else-if="!isSlefAuth" class="font-s12 text-colorc3">
                    没有获取优化原文权限...
                </div>
                <afp-button v-else @click="getText" size="default" type="primary" :plain="false">点我获取优化原文</afp-button>
            </div>
        </div>

        <div v-else-if="isScriptQuality" class="wordsBodyContainer ai-diagnose ai-content-box scriptQualityDetailContainer">
            <template v-if="Number(scriptQualityMonitorEnabled) !== 1 && (scriptQualityMonitorEnabled !== null && scriptQualityMonitorEnabled !== undefined && scriptQualityMonitorEnabled !== '') && Number(scriptQualityStatus) === 0">
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">
                    暂未生成话术质检报告...
                </div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    请前往客户端获取话术质检报告...
                </div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    没有生成话术质检报告权限...
                </div>
                <InspectionEmptyPage
                    v-else-if="isAiAnalysisFinished"
                    type="scriptQuality"
                    :secondary-visible="canEnableAutoMonitor('scriptQuality', scriptQualityMonitorEnabled)"
                    @secondary-click="switchQuality"
                    @primary-click="createInspectionReport('scriptQuality')"
                />
                <div v-else class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    智能分析完成后才可生成话术质检报告...
                </div>
            </template>
            <div v-else-if="scriptQualityStatus == 2">
                <div ref="scriptQuality" class="scriptQualityDetailContent">
                    <ScriptQualityReportContent :content="scriptQualityContents" />
                </div>
<!--                <ConfirmRead v-if="!readonly" type="scriptQuality" :reportId="scriptQualityReportId" @confirm="handleConfirmRead"/>-->
            </div>

            <div v-else-if="![undefined,null,''].includes(scriptQualityStatus) && Number(scriptQualityStatus) === 1">
                <div class="pd-t40">
                    <letterSpacing text="话术质检报告正在获取中，请五分钟后再查看..." :position="-3"></letterSpacing>
                </div>
            </div>
            <div v-else-if="![undefined,null,''].includes(scriptQualityStatus) && Number(scriptQualityStatus) === 3">
                <div class="font-s12 text-colorc2 text-center">
                    话术质检报告生成失败...
                </div>
                <afp-button v-if="isAiAnalysisFinished" size="medium" style="float: right;margin:12px" :disabled="!isAuthenticated" @click="createInspectionReport('scriptQuality')">重新生成报告</afp-button>
            </div>
            <template v-else>
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">
                    暂未生成话术质检报告...
                </div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    请前往客户端获取话术质检报告...
                </div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    没有生成话术质检报告权限...
                </div>
                <InspectionEmptyPage
                    v-else-if="isAiAnalysisFinished"
                    type="scriptQuality"
                    :secondary-visible="canEnableAutoMonitor('scriptQuality', scriptQualityMonitorEnabled)"
                    @secondary-click="switchQuality"
                    @primary-click="createInspectionReport('scriptQuality')"
                />
                <div v-else class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    智能分析完成后才可生成话术质检报告...
                </div>
            </template>
        </div>

        <div v-else-if="isInteractionInspection" class="wordsBodyContainer ai-diagnose ai-content-box scriptQualityDetailContainer">
            <template v-if="Number(interactionInspectionMonitorEnabled) !== 1 && (interactionInspectionMonitorEnabled !== null && interactionInspectionMonitorEnabled !== undefined && interactionInspectionMonitorEnabled !== '') && Number(interactionInspectionStatus) === 0">
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">
                    暂未生成互动巡检报告...
                </div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    请前往客户端获取互动巡检报告...
                </div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    没有生成互动巡检报告权限...
                </div>
                <InspectionEmptyPage
                    v-else
                    type="interactionInspection"
                    :secondary-visible="canEnableAutoMonitor('interactionInspection', interactionInspectionMonitorEnabled)"
                    @secondary-click="switchInspection"
                    @primary-click="createInspectionReport('interactionInspection')"
                />
            </template>
            <div v-else-if="interactionInspectionStatus == 2">
                <div ref="interactionInspection" class="scriptQualityDetailContent">
                    <ScriptQualityReportContent :content="interactionInspectionContents" />
                </div>

<!--                <ConfirmRead v-if="!readonly" type="interactionInspection" :reportId="interactionInspectionReportId" @confirm="handleConfirmRead"/>-->

            </div>

            <div v-else-if="![undefined,null,''].includes(interactionInspectionStatus) && Number(interactionInspectionStatus) === 1">
                <div class="pd-t40">
                    <letterSpacing text="互动巡检报告正在获取中，请五分钟后再查看..." :position="-3"></letterSpacing>
                </div>
            </div>
            <div v-else-if="![undefined,null,''].includes(interactionInspectionStatus) && Number(interactionInspectionStatus) === 3">
                <div class="font-s12 text-colorc2 text-center">
                    互动巡检报告生成失败...
                </div>
                <afp-button size="medium" style="float: right;margin:12px" :disabled="!isAuthenticated" @click="createInspectionReport('interactionInspection')">重新生成报告
                </afp-button>
            </div>
            <template v-else>
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">
                    暂未生成互动巡检报告...
                </div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    请前往客户端获取互动巡检报告...
                </div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    没有生成互动巡检报告权限...
                </div>
                <InspectionEmptyPage
                    v-else
                    type="interactionInspection"
                    :secondary-visible="canEnableAutoMonitor('interactionInspection', interactionInspectionMonitorEnabled)"
                    @secondary-click="switchInspection"
                    @primary-click="createInspectionReport('interactionInspection')"
                />
            </template>
        </div>

        <div v-else-if="isScriptRestoration" class="wordsBodyContainer ai-diagnose ai-content-box"
             :style="{ padding: scriptRestorationStatus == 2 ? '0 12px' : '0' }">
            <template v-if="Number(scriptRestorationMonitorEnabled) !== 1 && (scriptRestorationMonitorEnabled !== null && scriptRestorationMonitorEnabled !== undefined && scriptRestorationMonitorEnabled !== '') && Number(scriptRestorationStatus) === 0">
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">
                    暂未生成话术还原度报告...
                </div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    请前往客户端获取话术还原度报告...
                </div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    没有生成话术还原度报告权限...
                </div>
                <InspectionEmptyPage
                    type="scriptRestoration"
                    :secondary-visible="canEnableAutoMonitor('scriptRestoration', scriptRestorationMonitorEnabled)"
                    @secondary-click="openScriptRestorationDrawer('enable')"
                    @primary-click="openScriptRestorationDrawer('generate')"
                />
            </template>
            <div v-else-if="scriptRestorationStatus == 2">
                <div ref="scriptRestoration">
                    <div v-for="(item,index) in scriptRestorationContents" :key="index" :class="`scriptRestoration-${index+1}`"
                         class="pd-t4 pd-b4">
                        <div v-html="item"></div>
                    </div>
                </div>

<!--                <ConfirmRead v-if="!readonly" type="scriptRestoration" :reportId="scriptRestorationReportId" @confirm="handleConfirmRead"/>-->

            </div>

            <div v-else-if="![undefined,null,''].includes(scriptRestorationStatus) && Number(scriptRestorationStatus) === 1">
                <div class="pd-t40">
                    <letterSpacing text="话术还原度报告正在获取中，请五分钟后再查看..." :position="-3"></letterSpacing>
                </div>
            </div>
            <div v-else-if="![undefined,null,''].includes(scriptRestorationStatus) && Number(scriptRestorationStatus) === 3">
                <div class="font-s12 text-colorc2 text-center">
                    话术还原度报告生成失败...
                </div>
                <afp-button size="medium" style="float: right;margin:12px" :disabled="!isAuthenticated" @click="createInspectionReport('scriptRestoration')">重新生成报告
                </afp-button>
            </div>
            <template v-else>
                <div v-if="readonly" class="pd-t20 flex-jc-c font-s12 text-colorc2">
                    暂未生成话术还原度报告...
                </div>
                <div v-else-if="$isWeb" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    请前往客户端获取话术还原度报告...
                </div>
                <div v-else-if="!isSlefAuth" class="pd-t20 flex-jc-c font-s12 text-colorc3">
                    没有生成话术还原度报告权限...
                </div>
                <InspectionEmptyPage
                    type="scriptRestoration"
                    :secondary-visible="canEnableAutoMonitor('scriptRestoration', scriptRestorationMonitorEnabled)"
                    @secondary-click="openScriptRestorationDrawer('enable')"
                    @primary-click="openScriptRestorationDrawer('generate')"
                />
            </template>
        </div>

        <!-- 批注弹窗 -->
        <!-- <el-popover  v-model="showPopover" :reference="popoverReference" placement="top" width="500" trigger="manual">
            <div v-if="currentAnnotationItem">
                <div class="flex-jc-e">
                    <el-button class="pd-0" type="text" icon="el-icon-close" @click="closePopover"></el-button>
                </div>
                <Annotation ref="annotation" v-bind="currentAnnotationItem" :playData="annotationItemPlayData" @changePlayData="initPalyData" :videoCurrentTime="videoCurrentTime" :isPlayUrl="getPlayUrl" :item="currentAnnotationItem" type="popover" @play="annotationPlay"></Annotation>
            </div>
        </el-popover> -->

        <!-- 批注拖拽 -->
        <draggable v-if="showPopover" ref="draggable">
            <template #default="{collapse,unCollapse,status}">
                <div v-if="currentAnnotationItem" class="h100 w100 p-r" :class="status?'pd-4':'pd-10'">
                    <div class="flex-jc-e" :class="status?'p-a':''" :style="status?{right:0,top:'4px'}:{}">
                        <el-button v-if="status" class="pd-0" type="text" @click="unCollapse">
                            <img style="max-width: 15px;" src="@/assets/imgs/max.png" >
                        </el-button>
                        <el-button v-else class="pd-0" type="text" @click="collapse">
                            <img style="max-width: 15px;" src="@/assets/imgs/min.png" >
                        </el-button>
                        <el-button class="pd-0" type="text"  @click="closePopover">
                            <img style="max-width: 15px;" src="@/assets/imgs/close.png">
                        </el-button>
                    </div>
                    <Annotation ref="annotation"
                        v-bind="currentAnnotationItem"
                        :collapse="status"
                        :playData="annotationItemPlayData"
                        @changePlayData="initPalyData"
                        :videoCurrentTime="videoCurrentTime"
                        :isPlayUrl="getPlayUrl"
                        :item="currentAnnotationItem"
                        type="popover"
                        :isFileText="isFileText"
                        @play="annotationPlay">
                    </Annotation>
                </div>
            </template>
        </draggable>
        <AiDataReport ref="ai_data_report" reportType="video" @generateEnd="generateEnd" @jumpCurrentTab="jumpCurrentTab"/>
        <ScriptRestorationDrawer
            :visible.sync="scriptRestorationDrawerVisible"
            :textData="textData"
            :scene="scriptRestorationDrawerAction === 'enable' ? 'anchorConfig' : 'analysis'"
            :form="scriptRestorationDrawerForm"
            @confirmed="handleScriptRestorationConfirmed"
            @generate="createInspectionReport('scriptRestoration')"
        />
        <DiagnosisPop
            v-if="!monitorOnlyMode"
            :visible.sync="diagnosisPopVisible"
            :status="aiDiagnoseStatus"
            @nextStep="nextStep"/>
    </div>
</template>

<script>
/**
 * @description 段落内容容器：统一承接普通详情、云空间详情与 AI 助手的段落渲染，并按场景裁剪展示字段。
 */

import myUtils from '/src/utils/utils';
import Toolbar from './toolbar.vue'
import TextParagraph from './../textParagraph/index.vue'
import resize from './../../mixins/resize';
import OnlineNum from '../analysis/onlineNum.vue';
import TalkSpeed from '../analysis/talkSpeed.vue';
import NaturalTime from '../analysis/naturalTime.vue';
import SectionDeal from '../analysis/sectionDeal.vue'
import SectionInteractRate from '../analysis/sectionInteractRate.vue'
import SectionDealRate from '../analysis/sectionDealRate.vue';
import SectionSales from "@/components/analysis/sectionSales.vue";
import SectionRoiValue from "@/components/analysis/sectionRoiValue.vue";
import SectionUV from "@/components/analysis/sectionUV.vue";
import SpeedTime from '../analysis/speedTime.vue';
import AvatarImg from '../analysis/avatarImg.vue';
import bulletScreen from '../analysis/bulletScreen.vue';
import letterSpacing from './../letterShake/index.vue';
import copy from '@/utils/copy';
import contextmenu from '/src/mixins/contextmenu.js';
import ranges from './ranges.js';
import Annotation from '@/components/Annotation/index.vue';
import { exportHtmlToWord } from '@/utils/downFile.js';
import {isNil} from 'lodash'
import {VERSION_TYPE} from '@/enum'
import { AI_WORKBENCH_PANELS } from '@/utils/aiAgentRoute'
import draggable from './draggable.vue';
import auth from '@/mixins/auth.js';
import AiDataReport from "@/views/commonComponent/aiReport/aiDataReport.vue";
import AiContent from "@/components/analysis/ai/common/aiContent.vue";
import AiContentRenderer from "@/components/aiContentRenderer/index.vue";
import DiagnosisPop from "@/views/commonComponent/analysisLayout/component/diagnosisPop.vue";
import ScriptRestorationDrawer from '@/components/scriptRestoration/ScriptRestorationDrawer.vue';
import InspectionEmptyPage from './InspectionEmptyPage.vue';
import ScriptQualityReportContent from '@/components/aiMonitor/scriptQualityReportContent/index.vue';
import ProductDataTab from '@/components/analysis/productData/index.vue'
// import ConfirmRead from './confirmRead.vue';

export default {
    name: "",
    components: {
        DiagnosisPop,
        ScriptRestorationDrawer,
        InspectionEmptyPage,
        ScriptQualityReportContent,
        // ConfirmRead,
        AiContent,
        AiContentRenderer,
        AiDataReport,
        Toolbar,
        ProductDataTab,
        TextParagraph,
        // WordTable,
        // ControlTabs,
        OnlineNum,
        TalkSpeed,
        NaturalTime,
        SectionDeal,
        SectionDealRate,
        SectionInteractRate,
        SectionSales,
        SectionRoiValue,
        SectionUV,
        SpeedTime,
        AvatarImg,
        bulletScreen,
        letterSpacing,
        Annotation,
        draggable
    },
    mixins: [resize, contextmenu, auth],
    props: {
        // 所有数据
        textData: {
            type: Object,
            default: () => { return {} }
        },
        // 是否是对比分析。对比分析采用不同的样式
        isCompare: {
            type: Boolean,
            default: false
        },
        targetType: {
            type: String,
            default: ''
        },
        // 是否是视频数据
        isVideo: {
            type: Boolean,
            default: false
        },
        isFileText:{
            type: Boolean,
            default: false
        },
        // videoInfo: {
        //     type: Object,
        //     default: () => { return {} }
        // },
        // sentenceMarkList: {
        //     type: Array,
        //     default: null 
        // },
        treeData: {
            type: Array,
            default: () => { return [] }
        },
        videoCurrentTime: {
            type: Number,
            default: 0
        },
        selectedText: {
            type: String,
            default: ''
        },
        wordsInfo: {
            type: Object,
            default: () => {
                return {}
            }
        },
        enableSwappedTabs: {
            type: Boolean,
            default: false
        },
        currentParagraphIndex: {
            type: [Number],
            default: -1
        },
        isTable: {
            type: Boolean,
            default: true
        },
        name: {
            type: String,
            default: ''
        },
        notTarde: {
            type: Boolean,
            default: false
        },
        notSearch: {
            type: Boolean,
            default: false
        },
        notExport: {
            type: Boolean,
            default: false
        },
        textShow: {
            type: Boolean,
            default: false
        },
        isAi: {
            type:Boolean,
            default: false
        },
        notes: {
            type: Boolean,
            default: false,
        },
        readonly:{
            type: Boolean,
            default: false
        },
        isAnnotation:{
            type: Boolean,
            default: false
        },
        isRecording:{
            type: Boolean,
            default: false
        },
        isUpload:{
            type: Boolean,
            default: false
        },
        monitorOnlyMode: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        textTypeConfigForEmit() {
            return {
                aiOptimalStatus: this.aiOptimalStatus,
                aiShardingStatus: this.aiShardingStatus,
                aiDiagnoseStatus: this.aiDiagnoseStatus,
                scriptQualityStatus: this.scriptQualityStatus,
                interactionInspectionStatus: this.interactionInspectionStatus,
                scriptRestorationStatus: this.scriptRestorationStatus,
                scriptQualityUnread: this.scriptQualityUnread,
                interactionInspectionUnread: this.interactionInspectionUnread,
                scriptRestorationUnread: this.scriptRestorationUnread
            }
        },
        isAiOptimalStatus(){
            return !(['',0,3].some(d=>d=== Number(this.aiOptimalStatus)));
        },
        isAiShardingStatus(){
            return !(['',0,3].some(d=>d === Number(this.aiShardingStatus)));
        },
        // 什么源头类型可以选择行业
        isSelectTrade() {
            return this.targetType !== 'online' && this.targetType !== 'webOnline';
        },
        getPlayUrl() {
            return this.textData?.playUrl
        },
        getAnchorInfo() {
            return this.textData?.anchorInfo
        },
        notWordsType() {
            let wordsTypeMap = {
                '0': this.wordsInfo?.markSensitive,
                '1': this.wordsInfo?.markCrux
            }
            return wordsTypeMap;
        },
        // 获取所有文本列表（可对数据进行处理）
        // 获取所有文本列表（可对数据进行处理）
        getList() {
            const list = this.textArray || this.textData?.sentenceMarkList || []
            list.forEach(item => {
                item.batchNumber = this.textData?.videoInfo?.BatchNumber
            })
            return list
        },
        getWordListMap(){
            return this.wordListMap;
        },
        isText(){
            return this.textType === 'text'
        },
        isProductData() {
            return this.textType === 'productData' && this.refresh
        },
        isAiSharding(){
            return this.textType === 'aiSharding' && this.refresh
        },
        isAiDiagnose(){
            return this.textType === 'aiDiagnose' && this.refresh
        },
        isAiOptimal(){
            return this.textType === 'aiOptimal' && this.refresh
        },
        isPureRecordingVersion() {
            return this.versionType === VERSION_TYPE.PURE || this.$store.getters.isPure
        },
        isScriptQuality(){
            return !this.isPureRecordingVersion && this.textType === 'scriptQuality' && this.refresh
        },
        isInteractionInspection(){
            return !this.isPureRecordingVersion && this.textType === 'interactionInspection' && this.refresh
        },
        isScriptRestoration(){
            return !this.isPureRecordingVersion && this.textType === 'scriptRestoration' && this.refresh
        },
        isNilData() {
            return (num) => {
                return isNil(num)
            }
        },
        isShowBulletScreen(){
            if(this.isAi){
                return true
            }else{
                return this.isBulletScreen;
            }
        },
        isBuyInData(){
            return this.textData?.dataSourceType===1;
        },
        sliceRemarks(){
            return this.textData?.videoInfo?.videoSliceInfo?.remarks || this.textData?.uploadFile?.videoSliceInfo?.remarks
        },
        isAuthenticated () {
            const { videoInfo = {} } = this.textData
            return videoInfo?.UserId ? this.$auth([videoInfo?.UserId, videoInfo?.TenantId], 'every') : true
        },
        isAiAnalysisFinished() {
            const candidates = [
                this.textData?.analysisStatus,
                this.textData?.AnalysisStatus,
                this.textData?.videoInfo?.analysisStatus,
                this.textData?.videoInfo?.AnalysisStatus,
                this.textData?.fileInfo?.analysisStatus,
                this.textData?.fileInfo?.AnalysisStatus
            ]
            const raw = candidates.find(v => v !== undefined && v !== null && v !== '')
            if (raw === undefined) return false
            return Number(raw) === 2
        },
        getReplayType() {
            return myUtils.getReplayType(this.textData)
        },
        canUseAgentDisplay(){
            return this.isCompare || this.versionType === VERSION_TYPE.AGENT || this.targetType === 'webOnline' || this.targetType === 'online'
        },
        canShowParagraphRoi() {
            return !this.isAi
                && !this.isPureRecordingVersion
                && !this.isCompare
                && this.$store.getters.largeEnterprises
                && (
                    this.hasRoiValue(this.textData?.totalQianchuanCost)
                    || (this.textData?.sentenceMarkList || []).some(item => {
                        return this.hasRoiValue(item?.qianchuanCost)
                    })
                );
        },
        versionType(){
            return this.$store.getters.getVersionType
        }
    },
    data() {
        return {
            VERSION_TYPE,
            // 选中数据坐标，（敏感词，关键字，）公用。
            selectIndex: -1,
            // 搜索关键字
            selectWordIndex: -1,
            // 搜索文本内容
            keyWordText: '',
            // 所有字段以及铭感词等全部数据
            dataMap: {},
            // 搜索关键字数据
            keyWordMap: {},
            // 搜索关键字总数
            keyWordCount: 0,
            // 搜索关键字防抖函数
            FnWordCount: null,
            // 搜索加载
            keyWordLoad: false,
            // 选中数据，全部信息
            selectMark: {},
            // 上次选中数据
            oldSelectData: {},
            // 敏感词，关键词
            wordListMap: {},
            // 是否显示语速分析
            analysisChar: true,
            // 分页配置
            pages: [],
            wordTableLen: 0,
            refresh: false,
            clickFun: myUtils.debounce(200),
            clickTimes: 0,
            loadingTextNum: 0,
            textArray: [],
            loadingTimeOut:myUtils.debounce(300),
            isBulletScreen: true,//弹幕数
            dealVisible:true,//成交量
            interaction:false,//互动率
            dealRateVisible:true,//成交率
            salesVisible:true,//销售额
            uvVisible:false,//UV价值
            qianchuanCostVisible: true,//投放消耗
            // text: 正常文本  aiSharding: ai文本分段  aiOptimal: ai优化内容 aiDiagnose: ai数据诊断
            textType: 'text',
            typeMap:{
                0: 'text',
                1: 'aiSharding',
                2: 'aiOptimal',
                3: 'aiDiagnose'
            },
            aiShardings: [],
            aiOptimals:[],
            aiDiagnosis:[],
            aiShardingStatus: '',
            aiOptimalStatus: '',
            aiDiagnoseStatus:'',
            scriptQualityContents: [],
            interactionInspectionContents: [],
            scriptRestorationContents: [],
            scriptQualityStatus: '',
            interactionInspectionStatus: '',
            scriptRestorationStatus: '',
            scriptQualityMonitorEnabled: '',
            interactionInspectionMonitorEnabled: '',
            scriptRestorationMonitorEnabled: '',

            scriptQualityReportId: null,
            interactionInspectionReportId: null,
            scriptRestorationReportId: null,
            scriptQualityUnread: false,
            interactionInspectionUnread: false,
            scriptRestorationUnread: false,

            scriptRestorationDrawerVisible: false,
            scriptRestorationDrawerAction: 'generate',
            scriptRestorationDrawerForm: null,

            dataDisplay: {},
            textTypeKeyWordMap:{},
            rangeData: null,
            // 批注对象，集成了所有批注得位置储存器。
            annotationMap:{},

            showPopover: false,
            currentAnnotationItem: null,
            annotationItemPlayData: null,
            popoverReference: null,
            scrollHandler: null,
            playEventType: 'video',
            diagnosisPopVisible: false,
            aiDiagnoseId:'',
            observer: null
        };
    },
    watch:{
        textTypeConfigForEmit: {
            handler(val) {
                this.$emit('textTypeConfigChange', val)
            },
            deep: true,
            immediate: true
        },
        notes(val){
            if(val){
                // this.closePopover()
            }else{
                this.toActiveText();
            }
        },
        textType:{
            handler(val){
                if(val === 'aiDiagnose'&&this.aiDiagnoseStatus===2){
                   this.$nextTick(() => {
                       this.initObserver()
                   })
                }
            },
            deep: true,
            immediate: true,
        }
        // currentParagraphIndex(val,oldVal){
        //     if(val !== oldVal){
        //         if(this.currentAnnotationItem){
        //             this.annotationMarkClick({
        //                 annotationItem: this.currentAnnotationItem,
        //                 className: this.currentAnnotationItem?.className,
        //                 paragraphIndex:this.currentAnnotationItem.startIndex
        //             },1, 'notInit')
        //         }
        //     }
        // }
    },
    mounted() {
        if (!this.monitorOnlyMode) {
            this.getAiText();
        }
        this.setContextMenuData({
            permission: ['copy']
        })
        this.setTextScriptInfo()
        // 滚动关闭批注
        // this.addScrollListener();
    },
    created() {
        // 防抖处理查询关键字
        this.FnWordCount = myUtils.debounce(200, this.getWordCount);
        this.initLoadingArrayText();
    },
    beforeDestroy() {
        // 组件销毁前的回调
        this.loadingTextNum = 99999;
        this.removeScrollListener();
        if (this.observer) {
            this.observer.disconnect();
        }
    },
    methods: {
        normalizeAiMd(data) {
            if (Array.isArray(data)) {
                return data.filter(d => !!d).join('\n')
            }
            return data || ''
        },
        /**
         * @description 清洗导出前的渲染 DOM，移除样式节点、AI 标签残片和被当作正文的 CSS 文本。
         * 这里主要兜底 AI 脚本拆解/优化原文等通过 AiContentRenderer 渲染后的导出内容。
         * @param {HTMLElement|null} exportRoot 需要导出的根节点
         * @returns {string}
         */
        getCleanExportHtml(exportRoot) {
            if (!exportRoot) {
                return ''
            }
            const cloneRoot = exportRoot.cloneNode(true)
            const selectorsToRemove = [
                '.not-word',
                '.deepThinkingTitle',
                '.deepThinking',
                '.avatar-img-box',
                '.wordsBodyAvatar',
                '.online-num-img',
                '.onlineNumContainer',
                '.share-title-img'
            ]
            selectorsToRemove.forEach(selector => {
                cloneRoot.querySelectorAll(selector).forEach(node => node.remove?.())
            })
            cloneRoot.querySelectorAll('style,script,noscript,template,link,meta,title').forEach(node => node.remove?.())
            cloneRoot.querySelectorAll('.share-title img').forEach(node => node.remove?.())
            cloneRoot.querySelectorAll('*').forEach(node => {
                Array.from(node.attributes || []).forEach(attr => {
                    if (/^data-v-[\w-]+$/i.test(attr.name)) {
                        node.removeAttribute(attr.name)
                    }
                })
                const tagName = node.tagName?.toLowerCase?.() || ''
                if (tagName.startsWith('aifupan-')) {
                    const parentNode = node.parentNode
                    if (!parentNode) return
                    while (node.firstChild) {
                        parentNode.insertBefore(node.firstChild, node)
                    }
                    parentNode.removeChild(node)
                }
            })
            const stripArtifactText = (text = '') => {
                return String(text || '')
                    .replace(/&lt;style[\s\S]*?&lt;\/style&gt;/gi, '')
                    .replace(/<\/?aifupan-[^>\s]*/gi, '')
                    .split(/\r?\n/)
                    .filter(line => {
                        const lineText = String(line || '').trim()
                        if (!lineText) {
                            return true
                        }
                        return !(
                            /\.ai-mdtag-render-root\b|--ai-tag-primary:|--ai-tag-secondary:|--ai-tag-success:|--ai-tag-warning:|--ai-tag-danger:|--ai-tag-text:|--ai-tag-muted:/i.test(lineText)
                            || /\.aifupan-(?:card|title|tag|grid|alert|note|table|chart|collapse|statistic)\b/i.test(lineText)
                            || /^[.#][^{]+(?:\{|\s*,\s*[.#][^{]+)+(?:[\s\S]*)$/i.test(lineText)
                        )
                    })
                    .join('\n')
                    .trim()
            }
            const commentWalker = document.createTreeWalker(cloneRoot, NodeFilter.SHOW_COMMENT, null, false)
            const commentNodes = []
            while (commentWalker.nextNode()) {
                commentNodes.push(commentWalker.currentNode)
            }
            commentNodes.forEach(node => node.parentNode?.removeChild(node))
            const textWalker = document.createTreeWalker(cloneRoot, NodeFilter.SHOW_TEXT, null, false)
            const textNodesToRemove = []
            while (textWalker.nextNode()) {
                const currentNode = textWalker.currentNode
                const rawText = String(currentNode?.textContent || '')
                const cleanText = stripArtifactText(rawText)
                if (!cleanText.trim()) {
                    textNodesToRemove.push(currentNode)
                    continue
                }
                if (cleanText !== rawText.trim()) {
                    currentNode.textContent = cleanText
                }
            }
            textNodesToRemove.forEach(node => node.parentNode?.removeChild(node))
            return cloneRoot.innerHTML
        },
        setCharAndTime(time, char) {
            this.$refs?.toolbar?.setCharAndTime?.(time, char)
        },
        setPlayEventType(type){
            this.$set(this,'playEventType', type);
        },
        playStaus(data){
            this.$nextTick(()=>{
                const {playType,type} = data;
                this.setPlayEventType(playType);
                this.initPalyData({
                    playType,
                    type,
                    playStatus: {
                        play: false,
                        cycle: false,
                        [playType]: type === 'play'
                    }
                })
            })
        },
        exportWord(data={}){
            const {type,name} = data;
            const fileType = this.typeMap[type];
            if(!fileType){
                this.$message.error('文件类型错误')
                return;
            }
            let html = '';
            this.toActiveText(fileType);
            setTimeout(()=>{
                this.$nextTick(()=>{
                    if(fileType === 'text'){
                        html = this.getCleanExportHtml(this.$refs?.wordsBodyContainer);
                    }else{
                        html = this.getCleanExportHtml(this.$refs?.[fileType]);
                    }
                    if(this.$isAifupan){
                        exportHtmlToWord(html, name, '.docx', {output: true})
                            .then(blob => {
                                this.frontUpload(blob, name, {notFolder: 0, uploadType: 0, generationType:1, extension:'.docx'});
                            })
                    }else{
                        exportHtmlToWord(html, name, '.docx')
                    }
                })
            },500)
        },
        frontUpload(file, name, opt = {}) {
            const { notFolder, callback, otherObj, generationType, uploadType, finallyFn, extension = '.pdf' } = opt;
            let formData = new FormData();
            formData.append("file", file, `${name}${extension}`);
            formData.append("uploadType", uploadType);//1诊断报告-pdf
            formData.append("otherObj", JSON.stringify({
                notFolder,
                ...otherObj
            }));
            formData.append("generationType", generationType);
            return this.$httpClient.uploadFile.frontUpload(formData).then(res => {
                if (res.code === 0 && notFolder === 0) {
                    this.$message.success('文件下载成功');
                }
                callback && callback(res);
                return res.code === 0;
            }).catch(err => {
                return false;
            }).finally(() => {
                finallyFn && finallyFn();
            })
        },
        toTextMark(data){
            const { paraphStartNo, id } = data;
            this.$refs?.textParagraph?.[paraphStartNo]?.showAnnotationMark(id);
        },
        toActiveText(type){
            this.$nextTick(()=>{
                const targetType = type || 'text'
                if (this.$refs?.toolbar?.toActiveText) {
                    this.$refs.toolbar.toActiveText(targetType);
                    return
                }
                this.setTextType(targetType)
            })
        },
        annotationPlay(data){
            this.$emit('onTask',{
                type: 'play',
                data
            });
        },
        annotationMarkClick(o){
            const {annotationItem} = o;
            this.showPopover = true;
            this.palyInit();
            this.$nextTick(()=>{
                this.currentAnnotationItem = annotationItem;
                this.currentAnnotationItem.text =  this.currentAnnotationItem.text || this.currentAnnotationItem?.texts?.join('');
                // this.currentAnnotationItem.className = className;
                // this.$nextTick(()=>{
                //     this.$refs.draggable.init()
                // })
            })
        },
        palyInit(){
            this.$emit('onTask',{
                type: 'play',
                data: {
                    type: 'init'
                }
            });
        },
        closePopover(type) {
            this.showPopover = false;
            this.popoverReference = null;
            this.currentAnnotationItem = null;
            // if(type === 'notInit'){
            //     return;
            // }
            this.palyInit();
            this.initPalyData(null);
        },
        initPalyData(data){
            this.$set(this,'annotationItemPlayData',{
                playType: '',
                playStatus: {
                    play: false,
                    cycle: false
                },
                ...data
            });
        },
        // ...原有methods
        addScrollListener() {
            // 你可以根据需要监听 window 或某个父元素
            this.scrollHandler = () => {
                this.closePopover();
            };
            window.addEventListener('scroll', this.scrollHandler, true); // true 捕获阶段，适配更多场景
        },
        removeScrollListener() {
            if (this.scrollHandler) {
                window.removeEventListener('scroll', this.scrollHandler, true);
                this.scrollHandler = null;
            }
        },
        aiDataReport(){
            const row = {
                videoId:this.textData?.videoInfo?.VideoId,
                videoRename:this.textData?.videoInfo?.videoRename,
                videoName:this.textData?.videoInfo?.VideoName,
                anchorInfo:this.textData?.anchorInfo,
                startTime:this.textData?.videoInfo?.StartTime,
                durationStr:this.textData?.videoInfo?.Duration,
                outputName:this.textData?.videoInfo?.videoRename,
                reportFileName:this.textData?.videoInfo?.videoRename,
                tradeId: this.textData?.tradeInfo?.id,
                detailPage: true
            }
            this.$refs.ai_data_report?.changeDrawerStatus(true, row)
        },
        viewExample(){
            this.$httpClient.system.openUrl({url: 'https://ifupan.com/client/#/ai-share/0/d8f5575a-8cda-4ec3-8540-f6e4e97f97ce/4579819980654952448/0'});
        },
        generateEnd(callback){
            this.$emit('getAiReport', {type: 'aiDiagnose', status: this.aiDiagnoseStatus});
            callback?.()
        },
        jumpCurrentTab(){
            this.$refs.ai_data_report?.changeDrawerStatus?.(false, {})
        },
        createText(type){
            this.$emit('getText', {
                type: type,
                status:  type === 2? this.aiOptimalStatus : this.aiShardingStatus,
                event: 'click'
            });
        },
        rightContextMenuHanlder(event){
            let selectText = window.getSelection()?.toString()?.trim()
            this.rightContextMenu(event,{
                copyTxt: selectText
            })
        },
        copyText(type){
            this.$nextTick(()=>{
                let copyText = this.$refs[type]?.innerText;
                if(!copyText){return;}
                copy(copyText);
            })
        },
        getAiText(){
            if (this.monitorOnlyMode) return
            this.$nextTick(()=>{
                this.$emit('getText', {type: 'aiOptimal', status: this.aiOptimalStatus});
                this.$emit('getText', {type: 'aiSharding', status: this.aiShardingStatus});
                this.$emit('getAiReport', {type: 'aiDiagnose', status: this.aiDiagnoseStatus});
                // 话术质检/互动巡检/话术还原度状态由 wordDiscern.loadScriptMonitorStatuses 批量加载
                // this.$emit('getText', {type: 'scriptQuality', status: this.scriptQualityStatus});
                // this.$emit('getText', {type: 'interactionInspection', status: this.interactionInspectionStatus});
                // this.$emit('getText', {type: 'scriptRestoration', status: this.scriptRestorationStatus});
            })
        },
        getText(){
            this.$emit('getText', {
                type: this.textType,
                status: this.getTextTypeStatus(this.textType),
                event: 'click'
            });
        },
        // 点我开启XXX → emit 到 wordDiscern 执行 setMonitorEnabled + 弹窗 + 生成
        switchQuality(){
            this.$emit('enableMonitor', { type: 'scriptQuality', monitorType: 0 })
        },
        switchInspection(){
            this.$emit('enableMonitor', { type: 'interactionInspection', monitorType: 2 })
        },
        switchRestoration(){
            this.$emit('enableMonitor', { type: 'scriptRestoration', monitorType: 1 })
        },
        openScriptRestorationDrawer(action = 'generate') {
            const nextAction = action === 'enable' ? 'enable' : 'generate'
            const anchorInfo = this.textData?.anchorInfo || {}
            const secUid = anchorInfo?.secUid || anchorInfo?.SecUid || anchorInfo?.id || anchorInfo?.anchorId || anchorInfo?.anchorUrlUserId || this.textData?.secUid || this.textData?.SecUid || ''
            this.scriptRestorationDrawerAction = nextAction
            this.scriptRestorationDrawerForm = { secUid: String(secUid || '') }
            this.scriptRestorationDrawerVisible = true
        },
        handleScriptRestorationConfirmed(payload) {
            this.$emit('scriptRestorationConfirmed', payload)
        },
        createInspectionReport(type) {
            if (type === 'scriptQuality' && !this.isAiAnalysisFinished) return
            this.$emit('getText', {
                type,
                status: this.getTextTypeStatus(type),
                event: 'click'
            })
        },
        canEnableAutoMonitor(type, monitorEnabled) {
            if (Number(monitorEnabled ?? 0) === 1) return false
            if (this.readonly) return false
            if (this.$isWeb) return false
            if (!this.isSlefAuth) return false
            if (this.textData?.fileInfo?.fileId) return false
            if (Number(this.getTextTypeStatus(type) ?? 0) === 2) return false
            return ['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(type)
        },
        getTextTypeStatus(type) {
            const statusMap = {
                aiSharding: this.aiShardingStatus,
                aiOptimal: this.aiOptimalStatus,
                aiDiagnose: this.aiDiagnoseStatus,
                scriptQuality: this.scriptQualityStatus,
                interactionInspection: this.interactionInspectionStatus,
                scriptRestoration: this.scriptRestorationStatus
            }
            return statusMap[type]
        },
        setTextStatus(type,status){
            if(type === 'aiSharding'){
                this.aiShardingStatus = status;
            }else if(type === 'aiOptimal'){
                this.aiOptimalStatus = status;
            }else if(type === 'aiDiagnose'){
                this.aiDiagnoseStatus = status;
            }else if(type === 'scriptQuality'){
                this.scriptQualityStatus = status;
            }else if(type === 'interactionInspection'){
                this.interactionInspectionStatus = status;
            }else if(type === 'scriptRestoration'){
                this.scriptRestorationStatus = status;
            }
        },
        /**
         * @description 判断段落 ROI 值是否可展示，0 也视为有效值。
         * @param {string|number|null|undefined} value 指标值
         * @returns {boolean}
         */
        hasRoiValue(value) {
            return value !== undefined && value !== null && value !== '';
        },
        setMonitorEnabled(type, monitorEnabled) {
            const map = {
                scriptQuality: 'scriptQualityMonitorEnabled',
                interactionInspection: 'interactionInspectionMonitorEnabled',
                scriptRestoration: 'scriptRestorationMonitorEnabled'
            };
            const key = map[type];
            if (key) this[key] = monitorEnabled;
        },
        setReportId(type, reportId) {
            const map = {
                scriptQuality: 'scriptQualityReportId',
                interactionInspection: 'interactionInspectionReportId',
                scriptRestoration: 'scriptRestorationReportId'
            };
            const key = map[type];
            if (key) this[key] = reportId;
        },
        setUnread(type, unread) {
            const value = Boolean(unread)
            if (type === 'scriptQuality') {
                this.scriptQualityUnread = value
            } else if (type === 'interactionInspection') {
                this.interactionInspectionUnread = value
            } else if (type === 'scriptRestoration') {
                this.scriptRestorationUnread = value
            }
        },
        handleConfirmRead({ type, confirmRole }) {
            this.$emit('confirmRead', { type, confirmRole });
        },
        setTextData(type,data){
            //  // text: 正常文本  aiSharding: ai文本分段  aiOptimal: ai优化内容
            switch(type){
                case 'text':
                    this.refreshParagraph(data);
                    break;
                case 'aiSharding':
                    this.aiShardings = data;
                    break;
                case 'aiOptimal':
                    this.aiOptimals = data;
                    break;
                case 'aiDiagnose':
                    this.aiDiagnosis = data;
                    break;
                case 'scriptQuality':
                    this.scriptQualityContents = data;
                    break;
                case 'interactionInspection':
                    this.interactionInspectionContents = data;
                    break;
                case 'scriptRestoration':
                    this.scriptRestorationContents = data;
                    break;
            }
            this.setRefresh(true);
        },
        isShowOther(key) {
            if(this.isAi){
                let dMap = {
                    1:'startTime',
                    2:'natureTime',
                    3:'onlineNum',
                    4:'barrageNum',
                    5:'dealNum',
                    6:'interactionRate',
                    7:'dealRate',
                    8: 'analysisChar',
                    9: 'sales',
                    10: 'uv',
                    11: 'qianchuanCost'
                }
                if(typeof this.dataDisplay[key] === 'undefined'){
                    return this.dataDisplay[dMap[key]];
                }else{
                    return this.dataDisplay[key];
                }
            }else{
                return true
            }
        },
        setTextType(type) {
            if (this.isPureRecordingVersion && ['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(type)) {
                type = 'text'
            }
            if(type === 'aiSharding' && !this.isAi){
                this.$emit('getText', {type: 'aiSharding', status: this.aiShardingStatus});
            }
            if (type === 'aiDiagnose' && !this.isAi) {
                this.$emit('getAiReport', {type: 'aiDiagnose', status: this.aiDiagnoseStatus});
            }
            // 话术质检/互动巡检/话术还原度：切换时由 wordDiscern.changeTextType 调 reportStatus 刷新状态
            // if (['scriptQuality', 'interactionInspection', 'scriptRestoration'].includes(type) && !this.isAi) {
            //     this.$emit('getText', {type, status: this.getTextTypeStatus(type)});
            // }
            this.keyWord('');
            if(this.isAi && this.isVideo){
                if(this.textType === 'aiSharding' && !this.aiShardings?.length ||
                    this.textType === 'aiOptimal' && !this.aiOptimals?.length ||
                    this.textType === 'aiDiagnose' && !this.aiDiagnosis?.length||
                    this.textType === 'scriptQuality' && !this.scriptQualityContents?.length ||
                    this.textType === 'interactionInspection' && !this.interactionInspectionContents?.length||
                    this.textType === 'scriptRestoration' && !this.scriptRestorationContents?.length){
                    this.setRefresh(false);
                }
            }
            this.textType = type;
            // this.showPopover = false;
            this.$emit('changeTextType', type);
        },
        otherChange(val){
            this.dataDisplay = val;
        },
        initLoadingArrayText(){
            this.textArray = []
            this.loadingTextNum = 0;
            this.initLoadingText();
            this.setRefresh(true);
        },
        /**
         * 初始化加载文本函数
         * *
         * 此函数负责逐步加载和显示文本数据中的句子
         * 它通过递归调用自身来持续加载文本，直到达到预定义的停止条件
         */
         initLoadingText(){
            // 调用加载时间回调函数，用于控制加载节奏
            this.loadingTimeOut(()=>{
                
                // 检查是否达到最大加载次数，如果是，则停止加载
                if(this.loadingTextNum === 99999){
                    return
                }
                // 计算当前批次要加载的文本索引范围
                let s = this.loadingTextNum * 10;
                let e = s + 10;
                // 如果起始索引超出文本数据范围，则停止加载
                if(s > this.textData?.sentenceMarkList?.length){
                    // 文字加载完成 执行加载函数
                    this.$nextTick(()=>{
                        this.$emit('textLoad');
                    })
                    return;
                }
                // 从文本数据中截取当前批次的文本并添加到显示数组中
                let ds = this.textData?.sentenceMarkList?.slice(s,e)
                this.textArray.push(...ds);
                // 增加加载次数计数
                this.loadingTextNum+=1;
                // 递归调用自身以继续加载剩余文本
                this.initLoadingText();
            })
        },
        refreshParagraph(list, option={}){
            const {defaultHide} = option;
            if(defaultHide){
                this.setRefresh(false)
            }
            this.loadingTextNum = 99999;
            this.textArray = list || this.textData?.sentenceMarkList || [];
            this.refreshHandler(option);
        },
        setAnalysisChar(val) {
            this.analysisChar = val
            this.setTextScriptInfo()
        },
        setIsBulletScreen(val){
            this.isBulletScreen = val
            this.setTextScriptInfo()
        },
        setDeal(val){
            this.dealVisible = val;
            this.setTextScriptInfo()
        },
        setSales(val){
            this.salesVisible = val;
            this.setTextScriptInfo()
        },
        setUV(val){
            this.uvVisible = val;
            this.setTextScriptInfo()
        },
        setQianchuanCost(val){
            this.qianchuanCostVisible = val;
            this.setTextScriptInfo()
        },
        setInteractionRate(val){
            this.interaction = val;
            this.setTextScriptInfo()
        },
        setDealRate(val){
            this.dealRateVisible = val;
            this.setTextScriptInfo()
        },
        setTrade(id) {
            this.$refs?.toolbar?.setTradeId(id);
        },
        setTextScriptInfo() {
            this.$emit('setTextScriptInfo', {
                isBulletScreen: this.isBulletScreen,//弹幕数
                salesVisible:this.salesVisible && this.isBuyInData,//销售额
                uvVisible:this.uvVisible && this.isBuyInData,//uv价值
                dealVisible: this.dealVisible && this.isBuyInData,//成交量
                interaction: this.interaction,//&&item.isBulletScreen,//互动率
                dealRateVisible: this.dealRateVisible && this.isBuyInData,//成交率
                qianchuanCostVisible: this.qianchuanCostVisible,//投放消耗

                analysisChar: this.analysisChar,//语速
            })
        },
        dropDown() {
            this.$refs?.toolbar?.dropDown();
        },
        setRefresh(bl){
            this.refresh = bl;
            if(bl){
                this.$nextTick(()=>{

                    this.setAnnotationHtml();
                })
            }
        },
        treeChange(id) {
            this.$emit('treeChange', id)
        },
        treeLoad(tree) {
            this.$emit('treeLoad', tree)
        },
        refreshHandler(option={}) {
            this.setRefresh(false);
            this.$nextTick(() => {
                const { notLoadText } = option;
                if(notLoadText){
                    this.setRefresh(true);
                    
                    return
                }
                setTimeout(() => {
                    this.setRefresh(true);
                    this.initLoadingArrayText();
                }, 200)
            });
        },
        total(v) {
            this.wordTableLen = v;
        },

        paragraphClick(index, type) {
            if(typeof index === 'undefined' || index<0){return}
            this.$emit('update:currentParagraphIndex', index);

        },
        selectMarkHandler(name, number) {
            //储存上次选中状态
            this.$set(this, 'oldSelectData', JSON.parse(JSON.stringify(this.selectMark)))
            if (this.selectMark.name !== name) {
                // 选中数据清空
                this.selectMark = {};
                this.selectMarkDataIndex(name, 0);
            } else {
                // 根据上下操作内容
                this.selectMarkDataIndex(name, this.selectIndex + number);
            }
        },
        // 选中字段窗口跳转
        selectDomeScrollIntoView(paragraphIndex, data, type) {
            this.$nextTick(() => {
                let dom = null
                // 没有data，表示跳转段落开头
                if (data === undefined) {
                    dom = document.getElementsByClassName(this.name + 'textParagraph-' + paragraphIndex)?.[0];
                } else {
                    let p = typeof paragraphIndex !== 'undefined' ? paragraphIndex : this.selectMark.paragraphIndex;
                    let d = typeof data !== 'undefined' ? data : this.selectMark.datas[0];
                    dom = document.getElementsByClassName(this.name + `char_${p}_${d}`)?.[0];
                }
                // 跳转视窗
                if (dom) {
                    this.$nextTick(() => {
                        if(type === 'annotation'){
                            const domParent = this.$refs.wordsBodyContainer;
                            if (!domParent || !dom) return;
                            // 获取 span 相对于视口的位置
                            const spanRect = dom.getBoundingClientRect();
                            // 获取父容器相对于视口的位置
                            const parentRect = domParent.getBoundingClientRect();
                            // 计算 span 在父容器内部的相对位置
                            const spanPositionInParent = spanRect.top - parentRect.top + domParent.scrollTop;
                            // 调整滚动位置（例如，向上偏移 30px 让 span 不紧贴顶部）
                            const scrollToPosition = spanPositionInParent - 20;
                            // 确保不会滚动超出边界
                            const maxScroll = domParent.scrollHeight - domParent.clientHeight;
                            const finalScrollPosition = Math.max(0, Math.min(scrollToPosition, maxScroll));
                            // 平滑滚动
                            domParent.scrollTo({
                                top: finalScrollPosition,
                                behavior: 'smooth'
                            });
                        }else{
                            dom.scrollIntoView({ behavior: "smooth" })
                        }
                    })
                }
            })
        },
        // 选中敏感词或关键词
        selectMarkDataIndex(name, index) {
            let list = this.wordListMap[name];
            if (!list?.length) { return }
            if (index < 0) {
                index = list.length - 1
            } else if (index > list.length - 1) {
                index = 0;
            }
            this.selectIndex = index;
            this.saveSelectData(list[this.selectIndex], name)
        },
        // 搜索数据选中
        selectKeyWordIndex(index) {
            if(!this.isText){
                this.selectTextKeyWordIndex(index)
                return
            }
            this.$set(this, 'oldSelectData', JSON.parse(JSON.stringify(this.selectMark)))
            // 跳转第一条数据（向后选中）
            if (index > this.keyWordList.length - 1) {
                index = 0
            }
            // 跳转最后一条数据（向前选中）
            if (index < 0) {
                index = this.keyWordList.length - 1
            }
            // 减去1 获取数据坐标
            this.selectWordIndex = index;
            // 获取选中数据
            this.saveSelectData(this.keyWordList[this.selectWordIndex])
        },
        // 储存选中数据
        saveSelectData(selectData, name) {
            //储存新状态
            let ds = selectData?.datas || [];
            this.paragraphClick(selectData?.paragraphIndex, 'saveSelectData')
            let word = ds?.map(i => i.char).join('');
            this.$set(this, 'selectMark', {
                paragraphIndex: selectData?.paragraphIndex,
                datas: ds?.map(item => item.index) || [],
                word: word,
                name: name || word,
                // wordsType: item.wordsType
            })
            setTimeout(() => {
                if (this.selectMark?.datas?.[0] !== undefined) {
                    this.selectDomeScrollIntoView(this.selectMark.paragraphIndex, this.selectMark?.datas[0])
                    this.playerPause();// 暂停播放
                }
            }, 100)
        },
        // 获取搜索数据总数
        getWordCount() {
            if(!this.keyWordText){return}
            // 合并三维数组。最终获取所有段落排序好之后的查询数据（下放到每个字段的查询）
            this.keyWordList = [].concat(...Object.values(this.keyWordMap).map(item => {
                return [].concat(...Object.values(item).map(sItem => sItem.indexAll));
            }))
            // 计算三维数组和
            this.keyWordCount = this.keyWordList.length;
            if(this.keyWordCount<=0){return}
            this.selectKeyWordIndex(0);
            // 判断是否加载显示查询总数
            this.keyWordLoad = !!this.keyWordText
        },
        //更新全文本段搜索数据
        updateKeyWord(data) {
            //储存每段的查询数据
            if (data.notData) {
                // 清除数据
                this.$delete(this.keyWordMap, data.paragraphIndex);
            } else {
                // 储存数据
                this.keyWordMap[data.paragraphIndex] = data.data
            }
            this.FnWordCount();
        },
        // 更新全文本段，关键字，敏感字数据
        updateData(data) {
            //储存每段的字节数据(包含单个字符，最小粒子为一个字)
            this.dataMap[data.paragraphIndex] = data;
            // 储存所有敏感字，关键词数据map
            Object.keys(data.wordListMap).forEach(key => {
                if (!Array.isArray(this.wordListMap[key])) {
                    this.wordListMap[key] = [];
                }
                // 限制除去重复数据
                if (this.wordListMap[key].some(item => {
                    let indexData = data.wordListMap[key].indexAll[0];
                    return indexData.index === item.index && indexData.paragraphIndex === item.paragraphIndex;
                })) {
                    return
                }
                this.wordListMap[key].push(...data.wordListMap[key].indexAll);
            })
        },
        // 设置播放器进度，豪秒
        playerReadied(second) {
            this.setPlayEventType('click')
            this.$emit('playerReadied', second, 'click');
            // if (this.$refs.videoPlayer) {
            //     this.$refs.videoPlayer.player.currentTime(second / 1000);
            //     this.$refs.videoPlayer.player.play();
            // }
        },
        // 暂停播放器
        playerPause() {
            this.$emit('playerPause')
        },
        mousedownHandle($event){
            this.clickTimes = new Date().getTime();
        },
        initAnnotationText(){
            this.$set(this, 'annotationMap', {});
        },
        /**
         * 获取标注文本信息
         * @param {Object} config 配置对象
         * @param {Number} config.startIndex 开始索引
         * @param {Number} config.endIndex 结束索引
         * @param {Number} config.startOffset 开始偏移量
         * @param {Number} config.endOffset 结束偏移量
         * @return {Object} 返回标注文本相关信息
         */
        getAnnotationText(config){
            const { startIndex, endIndex, startOffset, endOffset} = config;
            const { sentenceMarkList } = this.textData;
            // 获取开始索引对应的时间（毫秒）
            // const sTime = (sentenceMarkList[startIndex]?.startTimeSecond || 0) * 1000;
            // 初始化返回数据对象
            const data = {
                text: '',         // 标注文本内容
                textTime: 0,      // 文本时间（格式化后）
                startTime: 0,     // 开始时间（原始值）
                endTime: 0
            };
            const paraphIndexs = Array.from({ length: endIndex - startIndex + 1 }, (_, i) => i + startIndex);
            let texts = [];
            paraphIndexs.forEach(index=>{
                if(this.annotationMap[index]?.some(d=>d[2].id === config.id)){
                    return;
                };
                // 确保标注映射对象中有对应索引的数组
                if(typeof this.annotationMap[index] === 'undefined'){
                    this.$set(this.annotationMap, index, []);
                };
                let d = this.dataMap[index]?.itemsMap;
                if (!d || typeof d.length === 'undefined') {
                    d = sentenceMarkList?.[index]?.items || []
                }
                const dValues = Array.isArray(d) ? d : Object.values(d || {});
                const dLen = dValues.length;
                let sO = 0,eO = 0;
                let isEnd;
                if(index === startIndex && startIndex === endIndex){
                    sO =  startOffset;
                    eO = endOffset;
                    // 设置开始时间
                    data.startTime = data.startTime || dValues?.[sO]?.startTime;
                    data.endTime =data.endTime || dValues?.[eO]?.endTime;
                    isEnd = false;
                }else if(index === startIndex){
                    sO = startOffset;
                    eO = dLen - 1;
                    data.startTime =data.startTime || dValues?.[sO]?.startTime;
                    data.endTime =data.endTime || dValues[dLen-1]?.endTime;
                    isEnd = false;
                }else if(index === endIndex){
                    sO = 0;
                    eO = endOffset;
                    data.startTime =data.startTime || dValues?.[0]?.startTime;
                    data.endTime =data.endTime || dValues?.[eO]?.endTime;
                    isEnd = true;
                }else{
                    sO = 0;
                    eO = dLen - 1;
                    data.startTime =data.startTime || dValues?.[0]?.startTime;
                    data.endTime =data.endTime || dValues[dLen-1]?.endTime;
                    isEnd = true;
                }
                // 转换时间格式为中文表示
                data.textTime = typeof data.startTime !== 'undefined' && myUtils.toformatTime(data.startTime) || '';
                // 获取文本片段
                const t = dValues?.slice(sO, eO + 1)?.map(d=>{
                    return d.char;
                }).join('');
                if(t){
                    texts.push(t);
                }
                if(!this.annotationMap[index]?.some(d=>d[2].id === config.id)){
                    this.annotationMap[index].push([sO, eO, {
                        ...config,
                        ...data,
                        texts,
                        isEnd
                    }]);
                }
            });
            data.text = texts.join('');
            return data;
        },
        // 设置批注
        setAnnotationHtml(){
            let iList = Object.keys(this.annotationMap);
            this.$nextTick(()=>{
                iList.forEach(i=>{
                    this.$refs?.textParagraph?.[i]?.setAnnotationHtml();
                })
            })    
        },
        saveAnnotation(data){
            const {paraphEndNo,paraphStartNo} = data || {};
            const indexs = Array.from({ length: paraphEndNo - paraphStartNo + 1 }, (_, i) => i + paraphStartNo);
            indexs?.forEach(i=>{
                this.annotationMap[i]?.forEach(d=>{
                    if(d[2].id === data.id){
                        this.$set(d,2,{
                            ...d[2],
                            ...data
                        });
                    }
                });
                this.$refs?.textParagraph?.[i]?.setAnnotationHtml();
            })
        },
        // 删除批注
        delAnnotationHtml(data={}){
            const { paraphEndNo , paraphStartNo, id } = data;
            const indexs = Array.from({ length: paraphEndNo - paraphStartNo + 1 }, (_, i) => i + paraphStartNo);
        
            this.$nextTick(()=>{
                indexs.forEach(i=>{
                    
                    if(this.annotationMap[i]?.length){
                        this.annotationMap[i] = this.annotationMap[i]?.filter(d=>{
                            return d[2].id !== id;
                        });
                        this.$nextTick(()=>{
                            this.$refs?.textParagraph?.[i]?.getContentHtml();
                        })
                    }
                })
            })
        },
        // 判断选区是否重叠批注
        ifAnnotation(startIndex, endIndex, startOffset, endOffset) {
            if (startIndex === endIndex) {
                // 如果在同一段落内
                const l = this.annotationMap[startIndex];
                // 确保annotationMap[startIndex]存在
                if (!l || !l.length) return false;
                return l.some(d => {
                    // 开始位置等于上次结束位置
                    if(startOffset === d[1]){
                        return true;
                    }else {
                        // 检查区间是否重叠: [startOffset, endOffset] 与 [d[0], d[1]]
                        // 两个区间重叠的条件是: !(endOffset < d[0] || d[1] < startOffset)
                        return !(endOffset <= d[0] || d[1] <= startOffset);
                    }
                });
            } else {
                // 跨段落的情况
                const sl = this.annotationMap[startIndex];
                const el = this.annotationMap[endIndex];
                
                // 检查开始段落
                const startOverlap = sl && sl.length ? sl.some(d => {
                    // 在开始段落中，我们检查 [startOffset, 段落结束] 与现有区间的重叠
                    return !(d[1] < startOffset); // startOffset在d[1]之前，则有重叠
                }) : false;
                
                // 检查结束段落
                const endOverlap = el && el.length ? el.some(d => {
                    // 在结束段落中，我们检查 [0, endOffset] 与现有区间的重叠
                    return !(endOffset < d[0]); // endOffset在d[0]之后，则有重叠
                }) : false;
                
                return startOverlap || endOverlap;
            }
        },
        // 选中文本
        handleSelectText(event) {
            const r = ranges(event);
            const { startIndex,endIndex,startOffset,endOffset} = r || {};
            if(typeof startIndex !== 'undefined' && typeof endIndex !== 'undefined'){
                const sD = this.dataMap[startIndex]?.itemsMap;
                const eD = this.dataMap[endIndex]?.itemsMap;
                // 如果结束位置进行数字化时出现nan则将结束位置设置为开始段落最后一个字符
                if(isNaN(Number(r.endIndex))){
                    r.endIndex = r.startIndex;
                    r.endOffset = this.dataMap[r.startIndex]?.itemsMap?.length - 1;
                }
                this.rangeData = {
                    ...r,
                    items: {
                        startItem: sD?.[startOffset],
                        endItem: eD?.[endOffset]
                    }
                };
                // 判断选区是否重叠批注
                this.rangeData.ifAnnotation = this.ifAnnotation(startIndex,endIndex,startOffset,endOffset);
            }

            if(event.button !== 0){return}
            // 获取当前选中的文本
            const duration = new Date().getTime() - this.clickTimes;
            const selection = window.getSelection();
            if(duration>300){
                if (selection.rangeCount > 0) {
                    const selectedTextString = selection.toString();
                    this.$emit("selectedText", selectedTextString.trim());

                    const pNodeTextParagraph = parseInt(selection?.anchorNode?.parentNode?.className?.split('textParagraph-')?.[1]);
                    let otherData = {}
                    if(!isNaN(pNodeTextParagraph) && pNodeTextParagraph){
                        const anchorOffset = selection?.anchorOffset;
                        let selectData = this.dataMap[pNodeTextParagraph]?.itemsMap?.[anchorOffset - 1];
                        otherData = {
                            ...selectData,
                            paragraphIndex: pNodeTextParagraph
                        }
                    }
                    this.$nextTick(() => {
                        let pointerEvent = new PointerEvent('contextmenu', {
                            bubbles: true,
                            cancelable: true,
                            // 基本鼠标属性
                            clientX: event.clientX,
                            clientY: event.clientY,
                            screenX: event.screenX,
                            screenY: event.screenY,
                            // PointerEvent 特殊属性
                            pointerId: 1,
                            pointerType: 'mouse',
                            isPrimary: true,
                            // 右键相关属性
                            button: 2,  // 右键
                            buttons: 2, // 右键按下
                            pressure: 0.5,
                            // 修饰键状态
                            altKey: event.altKey,
                            ctrlKey: event.ctrlKey,
                            shiftKey: event.shiftKey,
                            metaKey: event.metaKey,
                        });
                        setTimeout(()=>{
                            // pointerEvent.target.otherData= otherData,
                            event.target.dispatchEvent(pointerEvent);
                        },200)
                        event.preventDefault();
                    })
                }
            }else{
                this.clickTimes = 0;
                if(this.isAnnotation){
                    selection?.removeAllRanges();
                    return
                }
                if(event.target?.className?.indexOf('char_')>=0){
                    return
                }
                const pNodeTextParagraph = parseInt(selection?.anchorNode?.parentNode?.className?.split('textParagraph-')?.[1]);
                const anchorOffset = r.clickIndex || selection?.anchorOffset;

                if(isNaN(pNodeTextParagraph) || anchorOffset === -1){
                    event.preventDefault();
                }else if(this.currentParagraphIndex !== pNodeTextParagraph){
                    this.paragraphClick(pNodeTextParagraph,'SelectText');
                    this.$nextTick(() => {
                        let selectData = this.dataMap[pNodeTextParagraph]?.itemsMap?.[anchorOffset - 1];
                        this.$nextTick(() => {
                            if(selectData.startTime){
                                this.playerReadied(selectData.startTime)
                            }
                        })
                    })
                }
                selection?.removeAllRanges();
            }
        },
        // 毫秒时间戳转成时分秒格式
        toformatTime(val) {
            return myUtils.toformatTime(val);
        },
        //右键打开自定义菜单
        rightTickContextMenu(event) {
            // this.contextmenuVisible = true
            this.$nextTick(() => {
                this.$emit('rightTickContextMenu', {
                    event,
                    data: this.rangeData,
                })
            })
        },
        clearKeyWord(text) { 
            this.keyWordCount = 0;
            this.selectIndex = -1;
            this.selectWordIndex = -1;
            this.keyWordText = text ?? '';
            if(typeof text === 'undefined'){
                this.keyWordMap = {};
            }
        },  
        keyWord(text) {
            if(!this.isText){
                this.clearKeyWord();
                this.setTextTypeKeyWord(text);
                return
            }
            this.selectMark = {};
            this.clearKeyWord(text);
        },
        setTextTypeKeyWord(text){
            this.textTypeKeyWordMap[this.textType] = {
                text
            }
            this.selectWordIndex = 0;
            this.initTextTypeKeyWordDom();
        },
        selectTextKeyWordIndex(index){
            if(typeof this.textTypeKeyWordMap[this.textType] !== 'undefined'){
                if(this.keyWordCount < index + 1){
                    index = 0
                }else if(index<0){
                    index = this.keyWordCount -1;
                }
                this.textTypeKeyWordMap[this.textType].oldIndex = this.selectWordIndex;
                this.selectWordIndex = index;
                this.$nextTick(() => {
                    const {oldIndex} = this.textTypeKeyWordMap[this.textType];
                    let oldDom = document.getElementsByClassName(`${this.textType}-${oldIndex + 1}`)?.[0];
                    if(oldDom){
                        oldDom.style.background = 'darkorange';
                    }
                    let dom = document.getElementsByClassName(`${this.textType}-${index + 1}`)?.[0];
                    dom.style.background = 'limegreen';
                    // 跳转视窗
                    if (dom) {
                        this.$nextTick(() => {
                            dom.scrollIntoView({ behavior: "smooth" })
                        })
                    }
                })
            }
        },
        initTextTypeKeyWordDom(){
            const { text } = this.textTypeKeyWordMap[this.textType];
            let list = this[`${this.textType}s`] || [];
            if (!list.length || !text) { 
                document.querySelectorAll('.textType-keyWord').forEach((item) => {
                    item.style.background = '';
                });
                return; 
            }
            let countNum = 0;
            list = list.map((txt, index) => {
                const regex = new RegExp(text, 'gi');
                let matchCount = 0;
                const newText = txt.replace(regex, (match) => {
                    matchCount++;
                    countNum++;
                    return `<span class="${this.textType}-${countNum} ${this.textType}-${index}-${matchCount} textType-keyWord" style="background:darkorange;">${match}</span>`;
                });
                return newText;
            });
            this.keyWordCount = countNum;
            this.$set(this,`${this.textType}s`, list);
            this.selectTextKeyWordIndex(0);
        },
        changeDiagnosisPopVisible(status,id){
            this.diagnosisPopVisible = status
            if(id) this.aiDiagnoseId = id
        },
        nextStep(){
            this.changeDiagnosisPopVisible(false)
            this.openAiWorkbench()
        },
        /**
         * @description 跳转 AI 工作台并绑定当前主播和视频（与"AI诊断本场(新)"走同一入口）。
         * @returns {void}
         */
        openAiWorkbench() {
            const anchorInfo = this.textData?.anchorInfo || {}
            const videoInfo = this.textData?.videoInfo || {}
            const secUid = String(anchorInfo?.SecUid || anchorInfo?.secUid || '').trim()
            const videoId = String(videoInfo?.VideoId || videoInfo?.videoId || '').trim()
            if (this.$httpClient?.system?.openAIAgentWeb) {
                this.$httpClient.system.openAIAgentWeb({
                    panel: AI_WORKBENCH_PANELS.ROOM,
                    secUid,
                    videoId,
                    cue: '0',
                    openInBrowserWindow: this.targetType === 'webOnline'
                })
                return
            }
            this.$router.push({ path: '/aiAssistant' })
        },
        initObserver() {
            const options = {
                root: null,
                rootMargin: '0px',
                threshold: 0.1
            };

            this.observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        if (this.aiDiagnoseId) {
                            this.$httpBack.v2500.updateReadStatus({
                                ids: [this.aiDiagnoseId],
                                isRead: 1
                            })
                        }
                    }
                });
            }, options);

            this.observer.observe(this.$refs.aiDiagnose);
        }
    }
};
</script>

<style scoped lang="scss">
.discernSearchContainerContent {
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: hidden;
    position: relative;
}

.aiSliceRemarks{
    width: 100%;
    padding: 14px 16px;
    margin: 2px 0 10px 0;
    background: #F5F6FF;
    border-radius: 10px 10px 10px 10px;
}

.item-font{
    padding-right: 12px
}
.wordsBodyContainer::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 4px;
}

::v-deep(.ai-diagnose) {
    .ai-diagnose-content {
        padding: 0 120px 0 24px;
    }

    .deepThinking {
        display: none;
    }
}
.wordsBodyContainer::-webkit-scrollbar {
    width: 4px;
}

.wordsBodyContainer {
    text-rendering: optimizeLegibility;
    padding-right: 4px;
    flex: 1;
    // max-height: calc(100vh - 240px - 300px - 101px);
    overflow: hidden;
    overflow-y: auto;
    box-sizing: border-box;
    -webkit-user-select: text;
    -moz-user-select: text;
    -ms-user-select: text;
    user-select: text;

    ::v-deep(.md-format-box-left) {
        padding: 0 120px 0 24px;
    }
}

.productDataScrollWrapper {
    flex: 1;
    overflow: hidden;
    overflow-y: auto;
    box-sizing: border-box;
}

.scriptQualityDetailContainer {
    padding: 0 !important;
}

.scriptQualityDetailContent {
    padding: 0;
}

.scriptQualityDetailContent {
    ::v-deep(.scriptQualityReportContent) {
        padding-top: 0;
    }

    ::v-deep(.scriptQualityReportContent .sq-summary) {
        width: 100%;
    }

    ::v-deep(.scriptQualityReportContent .sq-sections) {
        width: calc(100% - 120px);
        margin: 0 auto 0 0;
    }
}

.wordsBodyItemContainer {
    margin-bottom: 12px;
    display: flex;
    word-wrap: break-word;
    word-break: break-all;
}

.wordsBodyAvatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: 0.5px #ccc solid;
}

.wordsBodyContentContainer {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    margin-left: 8px;
    flex-grow: 1;
}

//.wordsBodyContentText2 {
//    font-weight: 400;
//    font-size: 12px;
//    color: #95A1AF;
//}

.wordsBodyContentText1 {
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
}

.paragraphContainer-txt {
    font-size: 14px;
    color: #2E3742;
    position: relative;
    line-height: 22px;
    letter-spacing: 1px;
    
}

.is-annotation{
    // ::v-deep(>div){
    //     user-select: contain  !important;
    //     -webkit-user-select: text;
    // }
    // ::v-deep(.content-text){
    //     user-select:none !important;
    //     display:none;
    // }
    // ::v-deep(.text-paragraph-item){
    //     position: relative;
    // }
    // ::v-deep(span){
    //     background: none !important;
    // }
    // ::v-deep(.word-time-hover){
    //     background: var(--color-main) !important;
    // }
}


</style>
