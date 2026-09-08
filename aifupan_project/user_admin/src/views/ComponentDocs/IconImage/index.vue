<template>
  <div class="doc-container">
    <div class="doc-header">
      <h2>IconImage 图标图片组件</h2>
      <p>用于动态加载 <code>src/assets/images/icon</code> 目录下的图标图片。</p>
    </div>

    <div class="doc-content">
      <el-card class="box-card">
        <template #header>
          <div class="card-header">
            <span>基础用法</span>
          </div>
        </template>
        <div class="demo-block">
          <div class="demo-item">
            <span class="label">默认 (png):</span>
            <!-- 假设 assets/images/icon 下有图片，这里使用一个占位符或者说明 -->
            <el-alert
              title="请确保 src/assets/images/icon 目录下有对应的图片文件"
              type="info"
              :closable="false"
              style="margin-bottom: 10px"
            />
            <!-- <IconImage name="example" width="32px" height="32px" /> -->
          </div>
        </div>
        <div class="code-block">
          <pre><code>&lt;IconImage name="example" width="32px" height="32px" /&gt;</code></pre>
        </div>
      </el-card>

      <el-card class="box-card" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>属性说明</span>
          </div>
        </template>
        <el-table :data="attributes" style="width: 100%">
          <el-table-column prop="prop" label="参数" width="180" />
          <el-table-column prop="desc" label="说明" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="default" label="默认值" width="120" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue'
  // import IconImage from '@/components/IconImage/index.vue'

  const attributes = [
    {
      prop: 'name',
      desc: '图标文件名称（不含后缀），对应 src/assets/images/icon/ 下的文件名',
      type: 'String',
      default: '-'
    },
    { prop: 'ext', desc: '图片后缀名', type: 'String', default: 'png' },
    { prop: 'width', desc: '宽度 (CSS 属性值)', type: 'String', default: 'auto' },
    { prop: 'height', desc: '高度 (CSS 属性值)', type: 'String', default: 'auto' }
  ]
</script>

<style scoped>
  .doc-container {
    padding: 20px;
  }

  .doc-header {
    margin-bottom: 20px;
  }

  .doc-header h2 {
    font-size: 24px;
    margin-bottom: 10px;
  }

  .doc-header p {
    color: #666;
  }

  .demo-block {
    padding: 20px;
    border: 1px solid #ebebeb;
    border-radius: 3px;
    margin-bottom: 20px;
  }

  .demo-item {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .code-block {
    background-color: #fafafa;
    padding: 10px;
    border-radius: 3px;
    font-family: monospace;
  }
</style>
