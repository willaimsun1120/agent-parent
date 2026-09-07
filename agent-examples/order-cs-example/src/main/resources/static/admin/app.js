const state = {
  activeTab: 'orders',
  selectedOrderNo: null,
  selectedOrderDetail: null,
  editingDocCode: null,
  editingPromptCode: null,
  sessionId: localStorage.getItem('agentSessionId') || null,
  vectorStore: null
};

const ADMIN_DEMO = 'order-cs';
let loadingCount = 0;
let activeFetchCount = 0;
let statusMessage = '处理中...';

function updatePageStatus() {
  const bar = document.getElementById('pageStatus');
  const text = document.getElementById('pageStatusText');
  if (!bar || !text) {
    return;
  }
  if (loadingCount > 0) {
    text.textContent = statusMessage;
    bar.classList.remove('hidden');
    document.body.classList.add('is-loading');
  } else {
    bar.classList.add('hidden');
    document.body.classList.remove('is-loading');
    document.querySelectorAll('button.is-busy').forEach(button => button.classList.remove('is-busy'));
  }
}

function beginLoading(message = '处理中...') {
  loadingCount += 1;
  statusMessage = message;
  updatePageStatus();
}

function endLoading() {
  loadingCount = Math.max(0, loadingCount - 1);
  updatePageStatus();
}

function markButtonBusy(button, busy) {
  if (!button) {
    return;
  }
  button.classList.toggle('is-busy', busy);
  button.disabled = busy;
}

function loadingMessageForRequest(url, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  if (method === 'GET') {
    return '加载中...';
  }
  if (url.includes('/sync')) {
    return '向量同步中...';
  }
  if (url.includes('/publish')) {
    return '发布并同步中...';
  }
  if (url.includes('/agent/chat')) {
    return 'Agent 思考中...';
  }
  if (url.includes('/rag/eval')) {
    return 'RAG 评测中...';
  }
  if (url.includes('/simulation/')) {
    return '模拟执行中...';
  }
  if (url.includes('/confirm') || url.includes('/reject')) {
    return '处理 HITL 操作中...';
  }
  return '提交中...';
}

function logClick(action, detail = {}) {
  console.info('[admin-click]', {
    demo: ADMIN_DEMO,
    action,
    tab: state.activeTab,
    ...detail
  });
}

function installClickLogging() {
  document.addEventListener('click', (event) => {
    const button = event.target.closest('button');
    if (!button) {
      return;
    }
    const detail = {
      id: button.id || null,
      text: (button.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 120)
    };
    if (button.dataset.tab) detail.tabTarget = button.dataset.tab;
    if (button.dataset.confirm) detail.hitlActionId = button.dataset.confirm;
    if (button.dataset.reject) detail.hitlActionId = button.dataset.reject;
    logClick('button', detail);

    const fetchCountAtClick = activeFetchCount;
    markButtonBusy(button, true);
    beginLoading(`${detail.text || '操作'}进行中...`);
    window.setTimeout(() => {
      markButtonBusy(button, false);
      if (activeFetchCount === fetchCountAtClick) {
        endLoading();
      }
    }, 500);
  }, true);

  document.addEventListener('submit', (event) => {
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) {
      return;
    }
    logClick('form-submit', { id: form.id || null });
    beginLoading('表单提交中...');
  }, true);
}

function vectorStoreLabel(type) {
  if (type === 'milvus') return 'Milvus';
  if (type === 'qdrant') return 'Qdrant';
  return type || '向量库';
}

function vectorStoreUiUrl(type) {
  if (type === 'milvus') return 'http://127.0.0.1:8000';
  if (type === 'qdrant') return 'http://127.0.0.1:6333/dashboard';
  return null;
}

function applyVectorStoreUi(info) {
  if (!info) return;
  state.vectorStore = info;
  const label = vectorStoreLabel(info.type);
  const typeLabel = document.getElementById('vectorStoreTypeLabel');
  if (typeLabel) typeLabel.textContent = label;
  const syncBtn = document.getElementById('syncKnowledge');
  if (syncBtn) syncBtn.textContent = `同步到 ${label}`;
  const hint = document.getElementById('vectorStoreHint');
  if (hint) {
    hint.textContent = `保存后可通过「发布并同步」写入 ${label} 向量库（collection: ${info.collection}）。`;
  }
  const badge = document.getElementById('vectorStoreBadge');
  if (badge) {
    const uiUrl = vectorStoreUiUrl(info.type);
    badge.innerHTML = uiUrl
      ? `当前向量库：<strong>${label}</strong> · ${info.collection} · <a href="${uiUrl}" target="_blank" rel="noopener">打开 ${label} 管理 UI</a>`
      : `当前向量库：<strong>${label}</strong> · ${info.collection}`;
  }
}

async function loadVectorStoreInfo() {
  try {
    applyVectorStoreUi(await fetchJson('/api/knowledge/vector-store'));
  } catch (error) {
    console.warn('加载向量库配置失败', error);
  }
}

function formatSyncResult(result) {
  const label = vectorStoreLabel(result.vectorStore || state.vectorStore?.type);
  const collection = result.collection || state.vectorStore?.collection || '-';
  return `已同步到 ${label}（${collection}）：${result.points} points / ${result.articles} 篇文档`;
}

async function fetchJson(url, options = {}) {
  activeFetchCount += 1;
  const message = loadingMessageForRequest(url, options);
  if (loadingCount === 0) {
    beginLoading(message);
  } else {
    statusMessage = message;
    updatePageStatus();
  }
  try {
    const response = await fetch(url, options);
    if (!response.ok) {
      const text = await response.text();
      throw new Error(`${response.status} ${response.statusText}: ${text}`);
    }
    if (response.status === 204) {
      return null;
    }
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  } finally {
    activeFetchCount = Math.max(0, activeFetchCount - 1);
    endLoading();
  }
}

function renderJson(target, value) {
  document.getElementById(target).textContent = JSON.stringify(value, null, 2);
}

function renderList(id, items) {
  const list = document.getElementById(id);
  list.innerHTML = '';
  (items || []).forEach(item => {
    const li = document.createElement('li');
    li.textContent = item;
    list.appendChild(li);
  });
}

function updateOrderWorkspaceVisibility() {
  const hasOrder = Boolean(state.selectedOrderNo);
  document.getElementById('orderEmpty').classList.toggle('hidden', hasOrder);
  document.getElementById('orderContent').classList.toggle('hidden', !hasOrder);
  updateSimulationOrderState();
}

function updateSimulationOrderState() {
  const hasOrder = Boolean(state.selectedOrderNo);
  document.getElementById('simActionsWrap').classList.toggle('hidden', !hasOrder);
  document.getElementById('simOrderHint').classList.toggle('hidden', hasOrder);
  if (hasOrder) {
    document.getElementById('simOrderNo').textContent = state.selectedOrderNo;
    document.getElementById('simMeta').textContent =
      `当前选中 ${state.selectedOrderNo}，可执行支付回调与权益发放模拟`;
  } else {
    document.getElementById('simMeta').textContent = '创建新订单，或选择已有订单进行流程模拟';
  }
}

function switchTab(tabName) {
  state.activeTab = tabName;
  document.querySelectorAll('.tab').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.tab === tabName);
  });
  document.querySelectorAll('.tab-panel').forEach(panel => {
    panel.classList.toggle('active', panel.id === `tab-${tabName}`);
  });

  const isOrders = tabName === 'orders';
  const isSimulation = tabName === 'simulation';
  const isKnowledge = tabName === 'knowledge';
  const isPrompts = tabName === 'prompts';
  const isEditorMode = isKnowledge || isPrompts;

  const layout = document.querySelector('.layout');
  layout.classList.toggle('editor-mode', isEditorMode);

  document.getElementById('orderWorkspace').classList.toggle('hidden', !isOrders);
  document.getElementById('simulationWorkspace').classList.toggle('hidden', !isSimulation);
  document.getElementById('knowledgeWorkspace').classList.toggle('hidden', !isKnowledge);
  document.getElementById('promptWorkspace').classList.toggle('hidden', !isPrompts);

  document.getElementById('traceWorkspace').classList.toggle('hidden', isEditorMode);
  document.getElementById('knowledgeHelpPanel').classList.toggle('hidden', !isKnowledge);
  document.getElementById('promptHelpPanel').classList.toggle('hidden', !isPrompts);

  if (isOrders) {
    updateOrderWorkspaceVisibility();
  }
  if (isSimulation) {
    updateSimulationOrderState();
  }
  if (isKnowledge) {
    loadKnowledge();
  }
  if (isPrompts) {
    loadPrompts();
  }
}

function renderOrderRow(order) {
  const row = document.createElement('div');
  row.className = 'order-row' + (state.selectedOrderNo === order.orderNo ? ' selected' : '');

  const item = document.createElement('button');
  item.type = 'button';
  item.className = 'order-item side-list-item';
  item.innerHTML = `
    <strong>${order.orderNo}</strong>
    <span>${order.status}</span>
    <small>${order.userNo} · ¥${order.amount}</small>
  `;
  item.onclick = () => loadOrderDetail(order.orderNo);

  const delBtn = document.createElement('button');
  delBtn.className = 'order-delete warn';
  delBtn.type = 'button';
  delBtn.title = '删除订单及关联数据';
  delBtn.textContent = '删';
  delBtn.onclick = (event) => {
    event.stopPropagation();
    deleteOrder(order.orderNo);
  };

  row.appendChild(item);
  row.appendChild(delBtn);
  return row;
}

async function loadOrders() {
  const orders = await fetchJson('/api/orders');
  ['orders', 'ordersSim'].forEach(id => {
    const box = document.getElementById(id);
    if (!box) {
      return;
    }
    box.innerHTML = '';
    orders.forEach(order => box.appendChild(renderOrderRow(order)));
  });
}

async function deleteOrder(orderNo) {
  if (!confirm(`确定删除订单 ${orderNo}？\n将同时删除该订单的支付、退款、权益记录。`)) {
    return;
  }
  await fetchJson(`/api/orders/${orderNo}`, { method: 'DELETE' });
  if (state.selectedOrderNo === orderNo) {
    state.selectedOrderNo = null;
    state.selectedOrderDetail = null;
    updateOrderWorkspaceVisibility();
    renderQuestionSuggestions(null);
  }
  await loadOrders();
}

async function loadOrderDetail(orderNo) {
  state.selectedOrderNo = orderNo;
  const detail = await fetchJson(`/api/orders/${orderNo}`);
  state.selectedOrderDetail = detail;
  renderJson('orderDetail', detail);
  renderQuestionSuggestions(detail);
  updateOrderWorkspaceVisibility();
  await loadOrders();
}

function buildSuggestedQuestions(detail) {
  const orderNo = detail.orderNo;
  const suggestions = [];

  const add = (label, question) => suggestions.push({ label, question });

  if (detail.paymentStatus === 'SUCCESS' && detail.paymentCallbackStatus === 'FAILED') {
    add('支付异常', `订单 ${orderNo} 支付成功但状态异常，怎么处理？`);
  } else if (detail.status === 'PENDING_PAYMENT_CALLBACK') {
    add('支付回调', `订单 ${orderNo} 支付回调异常，如何排查？`);
  } else if (detail.paymentStatus === 'PENDING') {
    add('支付状态', `订单 ${orderNo} 为什么还没支付成功？`);
  }

  if (detail.refundStatus === 'REJECTED') {
    add('退款被拒', `订单 ${orderNo} 为什么不能退款？`);
  } else if (detail.refundStatus === 'PENDING' || detail.refundStatus === 'PROCESSING') {
    add('退款进度', `订单 ${orderNo} 退款进展如何？`);
  } else if (detail.status === 'COMPLETED' && (!detail.refundStatus || detail.refundStatus === 'NONE')) {
    add('能否退款', `订单 ${orderNo} 能否申请退款？`);
  }

  const benefits = detail.benefits || [];
  const benefitText = benefits.join(' ');
  if (detail.status === 'BENEFIT_FAILED' || benefitText.includes('FAILED')) {
    add('权益未到账', `订单 ${orderNo} 会员权益为什么没到账？`);
    if (detail.paymentStatus === 'SUCCESS') {
      add('权益重试', `订单 ${orderNo} 权益发放失败，能否重试？`);
    }
  } else if (benefitText.includes('PENDING')) {
    add('权益发放中', `订单 ${orderNo} 权益为什么还在发放中？`);
  }

  if (suggestions.length === 0) {
    add('订单概况', `订单 ${orderNo} 当前是什么状态？`);
    add('处理建议', `订单 ${orderNo} 请帮我分析下一步该怎么处理`);
  }

  const seen = new Set();
  return suggestions.filter(item => {
    if (seen.has(item.question)) {
      return false;
    }
    seen.add(item.question);
    return true;
  }).slice(0, 4);
}

function renderQuestionSuggestions(detail) {
  const box = document.getElementById('questionSuggestions');
  box.innerHTML = '';

  if (!detail || !detail.orderNo) {
    return;
  }

  const suggestions = buildSuggestedQuestions(detail);
  suggestions.forEach(({ label, question }) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = label;
    button.dataset.question = question;
    button.onclick = () => {
      document.getElementById('question').value = question;
    };
    box.appendChild(button);
  });

  const firstQuestion = suggestions[0]?.question;
  document.getElementById('question').placeholder =
    firstQuestion ? `输入客服问题，例如：${firstQuestion}` : `输入客服问题，例如：订单 ${detail.orderNo} ...`;
}

async function loadUsers() {
  const users = await fetchJson('/api/simulation/users');
  const select = document.getElementById('simUserNo');
  select.innerHTML = '';
  users.forEach(user => {
    const option = document.createElement('option');
    option.value = user.userNo;
    option.textContent = `${user.userNo} · ${user.nickname} (${user.levelName})`;
    select.appendChild(option);
  });
}

async function createOrder(event) {
  event.preventDefault();
  const body = {
    userNo: document.getElementById('simUserNo').value,
    amount: Number(document.getElementById('simAmount').value),
    channel: document.getElementById('simChannel').value
  };
  const detail = await fetchJson('/api/simulation/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  renderJson('simResult', detail);
  await loadOrders();
  await loadOrderDetail(detail.orderNo);
  switchTab('simulation');
}

async function simulatePaymentCallback(result) {
  if (!state.selectedOrderNo) {
    alert('请先在左侧选择一个订单');
    return;
  }
  const detail = await fetchJson(
    `/api/simulation/orders/${state.selectedOrderNo}/payment-callback?result=${result}`,
    { method: 'POST' }
  );
  renderJson('simResult', detail);
  await loadOrderDetail(detail.orderNo);
}

async function simulateBenefitIssue(result) {
  if (!state.selectedOrderNo) {
    alert('请先在左侧选择一个订单');
    return;
  }
  const detail = await fetchJson(
    `/api/simulation/orders/${state.selectedOrderNo}/benefit-issue?result=${result}`,
    { method: 'POST' }
  );
  renderJson('simResult', detail);
  await loadOrderDetail(detail.orderNo);
}

async function loadPrompts() {
  const prompts = await fetchJson('/api/prompts');
  const box = document.getElementById('promptList');
  box.innerHTML = '';
  prompts.forEach(prompt => {
    const item = document.createElement('button');
    item.type = 'button';
    item.className = 'side-list-item' + (state.editingPromptCode === prompt.promptCode ? ' selected' : '');
    const statusClass = prompt.status === 'ACTIVE' ? 'active' : 'draft';
    const statusLabel = prompt.status === 'ACTIVE' ? '生效中' : '草稿';
    item.innerHTML = `
      <strong>${prompt.promptCode}</strong>
      <span>${prompt.title}</span>
      <small class="item-breadcrumb">${promptBreadcrumb(prompt)}</small>
      <div class="side-list-item-footer">
        <span class="status-badge ${statusClass}">${statusLabel}</span>
        <span class="item-version">v${prompt.version}</span>
      </div>
    `;
    item.onclick = () => fillPromptForm(prompt);
    box.appendChild(item);
  });
  if (!state.editingPromptCode && prompts.length) {
    fillPromptForm(prompts[0]);
  }
}

function promptBreadcrumb(prompt) {
  const labels = {
    order: '订单',
    cs: '客服',
    chat: '对话',
    framework: '框架模式',
    manual: '手写模式',
    system: 'System',
    user: 'User',
    tool_retry: '工具重试'
  };
  return [prompt.bizDomain, prompt.bizModule, prompt.scene, prompt.agentMode, prompt.promptRole]
    .filter(Boolean)
    .map(value => labels[value] || value)
    .join(' · ');
}

function fillPromptForm(prompt) {
  state.editingPromptCode = prompt.promptCode;
  document.getElementById('pPromptCode').value = prompt.promptCode;
  document.getElementById('pTitle').value = prompt.title;
  document.getElementById('pDescription').value = prompt.description || '';
  document.getElementById('pStatus').value = prompt.status;
  document.getElementById('pContent').value = prompt.content;
  document.getElementById('promptEditorTitle').textContent = prompt.title || '编辑提示词';
  document.getElementById('promptMeta').textContent =
    `${promptBreadcrumb(prompt)} · v${prompt.version} · 更新于 ${prompt.updatedAt || '-'}`;
  document.querySelectorAll('#promptList .side-list-item').forEach(item => {
    item.classList.toggle('selected', item.querySelector('strong')?.textContent === prompt.promptCode);
  });
}

async function savePrompt(event) {
  event.preventDefault();
  const promptCode = document.getElementById('pPromptCode').value.trim();
  if (!promptCode) {
    alert('请先选择模板');
    return;
  }
  const body = {
    title: document.getElementById('pTitle').value.trim(),
    description: document.getElementById('pDescription').value.trim(),
    content: document.getElementById('pContent').value,
    status: document.getElementById('pStatus').value
  };
  const updated = await fetchJson(`/api/prompts/${encodeURIComponent(promptCode)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  alert(`提示词已保存：${updated.promptCode} v${updated.version}`);
  state.editingPromptCode = updated.promptCode;
  await loadPrompts();
  fillPromptForm(updated);
}

async function loadKnowledge() {
  const articles = await fetchJson('/api/knowledge/articles');
  const box = document.getElementById('knowledgeList');
  box.innerHTML = '';
  articles.forEach(article => {
    const item = document.createElement('button');
    item.type = 'button';
    item.className = 'side-list-item' + (state.editingDocCode === article.docCode ? ' selected' : '');
    const statusClass = article.status === 'PUBLISHED' ? 'active' : 'draft';
    const statusLabel = article.status === 'PUBLISHED' ? '已发布' : '草稿';
    item.innerHTML = `
      <strong>${article.docCode}</strong>
      <span>${article.title}</span>
      <div class="side-list-item-footer">
        <span class="status-badge ${statusClass}">${statusLabel}</span>
        <span class="item-version">${article.category} · v${article.version}</span>
      </div>
    `;
    item.onclick = () => fillKnowledgeForm(article);
    box.appendChild(item);
  });
}

function fillKnowledgeForm(article) {
  state.editingDocCode = article.docCode;
  document.getElementById('kDocCode').value = article.docCode;
  document.getElementById('kDocCode').disabled = true;
  document.getElementById('kTitle').value = article.title;
  document.getElementById('kCategory').value = article.category;
  document.getElementById('kStatus').value = article.status;
  document.getElementById('kContent').value = article.content;
  document.getElementById('knowledgeEditorTitle').textContent = article.title || '编辑知识文档';
  document.getElementById('knowledgeMeta').textContent =
    `${article.docCode} · ${article.category} · v${article.version}`;
  document.querySelectorAll('#knowledgeList .side-list-item').forEach(item => {
    item.classList.toggle('selected', item.querySelector('strong')?.textContent === article.docCode);
  });
}

function resetKnowledgeForm() {
  state.editingDocCode = null;
  document.getElementById('knowledgeForm').reset();
  document.getElementById('kDocCode').disabled = false;
  document.getElementById('knowledgeEditorTitle').textContent = '新建知识文档';
  document.getElementById('knowledgeMeta').textContent = '填写 docCode 与正文后保存';
  document.querySelectorAll('#knowledgeList .side-list-item').forEach(item => {
    item.classList.remove('selected');
  });
}

async function saveKnowledge(event) {
  event.preventDefault();
  const body = {
    docCode: document.getElementById('kDocCode').value.trim(),
    title: document.getElementById('kTitle').value.trim(),
    category: document.getElementById('kCategory').value,
    content: document.getElementById('kContent').value.trim(),
    status: document.getElementById('kStatus').value
  };
  if (state.editingDocCode) {
    await fetchJson(`/api/knowledge/articles/${state.editingDocCode}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
  } else {
    await fetchJson('/api/knowledge/articles', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
  }
  alert('知识文档已保存');
  state.editingDocCode = body.docCode;
  await loadKnowledge();
  const articles = await fetchJson('/api/knowledge/articles');
  const saved = articles.find(article => article.docCode === body.docCode);
  if (saved) {
    fillKnowledgeForm(saved);
  }
}

async function publishKnowledge() {
  const docCode = state.editingDocCode || document.getElementById('kDocCode').value.trim();
  if (!docCode) {
    alert('请先填写或选择 docCode');
    return;
  }
  if (!state.editingDocCode) {
    await saveKnowledge(new Event('submit'));
  }
  const article = await fetchJson(`/api/knowledge/articles/${docCode}/publish`, { method: 'POST' });
  alert(`已发布并同步：${article.docCode}`);
  await loadKnowledge();
  fillKnowledgeForm(article);
}

async function syncKnowledge() {
  const result = await fetchJson('/api/knowledge/sync', { method: 'POST' });
  alert(formatSyncResult(result));
  await loadKnowledge();
}

async function runRagEval() {
  const result = await fetchJson('/api/rag/eval', { method: 'POST' });
  alert(`RAG 评测：${result.passed}/${result.total} 通过，通过率 ${(result.passRate * 100).toFixed(1)}%`);
  renderJson('simResult', result);
}

async function loadPendingActions() {
  const pending = await fetchJson('/api/agent/actions/pending');
  const box = document.getElementById('pendingActions');
  box.innerHTML = '';
  if (!pending.length) {
    box.textContent = '暂无待确认操作';
    return;
  }
  pending.forEach(action => {
    const row = document.createElement('div');
    row.className = 'pending-action';
    row.innerHTML = `
      <div>
        <strong>${action.summary}</strong>
        <small>${action.actionId} · ${action.status}</small>
      </div>
      <div class="pending-buttons">
        <button data-confirm="${action.actionId}">确认执行</button>
        <button data-reject="${action.actionId}" class="warn">拒绝</button>
      </div>
    `;
    box.appendChild(row);
  });
  box.querySelectorAll('[data-confirm]').forEach(btn => {
    btn.onclick = () => confirmAction(btn.dataset.confirm);
  });
  box.querySelectorAll('[data-reject]').forEach(btn => {
    btn.onclick = () => rejectAction(btn.dataset.reject);
  });
}

async function confirmAction(actionId) {
  const result = await fetchJson(`/api/agent/actions/${actionId}/confirm`, { method: 'POST' });
  alert(`已执行：${result.actionType} ${result.orderNo}`);
  await loadPendingActions();
  if (state.selectedOrderNo) {
    await loadOrderDetail(state.selectedOrderNo);
  }
}

async function rejectAction(actionId) {
  const reason = prompt('拒绝原因（可选）') || '';
  await fetchJson(`/api/agent/actions/${actionId}/reject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason })
  });
  alert('已拒绝该操作');
  await loadPendingActions();
}

function startNewSession() {
  state.sessionId = null;
  localStorage.removeItem('agentSessionId');
  document.getElementById('sessionId').textContent = '-';
  document.getElementById('chatHistory').textContent = '新会话已开始';
}

async function sendQuestion() {
  const question = document.getElementById('question').value.trim();
  const mode = document.querySelector('input[name="agentMode"]:checked')?.value || 'manual';
  if (!question) {
    alert('请先输入问题');
    return;
  }
  if (!state.selectedOrderNo) {
    alert('请先在左侧选择一个订单');
    return;
  }
  const modeLabels = {
    manual: '手写模式',
    agentscope: 'AgentScope 模式',
    langchain4j: 'LangChain4j 模式'
  };
  document.getElementById('answer').textContent = `${modeLabels[mode] || '手写模式'}正在分析...`;
  const body = { question, mode };
  if (state.sessionId) {
    body.sessionId = state.sessionId;
  }
  const response = await fetchJson('/api/agent/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  state.sessionId = response.sessionId;
  localStorage.setItem('agentSessionId', response.sessionId);
  document.getElementById('answer').textContent = response.answer;
  document.getElementById('traceId').textContent = response.traceId;
  document.getElementById('sessionId').textContent = response.sessionId || '-';
  document.getElementById('turnIndex').textContent = response.turnIndex ?? '-';
  document.getElementById('duration').textContent = `${response.durationMs} ms`;
  document.getElementById('runtime').textContent = response.runtime || response.agentScopeRuntime;
  renderList('toolCalls', response.toolCalls);
  renderList('ragHits', response.ragHits);
  if (response.pendingActions && response.pendingActions.length) {
    document.getElementById('chatHistory').textContent =
      `本轮产生 ${response.pendingActions.length} 个待确认操作，请在右侧处理。`;
  }
  await loadPendingActions();
}

document.querySelectorAll('.tab').forEach(btn => {
  btn.onclick = () => switchTab(btn.dataset.tab);
});

document.getElementById('reloadOrders').onclick = loadOrders;
document.getElementById('reloadOrdersSim').onclick = loadOrders;
document.getElementById('createOrderForm').onsubmit = createOrder;
document.getElementById('payCallbackSuccess').onclick = () => simulatePaymentCallback('SUCCESS');
document.getElementById('payCallbackFail').onclick = () => simulatePaymentCallback('FAILED');
document.getElementById('benefitSuccess').onclick = () => simulateBenefitIssue('SUCCESS');
document.getElementById('benefitFail').onclick = () => simulateBenefitIssue('FAILED');
document.getElementById('knowledgeForm').onsubmit = saveKnowledge;
document.getElementById('publishKnowledge').onclick = publishKnowledge;
document.getElementById('syncKnowledge').onclick = syncKnowledge;
document.getElementById('runRagEval').onclick = runRagEval;
document.getElementById('reloadKnowledge').onclick = loadKnowledge;
document.getElementById('newKnowledge').onclick = resetKnowledgeForm;
document.getElementById('reloadPrompts').onclick = loadPrompts;
document.getElementById('promptForm').onsubmit = savePrompt;
document.getElementById('newSession').onclick = startNewSession;
document.getElementById('sendQuestion').onclick = sendQuestion;

installClickLogging();
loadOrders();
loadUsers();
loadVectorStoreInfo();
loadPendingActions();
switchTab('orders');
if (state.sessionId) {
  document.getElementById('sessionId').textContent = state.sessionId;
}
