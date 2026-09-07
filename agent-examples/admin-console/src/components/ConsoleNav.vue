<script setup>
import { computed, h } from 'vue';
import { NMenu, NIcon } from 'naive-ui';

const props = defineProps({
  modelValue: { type: String, required: true },
  items: { type: Array, required: true }
});

const emit = defineEmits(['update:modelValue']);

const options = computed(() =>
  props.items.map((item) => ({
    label: item.label,
    key: item.key,
    icon: () => h(NIcon, { component: item.icon, size: 18 })
  }))
);
</script>

<template>
  <nav class="console-nav">
    <n-menu
      :value="modelValue"
      :options="options"
      :indent="18"
      :root-indent="12"
      @update:value="emit('update:modelValue', $event)"
    />
  </nav>
</template>

<style scoped>
.console-nav {
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.045);
  border: 1px solid rgba(15, 23, 42, 0.05);
  padding: 5px;
  flex-shrink: 0;
}

.console-nav :deep(.n-menu-item) {
  margin-bottom: 2px;
}

.console-nav :deep(.n-menu-item-content) {
  border-radius: 10px;
  font-weight: 600;
  font-size: 13.5px;
  letter-spacing: -0.01em;
  height: 40px;
}

.console-nav :deep(.n-menu-item-content--selected) {
  background: #fff !important;
  box-shadow: 0 2px 10px rgba(79, 70, 229, 0.14);
  color: var(--n-item-text-color-active);
}
</style>
