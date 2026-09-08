<template>
  <div class="page">
    <CoreTable
      ref="table"
      :getDataApi="getList"
      :column="column"
      :searchConfig="searchConfig"
      :searchData="searchData"
      :menuConfig="menuConfig"
    >
      <template #exampleSlot="{ row }">
        <div>{{ row.exampleSlot }}</div>
      </template>
    </CoreTable>
  </div>
</template>

<script>
export default {
  name: 'CoreTableListPage',
  data() {
    return {
      searchData: {
        keyword: '',
      },
      searchConfig: {
        items: [
          {
            prop: 'keyword',
            label: '关键词',
            placeholder: '请输入',
          },
        ],
      },
      column: [
        {
          label: '名称',
          prop: 'name',
        },
        {
          label: '示例列（slot）',
          prop: 'exampleSlot',
          slot: true,
        },
        {
          label: '示例列（formatter）',
          prop: 'statusText',
          formatter: (row) => (row?.status === 1 ? '启用' : '停用'),
        },
        {
          label: '示例列（render）',
          prop: 'timeRange',
          option: {
            width: '160px',
            render: (row) => {
              return `<div style="line-height:1.1;">
                <div>${row?.startTime?.substring(0, 16) || '-'}</div>
                <div style="line-height: 0.6;">~</div>
                <div>${row?.endTime?.substring(0, 16) || '-'}</div>
              </div>`
            },
          },
        },
      ],
      menuConfig: {
        label: '操作',
        width: '140px',
        items: [
          {
            label: '编辑',
            on: (row) => this.handleEdit(row),
          },
          {
            label: '删除',
            type: 'danger',
            on: (row) => this.handleDelete(row),
          },
        ],
      },
    }
  },
  methods: {
    getList(formData) {
      return this.$httpBack.example.list(formData)
    },
    handleEdit(row) {
      this.$router.push({ path: '/example/edit', query: { id: row.id } })
    },
    handleDelete(row) {
      return this.$httpBack.example.delete({ id: row.id }).then(() => {
        this.$refs.table && this.$refs.table.getTableList && this.$refs.table.getTableList()
      })
    },
  },
}
</script>

