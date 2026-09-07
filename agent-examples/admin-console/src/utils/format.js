export function parseToolCall(raw) {
  const s = String(raw || '').trim();
  const match = s.match(/^([\w.]+)\s*\((.*)\)\s*$/);
  if (match) {
    return { name: match[1], args: match[2].trim(), raw: s };
  }
  return { name: s, args: '', raw: s };
}

const ORDER_STATUS_LABELS = {
  COMPLETED: '已完成',
  PENDING_PAYMENT: '待支付',
  PENDING_PAYMENT_CALLBACK: '待支付回调',
  BENEFIT_FAILED: '权益失败',
  BENEFIT_PENDING: '权益发放中',
  CANCELLED: '已取消'
};

export function formatStatusLabel(status) {
  if (!status) return '-';
  if (ORDER_STATUS_LABELS[status]) return ORDER_STATUS_LABELS[status];
  if (status.length <= 14) return status;
  return status.replace(/_/g, ' ').slice(0, 12) + '…';
}

export function highlightEntities(text) {
  if (!text) return [];
  const parts = [];
  const regex = /(ORD-\d+|EMP-\d+|U\d+)/g;
  let last = 0;
  let m;
  while ((m = regex.exec(text)) !== null) {
    if (m.index > last) parts.push({ type: 'text', value: text.slice(last, m.index) });
    parts.push({ type: 'entity', value: m[0] });
    last = m.index + m[0].length;
  }
  if (last < text.length) parts.push({ type: 'text', value: text.slice(last) });
  return parts.length ? parts : [{ type: 'text', value: text }];
}

/** 本地时间 ISO（无时区），与后端 createdAt 格式对齐 */
export function nowLocalIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}
