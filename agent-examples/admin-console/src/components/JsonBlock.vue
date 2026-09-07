<script setup>
import { NCode } from 'naive-ui';

defineProps({
  data: { type: [Object, Array, String, Number, Boolean, null], default: null },
  maxHeight: { type: String, default: '320px' }
});
</script>

<template>
  <n-code
    :code="JSON.stringify(data, null, 2)"
    language="json"
    :word-wrap="true"
    :style="{ maxHeight, overflow: 'auto' }"
    show-line-numbers
  />
</template>
