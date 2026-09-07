<script setup>
import { ref, computed, nextTick, watch, onMounted } from 'vue';
import {
  NSpace,
  NButton,
  NTag,
  NInput,
  NIcon,
  NTooltip,
  NSpin,
  NDropdown
} from 'naive-ui';
import {
  SendOutline,
  RefreshOutline,
  ChatbubbleEllipsesOutline,
  PersonOutline,
  HandLeftOutline,
  ChevronDownOutline
} from '@vicons/ionicons5';
import { highlightEntities } from '../utils/format';

const MODE_KEY = 'agentChatMode';

const props = defineProps({
  suggestions: { type: Array, default: () => [] },
  sessionId: { type: String, default: null },
  messages: { type: Array, default: () => [] },
  answer: { type: String, default: '' },
  chatHint: { type: String, default: '' },
  requireSelection: { type: Boolean, default: true },
  hasSelection: { type: Boolean, default: false },
  embedded: { type: Boolean, default: false },
  streaming: { type: Boolean, default: false },
  sending: { type: Boolean, default: false },
  statusMessage: { type: String, default: '' },
  hasMore: { type: Boolean, default: false },
  loadingHistory: { type: Boolean, default: false },
  pendingCount: { type: Number, default: 0 },
  /** 当前观测区展示的 traceId，用于高亮对应气泡 */
  selectedTraceId: { type: String, default: null }
});

const emit = defineEmits(['send', 'new-session', 'load-more', 'focus-hitl', 'select-trace']);

const question = ref('');
const savedMode = localStorage.getItem(MODE_KEY);
const mode = ref(['manual', 'agentscope', 'langchain4j'].includes(savedMode) ? savedMode : 'manual');
const threadRef = ref(null);
const stickToBottom = ref(true);
const allowLoadMore = ref(false);
let pendingScrollRestore = null;
let pinBottomSeq = 0;

const modes = [
  { id: 'manual', label: '手写模式' },
  { id: 'agentscope', label: 'AgentScope' },
  { id: 'langchain4j', label: 'LangChain4j' }
];

const modeOptions = computed(() =>
  modes.map((m) => ({
    label: m.label,
    key: m.id
  }))
);

const modeLabel = computed(() => modes.find((m) => m.id === mode.value)?.label || '手写模式');

const placeholder = computed(() => {
  const first = props.suggestions[0]?.question;
  if (first) return `输入客服问题，例如：${first}`;
  return props.chatHint || '输入客服问题，例如：订单 ORD-1001 为什么不能退款？';
});

const shortSession = computed(() => {
  if (!props.sessionId) return null;
  if (props.sessionId.length <= 16) return props.sessionId;
  return `${props.sessionId.slice(0, 8)}…${props.sessionId.slice(-6)}`;
});

const displayMessages = computed(() => {
  const list = Array.isArray(props.messages) ? [...props.messages] : [];
  if (props.streaming && props.answer) {
    const last = list[list.length - 1];
    if (!last || last.role !== 'assistant' || last.content !== props.answer) {
      if (last && last.role === 'assistant' && props.sending) {
        list[list.length - 1] = { ...last, content: props.answer };
      } else if (!last || last.role !== 'assistant') {
        list.push({ role: 'assistant', content: props.answer });
      }
    }
  }
  return list;
});

const showThinking = computed(() => {
  if (!props.sending) return false;
  const last = displayMessages.value[displayMessages.value.length - 1];
  return !(last && last.role === 'assistant' && last.content);
});

function selectMode(key) {
  mode.value = key;
  localStorage.setItem(MODE_KEY, key);
}

function paragraphs(content) {
  if (!content) return [];
  return String(content).split(/\n{2,}/).filter(Boolean);
}

function messageKey(msg, idx) {
  if (msg?.id != null) return `id-${msg.id}`;
  return `${idx}-${msg?.role}-${(msg?.content || '').slice(0, 24)}`;
}

function canSelectTrace(msg) {
  return msg?.role === 'assistant' && !!(msg?.traceId && String(msg.traceId).trim());
}

function isSelectedTrace(msg) {
  return canSelectTrace(msg) && props.selectedTraceId === msg.traceId;
}

function onSelectTrace(msg) {
  if (!canSelectTrace(msg) || props.sending) return;
  emit('select-trace', {
    traceId: msg.traceId,
    turnIndex: msg.turnIndex ?? null,
    role: msg.role
  });
}

/** 气泡时间：今天只显示时分，跨天显示月日+时分 */
function formatMessageTime(value) {
  if (!value) return '';
  const date = value instanceof Date ? value : new Date(String(value).replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return String(value);
  const now = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  const hm = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  const sameDay =
    date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate();
  if (sameDay) return hm;
  const sameYear = date.getFullYear() === now.getFullYear();
  if (sameYear) return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${hm}`;
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${hm}`;
}

function send() {
  const q = question.value.trim();
  if (!q || props.sending) return;
  question.value = '';
  stickToBottom.value = true;
  emit('send', { question: q, mode: mode.value });
  scrollToBottom(true);
}

function newSession() {
  question.value = '';
  stickToBottom.value = true;
  emit('new-session');
}

function scrollToBottom(force = false) {
  const seq = ++pinBottomSeq;
  const run = () => {
    if (seq !== pinBottomSeq) return;
    const el = threadRef.value;
    if (!el) return;
    el.scrollTop = el.scrollHeight;
  };
  nextTick(() => {
    run();
    requestAnimationFrame(() => {
      run();
      requestAnimationFrame(run);
    });
    if (force) {
      [32, 80, 160, 320].forEach((ms) => {
        setTimeout(() => {
          if (seq !== pinBottomSeq) return;
          run();
        }, ms);
      });
    }
  });
}

function onThreadScroll() {
  const el = threadRef.value;
  if (!el) return;
  const distanceFromBottom = el.scrollHeight - el.clientHeight - el.scrollTop;
  stickToBottom.value = distanceFromBottom < 64;
  if (!allowLoadMore.value) return;
  // 滚到顶部附近加载更早消息
  if (el.scrollTop < 72 && props.hasMore && !props.loadingHistory) {
    pendingScrollRestore = {
      prevHeight: el.scrollHeight,
      prevTop: el.scrollTop
    };
    emit('load-more');
  }
}

watch(() => props.loadingHistory, (loading, prev) => {
  if (!(prev && !loading)) return;
  if (pendingScrollRestore) {
    const snapshot = pendingScrollRestore;
    pendingScrollRestore = null;
    nextTick(() => {
      const el = threadRef.value;
      if (!el) return;
      const delta = el.scrollHeight - snapshot.prevHeight;
      el.scrollTop = snapshot.prevTop + delta;
    });
    return;
  }
  stickToBottom.value = true;
  scrollToBottom(true);
});

watch(() => props.answer, () => {
  if (stickToBottom.value) scrollToBottom();
});
watch(() => props.sessionId, () => {
  stickToBottom.value = true;
  allowLoadMore.value = false;
  scrollToBottom(true);
  setTimeout(() => { allowLoadMore.value = true; }, 400);
});
watch(() => props.streaming, () => {
  if (stickToBottom.value) scrollToBottom();
});
watch(
  () => props.messages?.length || 0,
  () => {
    if (!props.messages?.length) return;
    if (props.loadingHistory || pendingScrollRestore) return;
    if (stickToBottom.value) scrollToBottom(true);
  }
);

onMounted(() => {
  stickToBottom.value = true;
  scrollToBottom(true);
  // 等首屏贴底完成后再允许向上加载，避免 scrollTop=0 误触发 load-more
  setTimeout(() => { allowLoadMore.value = true; }, 450);
});

defineExpose({
  scrollToLatest: () => {
    stickToBottom.value = true;
    scrollToBottom(true);
  }
});
</script>

<template>
  <section class="chat-shell" :class="{ embedded }">
    <header class="chat-shell-header">
      <div class="chat-title">
        <n-icon :component="ChatbubbleEllipsesOutline" size="20" class="chat-title-icon" />
        <div>
          <div class="chat-title-text">客服提问</div>
          <div class="chat-title-sub">
            {{ statusMessage || '支持多轮对话：同一会话内可追问上下文' }}
          </div>
        </div>
      </div>
      <n-space :size="8" align="center">
        <n-tooltip v-if="sessionId" trigger="hover">
          <template #trigger>
            <n-tag size="small" round :bordered="false" type="info">
              {{ shortSession }}
            </n-tag>
          </template>
          {{ sessionId }}
        </n-tooltip>
        <n-button size="small" quaternary @click="newSession">
          <template #icon><n-icon :component="RefreshOutline" /></template>
          新会话
        </n-button>
      </n-space>
    </header>

    <button
      v-if="pendingCount > 0"
      type="button"
      class="hitl-tip"
      @click="emit('focus-hitl')"
    >
      <n-icon :component="HandLeftOutline" />
      <span>有 {{ pendingCount }} 条待确认操作</span>
      <span class="hitl-tip-action">查看右侧观测 →</span>
    </button>

    <div ref="threadRef" class="chat-thread" @scroll.passive="onThreadScroll">
      <div class="chat-thread-inner">
        <div v-if="loadingHistory" class="history-loading">
          <n-spin size="small" />
          <span>加载更早消息…</span>
        </div>
        <div v-else-if="hasMore" class="history-hint">上滑加载更早消息</div>

        <div v-if="!displayMessages.length && !sending" class="thread-empty">
          <n-icon :component="ChatbubbleEllipsesOutline" size="32" />
          <p>从下方快捷问题点选，或直接输入客服问题（刷新后可恢复历史）</p>
        </div>

        <div
          v-for="(msg, idx) in displayMessages"
          :key="messageKey(msg, idx)"
          class="bubble-row"
          :class="[
            msg.role === 'user' ? 'user' : 'agent',
            { selected: isSelectedTrace(msg), selectable: canSelectTrace(msg) }
          ]"
          :title="canSelectTrace(msg) ? '点击查看该轮运行观测' : undefined"
          @click="onSelectTrace(msg)"
        >
          <div v-if="msg.role === 'user'" class="bubble-avatar user-avatar">
            <n-icon :component="PersonOutline" />
          </div>
          <div v-else class="bubble-avatar agent-avatar">AI</div>
          <div class="bubble" :class="msg.role === 'user' ? 'user-bubble' : 'agent-bubble'">
            <div class="bubble-meta">
              <span class="bubble-role">{{ msg.role === 'user' ? '你' : 'Agent 回复' }}</span>
              <span v-if="formatMessageTime(msg.createdAt)" class="bubble-time">
                {{ formatMessageTime(msg.createdAt) }}
              </span>
              <span
                v-if="streaming && msg.role === 'assistant' && idx === displayMessages.length - 1"
                class="stream-badge"
              >生成中</span>
              <span v-else-if="canSelectTrace(msg)" class="trace-hint">观测</span>
            </div>
            <div class="bubble-body">
              <p v-for="(para, i) in paragraphs(msg.content)" :key="i" class="answer-para">
                <template v-for="(part, j) in highlightEntities(para)" :key="j">
                  <n-tag
                    v-if="part.type === 'entity'"
                    size="small"
                    round
                    :bordered="false"
                    :type="msg.role === 'user' ? 'default' : 'info'"
                    :class="msg.role === 'user' ? 'entity-on-user' : ''"
                  >
                    {{ part.value }}
                  </n-tag>
                  <span v-else>{{ part.value }}</span>
                </template>
              </p>
              <span
                v-if="streaming && msg.role === 'assistant' && idx === displayMessages.length - 1"
                class="stream-cursor"
              />
            </div>
          </div>
        </div>

        <div v-if="showThinking" class="bubble-row agent thinking">
          <div class="bubble-avatar agent-avatar">AI</div>
          <div class="bubble agent-bubble shimmer">
            <div class="thinking-label">{{ statusMessage || '模型推理中…' }}</div>
            <div class="thinking-dots">
              <span class="thinking-dot" />
              <span class="thinking-dot" />
              <span class="thinking-dot" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <div v-if="suggestions.length" class="quick-tags-row">
        <span class="quick-tags-label">快捷问题</span>
        <n-space class="quick-tags" size="small" wrap>
          <n-button
            v-for="item in suggestions"
            :key="item.question"
            size="small"
            round
            secondary
            type="primary"
            @click="question = item.question"
          >
            {{ item.label }}
          </n-button>
        </n-space>
      </div>

      <div class="compose-box">
        <n-dropdown :options="modeOptions" trigger="click" @select="selectMode">
          <n-button size="tiny" quaternary class="mode-dropdown">
            {{ modeLabel }}
            <n-icon :component="ChevronDownOutline" :size="14" style="margin-left: 2px" />
          </n-button>
        </n-dropdown>
        <n-input
          v-model:value="question"
          type="textarea"
          :rows="3"
          :placeholder="placeholder"
          :autosize="{ minRows: 2, maxRows: 5 }"
          @keydown.enter.prevent="send"
        />
        <div class="compose-footer">
          <n-button
            class="send-btn"
            type="primary"
            size="small"
            :loading="sending"
            :disabled="(requireSelection && !hasSelection) || sending"
            @click="send"
          >
            <template #icon><n-icon :component="SendOutline" /></template>
            发送给 Agent
          </n-button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.chat-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.chat-shell.embedded {
  border-top: none;
}

.chat-shell-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 18px 12px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.07);
  background: rgba(255, 255, 255, 0.92);
}

.chat-title-text {
  font-weight: 700;
  font-size: 15.5px;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.chat-title-sub {
  font-size: 12px;
  color: #64748b;
  margin-top: 3px;
  font-weight: 500;
}

.hitl-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 8px 16px;
  border: none;
  border-bottom: 1px solid #fed7aa;
  background: #fff7ed;
  color: #9a3412;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
  width: 100%;
  flex-shrink: 0;
}

.hitl-tip:hover {
  background: #ffedd5;
}

.hitl-tip-action {
  margin-left: auto;
  color: #c2410c;
}

.chat-title {
  display: flex;
  gap: 12px;
  align-items: center;
}

.chat-title-icon {
  color: var(--n-color-primary);
}

.chat-thread {
  flex: 1 1 0;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: scroll;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.55) rgba(148, 163, 184, 0.18);
  padding: 16px 14px 12px 20px;
  display: flex;
  flex-direction: column;
  background: transparent;
}

.chat-thread::-webkit-scrollbar {
  width: 8px;
}

.chat-thread::-webkit-scrollbar-track {
  background: rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  margin: 6px 0;
}

.chat-thread::-webkit-scrollbar-thumb {
  background: rgba(100, 116, 139, 0.55);
  border-radius: 999px;
}

.chat-thread::-webkit-scrollbar-thumb:hover {
  background: rgba(71, 85, 105, 0.75);
}

.chat-thread-inner {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-height: min-content;
  padding-bottom: 12px;
}

.history-loading,
.history-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
  padding: 8px 0 12px;
}

.thread-empty {
  text-align: center;
  padding: 56px 20px;
  color: #94a3b8;
  border: 1px dashed rgba(15, 23, 42, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.55);
}

.thread-empty p {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.6;
}

.bubble-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.bubble-row.user {
  flex-direction: row-reverse;
}

.bubble-row.selectable {
  cursor: pointer;
}

.bubble-row.selectable:hover .bubble {
  outline: 1px solid rgba(99, 102, 241, 0.35);
}

.bubble-row.selected .bubble {
  outline: 2px solid rgba(99, 102, 241, 0.55);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}

.bubble-row + .bubble-row {
  margin-top: 2px;
}

.trace-hint {
  margin-left: auto;
  font-size: 10px;
  font-weight: 600;
  color: #6366f1;
  opacity: 0.85;
}

.bubble-avatar {
  width: 34px;
  height: 34px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 800;
  flex-shrink: 0;
  margin-top: 2px;
}

.user-avatar {
  background: #e2e8f0;
  color: #475569;
}

.agent-avatar {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

.bubble {
  max-width: min(100%, 920px);
  border-radius: 16px;
  padding: 12px 16px 14px;
}

.user-bubble {
  background: #eef2ff;
  color: #1e1b4b;
  border: 1px solid #c7d2fe;
  border-bottom-right-radius: 5px;
  box-shadow: none;
}

.user-bubble .bubble-meta {
  opacity: 0.75;
  color: #4338ca;
}

.user-bubble .bubble-time {
  color: #6366f1;
}

.user-bubble :deep(.entity-on-user) {
  background: #fff !important;
  color: #4338ca !important;
  border: 1px solid #c7d2fe !important;
}

.agent-bubble {
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-bottom-left-radius: 5px;
  box-shadow: 0 2px 14px rgba(15, 23, 42, 0.05);
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  font-weight: 700;
  opacity: 0.72;
  margin-bottom: 7px;
  letter-spacing: 0.03em;
}

.bubble-body {
  font-size: 14.5px;
  line-height: 1.7;
  color: #1e293b;
  font-weight: 400;
}

.bubble-time {
  display: inline;
  margin-top: 0;
  margin-left: 0;
  font-size: 11px;
  font-weight: 500;
  opacity: 0.65;
  font-variant-numeric: tabular-nums;
}

.answer-para {
  margin: 0 0 10px;
}

.answer-para:last-of-type {
  margin-bottom: 0;
}

.shimmer {
  display: flex;
  gap: 6px;
  align-items: center;
  min-height: 40px;
}

.thinking-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  animation: pulse 1.2s ease-in-out infinite;
}

.thinking-dot:nth-child(2) { animation-delay: 0.15s; }
.thinking-dot:nth-child(3) { animation-delay: 0.3s; }

@keyframes pulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.chat-input-area {
  flex-shrink: 0;
  padding: 12px 16px 16px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.96);
}

.quick-tags-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 10px;
  margin-bottom: 10px;
}

.quick-tags-label {
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}

.quick-tags {
  margin-bottom: 0;
}

.compose-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.mode-dropdown {
  align-self: flex-start;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  padding: 0 2px;
  height: 22px;
  margin-left: -2px;
}

.mode-dropdown:hover {
  color: #4f46e5;
}

.compose-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.send-btn {
  height: 34px;
  min-width: 120px;
  padding: 0 14px;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  border-radius: 8px;
}

.stream-badge {
  margin-left: 8px;
  font-size: 11px;
  color: #6366f1;
  font-weight: 600;
}

.stream-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: #6366f1;
  animation: blink 1s step-end infinite;
}

.thinking-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.thinking-dots {
  display: flex;
  gap: 6px;
}

@keyframes blink {
  50% { opacity: 0; }
}

@media (max-width: 640px) {
  .compose-footer {
    justify-content: stretch;
  }

  .send-btn {
    width: 100%;
  }
}
</style>
