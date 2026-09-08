<template>
  <div class="page-tabs-doc-container">
    <div class="doc-section">
      <h2>PageTabs 页面级标签页</h2>
      <p>用于页面内容切换的标签页组件，支持自定义标签名称、Badge 徽标、插槽内容。</p>
    </div>

    <div class="doc-section">
      <h3>基础用法</h3>
      <p>传入字符串数组，默认生成 tab1, tab2... 作为 value。</p>
      <PageTabs :tabs="['直播业绩', '直播场次']">
        <template #tab1>
          <div class="demo-content">这是直播业绩的内容 (Slot: tab1)</div>
        </template>
        <template #tab2>
          <div class="demo-content">这是直播场次的内容 (Slot: tab2)</div>
        </template>
      </PageTabs>
    </div>

    <div class="doc-section">
      <h3>自定义配置 (Badge & Value)</h3>
      <p>传入对象数组，自定义 label, value, badge。</p>
      <PageTabs :tabs="customTabs" v-model="activeTab">
        <template #performance>
          <div class="demo-content">业绩数据面板 (Active: {{ activeTab }})</div>
        </template>
        <template #session>
          <div class="demo-content">场次列表面板</div>
        </template>
        <template #analysis>
          <div class="demo-content">数据分析面板</div>
        </template>
      </PageTabs>
    </div>

    <div class="doc-section">
      <h3>卡片样式 (Card Style)</h3>
      <p>添加 <code>card</code> 属性，使容器呈现卡片样式（白色背景、阴影、内边距）。</p>
      <div style="background: #f5f7fa; padding: 20px; border-radius: 4px">
        <PageTabs :tabs="['直播业绩', '直播场次']" card>
          <template #tab1>
            <div class="demo-content">卡片模式下的内容 (Tab 1)</div>
          </template>
          <template #tab2>
            <div class="demo-content">卡片模式下的内容 (Tab 2)</div>
          </template>
        </PageTabs>
      </div>
    </div>

    <div class="doc-section">
      <h3>代码示例</h3>
      <pre><code>
&lt;PageTabs :tabs="['Tab A', 'Tab B']"&gt;
  &lt;template #tab1&gt;Content A&lt;/template&gt;
  &lt;template #tab2&gt;Content B&lt;/template&gt;
&lt;/PageTabs&gt;

&lt;PageTabs :tabs="[{ label: '业绩', value: 'p', badge: 1 }, { label: '场次', value: 's' }]"&gt;
  &lt;template #p&gt;Performance&lt;/template&gt;
  &lt;template #s&gt;Sessions&lt;/template&gt;
&lt;/PageTabs&gt;
      </code></pre>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  import PageTabs from '@/components/PageTabs/index.vue'

  const activeTab = ref('performance')

  const customTabs = [
    { label: '直播业绩', value: 'performance', badge: 12 },
    { label: '直播场次', value: 'session' },
    { label: '数据分析', value: 'analysis', badge: 'New' }
  ]
</script>

<style scoped lang="scss">
  .page-tabs-doc-container {
    padding: 24px;
    background-color: #f5f7fa;
    min-height: 100vh;
  }

  .doc-section {
    background: white;
    padding: 24px;
    border-radius: 8px;
    margin-bottom: 24px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

    h2 {
      margin-bottom: 16px;
      font-size: 24px;
      font-weight: 600;
    }

    h3 {
      margin-bottom: 16px;
      font-size: 18px;
      font-weight: 600;
      border-left: 4px solid #409eff;
      padding-left: 12px;
    }

    p {
      margin-bottom: 16px;
      color: #606266;
    }

    pre {
      background: #f5f7fa;
      padding: 16px;
      border-radius: 4px;
      overflow-x: auto;
      font-family: monospace;
    }
  }

  .demo-content {
    padding: 32px;
    background: #ecf5ff;
    border: 1px dashed #409eff;
    border-radius: 4px;
    color: #409eff;
    text-align: center;
  }
</style>
