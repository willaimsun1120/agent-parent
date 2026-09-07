import { loadingBar } from '../naive.js';

export async function fetchJson(base, url, options = {}) {
  const fullUrl = `${base}${url}`;
  loadingBar.start();
  try {
    const response = await fetch(fullUrl, options);
    if (!response.ok) {
      const text = await response.text();
      throw new Error(`${response.status} ${response.statusText}: ${text}`);
    }
    if (response.status === 204) return null;
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  } finally {
    loadingBar.finish();
  }
}

/**
 * 统一 SSE 流式问答。handlers: onMeta/onTool/onRag/onDelta/onDone/onError
 * 注意：流式请求不使用全局 loadingBar，避免「整段结束才动」的错觉。
 */
export async function streamAgentChat(base, body, handlers = {}) {
  const fullUrl = `${base}/api/agent/chat/stream`;
  const response = await fetch(fullUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      'Cache-Control': 'no-cache'
    },
    body: JSON.stringify(body || {})
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`${response.status} ${response.statusText}: ${text}`);
  }
  if (!response.body) {
    throw new Error('浏览器不支持流式响应');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let donePayload = null;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const parts = buffer.split('\n\n');
    buffer = parts.pop() || '';
    for (const part of parts) {
      const event = parseSseBlock(part);
      if (!event) continue;
      const data = safeJson(event.data);
      switch (event.event) {
        case 'meta':
          handlers.onMeta?.(data);
          break;
        case 'tool':
          handlers.onTool?.(data);
          break;
        case 'rag':
          handlers.onRag?.(data);
          break;
        case 'delta':
          handlers.onDelta?.(typeof data === 'string' ? data : (data?.text || ''));
          break;
        case 'done':
          donePayload = data;
          handlers.onDone?.(data);
          break;
        case 'error':
          handlers.onError?.(data);
          throw new Error(data?.message || '流式问答失败');
        default:
          break;
      }
    }
  }
  return donePayload;
}

function parseSseBlock(block) {
  if (!block || !block.trim()) return null;
  let event = 'message';
  const dataLines = [];
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim());
    }
  }
  if (!dataLines.length) return null;
  return { event, data: dataLines.join('\n') };
}

function safeJson(text) {
  if (text == null || text === '') return null;
  try {
    let parsed = JSON.parse(text);
    // 兼容被二次 JSON.stringify 的情况："{\"text\":\"hi\"}"
    if (typeof parsed === 'string') {
      try {
        parsed = JSON.parse(parsed);
      } catch {
        return parsed;
      }
    }
    return parsed;
  } catch {
    return text;
  }
}

export function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** 前端打字机：代理缓冲导致只能一次拿到全文时，仍保证可见的「流式」体验 */
export async function typewriterText(fullText, onUpdate, { chunkSize = 2, delayMs = 22 } = {}) {
  const text = fullText || '';
  let current = '';
  for (let i = 0; i < text.length; i += chunkSize) {
    current = text.slice(0, i + chunkSize);
    onUpdate(current);
    await sleep(delayMs);
  }
  onUpdate(text);
}

export function createApi(base) {
  return {
    base,
    get: (url) => fetchJson(base, url),
    post: (url, body) => fetchJson(base, url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined
    }),
    put: (url, body) => fetchJson(base, url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    }),
    delete: (url) => fetchJson(base, url, { method: 'DELETE' }),
    streamChat: (body, handlers) => streamAgentChat(base, body, handlers)
  };
}

export const orderApi = createApi('/order-api');
export const hrApi = createApi('/hr-api');
