<script setup>
import { computed, ref } from 'vue';
import { NTag, NSpace, NButton, NIcon } from 'naive-ui';
import { ChevronDownOutline, ChevronUpOutline } from '@vicons/ionicons5';
import { formatStatusLabel } from '../utils/format';
import { statusTagType } from '../utils/helpers';

const props = defineProps({
  detail: { type: Object, default: null }
});

const expanded = ref(false);

const refundLabel = computed(() => {
  const s = props.detail?.refundStatus;
  if (!s || s === 'NONE') return '无';
  return s;
});
</script>

<template>
  <div v-if="detail" class="context-bar">
    <div class="context-strip">
      <div class="context-primary">
        <div class="kv">
          <span class="k">订单</span>
          <span class="mono">{{ detail.orderNo }}</span>
        </div>
        <div class="kv">
          <span class="k">状态</span>
          <n-tag size="small" round :type="statusTagType(detail.status)">
            {{ formatStatusLabel(detail.status) }}
          </n-tag>
        </div>
        <div class="kv">
          <span class="k">用户</span>
          <span class="v">{{ detail.userNo || '-' }}</span>
        </div>
        <div class="kv">
          <span class="k">金额</span>
          <span class="accent">¥{{ detail.amount }}</span>
        </div>
        <div class="kv">
          <span class="k">支付</span>
          <span class="v">{{ detail.paymentStatus || '-' }}</span>
        </div>
        <div class="kv">
          <span class="k">退款</span>
          <span class="v">{{ refundLabel }}</span>
        </div>
      </div>
      <n-button size="tiny" quaternary class="ctx-toggle" @click="expanded = !expanded">
        {{ expanded ? '收起' : '详情' }}
        <template #icon>
          <n-icon :component="expanded ? ChevronUpOutline : ChevronDownOutline" />
        </template>
      </n-button>
    </div>

    <div v-if="expanded" class="context-extra">
      <div v-if="detail.benefits?.length" class="benefits">
        <span class="label">权益</span>
        <n-space size="small">
          <n-tag v-for="(b, i) in detail.benefits" :key="i" size="small" round :bordered="false">
            {{ b }}
          </n-tag>
        </n-space>
      </div>
      <div v-else class="muted small">暂无权益信息</div>
    </div>
  </div>
</template>

<style scoped>
.context-bar {
  flex-shrink: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), rgba(14, 165, 233, 0.05));
  border: 1px solid rgba(79, 70, 229, 0.14);
}

.k {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #94a3b8;
  flex-shrink: 0;
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
  max-width: 100%;
}

.v {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.mono {
  font-family: var(--af-font-mono, "JetBrains Mono", ui-monospace, monospace);
  font-size: 12.5px;
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

.muted.small {
  font-size: 12px;
}

.context-extra {
  padding: 0 12px 10px;
  border-top: 1px dashed rgba(15, 23, 42, 0.08);
  padding-top: 10px;
}

.benefits .label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 6px;
}
</style>
