<script setup>
import { ref } from 'vue';
import {
  NConfigProvider,
  NLayout,
  NLayoutHeader,
  NLayoutContent,
  NSpace,
  NButton,
  NDropdown,
  NIcon
} from 'naive-ui';
import { SettingsOutline } from '@vicons/ionicons5';
import { themeOverrides } from './naive';
import OrderConsole from './views/OrderConsole.vue';
import HrConsole from './views/HrConsole.vue';

const activeDemo = ref('order');

const demos = [
  { id: 'order', label: '订单客服' },
  { id: 'hr', label: 'HR 客服' }
];

const opsOptions = [
  { label: 'Qdrant', key: 'http://127.0.0.1:6333/dashboard' },
  { label: 'Attu (Milvus)', key: 'http://127.0.0.1:8000' },
  { label: 'Prometheus', key: 'http://127.0.0.1:9090' },
  { label: 'Grafana', key: 'http://127.0.0.1:3000' }
];

function openOps(key) {
  if (key) window.open(key, '_blank', 'noopener');
}
</script>

<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <n-layout class="app-layout">
      <n-layout-header class="app-header" bordered>
        <div class="header-inner">
          <div class="brand">
            <div class="brand-mark">AF</div>
            <div>
              <div class="brand-title">AgentForge</div>
              <div class="brand-sub">统一管理控制台</div>
            </div>
          </div>

          <n-space :size="6" align="center">
            <n-button
              v-for="demo in demos"
              :key="demo.id"
              :type="activeDemo === demo.id ? 'primary' : 'default'"
              :secondary="activeDemo !== demo.id"
              round
              @click="activeDemo = demo.id"
            >
              {{ demo.label }}
            </n-button>
          </n-space>

          <n-dropdown :options="opsOptions" trigger="click" @select="openOps">
            <n-button size="small" round secondary>
              <template #icon><n-icon :component="SettingsOutline" /></template>
              运维
            </n-button>
          </n-dropdown>
        </div>
      </n-layout-header>

      <n-layout-content class="app-body">
        <transition name="fade-slide" mode="out-in">
          <OrderConsole v-if="activeDemo === 'order'" key="order" />
          <HrConsole v-else key="hr" />
        </transition>
      </n-layout-content>
    </n-layout>
  </n-config-provider>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
  height: auto;
  background: transparent;
}

.app-layout :deep(> .n-layout-scroll-container) {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  height: auto;
}

.app-body {
  flex: 1 0 auto;
  min-height: calc(100vh - 64px);
  overflow: visible;
}

.app-body :deep(> .n-layout-scroll-container) {
  min-height: 100%;
  height: auto;
  overflow: visible !important;
}

.app-body :deep(.console-grid) {
  min-height: var(--console-min-h, 800px);
  height: auto;
}
</style>
