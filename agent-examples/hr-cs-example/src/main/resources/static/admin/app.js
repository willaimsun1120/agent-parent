const state = {
  activeTab: 'employees',
  selectedEmpNo: null,
  selectedEmployeeDetail: null,
  editingDocCode: null,
  editingPromptCode: null,
  sessionId: localStorage.getItem('agentSessionId') || null,
  vectorStore: null
};

const ADMIN_DEMO = 'hr-cs';
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

function updateEmployeeWorkspaceVisibility() {
  const hasEmployee = Boolean(state.selectedEmpNo);
  document.getElementById('employeeEmpty').classList.toggle('hidden', hasEmployee);
  document.getElementById('employeeContent').classList.toggle('hidden', !hasEmployee);
}

function switchTab(tabName) {
  state.activeTab = tabName;
  document.querySelectorAll('.tab').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.tab === tabName);
  });
  document.querySelectorAll('.tab-panel').forEach(panel => {
    panel.classList.toggle('active', panel.id === `tab-${tabName}`);
  });

  const isEmployees = tabName === 'employees';
  const isKnowledge = tabName === 'knowledge';
  const isPrompts = tabName === 'prompts';
  const isEditorMode = isKnowledge || isPrompts;

  document.querySelector('.layout').classList.toggle('editor-mode', isEditorMode);
  document.getElementById('employeeWorkspace').classList.toggle('hidden', !isEmployees);
  document.getElementById('knowledgeWorkspace').classList.toggle('hidden', !isKnowledge);
  document.getElementById('promptWorkspace').classList.toggle('hidden', !isPrompts);
  document.getElementById('traceWorkspace').classList.toggle('hidden', isEditorMode);
  document.getElementById('knowledgeHelpPanel').classList.toggle('hidden', !isKnowledge);
  document.getElementById('promptHelpPanel').classList.toggle('hidden', !isPrompts);

  if (isEmployees) {
    updateEmployeeWorkspaceVisibility();
  }
  if (isKnowledge) {
    loadKnowledge();
  }
  if (isPrompts) {
    loadPrompts();
  }
}

function renderEmployeeRow(employee) {
  const row = document.createElement('div');
  row.className = 'employee-row' + (state.selectedEmpNo === employee.empNo ? ' selected' : '');

  const item = document.createElement('button');
  item.type = 'button';
  item.className = 'employee-item side-list-item';
  item.innerHTML = `
    <strong>${employee.empNo}</strong>
    <span>${employee.name}</span>
    <small>${employee.department} · 年假 ${employee.annualLeaveBalance} 天</small>
  `;
  item.onclick = () => loadEmployeeDetail(employee.empNo);
  row.appendChild(item);
  return row;
}

async function loadEmployees() {
  const employees = await fetchJson('/api/employees');
  const box = document.getElementById('employees');
  box.innerHTML = '';
  employees.forEach(employee => box.appendChild(renderEmployeeRow(employee)));
}

async function loadEmployeeDetail(empNo) {
  state.selectedEmpNo = empNo;
  const detail = await fetchJson(`/api/employees/${empNo}`);
  state.selectedEmployeeDetail = detail;
  renderJson('employeeDetail', detail);
  renderQuestionSuggestions(detail);
  updateEmployeeWorkspaceVisibility();
  await loadEmployees();
}

function buildSuggestedQuestions(detail) {
  const empNo = detail.empNo;
  const suggestions = [];
  const add = (label, question) => suggestions.push({ label, question });

  const leaves = (detail.leaveRequests || []).join(' ');
  if (leaves.includes('REJECTED')) {
    add('请假被拒', `员工 ${empNo} 为什么不能请假？`);
  } else if (leaves.includes('PENDING')) {
    add('请假进度', `员工 ${empNo} 请假申请进展如何？`);
  } else if ((detail.annualLeaveBalance || 0) > 0) {
    add('申请年假', `员工 ${empNo} 如何申请年假？`);
  }

  const payrolls = (detail.payrollRecords || []).join(' ');
  if (payrolls.includes('DELAYED')) {
    add('工资延迟', `员工 ${empNo} 工资为什么还没到账？`);
  }

  const benefits = (detail.benefits || []).join(' ');
  if (benefits.includes('PENDING')) {
    add('福利待确认', `员工 ${empNo} 补充医疗福利为什么待确认？`);
  } else if (benefits.includes('INACTIVE')) {
    add('福利未开通', `员工 ${empNo} 如何开通公积金？`);
  }

  if (suggestions.length === 0) {
    add('员工概况', `员工 ${empNo} 当前 HR 状态如何？`);
    add('处理建议', `员工 ${empNo} 请帮我分析下一步该怎么处理`);
  }

  return suggestions.slice(0, 4);
}

function renderQuestionSuggestions(detail) {
  const box = document.getElementById('questionSuggestions');
  box.innerHTML = '';
  if (!detail || !detail.empNo) {
    return;
  }
  buildSuggestedQuestions(detail).forEach(({ label, question }) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = label;
    button.onclick = () => { document.getElementById('question').value = question; };
    box.appendChild(button);
  });
}

async function loadPrompts() {
  const prompts = await fetchJson('/api/prompts');
  const box = document.getElementById('promptList');
  box.innerHTML = '';
  prompts.forEach(prompt => {
    const item = document.createElement('button');
    item.type = 'button';
    item.className = 'side-list-item' + (state.editingPromptCode === prompt.promptCode ? ' selected' : '');
    item.innerHTML = `
      <strong>${prompt.promptCode}</strong>
      <span>${prompt.title}</span>
      <small class="item-breadcrumb">${promptBreadcrumb(prompt)}</small>
    `;
    item.onclick = () => fillPromptForm(prompt);
    box.appendChild(item);
  });
  if (!state.editingPromptCode && prompts.length) {
    fillPromptForm(prompts[0]);
  }
}

function promptBreadcrumb(prompt) {
  const labels = { hr: 'HR', cs: '客服', chat: '对话', framework: '框架模式', manual: '手写模式' };
  return [prompt.bizDomain, prompt.bizModule, prompt.scene, prompt.agentMode, prompt.promptRole]
    .filter(Boolean).map(v => labels[v] || v).join(' · ');
}

function fillPromptForm(prompt) {
  state.editingPromptCode = prompt.promptCode;
  document.getElementById('pPromptCode').value = prompt.promptCode;
  document.getElementById('pTitle').value = prompt.title;
  document.getElementById('pDescription').value = prompt.description || '';
  document.getElementById('pStatus').value = prompt.status;
  document.getElementById('pContent').value = prompt.content;
}

async function savePrompt(event) {
  event.preventDefault();
  const promptCode = document.getElementById('pPromptCode').value.trim();
  const body = {
    title: document.getElementById('pTitle').value.trim(),
    description: document.getElementById('pDescription').value.trim(),
    content: document.getElementById('pContent').value,
    status: document.getElementById('pStatus').value
  };
  await fetchJson(`/api/prompts/${encodeURIComponent(promptCode)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  await loadPrompts();
}

async function loadKnowledge() {
  const articles = await fetchJson('/api/knowledge/articles');
  const box = document.getElementById('knowledgeList');
  box.innerHTML = '';
  articles.forEach(article => {
    const item = document.createElement('button');
    item.type = 'button';
    item.className = 'side-list-item';
    item.innerHTML = `<strong>${article.docCode}</strong><span>${article.title}</span>`;
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
}

function resetKnowledgeForm() {
  state.editingDocCode = null;
  document.getElementById('knowledgeForm').reset();
  document.getElementById('kDocCode').disabled = false;
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
      method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    });
  } else {
    await fetchJson('/api/knowledge/articles', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    });
  }
  state.editingDocCode = body.docCode;
  await loadKnowledge();
}

async function publishKnowledge() {
  const docCode = state.editingDocCode || document.getElementById('kDocCode').value.trim();
  if (!docCode) return alert('请先填写 docCode');
  await fetchJson(`/api/knowledge/articles/${docCode}/publish`, { method: 'POST' });
  await loadKnowledge();
}

async function syncKnowledge() {
  const result = await fetchJson('/api/knowledge/sync', { method: 'POST' });
  alert(formatSyncResult(result));
}

async function runRagEval() {
  const result = await fetchJson('/api/rag/eval', { method: 'POST' });
  alert(`RAG 评测：${result.passed}/${result.total} 通过`);
}

async function loadPendingActions() {
  const pending = await fetchJson('/api/agent/actions/pending');
  const box = document.getElementById('pendingActions');
  box.innerHTML = pending.length ? '' : '暂无待确认操作';
  pending.forEach(action => {
    const row = document.createElement('div');
    row.className = 'pending-action';
    row.innerHTML = `
      <div><strong>${action.summary}</strong><small>${action.actionId}</small></div>
      <div class="pending-buttons">
        <button data-confirm="${action.actionId}">确认执行</button>
        <button data-reject="${action.actionId}" class="warn">拒绝</button>
      </div>`;
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
  await fetchJson(`/api/agent/actions/${actionId}/confirm`, { method: 'POST' });
  await loadPendingActions();
  if (state.selectedEmpNo) await loadEmployeeDetail(state.selectedEmpNo);
}

async function rejectAction(actionId) {
  const reason = prompt('拒绝原因（可选）') || '';
  await fetchJson(`/api/agent/actions/${actionId}/reject`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason })
  });
  await loadPendingActions();
}

function startNewSession() {
  state.sessionId = null;
  localStorage.removeItem('agentSessionId');
  document.getElementById('sessionId').textContent = '-';
}

async function sendQuestion() {
  const question = document.getElementById('question').value.trim();
  const mode = document.querySelector('input[name="agentMode"]:checked')?.value || 'manual';
  if (!question) return alert('请先输入问题');
  if (!state.selectedEmpNo) return alert('请先在左侧选择一个员工');
  document.getElementById('answer').textContent = '正在分析...';
  const body = { question, mode };
  if (state.sessionId) body.sessionId = state.sessionId;
  const response = await fetchJson('/api/agent/chat', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  });
  state.sessionId = response.sessionId;
  localStorage.setItem('agentSessionId', response.sessionId);
  document.getElementById('answer').textContent = response.answer;
  document.getElementById('traceId').textContent = response.traceId;
  document.getElementById('sessionId').textContent = response.sessionId || '-';
  document.getElementById('turnIndex').textContent = response.turnIndex ?? '-';
  document.getElementById('duration').textContent = `${response.durationMs} ms`;
  document.getElementById('runtime').textContent = response.runtime || '-';
  renderList('toolCalls', response.toolCalls);
  renderList('ragHits', response.ragHits);
  await loadPendingActions();
}

document.querySelectorAll('.tab').forEach(btn => { btn.onclick = () => switchTab(btn.dataset.tab); });
document.getElementById('reloadEmployees').onclick = loadEmployees;
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
loadEmployees();
loadVectorStoreInfo();
loadPendingActions();
switchTab('employees');
if (state.sessionId) document.getElementById('sessionId').textContent = state.sessionId;
