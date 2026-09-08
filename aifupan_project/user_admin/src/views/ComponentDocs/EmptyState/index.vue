<template>
  <div class="component-doc">
    <h2>空白页指令 (v-empty)</h2>

    <div class="doc-section">
      <h3>组件说明</h3>
      <p>提供一个可配置的空白页遮罩，支持自定义标题、描述、图片、模糊度和操作按钮。</p>
    </div>

    <div class="doc-section">
      <h3>使用示例</h3>

      <div class="demo-box">
        <div class="demo-controls">
          <el-switch v-model="config.visible" active-text="显示空白页" />
          <el-input v-model="config.title" placeholder="标题" style="width: 200px; margin-left: 12px" />
          <el-input v-model="config.description" placeholder="描述" style="width: 200px; margin-left: 12px" />
        </div>

        <div class="demo-container" v-empty="config">
          <p>这里是容器原始内容...</p>
          <el-table :data="[{ date: '2024-01-01', name: 'Test' }]" style="width: 100%">
            <el-table-column prop="date" label="Date" />
            <el-table-column prop="name" label="Name" />
          </el-table>
        </div>
      </div>
    </div>

    <div class="doc-section">
      <h3>配置文档</h3>
      <table class="doc-table">
        <thead
          ><tr><th>参数名</th><th>说明</th><th>类型</th><th>默认值</th></tr></thead
        >
        <tbody>
          <tr><td>visible</td><td>控制显示隐藏</td><td>Boolean</td><td>false</td></tr>
          <tr><td>title</td><td>标题文本</td><td>String</td><td>'暂无数据'</td></tr>
          <tr><td>description</td><td>描述文本</td><td>String</td><td>-</td></tr>
          <tr><td>image</td><td>图片URL</td><td>String</td><td>默认SVG</td></tr>
          <tr><td>blur</td><td>背景模糊度(px)</td><td>Number</td><td>4</td></tr>
          <tr><td>buttons</td><td>按钮配置数组</td><td>Array</td><td>[]</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
  .component-doc {
    padding: 20px;
  }
  .doc-section {
    margin-bottom: 30px;
  }
  .demo-box {
    border: 1px solid #ebeef5;
    padding: 20px;
    border-radius: 4px;
  }
  .demo-controls {
    margin-bottom: 20px;
    display: flex;
    align-items: center;
  }
  .demo-container {
    height: 300px;
    border: 1px dashed #dcdfe6;
    padding: 20px;
    position: relative;
  }
  .doc-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
  }
  .doc-table th,
  .doc-table td {
    border: 1px solid #dcdfe6;
    padding: 10px;
    text-align: left;
  }
  .doc-table th {
    background-color: #f5f7fa;
  }
</style>
