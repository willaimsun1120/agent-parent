const ORDER_LABELS = {
  order: '订单', cs: '客服', chat: '对话', framework: '框架模式',
  manual: '手写模式', system: 'System', user: 'User', tool_retry: '工具重试'
};

const HR_LABELS = {
  hr: 'HR', cs: '客服', chat: '对话', framework: '框架模式', manual: '手写模式'
};

export function promptBreadcrumb(prompt, labels = ORDER_LABELS) {
  return [prompt.bizDomain, prompt.bizModule, prompt.scene, prompt.agentMode, prompt.promptRole]
    .filter(Boolean)
    .map(v => labels[v] || v)
    .join(' · ');
}

export function buildOrderSuggestions(detail) {
  if (!detail?.orderNo) return [];
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
    if (seen.has(item.question)) return false;
    seen.add(item.question);
    return true;
  }).slice(0, 4);
}

export function buildHrSuggestions(detail) {
  if (!detail?.empNo) return [];
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

export function vectorStoreLabel(type) {
  if (type === 'milvus') return 'Milvus';
  if (type === 'qdrant') return 'Qdrant';
  return type || '向量库';
}

export function formatSyncResult(result, vectorStore) {
  const label = vectorStoreLabel(result.vectorStore || vectorStore?.type);
  const collection = result.collection || vectorStore?.collection || '-';
  return `已同步到 ${label}（${collection}）：${result.points} points / ${result.articles} 篇文档`;
}

export function logClick(demo, action, detail = {}) {
  console.info('[admin-click]', { demo, action, ...detail });
}

export function statusBadgeClass(status) {
  if (!status) return '';
  const s = String(status).toUpperCase();
  if (s.includes('COMPLETED') || s.includes('SUCCESS')) return 'success';
  if (s.includes('FAILED') || s.includes('REJECT')) return 'warning';
  if (s.includes('PENDING')) return 'primary';
  return '';
}

export function statusTagType(status) {
  const cls = statusBadgeClass(status);
  if (cls === 'success') return 'success';
  if (cls === 'warning') return 'warning';
  if (cls === 'primary') return 'info';
  return 'default';
}
