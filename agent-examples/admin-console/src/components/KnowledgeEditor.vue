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
import { RefreshOutline, AddOutline } from '@vicons/ionicons5';
import { logClick } from '../utils/helpers';
import { message } from '../naive';

const props = defineProps({
  api: { type: Object, required: true },
  demo: { type: String, required: true },
  vectorStore: { type: Object, default: null }
});

const articles = ref([]);
const editingDocCode = ref(null);
const form = ref({
  docCode: '',
  title: '',
  category: 'faq',
  status: 'DRAFT',
  content: ''
});

const meta = ref('填写 docCode 与正文后保存');
const editorTitle = ref('新建知识文档');

const categoryOptions = [
  { label: 'faq', value: 'faq' },
  { label: 'policy', value: 'policy' },
  { label: 'guide', value: 'guide' }
];

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' }
];

async function loadArticles() {
  logClick(props.demo, 'reload-knowledge');
  articles.value = await props.api.get('/api/knowledge/articles');
  if (!editingDocCode.value && articles.value.length) {
    selectArticle(articles.value[0]);
  }
}

function selectArticle(article) {
  editingDocCode.value = article.docCode;
  form.value = {
    docCode: article.docCode,
    title: article.title,
    category: article.category,
    status: article.status,
    content: article.content
  };
  editorTitle.value = article.title || '编辑知识文档';
  meta.value = `${article.docCode} · ${article.category} · v${article.version}`;
}

function resetForm() {
  logClick(props.demo, 'new-knowledge');
  editingDocCode.value = null;
  form.value = { docCode: '', title: '', category: 'faq', status: 'DRAFT', content: '' };
  editorTitle.value = '新建知识文档';
  meta.value = '填写 docCode 与正文后保存';
}

async function saveArticle() {
  logClick(props.demo, 'save-knowledge', { docCode: form.value.docCode });
  const body = { ...form.value };
  if (editingDocCode.value) {
    await props.api.put(`/api/knowledge/articles/${editingDocCode.value}`, body);
  } else {
    await props.api.post('/api/knowledge/articles', body);
  }
  message.success('知识文档已保存');
  editingDocCode.value = body.docCode;
  await loadArticles();
  const saved = articles.value.find(a => a.docCode === body.docCode);
  if (saved) selectArticle(saved);
}

async function publishArticle() {
  const docCode = editingDocCode.value || form.value.docCode.trim();
  if (!docCode) {
    message.warning('请先填写或选择 docCode');
    return;
  }
  if (!editingDocCode.value) await saveArticle();
  logClick(props.demo, 'publish-knowledge', { docCode });
  const article = await props.api.post(`/api/knowledge/articles/${docCode}/publish`);
  message.success(`已发布并同步：${article.docCode}`);
  await loadArticles();
  selectArticle(article);
}

onMounted(loadArticles);
watch(() => props.demo, loadArticles);
</script>

<template>
  <n-grid :cols="24" :x-gap="16">
    <n-gi :span="7">
      <n-card size="small" title="知识文档" :bordered="false">
        <template #header-extra>
          <n-space :size="6">
            <n-button size="small" quaternary circle @click="loadArticles">
              <template #icon><n-icon :component="RefreshOutline" /></template>
            </n-button>
            <n-button size="small" secondary @click="resetForm">
              <template #icon><n-icon :component="AddOutline" /></template>
              新建
            </n-button>
          </n-space>
        </template>

        <n-tag v-if="vectorStore" size="small" :bordered="false" type="info" style="margin-bottom: 10px">
          {{ vectorStore.type }} · {{ vectorStore.collection }}
        </n-tag>

        <n-scrollbar class="list-scroll">
          <div class="kb-list">
            <div
              v-for="article in articles"
              :key="article.docCode"
              class="kb-item"
              :class="{ selected: editingDocCode === article.docCode }"
              @click="selectArticle(article)"
            >
              <div class="kb-row">
                <strong class="kb-code">{{ article.docCode }}</strong>
                <n-tag
                  size="small"
                  :type="article.status === 'PUBLISHED' ? 'success' : 'default'"
                  round
                >
                  {{ article.status }}
                </n-tag>
              </div>
              <div class="kb-title">{{ article.title }}</div>
              <div class="kb-meta">{{ article.category }} · v{{ article.version }}</div>
            </div>
          </div>
        </n-scrollbar>
      </n-card>
    </n-gi>

    <n-gi :span="17">
      <n-card size="small" :title="editorTitle" :bordered="false">
        <template #header-extra>
          <n-space>
            <n-button size="small" @click="saveArticle">保存</n-button>
            <n-button size="small" type="primary" @click="publishArticle">发布并同步</n-button>
          </n-space>
        </template>
        <div style="font-size: 12px; color: #94a3b8; margin-bottom: 12px">{{ meta }}</div>

        <n-form label-placement="top" size="small">
          <n-grid :cols="2" :x-gap="12">
            <n-gi>
              <n-form-item label="docCode">
                <n-input v-model:value="form.docCode" :disabled="!!editingDocCode" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="标题">
                <n-input v-model:value="form.title" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="分类">
                <n-select v-model:value="form.category" :options="categoryOptions" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="状态">
                <n-select v-model:value="form.status" :options="statusOptions" />
              </n-form-item>
            </n-gi>
          </n-grid>
          <n-form-item label="正文">
            <n-input v-model:value="form.content" type="textarea" :rows="16" />
          </n-form-item>
        </n-form>
      </n-card>
    </n-gi>
  </n-grid>
</template>

<style scoped>
.kb-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kb-item {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.kb-item::before {
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

.kb-item:hover {
  background: #fff;
  border-color: rgba(15, 23, 42, 0.08);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.kb-item.selected {
  border-color: rgba(99, 102, 241, 0.25);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.06), rgba(255, 255, 255, 0.95));
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.1);
}

.kb-item.selected::before {
  background: linear-gradient(180deg, #6366f1, #4f46e5);
}

.kb-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.kb-code {
  font-size: 13px;
  color: #0f172a;
  word-break: break-all;
}

.kb-title {
  font-size: 13px;
  margin-top: 4px;
  color: #334155;
}

.kb-meta {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}
</style>
