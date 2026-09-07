import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers';

function sseProxy(target) {
  return {
    target,
    changeOrigin: true,
    timeout: 0,
    proxyTimeout: 0,
    configure: (proxy) => {
      proxy.on('proxyRes', (proxyRes, _req, res) => {
        const contentType = proxyRes.headers['content-type'] || '';
        if (String(contentType).includes('text/event-stream')) {
          // 避免代理缓冲导致前端只能「最后一次」收到整段 SSE
          res.setHeader('Cache-Control', 'no-cache, no-transform');
          res.setHeader('X-Accel-Buffering', 'no');
          res.setHeader('Connection', 'keep-alive');
          if (typeof res.flushHeaders === 'function') {
            res.flushHeaders();
          }
        }
      });
    }
  };
}

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue'],
      dts: false
    }),
    Components({
      resolvers: [NaiveUiResolver()],
      dts: false
    })
  ],
  server: {
    port: 8090,
    proxy: {
      '/order-api': {
        ...sseProxy('http://127.0.0.1:8080'),
        rewrite: (path) => path.replace(/^\/order-api/, '')
      },
      '/hr-api': {
        ...sseProxy('http://127.0.0.1:8081'),
        rewrite: (path) => path.replace(/^\/hr-api/, '')
      }
    }
  }
});
