<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="220px" class="app-shell__aside">
      <h1>技术需求管理</h1>
      <nav>
        <RouterLink to="/dashboard">首页</RouterLink>
        <RouterLink to="/requests">我的需求</RouterLink>
        <RouterLink to="/requests/new">提交需求</RouterLink>
        <RouterLink to="/workspace">技术组工作台</RouterLink>
        <RouterLink to="/admin/members">成员管理</RouterLink>
        <RouterLink to="/admin/categories">分类管理</RouterLink>
        <RouterLink to="/admin/statistics">数据概览</RouterLink>
        <RouterLink to="/settings">个人设置</RouterLink>
      </nav>
    </el-aside>
    <el-container>
      <el-header class="app-shell__header">计算机技术组外包需求管理系统</el-header>
      <el-main>
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-shell__aside {
  padding: 24px 16px;
  color: #fff;
  background: #1f2d3d;
}

.app-shell__aside h1 {
  margin: 0 0 24px;
  font-size: 18px;
}

.app-shell__aside nav {
  display: grid;
  gap: 8px;
}

.app-shell__aside a {
  padding: 10px 12px;
  border-radius: 6px;
}

.app-shell__aside a.router-link-active {
  background: #409eff;
}

.app-shell__header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}
</style>
