<script setup>
import { computed } from 'vue'

const props = defineProps({
  menuTreeList: {
    required: true,
    type: Array
  },
  collapse: {
    required: true,
    type: Boolean
  }
})

const iconStyle = computed(() => ({
  width: '20px',
  height: '20px',
  ...(props.collapse ? {} : {margin: '-2px 10px 0 0'})
}))
defineOptions({
  name: 'MenuItem'
})
</script>

<template>
  <template v-for="item in props.menuTreeList" :key="item.id">
    <!--没有子菜单-->
    <el-menu-item
        v-if="!item.children || item.children.length === 0"
        :index="item.url"
    >
      <template v-if="item.img">
        <SvgIcon :iconStyle="iconStyle" :name="item?.img" class="svg-icon"/>
      </template>
      <span>{{ item.name }}</span>
    </el-menu-item>
    <!--有且只有一个子菜单-->
    <el-menu-item
        v-else-if="item.children && item.children.length === 1"
        :index="item.children[0].url"
    >
      <template v-if="item.children[0].img">
        <SvgIcon
            :iconStyle="iconStyle"
            :name="item.children[0]?.img"
            class="svg-icon"
        />
      </template>
      <span>{{ item.children[0].name }}</span>
    </el-menu-item>
    <!--有且大于一个子菜单-->
    <el-sub-menu v-else :index="item.url">
      <template #title>
        <template v-if="item.img">
          <SvgIcon :iconStyle="iconStyle" :name="item?.img" class="svg-icon"/>
        </template>
        <span>{{ item.name }}</span>
      </template>
      <MenuItem :collapse="collapse" :menuTreeList="item.children"/>
    </el-sub-menu>
  </template>
</template>

<style lang="scss" scoped>
//.svg-icon {
//  margin: -2px 10px 0 0;
//}
</style>
