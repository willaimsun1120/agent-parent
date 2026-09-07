<script setup>
import { ref, computed, onMounted, h, nextTick } from 'vue';
import {
  NCard,
  NButton,
  NSpace,
  NTag,
  NScrollbar,
  NEmpty,
  NForm,
  NFormItem,
  NSelect,
  NInputNumber,
  NIcon,
  NInput,
  NCollapse,
  NCollapseItem
} from 'naive-ui';
import {
  RefreshOutline,
  ListOutline,
  PlayOutline,
  BookOutline,
  DocumentTextOutline
} from '@vicons/ionicons5';
import { orderApi, typewriterText } from '../api/http';
import TracePanel from '../components/TracePanel.vue';
import ChatPanel from '../components/ChatPanel.vue';
import KnowledgeEditor from '../components/KnowledgeEditor.vue';
import PromptEditor from '../components/PromptEditor.vue';
import JsonBlock from '../components/JsonBlock.vue';
import ConsoleNav from '../components/ConsoleNav.vue';
import OrderListItem from '../components/OrderListItem.vue';
import OrderDetailSummary from '../components/OrderDetailSummary.vue';
import { buildOrderSuggestions, logClick } from '../utils/helpers';
import { nowLocalIso } from '../utils/format';
import { message, dialog } from '../naive';

const DEMO = 'order-cs';
const SESSION_KEY = 'orderAgentSessionId';

const activeTab = ref('orders');
const orders = ref([]);
const selectedOrderNo = ref(null);
const orderDetail = ref(null);
const users = ref([]);
const simForm = ref({ userNo: '', amount: 99, channel: 'WECHAT' });
const simResult = ref(null);
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

const suggestions = computed(() => buildOrderSuggestions(orderDetail.value));

const userOptions = computed(() =>
  users.value.map(u => ({
    label: `${u.userNo} · ${u.nickname} (${u.levelName})`,
    value: u.userNo
  }))
);

const channelOptions = [
  { label: 'WECHAT', value: 'WECHAT' },
  { label: 'ALIPAY', value: 'ALIPAY' }
];

const navItems = [
  { key: 'orders', label: '订单', icon: ListOutline },
  { key: 'simulation', label: '业务模拟', icon: PlayOutline },
  { key: 'knowledge', label: '知识库', icon: BookOutline },
  { key: 'prompts', label: '提示词', icon: DocumentTextOutline }
];

async function loadOrders() {
  logClick(DEMO, 'reload-orders');
  orders.value = await orderApi.get('/api/orders');
}

async function loadUsers() {
  users.value = await orderApi.get('/api/simulation/users');
  if (users.value.length && !simForm.value.userNo) {
    simForm.value.userNo = users.value[0].userNo;
  }
}

async function loadVectorStore() {
  try {
    vectorStore.value = await orderApi.get('/api/knowledge/vector-store');
  } catch (e) {
    console.warn('加载向量库配置失败', e);
  }
}

async function loadPending() {
  pending.value = await orderApi.get('/api/agent/actions/pending');
}

async function selectOrder(orderNo) {
  logClick(DEMO, 'select-order', { orderNo });
  selectedOrderNo.value = orderNo;
  orderDetail.value = null;
  try {
    orderDetail.value = await orderApi.get(`/api/orders/${orderNo}`);
  } catch (e) {
    message.error(`加载订单失败：${e.message || e}`);
    return;
  }
  await loadOrders();
}

function deleteOrder(orderNo) {
  dialog.warning({
    title: '删除订单',
    content: `确定删除订单 ${orderNo}？将同时删除支付、退款、权益记录。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      logClick(DEMO, 'delete-order', { orderNo });
      await orderApi.delete(`/api/orders/${orderNo}`);
      if (selectedOrderNo.value === orderNo) {
        selectedOrderNo.value = null;
        orderDetail.value = null;
      }
      await loadOrders();
      if (!selectedOrderNo.value && orders.value.length) {
        await selectOrder(orders.value[0].orderNo);
      }
      message.success('订单已删除');
    }
  });
}

async function createOrder() {
  logClick(DEMO, 'create-order');
  const detail = await orderApi.post('/api/simulation/orders', simForm.value);
  simResult.value = detail;
  await loadOrders();
  await selectOrder(detail.orderNo);
  activeTab.value = 'simulation';
  message.success('订单已创建');
}

async function simulatePayment(result) {
  if (!selectedOrderNo.value) return message.warning('请先在左侧选择一个订单');
  logClick(DEMO, 'simulate-payment', { result });
  const detail = await orderApi.post(
    `/api/simulation/orders/${selectedOrderNo.value}/payment-callback?result=${result}`
  );
  simResult.value = detail;
  await selectOrder(detail.orderNo);
}

async function simulateBenefit(result) {
  if (!selectedOrderNo.value) return message.warning('请先在左侧选择一个订单');
  logClick(DEMO, 'simulate-benefit', { result });
  const detail = await orderApi.post(
    `/api/simulation/orders/${selectedOrderNo.value}/benefit-issue?result=${result}`
  );
  simResult.value = detail;
  await selectOrder(detail.orderNo);
}

async function syncKnowledge() {
  logClick(DEMO, 'sync-knowledge-panel');
  const result = await orderApi.post('/api/knowledge/sync');
  message.success(`已同步：${result.points} points / ${result.articles} 篇`);
}

async function runRagEval() {
  logClick(DEMO, 'rag-eval');
  const result = await orderApi.post('/api/rag/eval');
  message.success(`RAG 评测 ${result.passed}/${result.total} 通过`);
  simResult.value = result;
}

async function confirmAction(actionId) {
  logClick(DEMO, 'hitl-confirm', { actionId });
  const result = await orderApi.post(`/api/agent/actions/${actionId}/confirm`);
  message.success(`已执行 ${result.actionType || ''} ${result.orderNo || ''}`);
  await loadPending();
  if (selectedOrderNo.value) await selectOrder(selectedOrderNo.value);
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
      await orderApi.post(`/api/agent/actions/${actionId}/reject`, { reason: reasonRef.value });
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
    const page = await orderApi.get(
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
    const data = await orderApi.get(
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
  if (!selectedOrderNo.value) return message.warning('请先在左侧选择一个订单');
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
    const response = await orderApi.streamChat(body, {
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
          runtime: done.runtime || done.agentScopeRuntime,
          sessionId: done.sessionId,
          turnIndex: done.turnIndex,
          toolCalls: done.toolCalls || [],
          ragHits: done.ragHits || [],
          ragHitDetails: done.ragHitDetails || []
        };
        // 若代理缓冲导致没有增量事件，前端补打字机效果
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
  await Promise.all([loadOrders(), loadUsers(), loadVectorStore(), loadPending()]);
  if (orders.value.length && !selectedOrderNo.value) {
    await selectOrder(orders.value[0].orderNo);
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
      title="订单工作台"
      :bordered="true"
      :content-style="{ display: 'flex', flexDirection: 'column', minHeight: '0', height: 'auto', padding: '12px 14px', overflow: 'visible' }"
    >
      <template #header-extra>
        <n-button size="small" quaternary circle @click="loadOrders">
          <template #icon><n-icon :component="RefreshOutline" /></template>
        </n-button>
      </template>

      <ConsoleNav v-model="activeTab" :items="navItems" />

      <template v-if="activeTab === 'orders' || activeTab === 'simulation'">
        <div class="side-list-label">
          <span>订单列表</span>
          <n-tag size="small" round :bordered="false">{{ orders.length }}</n-tag>
        </div>
        <n-scrollbar class="list-scroll side-list-scroll">
          <div class="entity-list">
            <OrderListItem
              v-for="order in orders"
              :key="order.orderNo"
              :order="order"
              :selected="selectedOrderNo === order.orderNo"
              @select="selectOrder"
              @delete="deleteOrder"
            />
          </div>
        </n-scrollbar>
      </template>
      <n-empty v-else size="small" description="当前模块无需选择订单" class="side-empty" />
    </n-card>

    <n-card
      class="console-column main main-card"
      size="small"
      :bordered="true"
      :content-style="{ padding: '12px 14px', display: 'flex', flexDirection: 'column', minHeight: '0', height: '100%', overflow: 'hidden' }"
    >
      <div v-if="activeTab === 'orders'" class="main-pane">
        <n-empty
          v-if="!selectedOrderNo"
          description="从左侧选择订单，查看详情并发起对话"
          style="padding: 48px 0; margin: auto"
        />
        <template v-else>
          <div v-if="!orderDetail" class="loading-hint">正在加载订单详情…</div>

          <template v-else>
            <OrderDetailSummary :detail="orderDetail" />
            <div class="chat-panel-wrapper">
              <ChatPanel
                ref="chatPanelRef"
                :suggestions="suggestions"
                :session-id="sessionId"
                :messages="messages"
                :answer="answer"
                :has-selection="!!selectedOrderNo"
                :streaming="streaming"
                :sending="sending"
                :status-message="statusMessage"
                :has-more="hasMoreHistory"
                :loading-history="loadingHistory"
                :pending-count="pending.length"
                :selected-trace-id="selectedTraceId"
                @send="sendQuestion"
                @new-session="newSession"
                @load-more="loadMoreHistory"
                @focus-hitl="focusHitl"
                @select-trace="selectTrace"
              />
            </div>
            <n-collapse class="detail-collapse">
              <n-collapse-item title="原始 JSON 数据" name="json">
                <JsonBlock :data="orderDetail" max-height="160px" />
              </n-collapse-item>
            </n-collapse>
          </template>
        </template>
      </div>

      <div v-else-if="activeTab === 'simulation'" class="main-pane scrollable">
        <div class="main-title">业务模拟</div>
        <div class="main-sub" style="margin-bottom: 16px">
          {{ selectedOrderNo
            ? `当前选中 ${selectedOrderNo}`
            : '创建新订单，或选择已有订单进行流程模拟' }}
        </div>

        <n-form label-placement="top" size="small">
          <n-space vertical :size="12">
            <n-form-item label="用户">
              <n-select v-model:value="simForm.userNo" :options="userOptions" />
            </n-form-item>
            <n-form-item label="金额">
              <n-input-number v-model:value="simForm.amount" :min="1" :step="0.01" style="width: 100%" />
            </n-form-item>
            <n-form-item label="渠道">
              <n-select v-model:value="simForm.channel" :options="channelOptions" />
            </n-form-item>
            <n-button type="primary" block @click="createOrder">创建订单</n-button>
          </n-space>
        </n-form>

        <n-space v-if="selectedOrderNo" style="margin-top: 16px" wrap>
          <n-button size="small" type="success" secondary @click="simulatePayment('SUCCESS')">
            支付成功
          </n-button>
          <n-button size="small" type="warning" secondary @click="simulatePayment('FAILED')">
            支付失败
          </n-button>
          <n-button size="small" type="success" secondary @click="simulateBenefit('SUCCESS')">
            权益成功
          </n-button>
          <n-button size="small" type="warning" secondary @click="simulateBenefit('FAILED')">
            权益失败
          </n-button>
        </n-space>

        <n-card v-if="simResult" size="small" title="执行结果" style="margin-top: 16px" embedded>
          <JsonBlock :data="simResult" max-height="240px" />
        </n-card>
      </div>

      <div v-else-if="activeTab === 'knowledge'" class="main-pane scrollable">
        <KnowledgeEditor
          :api="orderApi"
          :demo="DEMO"
          :vector-store="vectorStore"
        />
      </div>
      <div v-else-if="activeTab === 'prompts'" class="main-pane scrollable">
        <PromptEditor :api="orderApi" :demo="DEMO" />
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
