# Browser E2E CI 接入方案

记录日期：2026-06-12

本文档沉淀移动端 E2E 与 showcase 截图接入 GitHub Actions 的方案。当前本地已验证 YAML 语法和项目质量门禁，但推送 `.github/workflows/quality-gate.yml` 时被 GitHub 拒绝：当前凭据缺少 `workflow` scope，不能创建或更新 workflow 文件。

## 1. 接入目标

- 在 GitHub Actions 中启动 H2 后端，不依赖 MySQL / Redis。
- 启动 Vite 前端，并通过代理访问后端。
- 安装 Playwright Chromium。
- 运行 `scripts/mobile-e2e.sh`，覆盖移动端主链路、短链回流和后台指标。
- 运行 `scripts/capture-showcase-screenshots.sh`，生成 iPhone SE、安卓宽屏和桌面后台截图。
- 上传后端日志、前端日志和 showcase 截图 artifact，方便失败排查和作品集归档。

## 2. 启用前提

需要使用具备 GitHub `workflow` scope 的 token 推送 workflow 文件，或在 GitHub 网页端手动编辑 `.github/workflows/quality-gate.yml`。

## 3. 建议 Job

将下面 job 追加到 `.github/workflows/quality-gate.yml`：

```yaml
  browser-e2e:
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: frontend/package-lock.json

      - name: Install frontend dependencies
        run: npm --prefix frontend ci

      - name: Install Playwright Chromium
        run: npm --prefix frontend exec -- playwright install --with-deps chromium

      - name: Start backend in local H2 mode
        env:
          SERVER_PORT: '48081'
          APP_BASE_URL: http://127.0.0.1:5174
          ADMIN_TOKEN: dev-token
        run: |
          nohup mvn -q -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local > backend-e2e.log 2>&1 &

      - name: Wait for backend
        run: |
          for i in {1..60}; do
            if curl -fsS http://127.0.0.1:48081/api/health >/dev/null; then
              exit 0
            fi
            sleep 2
          done
          cat backend-e2e.log
          exit 1

      - name: Start frontend
        env:
          BACKEND_PROXY_TARGET: http://127.0.0.1:48081
        run: |
          nohup npm --prefix frontend run dev -- --host 127.0.0.1 --port 5174 --strictPort > frontend-e2e.log 2>&1 &

      - name: Wait for frontend
        run: |
          for i in {1..60}; do
            if curl -fsS http://127.0.0.1:5174/ >/dev/null; then
              exit 0
            fi
            sleep 2
          done
          cat frontend-e2e.log
          exit 1

      - name: Run mobile E2E
        env:
          E2E_BASE_URL: http://127.0.0.1:5174
          E2E_ADMIN_TOKEN: dev-token
        run: scripts/mobile-e2e.sh

      - name: Capture showcase screenshots
        env:
          E2E_BASE_URL: http://127.0.0.1:5174
          E2E_ADMIN_TOKEN: dev-token
          SHOWCASE_SCREENSHOT_DIR: docs/screenshots/showcase
        run: scripts/capture-showcase-screenshots.sh

      - name: Upload E2E artifacts
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: browser-e2e-artifacts
          path: |
            backend-e2e.log
            frontend-e2e.log
            docs/screenshots/showcase/
          if-no-files-found: ignore
```

## 4. 当前边界

- 该 job 覆盖 Chromium 移动视口，不替代真实 iOS、Android 和微信内置浏览器验收。
- 该 job 使用 H2 演示模式，不替代生产 MySQL / Redis / Nginx 的线上 smoke。
- 截图 artifact 需要人工挑选后再纳入作品集宣传图。
