# Fenxiao Frontend

网页端 MVP 前端，基于 Vite + React + TypeScript。

## 本地启动

```bash
npm install
npm run dev
```

默认通过 `VITE_API_BASE_URL` 指向后端：

```bash
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

如果通过 Nginx 同域反代部署，可不设置该变量。
