<script setup>
import { computed, ref, watch } from 'vue';
import {
  NCard,
  NEmpty,
  NButton,
  NSpace,
  NTag,
  NAlert,
  NCode,
  NIcon,
  NTooltip,
  NBadge,
  NCollapse,
  NCollapseItem
} from 'naive-ui';
import {
  ConstructOutline,
  SearchOutline,
  HandLeftOutline,
  TimeOutline,
  GitNetworkOutline,
  PulseOutline,
  CopyOutline
} from '@vicons/ionicons5';
import { parseToolCall } from '../utils/format';

const props = defineProps({
  trace: { type: Object, default: () => ({}) },
  pending: { type: Array, default: () => [] },
  vectorStore: { type: Object, default: null },
  showKnowledgeHelp: { type: Boolean, default: false },
  /** 中间栏已有对话记录（刷新后仍有消息时，避免再显示「发起对话后…」空态） */
  hasConversation: { type: Boolean, default: false }
});

const emit = defineEmits(['confirm', 'reject', 'sync', 'ragEval']);

const activePanel = ref([]);
const lastSummary = ref(null);

const toolCalls = computed(() =>
  (props.trace.toolCalls || []).map((raw) => parseToolCall(raw))
);

const ragHits = computed(() => {
  const details = props.trace.ragHitDetails;
  if (Array.isArray(details) && details.length) {
    return details.map((hit) => ({
      kind: 'detail',
      docCode: hit.docCode || '',
      title: hit.title || '',
      score: hit.score,
      snippet: hit.snippet || hit.summary || ''
    }));
  }
  return (props.trace.ragHits || []).map((hit) => {
    if (hit && typeof hit === 'object') {
      return {
        kind: 'detail',
        docCode: hit.docCode || '',
        title: hit.title || '',
        score: hit.score,
        snippet: hit.snippet || hit.summary || ''
      };
    }
    return { kind: 'text', text: String(hit) };
  });
});

const hasTrace = computed(() =>
  props.trace.traceId || props.trace.durationMs != null || toolCalls.value.length || ragHits.value.length
);

const shortRuntime = computed(() => {
  const r = props.trace.runtime || '';
  if (!r) return '';
  const head = r.split('+')[0].trim();
  return head.length > 28 ? `${head.slice(0, 28)}…` : head;
});

watch(
  () => [toolCalls.value.length, ragHits.value.length, hasTrace.value],
  () => {
    const next = [];
    if (toolCalls.value.length) next.push('tools');
    if (ragHits.value.length) next.push('rag');
    if (next.length) activePanel.value = next;
  },
  { immediate: true }
);

watch(
  () => [hasTrace.value, props.trace.traceId, props.trace.durationMs, toolCalls.value.length, ragHits.value.length],
  () => {
    if (!hasTrace.value) return;
    lastSummary.value = {
      durationMs: props.trace.durationMs,
      tools: toolCalls.value.length,
      rag: ragHits.value.length,
      traceId: props.trace.traceId || null
    };
  }
);

function vectorLabel(type) {
  if (type === 'milvus') return 'Milvus';
  if (type === 'qdrant') return 'Qdrant';
  return type || '向量库';
}

function vectorUi(type) {
  if (type === 'milvus') return 'http://127.0.0.1:8000';
  if (type === 'qdrant') return 'http://127.0.0.1:6333/dashboard';
  return null;
}

function shortId(id) {
  if (!id || id.length <= 18) return id || '-';
  return `${id.slice(0, 8)}…${id.slice(-6)}`;
}

function formatDuration(ms) {
  if (ms == null) return '-';
  if (ms < 1000) return `${ms}`;
  return (ms / 1000).toFixed(1);
}

function durationUnit(ms) {
  if (ms == null) return '';
  return ms < 1000 ? 'ms' : 's';
}

function formatScore(score) {
  if (score == null || Number.isNaN(Number(score))) return '';
  return Number(score).toFixed(4);
}
</script>

<template>
  <n-card
    class="console-column trace-shell"
    size="small"
    :bordered="true"
    :content-style="{ padding: '14px', display: 'flex', flexDirection: 'column', minHeight: '0', height: 'auto', overflow: 'visible' }"
  >
    <template v-if="!showKnowledgeHelp">
      <div class="tp-top">
        <div class="tp-brand">
          <div class="tp-brand-icon">
            <n-icon :component="PulseOutline" size="18" />
          </div>
          <div class="tp-brand-text">
            <div class="tp-title">运行观测</div>
            <n-tooltip v-if="trace.runtime" trigger="hover" :style="{ maxWidth: '360px' }">
              <template #trigger>
                <div class="tp-runtime">{{ shortRuntime }}</div>
              </template>
              {{ trace.runtime }}
            </n-tooltip>
            <div v-else class="tp-runtime muted">
              {{ pending.length ? '有待确认操作' : (hasConversation ? '本会话可继续追问' : '等待 Agent 响应') }}
            </div>
          </div>
        </div>
        <div v-if="pending.length" class="tp-hitl-banner">
          <n-badge :value="pending.length" :max="99" type="warning">
            <n-tag type="warning" round :bordered="false" size="small">
              <template #icon><n-icon :component="HandLeftOutline" /></template>
              待确认 HITL
            </n-tag>
          </n-badge>
        </div>
      </div>

      <!-- 有待确认时始终展示，不依赖本次是否已有 trace -->
      <div v-if="pending.length" class="tp-hitl-block">
        <div class="tp-hitl-block-title">需人工确认</div>
        <div class="tp-hitl-list">
          <div v-for="action in pending" :key="action.actionId" class="tp-hitl-item">
            <div class="tp-hitl-summary">{{ action.summary }}</div>
            <div class="tp-hitl-id">{{ action.actionId }} · {{ action.actionType }} · {{ action.businessKey }}</div>
            <n-space size="small" style="margin-top: 8px">
              <n-button size="tiny" type="primary" @click="emit('confirm', action.actionId)">
                确认
              </n-button>
              <n-button size="tiny" secondary type="warning" @click="emit('reject', action.actionId)">
                拒绝
              </n-button>
            </n-space>
          </div>
        </div>
      </div>

      <div v-if="!hasTrace && !lastSummary && !pending.length && !hasConversation" class="tp-guide-wrap">
        <div class="tp-guide">
          <div class="tp-guide-title">发起对话后这里会显示</div>
          <ul class="tp-guide-list">
            <li>耗时与 Trace ID</li>
            <li>工具调用链路</li>
            <li>RAG 命中片段</li>
            <li>需人工确认的 HITL</li>
          </ul>
        </div>
      </div>

      <div v-else-if="!hasTrace && lastSummary" class="tp-last">
        <div class="tp-last-label">上次对话摘要</div>
        <div class="tp-metrics">
          <div class="tp-metric tp-metric-time">
            <n-icon :component="TimeOutline" class="tp-metric-icon" />
            <div class="tp-metric-body">
              <div class="tp-metric-value">
                {{ formatDuration(lastSummary.durationMs) }}
                <span class="tp-metric-unit">{{ durationUnit(lastSummary.durationMs) }}</span>
              </div>
              <div class="tp-metric-label">耗时</div>
            </div>
          </div>
          <div class="tp-metric tp-metric-turn">
            <n-icon :component="ConstructOutline" class="tp-metric-icon" />
            <div class="tp-metric-body">
              <div class="tp-metric-value">{{ lastSummary.tools }}</div>
              <div class="tp-metric-label">工具</div>
            </div>
          </div>
          <div class="tp-metric tp-metric-trace">
            <n-icon :component="SearchOutline" class="tp-metric-icon" />
            <div class="tp-metric-body">
              <div class="tp-metric-value">{{ lastSummary.rag }}</div>
              <div class="tp-metric-label">RAG</div>
            </div>
          </div>
        </div>
        <div class="tp-last-hint">发送新问题后将刷新为本次观测</div>
      </div>

      <div v-else-if="!hasTrace && hasConversation" class="tp-soft-hint">
        本会话已有对话记录。继续提问后，这里会展示本次耗时、工具与 RAG。
      </div>

      <template v-if="hasTrace">
        <div class="tp-metrics">
          <div class="tp-metric tp-metric-time">
            <n-icon :component="TimeOutline" class="tp-metric-icon" />
            <div class="tp-metric-body">
              <div class="tp-metric-value">
                {{ formatDuration(trace.durationMs) }}
                <span class="tp-metric-unit">{{ durationUnit(trace.durationMs) }}</span>
              </div>
              <div class="tp-metric-label">耗时</div>
            </div>
          </div>

          <div class="tp-metric tp-metric-turn">
            <n-icon :component="GitNetworkOutline" class="tp-metric-icon" />
            <div class="tp-metric-body">
              <div class="tp-metric-value">{{ trace.turnIndex ?? '-' }}</div>
              <div class="tp-metric-label">对话轮次</div>
            </div>
          </div>

          <div class="tp-metric tp-metric-trace">
            <n-icon :component="CopyOutline" class="tp-metric-icon" />
            <div class="tp-metric-body">
              <n-tooltip v-if="trace.traceId" trigger="hover">
                <template #trigger>
                  <div class="tp-metric-value tp-mono">{{ shortId(trace.traceId) }}</div>
                </template>
                {{ trace.traceId }}
              </n-tooltip>
              <div v-else class="tp-metric-value muted">-</div>
              <div class="tp-metric-label">Trace ID</div>
            </div>
          </div>
        </div>

        <n-collapse v-model:expanded-names="activePanel" class="tp-accordion">
          <n-collapse-item name="tools">
            <template #header>
              <div class="tp-acc-head">
                <n-icon :component="ConstructOutline" />
                <span>工具调用</span>
                <n-badge :value="toolCalls.length" :max="99" type="info" />
              </div>
            </template>
            <div v-if="toolCalls.length" class="tp-tool-list">
              <div v-for="(call, i) in toolCalls" :key="i" class="tp-tool-item">
                <div class="tp-tool-name">{{ call.name }}</div>
                <n-code
                  v-if="call.args"
                  :code="call.args"
                  language="text"
                  :word-wrap="true"
                  class="tp-tool-args"
                />
              </div>
            </div>
            <n-empty v-else size="small" description="未调用工具" />
          </n-collapse-item>

          <n-collapse-item name="rag">
            <template #header>
              <div class="tp-acc-head">
                <n-icon :component="SearchOutline" />
                <span>RAG 命中</span>
                <n-badge :value="ragHits.length" :max="99" type="success" />
              </div>
            </template>
            <div v-if="ragHits.length" class="tp-rag-list">
              <div v-for="(hit, i) in ragHits" :key="i" class="tp-rag-item detail">
                <div class="tp-rag-head">
                  <n-tag size="small" round :bordered="false" type="success">{{ i + 1 }}</n-tag>
                  <template v-if="hit.kind === 'detail'">
                    <span class="tp-rag-code">{{ hit.docCode || hit.title || '未命名文档' }}</span>
                    <n-tag v-if="formatScore(hit.score)" size="tiny" round :bordered="false">
                      {{ formatScore(hit.score) }}
                    </n-tag>
                  </template>
                </div>
                <p v-if="hit.kind === 'detail'" class="tp-rag-snippet">{{ hit.snippet || hit.title }}</p>
                <p v-else>{{ hit.text }}</p>
              </div>
            </div>
            <n-empty v-else size="small" description="暂无命中" />
          </n-collapse-item>
        </n-collapse>
      </template>
    </template>

    <template v-else>
      <div class="tp-title" style="margin-bottom: 12px">知识库工具</div>
      <n-alert v-if="vectorStore" type="info" :bordered="false" class="tp-vector-alert">
        <template #header>{{ vectorLabel(vectorStore.type) }}</template>
        {{ vectorStore.collection }}
        <n-button
          v-if="vectorUi(vectorStore.type)"
          tag="a"
          :href="vectorUi(vectorStore.type)"
          target="_blank"
          rel="noopener"
          text
          type="primary"
          style="margin-top: 6px"
        >
          打开管理 UI →
        </n-button>
      </n-alert>
      <n-space vertical>
        <n-button type="primary" block @click="emit('sync')">全量同步向量库</n-button>
        <n-button block secondary @click="emit('ragEval')">RAG 评测</n-button>
      </n-space>
    </template>
  </n-card>
</template>

<style scoped>
.trace-shell {
  container-type: inline-size;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tp-top {
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tp-brand {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.tp-brand-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(14, 165, 233, 0.12));
  color: #4f46e5;
  flex-shrink: 0;
}

.tp-brand-text {
  min-width: 0;
  flex: 1;
}

.tp-title {
  font-size: 15.5px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #0f172a;
  white-space: nowrap;
  line-height: 1.3;
}

.tp-runtime {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 3px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  font-weight: 500;
}

.tp-empty {
  padding: 12px 0;
}

.tp-guide-wrap {
  padding: 8px 0 12px;
  flex: 1;
}

.tp-guide {
  text-align: left;
  padding: 12px 12px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px dashed rgba(15, 23, 42, 0.1);
}

.tp-guide-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 8px;
}

.tp-guide-list {
  margin: 0;
  padding-left: 18px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.tp-guide-hitl {
  margin-top: 10px;
  font-size: 12px;
  font-weight: 600;
  color: #c2410c;
  background: #fff7ed;
  border-radius: 8px;
  padding: 8px 10px;
}

.tp-hitl-block {
  margin-bottom: 14px;
  padding: 12px;
  border-radius: 12px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
}

.tp-hitl-block-title {
  font-size: 12px;
  font-weight: 700;
  color: #9a3412;
  margin-bottom: 10px;
}

.tp-soft-hint {
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
  padding: 12px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(15, 23, 42, 0.06);
  margin-bottom: 8px;
}

.tp-last {
  margin-bottom: 8px;
}

.tp-last-label {
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 8px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.tp-last-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-top: -6px;
  margin-bottom: 12px;
}

.tp-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 14px;
}

.tp-metric {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 10px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: #fff;
  min-width: 0;
}

.tp-metric-time {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(99, 102, 241, 0.02));
}

.tp-metric-turn {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.08), rgba(14, 165, 233, 0.02));
}

.tp-metric-trace {
  background: linear-gradient(135deg, rgba(100, 116, 139, 0.08), rgba(100, 116, 139, 0.02));
}

.tp-metric-icon {
  opacity: 0.45;
  flex-shrink: 0;
  font-size: 18px;
}

.tp-metric-body {
  min-width: 0;
  flex: 1;
}

.tp-metric-value {
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.tp-metric-unit {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  margin-left: 2px;
}

.tp-metric-label {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
  white-space: nowrap;
}

.tp-mono {
  font-family: JetBrains Mono, ui-monospace, monospace;
  font-size: 12px !important;
  font-weight: 600 !important;
}

.muted {
  color: #94a3b8 !important;
}

.tp-accordion :deep(.n-collapse-item__header) {
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.tp-accordion :deep(.n-collapse-item__content-inner) {
  padding: 0 12px 12px;
}

.tp-acc-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tp-tool-list,
.tp-rag-list,
.tp-hitl-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tp-tool-item {
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.05);
  border: 1px solid rgba(99, 102, 241, 0.1);
}

.tp-tool-name {
  font-family: JetBrains Mono, ui-monospace, monospace;
  font-size: 12px;
  font-weight: 700;
  color: #4f46e5;
  margin-bottom: 4px;
}

.tp-tool-args {
  font-size: 11px !important;
}

.tp-rag-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(5, 150, 105, 0.06);
  border: 1px solid rgba(5, 150, 105, 0.12);
}

.tp-rag-item p {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #334155;
  flex: 1;
}

.tp-hitl-item {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid rgba(217, 119, 6, 0.2);
  background: rgba(217, 119, 6, 0.06);
}

.tp-hitl-summary {
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
}

.tp-hitl-id {
  font-family: JetBrains Mono, monospace;
  font-size: 10px;
  color: #94a3b8;
  margin-top: 4px;
}


.tp-top {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tp-hitl-banner {
  display: flex;
  justify-content: flex-start;
}

.tp-rag-item.detail {
  flex-direction: column;
  gap: 6px;
}

.tp-rag-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tp-rag-code {
  font-size: 12px;
  font-weight: 700;
  color: #047857;
}

.tp-rag-snippet {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #334155;
}

.tp-vector-alert {
  border-radius: 12px;
  margin-bottom: 12px;
}

@container (min-width: 900px) {
  .tp-metrics {
    gap: 12px;
  }

  .tp-metric {
    padding: 12px 14px;
  }
}

@container (max-width: 280px) {
  .tp-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .tp-metric-trace {
    grid-column: 1 / -1;
  }
}
</style>
