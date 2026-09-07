<script setup>
import { ref, watch, onMounted } from 'vue';
import {
  NGrid,
  NGi,
  NCard,
  NButton,
  NSpace,
  NTag,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NIcon,
  NScrollbar
} from 'naive-ui';
import { RefreshOutline } from '@vicons/ionicons5';
import { promptBreadcrumb, logClick } from '../utils/helpers';
import { message } from '../naive';

const props = defineProps({
  api: { type: Object, required: true },
  demo: { type: String, required: true },
  labelMap: { type: Object, default: () => ({}) }
});

const prompts = ref([]);
const editingPromptCode = ref(null);
const form = ref({
  promptCode: '',
  title: '',
  description: '',
  status: 'ACTIVE',
  content: ''
});
const meta = ref('');

const statusOptions = [
  { label: '生效中', value: 'ACTIVE' },
  { label: '草稿', value: 'DRAFT' }
];

async function loadPrompts() {
  logClick(props.demo, 'reload-prompts');
  prompts.value = await props.api.get('/api/prompts');
  if (!editingPromptCode.value && prompts.value.length) {
    selectPrompt(prompts.value[0]);
  }
}

function selectPrompt(prompt) {
  editingPromptCode.value = prompt.promptCode;
  form.value = {
    promptCode: prompt.promptCode,
    title: prompt.title,
    description: prompt.description || '',
    status: prompt.status,
    content: prompt.content
  };
  meta.value = `${promptBreadcrumb(prompt, props.labelMap)} · v${prompt.version} · ${prompt.updatedAt || '-'}`;
}

async function savePrompt() {
  const promptCode = form.value.promptCode.trim();
  if (!promptCode) {
    message.warning('请先选择模板');
    return;
  }
  logClick(props.demo, 'save-prompt', { promptCode });
  const body = {
    title: form.value.title.trim(),
    description: form.value.description.trim(),
    content: form.value.content,
    status: form.value.status
  };
  const updated = await props.api.put(`/api/prompts/${encodeURIComponent(promptCode)}`, body);
  message.success(`提示词已保存 v${updated.version}`);
  editingPromptCode.value = updated.promptCode;
  await loadPrompts();
  selectPrompt(updated);
}

onMounted(loadPrompts);
watch(() => props.demo, loadPrompts);
</script>

<template>
  <n-grid :cols="24" :x-gap="16">
    <n-gi :span="8">
      <n-card size="small" title="提示词模板" :bordered="false">
        <template #header-extra>
          <n-button size="small" quaternary circle @click="loadPrompts">
            <template #icon><n-icon :component="RefreshOutline" /></template>
          </n-button>
        </template>

        <n-scrollbar class="list-scroll">
          <div class="prompt-list">
            <div
              v-for="prompt in prompts"
              :key="prompt.promptCode"
              class="prompt-item"
              :class="{ selected: editingPromptCode === prompt.promptCode }"
              @click="selectPrompt(prompt)"
            >
              <div class="prompt-row">
                <strong class="prompt-code">{{ prompt.promptCode }}</strong>
                <n-tag size="small" :type="prompt.status === 'ACTIVE' ? 'success' : 'default'" round>
                  {{ prompt.status === 'ACTIVE' ? '生效' : '草稿' }}
                </n-tag>
              </div>
              <div class="prompt-title">{{ prompt.title }}</div>
              <div class="prompt-breadcrumb">{{ promptBreadcrumb(prompt, labelMap) }}</div>
            </div>
          </div>
        </n-scrollbar>
      </n-card>
    </n-gi>

    <n-gi :span="16">
      <n-card size="small" :title="form.title || '编辑提示词'" :bordered="false">
        <template #header-extra>
          <n-button size="small" type="primary" @click="savePrompt">保存</n-button>
        </template>
        <div style="font-size: 12px; color: #94a3b8; margin-bottom: 12px">{{ meta }}</div>

        <n-form label-placement="top" size="small">
          <n-grid :cols="2" :x-gap="12">
            <n-gi>
              <n-form-item label="promptCode">
                <n-input v-model:value="form.promptCode" disabled />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="标题">
                <n-input v-model:value="form.title" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="描述">
                <n-input v-model:value="form.description" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="状态">
                <n-select v-model:value="form.status" :options="statusOptions" />
              </n-form-item>
            </n-gi>
          </n-grid>
          <n-form-item label="内容">
            <n-input v-model:value="form.content" type="textarea" :rows="18" />
          </n-form-item>
        </n-form>
      </n-card>
    </n-gi>
  </n-grid>
</template>

<style scoped>
.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.prompt-item {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.prompt-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: transparent;
  transition: background 0.2s ease;
}

.prompt-item:hover {
  background: #fff;
  border-color: rgba(15, 23, 42, 0.08);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.prompt-item.selected {
  border-color: rgba(99, 102, 241, 0.25);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.06), rgba(255, 255, 255, 0.95));
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.1);
}

.prompt-item.selected::before {
  background: linear-gradient(180deg, #6366f1, #4f46e5);
}

.prompt-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.prompt-code {
  font-size: 13px;
  color: #0f172a;
  word-break: break-all;
}

.prompt-title {
  font-size: 13px;
  margin-top: 4px;
  color: #334155;
}

.prompt-breadcrumb {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}
</style>
