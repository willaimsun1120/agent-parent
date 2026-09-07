<script setup>
import { ref } from 'vue';
import { NTag, NButton, NIcon } from 'naive-ui';
import { ChevronDownOutline, ChevronUpOutline } from '@vicons/ionicons5';

defineProps({
  detail: { type: Object, default: null }
});

const expanded = ref(false);
</script>

<template>
  <div v-if="detail" class="context-bar">
    <div class="context-strip">
      <div class="context-primary">
        <div class="kv">
          <span class="k">工号</span>
          <span class="mono">{{ detail.empNo }}</span>
        </div>
        <div class="kv">
          <span class="k">姓名</span>
          <span class="v">{{ detail.name }}</span>
        </div>
        <div class="kv">
          <span class="k">部门</span>
          <n-tag size="small" round type="info">{{ detail.department }}</n-tag>
        </div>
        <div class="kv">
          <span class="k">年假</span>
          <span class="accent">{{ detail.annualLeaveBalance }} 天</span>
        </div>
      </div>
      <n-button size="tiny" quaternary class="ctx-toggle" @click="expanded = !expanded">
        {{ expanded ? '收起' : '详情' }}
        <template #icon>
          <n-icon :component="expanded ? ChevronUpOutline : ChevronDownOutline" />
        </template>
      </n-button>
    </div>
    <div v-if="expanded" class="context-extra muted">
      工号 {{ detail.empNo }} · {{ detail.name }} · {{ detail.department }} · 年假余额 {{ detail.annualLeaveBalance }} 天
    </div>
  </div>
</template>

<style scoped>
.context-bar {
  flex-shrink: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.07), rgba(14, 165, 233, 0.04));
  border: 1px solid rgba(99, 102, 241, 0.12);
}

.context-strip {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
}

.context-primary {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
  min-width: 0;
}

.kv {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.k {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
}

.v {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.mono {
  font-family: JetBrains Mono, ui-monospace, monospace;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
}

.accent {
  color: #4f46e5;
  font-weight: 700;
  font-size: 13px;
}

.ctx-toggle {
  flex-shrink: 0;
}

.muted {
  font-size: 12px;
  color: #64748b;
}

.context-extra {
  padding: 0 12px 10px;
  border-top: 1px dashed rgba(15, 23, 42, 0.08);
  padding-top: 10px;
}
</style>
