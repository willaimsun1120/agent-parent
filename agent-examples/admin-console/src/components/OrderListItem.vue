<script setup>
import { NTag, NButton, NIcon, NTooltip } from 'naive-ui';
import { TrashOutline } from '@vicons/ionicons5';
import { formatStatusLabel } from '../utils/format';
import { statusTagType } from '../utils/helpers';

defineProps({
  order: { type: Object, required: true },
  selected: { type: Boolean, default: false }
});

const emit = defineEmits(['select', 'delete']);
</script>

<template>
  <div
    class="order-item"
    :class="{ selected }"
    @click="emit('select', order.orderNo)"
  >
    <span class="order-no">{{ order.orderNo }}</span>
    <n-tooltip trigger="hover">
      <template #trigger>
        <n-tag size="tiny" :type="statusTagType(order.status)" round :bordered="false">
          {{ formatStatusLabel(order.status) }}
        </n-tag>
      </template>
      {{ order.status }}
    </n-tooltip>
    <span class="order-amt">¥{{ order.amount }}</span>
    <n-button
      size="tiny"
      quaternary
      circle
      type="error"
      class="order-del"
      @click.stop="emit('delete', order.orderNo)"
    >
      <template #icon><n-icon :component="TrashOutline" /></template>
    </n-button>
  </div>
</template>

<style scoped>
.order-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: 6px 8px;
  padding: 9px 10px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

.order-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: transparent;
}

.order-item:hover {
  background: #fff;
  border-color: rgba(15, 23, 42, 0.08);
}

.order-item.selected {
  border-color: rgba(99, 102, 241, 0.25);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.07), rgba(255, 255, 255, 0.95));
}

.order-item.selected::before {
  background: linear-gradient(180deg, #6366f1, #4f46e5);
}

.order-no {
  font-weight: 700;
  font-size: 12.5px;
  font-family: var(--af-font-mono, ui-monospace, monospace);
  letter-spacing: -0.02em;
  color: #4f46e5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-amt {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
  font-variant-numeric: tabular-nums;
}

.order-del {
  opacity: 0.55;
}

.order-item:hover .order-del {
  opacity: 1;
}
</style>
