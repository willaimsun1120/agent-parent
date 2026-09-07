<script setup>
import { ref, computed, onMounted, h, nextTick } from 'vue';
import {
  NCard,
  NButton,
  NSpace,
  NTag,
  NScrollbar,
  NEmpty,
  NIcon,
  NInput,
  NCollapse,
  NCollapseItem
} from 'naive-ui';
import {
  RefreshOutline,
  PeopleOutline,
  BookOutline,
  DocumentTextOutline
} from '@vicons/ionicons5';
import { hrApi, typewriterText } from '../api/http';
import TracePanel from '../components/TracePanel.vue';
import ChatPanel from '../components/ChatPanel.vue';
import KnowledgeEditor from '../components/KnowledgeEditor.vue';
import PromptEditor from '../components/PromptEditor.vue';
import JsonBlock from '../components/JsonBlock.vue';
import ConsoleNav from '../components/ConsoleNav.vue';
import EmployeeDetailSummary from '../components/EmployeeDetailSummary.vue';
import { buildHrSuggestions, logClick } from '../utils/helpers';
import { nowLocalIso } from '../utils/format';
import { message, dialog } from '../naive';

const SESSION_KEY = 'hrAgentSessionId';
const DEMO = 'hr-cs';
const HR_LABELS = { hr: 'HR', cs: '客服', chat: '对话', framework: '框架模式', manual: '手写模式' };

const activeTab = ref('employees');
const employees = ref([]);
const selectedEmpNo = ref(null);
const employeeDetail = ref(null);
const vectorStore = ref(null);
const pending = ref([]);
const trace = ref({});
const selectedTraceId = ref(null);
const answer = ref('');
const sending = ref(false);
const streaming = ref(false);
const statusMessage = ref('');
const sessionId = ref(localStorage.getItem(SESSION_KEY) || null);
const messages = ref([]);
const hasMoreHistory = ref(false);
const nextBeforeId = ref(null);
const loadingHistory = ref(false);
const chatPanelRef = ref(null);
const HISTORY_PAGE_SIZE = 20;

const suggestions = computed(() => buildHrSuggestions(employeeDetail.value));

const navItems = [
  { key: 'employees', label: '员工', icon: PeopleOutline },
  { key: 'knowledge', label: '知识库', icon: BookOutline },
  { key: 'prompts', label: '提示词', icon: DocumentTextOutline }
];

async function loadEmployees() {
  logClick(DEMO, 'reload-employees');
  employees.value = await hrApi.get('/api/employees');
}

async function loadVectorStore() {
  try {
    vectorStore.value = await hrApi.get('/api/knowledge/vector-store');
  } catch (e) {
    console.warn('加载向量库配置失败', e);
  }
}

async function loadPending() {
  pending.value = await hrApi.get('/api/agent/actions/pending');
}

async function selectEmployee(empNo) {
  logClick(DEMO, 'select-employee', { empNo });
  selectedEmpNo.value = empNo;
  employeeDetail.value = null;
  try {
    employeeDetail.value = await hrApi.get(`/api/employees/${empNo}`);
  } catch (e) {
    message.error(`加载员工失败：${e.message || e}`);
    return;
  }
  await loadEmployees();
}

async function syncKnowledge() {
  logClick(DEMO, 'sync-knowledge-panel');
  const result = await hrApi.post('/api/knowledge/sync');
  message.success(`已同步：${result.points} points / ${result.articles} 篇`);
}

async function runRagEval() {
  logClick(DEMO, 'rag-eval');
  const result = await hrApi.post('/api/rag/eval');
  message.success(`RAG 评测 ${result.passed}/${result.total} 通过`);
}

async function confirmAction(actionId) {
  logClick(DEMO, 'hitl-confirm', { actionId });
  await hrApi.post(`/api/agent/actions/${actionId}/confirm`);
  message.success('操作已确认');
  await loadPending();
  if (selectedEmpNo.value) await selectEmployee(selectedEmpNo.value);
}

function rejectAction(actionId) {
  const reasonRef = { value: '' };
  dialog.warning({
    title: '拒绝操作',
    content: () => h(NInput, {
      type: 'textarea',
      placeholder: '拒绝原因（可选）',
      rows: 3,
      onUpdateValue: (v) => { reasonRef.value = v; }
    }),
    positiveText: '确认拒绝',
    negativeText: '取消',
    onPositiveClick: async () => {
      logClick(DEMO, 'hitl-reject', { actionId });
      await hrApi.post(`/api/agent/actions/${actionId}/reject`, { reason: reasonRef.value });
      message.info('已拒绝该操作');
      await loadPending();
    }
  });
}

function newSession() {
  logClick(DEMO, 'new-session');
  sessionId.value = null;
  localStorage.removeItem(SESSION_KEY);
  answer.value = '';
  messages.value = [];
  hasMoreHistory.value = false;
  nextBeforeId.value = null;
  streaming.value = false;
  sending.value = false;
  statusMessage.value = '';
  trace.value = {};
  selectedTraceId.value = null;
}

function mapHistoryItems(items) {
  return (items || []).map((item) => ({
    id: item.id,
    role: item.role,
    content: item.content,
    turnIndex: item.turnIndex,
    traceId: item.traceId,
    createdAt: item.createdAt
  }));
}

async function loadMessages({ reset = false } = {}) {
  if (!sessionId.value) return;
  if (loadingHistory.value) return;
  if (!reset && !hasMoreHistory.value) return;
  loadingHistory.value = true;
  try {
    const params = new URLSearchParams({ limit: String(HISTORY_PAGE_SIZE) });
    if (!reset && nextBeforeId.value != null) {
      params.set('beforeId', String(nextBeforeId.value));
    }
    const page = await hrApi.get(
      `/api/agent/sessions/${encodeURIComponent(sessionId.value)}/messages?${params}`
    );
    const items = mapHistoryItems(page?.items);
    messages.value = reset ? items : [...items, ...messages.value];
    hasMoreHistory.value = !!page?.hasMore;
    nextBeforeId.value = page?.nextBeforeId ?? null;
    if (reset && items.length) {
      const latest = [...items].reverse().find((m) => m.traceId);
      if (latest?.traceId) {
        await selectTrace({ traceId: latest.traceId });
      }
    }
  } catch (e) {
    if (reset) {
      console.warn('加载聊天记录失败', e);
      messages.value = [];
      hasMoreHistory.value = false;
      nextBeforeId.value = null;
    } else {
      message.error(`加载更早消息失败：${e.message || e}`);
    }
  } finally {
    loadingHistory.value = false;
    if (reset) {
      await nextTick();
      chatPanelRef.value?.scrollToLatest?.();
      setTimeout(() => chatPanelRef.value?.scrollToLatest?.(), 80);
      setTimeout(() => chatPanelRef.value?.scrollToLatest?.(), 240);
    }
  }
}

function loadMoreHistory() {
  loadMessages({ reset: false });
}

function focusHitl() {
  const el = document.querySelector('.trace-shell');
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function selectTrace({ traceId }) {
  if (!traceId || sending.value) return;
  selectedTraceId.value = traceId;
  try {
    const data = await hrApi.get(
      `/api/observability/sessions/${encodeURIComponent(traceId)}`
    );
    trace.value = {
      traceId: data?.traceId || traceId,
      sessionId: data?.sessionId ?? null,
      turnIndex: data?.turnIndex ?? null,
      durationMs: data?.durationMs ?? null,
      toolCalls: data?.toolCalls || [],
      ragHitDetails: data?.ragHitDetails || [],
      ragHits: []
    };
  } catch (e) {
    message.error(`加载观测数据失败：${e.message || e}`);
  }
}

async function sendQuestion({ question, mode }) {
  if (!question) return message.warning('请先输入问题');
  if (!selectedEmpNo.value) return message.warning('请先在左侧选择一个员工');
  answer.value = '';
  messages.value = [...messages.value, { role: 'user', content: question, createdAt: nowLocalIso() }];
  sending.value = true;
  streaming.value = false;
  statusMessage.value = '模型推理中，请稍候…';
  selectedTraceId.value = null;
  trace.value = { toolCalls: [], ragHits: [], ragHitDetails: [] };
  logClick(DEMO, 'agent-chat', { mode, stream: true });
  const body = { question, mode };
  if (sessionId.value) body.sessionId = sessionId.value;
  let gotDelta = false;
  try {
    const response = await hrApi.streamChat(body, {
      onMeta: (meta) => {
        if (meta?.status) statusMessage.value = meta.status;
        if (meta?.traceId) {
          selectedTraceId.value = meta.traceId;
          trace.value = {
            ...trace.value,
            traceId: meta.traceId,
            sessionId: meta.sessionId,
            turnIndex: meta.turnIndex,
            runtime: meta.runtime
          };
        }
      },
      onTool: (tool) => {
        const raw = tool?.raw;
        if (!raw) return;
        const list = [...(trace.value.toolCalls || [])];
        list.push(raw);
        trace.value = { ...trace.value, toolCalls: list };
        statusMessage.value = '已触发工具调用…';
      },
      onRag: (hit) => {
        if (!hit) return;
        if (hit.docCode != null || hit.snippet != null || hit.title != null) {
          const details = [...(trace.value.ragHitDetails || [])];
          details.push(hit);
          trace.value = { ...trace.value, ragHitDetails: details };
        } else if (hit.summary) {
          const list = [...(trace.value.ragHits || [])];
          list.push(hit.summary);
          trace.value = { ...trace.value, ragHits: list };
        }
        statusMessage.value = 'RAG 命中已返回…';
      },
      onDelta: (text) => {
        if (!text) return;
        gotDelta = true;
        streaming.value = true;
        statusMessage.value = '正在流式输出…';
        answer.value += text;
      },
      onDone: async (done) => {
        if (!done) return;
        sessionId.value = done.sessionId;
        if (done.sessionId) localStorage.setItem(SESSION_KEY, done.sessionId);
        selectedTraceId.value = done.traceId || selectedTraceId.value;
        trace.value = {
          traceId: done.traceId,
          durationMs: done.durationMs,
          runtime: done.runtime,
          sessionId: done.sessionId,
          turnIndex: done.turnIndex,
          toolCalls: done.toolCalls || [],
          ragHits: done.ragHits || [],
          ragHitDetails: done.ragHitDetails || []
        };
        if (!gotDelta && done.answer) {
          statusMessage.value = '正在流式输出…';
          streaming.value = true;
          await typewriterText(done.answer, (partial) => {
            answer.value = partial;
          });
        } else {
          answer.value = done.answer || answer.value;
        }
        const next = [...messages.value];
        for (let i = next.length - 1; i >= 0; i -= 1) {
          if (next[i].role === 'user') {
            next[i] = {
              ...next[i],
              turnIndex: done.turnIndex,
              traceId: done.traceId
            };
            break;
          }
        }
        next.push({
          role: 'assistant',
          content: done.answer || answer.value || '',
          turnIndex: done.turnIndex,
          traceId: done.traceId,
          createdAt: nowLocalIso()
        });
        messages.value = next;
        answer.value = '';
        statusMessage.value = '';
      }
    });
    if (response?.sessionId) {
      sessionId.value = response.sessionId;
      localStorage.setItem(SESSION_KEY, response.sessionId);
    }
    await loadPending();
  } catch (e) {
    message.error(`对话失败：${e.message || e}`);
    statusMessage.value = '';
  } finally {
    streaming.value = false;
    sending.value = false;
  }
}

onMounted(async () => {
  await Promise.all([loadEmployees(), loadVectorStore(), loadPending()]);
  if (employees.value.length && !selectedEmpNo.value) {
    await selectEmployee(employees.value[0].empNo);
  }
  // 等聊天面板挂载后再灌历史，保证能滚到最新
  if (sessionId.value) {
    await loadMessages({ reset: true });
  }
});
</script>

<template>
  <div class="console-grid">
    <n-card
      class="console-column side-card"
      size="small"
      title="HR 工作台"
      :bordered="true"
      :content-style="{ display: 'flex', flexDirection: 'column', minHeight: '0', height: 'auto', padding: '12px 14px', overflow: 'visible' }"
    >
      <template #header-extra>
        <n-button size="small" quaternary circle @click="loadEmployees">
          <template #icon><n-icon :component="RefreshOutline" /></template>
        </n-button>
      </template>

      <ConsoleNav v-model="activeTab" :items="navItems" />

      <template v-if="activeTab === 'employees'">
        <div class="side-list-label">
          <span>员工列表</span>
          <n-tag size="small" round :bordered="false">{{ employees.length }}</n-tag>
        </div>
        <n-scrollbar class="list-scroll side-list-scroll">
          <div class="entity-list">
            <div
              v-for="emp in employees"
              :key="emp.empNo"
              class="emp-item"
              :class="{ selected: selectedEmpNo === emp.empNo }"
              @click="selectEmployee(emp.empNo)"
            >
              <span class="emp-no">{{ emp.empNo }}</span>
              <span class="emp-name">{{ emp.name }}</span>
              <span class="emp-meta">{{ emp.department }} · {{ emp.annualLeaveBalance }}天</span>
            </div>
          </div>
        </n-scrollbar>
      </template>
      <n-empty v-else size="small" description="当前模块无需选择员工" class="side-empty" />
    </n-card>

    <n-card class="console-column main main-card" size="small" :bordered="true"
      :content-style="{ padding: '12px 14px', display: 'flex', flexDirection: 'column', minHeight: '0', height: '100%', overflow: 'hidden' }"
    >
      <div v-if="activeTab === 'employees'" class="main-pane">
        <n-empty
          v-if="!selectedEmpNo"
          description="从左侧选择员工，查看详情并发起对话"
          style="padding: 48px 0; margin: auto"
        />
        <template v-else>
          <div v-if="!employeeDetail" class="loading-hint">正在加载员工详情…</div>

          <template v-else>
            <EmployeeDetailSummary :detail="employeeDetail" />
            <div class="chat-panel-wrapper">
              <ChatPanel
                ref="chatPanelRef"
                :suggestions="suggestions"
                :session-id="sessionId"
                :messages="messages"
                :answer="answer"
                :has-selection="!!selectedEmpNo"
                :streaming="streaming"
                :sending="sending"
                :status-message="statusMessage"
                :has-more="hasMoreHistory"
                :loading-history="loadingHistory"
                :pending-count="pending.length"
                :selected-trace-id="selectedTraceId"
                chat-hint="输入客服问题，例如：员工 EMP-1001 为什么不能请假？"
                @send="sendQuestion"
                @new-session="newSession"
                @load-more="loadMoreHistory"
                @focus-hitl="focusHitl"
                @select-trace="selectTrace"
              />
            </div>
            <n-collapse class="detail-collapse">
              <n-collapse-item title="原始 JSON 数据" name="json">
                <JsonBlock :data="employeeDetail" max-height="160px" />
              </n-collapse-item>
            </n-collapse>
          </template>
        </template>
      </div>

      <div v-else-if="activeTab === 'knowledge'" class="main-pane scrollable">
        <KnowledgeEditor
          :api="hrApi"
          :demo="DEMO"
          :vector-store="vectorStore"
        />
      </div>

      <div v-else-if="activeTab === 'prompts'" class="main-pane scrollable">
        <PromptEditor
          :api="hrApi"
          :demo="DEMO"
          :label-map="HR_LABELS"
        />
      </div>
    </n-card>

    <TracePanel
      :trace="trace"
      :pending="pending"
      :vector-store="vectorStore"
      :show-knowledge-help="activeTab === 'knowledge'"
      :has-conversation="messages.length > 0"
      @confirm="confirmAction"
      @reject="rejectAction"
      @sync="syncKnowledge"
      @rag-eval="runRagEval"
    />
  </div>
</template>

<style scoped>
.main-title {
  font-weight: 700;
  font-size: 16px;
}

.main-sub {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.emp-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px 8px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.15s ease;
  margin-bottom: 6px;
  position: relative;
}

.emp-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: transparent;
  transition: background 0.2s ease;
}

.emp-item:hover {
  background: #fff;
  border-color: rgba(15, 23, 42, 0.08);
}

.emp-item.selected {
  border-color: rgba(99, 102, 241, 0.25);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.06), rgba(255, 255, 255, 0.95));
}

.emp-item.selected::before {
  background: linear-gradient(180deg, #6366f1, #4f46e5);
}

.emp-no {
  font-weight: 700;
  font-size: 12px;
  color: var(--n-color-primary);
}

.emp-name {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.emp-meta {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}
</style>
