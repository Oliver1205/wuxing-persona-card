# Codex 8-Hour Worklog

Start date: 2026-06-11

Latest update: 2026-06-14

Final delivery entry: [`docs/eight-hour-performance-showcase-delivery.md`](eight-hour-performance-showcase-delivery.md)

This file is the detailed engineering log. For quick review, open the final delivery entry first, then come back here only when you need phase-by-phase evidence.

## Goal

Use a staged 8-hour workflow to improve the Wuxing Persona Card project with
multi-role review support. The main outcomes are:

- Make the web interaction feel more natural and intuitive.
- Strengthen the backend design for burst traffic, low latency, and graceful degradation.
- Keep every change small, verifiable, and easy to explain in an interview.
- Produce final project promotion and interview learning documents.

## Guardrails

- Do not remove existing product capabilities.
- Do not add large new business features before the core flow is polished.
- Keep user-facing changes aligned with the existing H5 personality-test product.
- Treat analytics and admin features as supporting systems, not as blockers for users.
- Prefer lightweight single-server improvements before adding MQ, sharding, or heavy infrastructure.

## Role Board

| Role | Focus |
| --- | --- |
| Product manager | User intent, conversion flow, product copy, acceptance criteria |
| Ordinary visitor | First-visit clarity, mobile completion, result understanding, sharing motivation |
| Art director | Visual hierarchy, five-elements identity, screenshot quality, brand consistency |
| Frontend engineer | H5 ergonomics, responsive layout, loading/error states, interaction feedback |
| Senior architect | Hot paths, cache strategy, statistics pressure, degradation, deployment topology |
| Backend engineer | Spring Boot implementation, transactions, Redis, MySQL queries, tests |
| Big-tech interviewer | Project authenticity, tradeoffs, performance, consistency, safety, operations |

## Phase Plan

| Phase | Target |
| --- | --- |
| 0. Baseline | Confirm git state, docs, scripts, frontend/backend entry points |
| 1. Experience audit | Walk homepage, test page, result page, sharing, and mobile layout |
| 2. Backend audit | Trace result creation, result read, short-link redirect, events, admin overview |
| 3. First scoped changes | Implement the safest high-value UX/performance/documentation improvements |
| 4. Verification | Run focused build/test/check commands and capture residual risks |
| 5. Documentation | Add promotion kit and interview learning manual with diagrams |

## Current Baseline

- Branch: `main`
- Last known commit before this workflow: `cdf56fb docs: add static project documentation site`
- Git status at start: clean, `main...origin/main`
- Existing docs-site: `docs-site/index.html`, `docs-site/styles.css`, `docs-site/README.md`

## Running Notes

### Phase 0

- Read `README.md`, `docs/project-plan.md`, and `docs/quality-scorecard.md`.
- Confirmed the project is already at v2.6 with card-based question flow, growth attribution, daily metrics, Docker Compose deployment, and a static documentation site.
- Multi-role review signals are being consolidated into the phase checklist before implementation.

### Phase 1

- Product/user review priority: make the shared result page clearly feel like a friend's card, then guide the visitor into their own test.
- Frontend change: result page now shows a shared-entry banner when opened from a short-link redirect query, and the "I also want to test" action is promoted ahead of retesting.

### Phase 2

- Backend architecture priority: remove expensive real-time PV/UV/UIP aggregation from the short-link redirect hot path.
- Backend change: internal short-link redirect now records the visit event and only touches `last_visit_at`; admin pages still compute PV/UV/UIP from the event table or existing aggregation paths.
- Verification so far:
  - `mvn -q -f backend/pom.xml -Dtest=InternalShortLinkProviderTest test`
  - `mvn -q -f backend/pom.xml -Dtest=MvpFlowIntegrationTest#shouldReturnShortLinkListAndVisitDetailStats test`

### Phase 3

- Frontend priority: make the mobile test-page sticky action area guide the user toward the next step without crowding secondary actions.
- Frontend change: mobile sticky actions now put the primary next-step button on its own full-width row, with previous/home actions below it.
- Backend priority: keep admin overview useful during traffic spikes without making each refresh repeat all live aggregate queries.
- Backend change: admin overview now uses a short Redis cache keyed by the selected date range; Redis failures degrade to live calculation.

### Phase 4

- Verification priority: make the burst-traffic story repeatable without introducing heavy benchmark infrastructure.
- Tooling change: added `scripts/performance-smoke-test.sh` to create a real result, read the result, repeatedly hit the same short link, and repeat admin overview reads.
- Interview value: the script output gives concrete `shortlinkAvgMs` and `adminAvgMs` values for explaining why short-link redirects and admin aggregate refreshes were optimized separately.
- Local smoke result: with `BASE_URL=http://127.0.0.1:18080`, `SHORTLINK_HITS=30`, and `ADMIN_HITS=2`, the script passed with `shortlinkAvgMs=17` and `adminAvgMs=37` on the local H2 profile.

### Phase 5

- Frontend priority: reduce the repeated tap friction in the mobile question flow while keeping the final submit action explicit.
- Frontend change: choosing an answer now briefly confirms the selection and auto-advances to the next question, except on the last question where the user still taps "生成我的人格卡".
- Test update: mobile e2e was adjusted to follow the faster answer flow instead of requiring a manual "下一题" tap after every answer.

### Phase 6

- Documentation priority: turn the requested multi-role agent strategy into a repeatable review matrix.
- Documentation change: added `docs/role-review-matrix.md`, covering product manager, ordinary visitor, art director, frontend developer, senior architect, backend developer, and big-tech interviewer perspectives.
- Workflow value: later UX/backend/documentation changes can be checked against role-specific acceptance criteria instead of relying on a single engineering viewpoint.

### Phase 7

- Visual/product priority: make the generated result share image feel closer to a real spreadable asset.
- Frontend change: upgraded `frontend/src/utils/shareCard.ts` with element-aware colors, stronger result identity hierarchy, clearer short-link copy, and a friend-return prompt.
- Promotion value: the share image now better matches the project poster and can be used as a screenshot target in the promotion kit.

### Phase 8

- Backend priority: keep admin analytics usable after bursts without adding heavyweight infrastructure.
- Data change: added lightweight MySQL/H2 indexes for `visit_event.created_at`, date-range UV/UIP counts, event-type/date/short-code aggregation, and `short_link.status + created_at` list queries.
- Interview value: this gives a concrete answer for the big-tech interviewer role: cache helps, but the fallback SQL path also has supporting indexes.

### Phase 9

- Backend priority: reduce non-critical write amplification on the short-link redirect hot path.
- Backend change: `SHORT_LINK_VISIT` events are still recorded for every redirect, but `short_link.last_visit_at` is now touched only when the previous value is stale by at least 30 seconds.
- Tradeoff: operations still get a recent last-visit signal, while a single viral short code no longer rewrites the same `short_link` row on every hit.
- Local smoke result: after throttling the last-visit touch, the local H2 profile passed with `SHORTLINK_HITS=30`, `ADMIN_HITS=2`, `shortlinkAvgMs=16`, and `adminAvgMs=36`.

### Phase 10

- Backend priority: keep the admin short-link list responsive as rows grow.
- Backend change: the list now batches result lookups and local PV/UV/UIP aggregation for all short codes on the page instead of running per-row result and distinct-count queries.
- Verification value: added a two-short-link integration test to prove batched stats still map back to the correct list item.

### Phase 11

- Visitor priority: make the question flow feel calmer and more predictable on mobile.
- Frontend change: the displayed default birth year is now also the selected birth year, answer auto-advance pauses long enough to show a confirmation state, and the mobile question stage has enough bottom safe area for the sticky action bar.
- Verification value: browser-checked `/test` with local backend and frontend preview; selecting a month now unlocks the first question, selecting an answer shows the confirmation state, and the final option remains reachable above the sticky controls after scrolling.

### Phase 12

- Documentation priority: make the workflow useful after this session ends.
- Documentation change: added a project promotion poster, architecture map, role review matrix, showcase screenshot flow, and a five-minute interview walkthrough.
- Interview value: the project can now be explained from product loop, hot path, analytics, deployment, and tradeoff angles instead of only listing features.

### Phase 13

- Frontend priority: make non-happy paths feel intentional.
- Frontend change: the result page now uses polished loading and error cards, and the test page locks navigation and submission while a result is being created.
- User value: slow network or impatient repeated taps no longer produce confusing duplicate actions.

### Phase 14

- Backend priority: keep non-critical analytics writes off the critical path.
- Backend change: visit-event insert failures are logged and degraded without breaking result reads or short-link redirects.
- Verification value: `VisitEventServiceTest` covers the failure path, and the full quality gate passed after the change.

### Phase 15

- Admin priority: reduce accidental repeated operations during refresh or export.
- Frontend change: admin filters, refresh, export, aggregation, and runtime-check actions now share a busy guard while data is loading.
- Product value: the admin page behaves more like an operations tool and less like a static demo page.

### Phase 16

- Quality-score priority: keep the final self-assessment honest.
- Documentation change: updated README and `docs/quality-scorecard.md` with an eight-hour workflow stage score, explicit evidence, and remaining risks.
- Boundary: this is not marked as a new large business version; it is a hardening and evidence stage on top of v2.6.

### Phase 17

- Visitor priority: make sharing feedback clear even when browser clipboard permissions fail.
- Frontend change: the share link box now shows copy/share busy states, keeps the short-link button from being double-triggered, and selects the URL when automatic copy is unavailable.
- Browser value: local result-page verification confirmed the fallback message and selected short URL are visible in the share panel.

### Phase 18

- Backend priority: keep noisy analytics payloads from creating avoidable database write failures.
- Backend change: `VisitEventService` trims `pagePath`, `resultId`, and `shortCode` to their database column bounds before insertion.
- Verification value: added a unit test for dimension trimming and passed the full quality gate.

### Phase 19

- Admin priority: make short-link visit detail feel like an operations page, not a raw table.
- Frontend change: short-link detail filters now disable while loading, show a loading hint, and display a clear empty state when a date range has no visits.
- Browser value: local H2 + Vite verification covered both an existing `SHORT_LINK_VISIT` row and a future-date empty result.

### Phase 20

- Admin priority: make cold-start and no-result states readable in the main dashboard.
- Frontend change: recent results, recent short links, and the short-link list now show explicit empty states instead of blank tables.
- Browser value: local admin verification with a future date range confirmed all three empty states render correctly.

### Phase 21

- Visitor priority: remove the expectation gap between the homepage promise and the first test step.
- Frontend change: the homepage now promises "birth year/month + 5 questions", and the test page defaults to a simpler birth-year/month step while folding optional day/time controls into a secondary section.
- Browser value: local mobile verification confirmed the optional day/time section is closed by default, can be expanded, and still exposes the existing date/time controls.

### Phase 22

- Backend priority: keep short-link redirects from waiting on analytics inserts during burst traffic.
- Backend change: short-link visit events now use a bounded in-memory queue and daemon writer, so `/s/{shortCode}` prepares the event and enqueues it instead of inserting from the request thread.
- Verification value: targeted unit tests cover async insertion and short-link provider delegation, while the existing synchronous record path keeps its failure-degrade behavior.
- Local smoke result: with local H2 profile, `SHORTLINK_HITS=100`, `ADMIN_HITS=2`, `shortlinkAvgMs=15`, and `adminAvgMs=34`.

### Phase 23

- Backend priority: keep historical admin short-link list queries from scanning the full visit-event detail table.
- Backend change: when the selected date range is fully before today, short-link list PV/UV/UIP now comes from `short_link_daily_metric`; today and open-ended ranges still use live visit events.
- Verification value: the analytics aggregation integration test now verifies that a closed-date short-link list returns the same local PV/UV/UIP from daily metrics.

### Phase 24

- Backend priority: avoid repeated external stats HTTP calls when admins refresh the short-link list.
- Backend change: external PV/UV/UIP snapshots now use a configurable local TTL cache keyed by short URL, group, enable status, and date range.
- Verification value: adapter tests cover cache hits and `SHORT_LINK_EXTERNAL_STATS_CACHE_TTL_SECONDS=0` bypass behavior.

### Phase 25

- Visitor priority: keep auto-advance useful without making users feel rushed.
- Frontend change: answer confirmation now stays visible for 1100 ms, and the sticky action text tells users they can tap next immediately or wait for automatic progress.
- Product value: the question flow still feels fast, but users have a clearer moment to confirm or correct a choice.

### Phase 26

- Interview priority: turn big-tech-style scrutiny into a study artifact the owner can rehearse.
- Documentation change: added a pressure-question guide covering short-link hot paths, async events, admin statistics, external stats caching, Redis degradation, indexing, migration, external dependency failure, frontend product choices, and honest QPS boundaries.
- Learning value: every answer now separates repository evidence from current limitations, so the project can be presented confidently without overstating production readiness.

### Phase 27

- Visitor priority: make the result page answer "why does this feel like me?" before the longer explanation.
- Frontend change: added a three-card resonance section derived from existing result keywords, primary/secondary elements, and star officer data.
- Product value: users now see concrete behavior cues before the detailed layout and relationship text, which should make the result easier to recognize and share.

### Phase 28

- Sharing priority: let users act on the result while the main persona card is still in view.
- Frontend change: added a compact quick-action strip below the result card with save-image and retake/share-entry actions, while keeping the full short-link panel later in the page.
- Browser value: local H2 + Vite verification confirmed the quick-action strip renders on a real result page and the new resonance cards still show three behavior cues.

### Phase 29

- Showcase priority: make the static docs site first screen feel like a product entry before an engineering defense.
- Docs-site change: the hero now leads with H5 experience, result screenshot, and engineering evidence actions, plus a compact result-card preview that shows identity, keywords, ratio, and share actions.
- Product value: ordinary visitors can understand what the H5 produces before reading the deeper architecture and interview material.

### Phase 30

- Visitor priority: reduce first-step action noise before users enter the question flow.
- Frontend change: the birth-info step no longer shows a disabled previous button, and the home link is demoted into the summary area while the main "进入第 1 题" action stays prominent.
- Browser value: local H2 + Vite verification confirmed the first step has no previous button, while question steps still show "上一张" when returning is useful.

### Phase 31

- Validation priority: turn "natural interaction" into observable user and device checks.
- Documentation change: added a real-user validation checklist covering first-visit understanding, birth-info friction, answer pacing, result resonance, sharing, friend return flow, device matrix, screenshots, and interview wording.
- Product value: the next UX iteration can be judged with user evidence instead of only role-agent heuristics.

### Phase 32

- Promotion priority: keep the public project package aligned with the latest result-page and validation work.
- Documentation change: updated the promotion kit to call out result resonance cards, near-card sharing actions, and real-user validation assets.
- Showcase value: the marketing-facing story now mentions not only the business loop, but also why the result feels shareable and how that claim will be checked.

### Phase 33

- Backend priority: keep warm short-link redirects from touching MySQL on the read path.
- Backend change: internal short-link resolution now returns the cached resultId directly when Redis has the short-code mapping, and the low-value last-visit touch degrades to a warning if MySQL is busy.
- Verification value: targeted unit tests prove Redis-hit redirects do not call `selectByShortCode` and still return the resultId when `touchLastVisitAtIfStale` throws.

### Phase 34

- Backend priority: keep non-critical frontend tracking from waiting on MySQL inserts.
- Backend change: `/api/events` now uses the existing bounded async visit-event queue with session/channel/campaign attribution preserved, while result creation events remain synchronous business evidence.
- Verification value: `VisitEventServiceTest` covers async attribution fields, and `MvpFlowIntegrationTest` now waits for async growth events before asserting admin funnel counts.

### Phase 35

- Backend priority: make the async visit-event worker reduce database round trips instead of only moving them off the request thread.
- Backend change: async visit events now flush through a MyBatis batch insert, with a per-row degraded fallback if the batch write fails.
- Verification value: service tests cover batch insertion and fallback, and the MVP integration flow still passes with async frontend event tracking.

### Phase 36

- Admin priority: let operators scan conversion and sharing signals before seeing engineering diagnostics.
- Frontend change: the external short-link runtime block now sits in a collapsed debug panel after the core KPI cards, the funnel table keeps human-readable step labels first, and raw event codes move to a secondary code column.
- Detail change: short-link visit rows now lead with source, visit action, channel, campaign, device, and referer; anonymous hashes and raw event code remain available inside an inline technical detail disclosure.
- Verification value: local H2 + Vite browser verification confirmed the debug panel is collapsed by default, short-link visits show readable attribution, and expanding "查看" still reveals event code plus anonymous visitor/IP/device fingerprints.

### Phase 37

- Performance priority: make low-latency smoke evidence enforceable when a target environment has agreed thresholds.
- Tooling change: `scripts/performance-smoke-test.sh` now emits avg and TP95 timings, and accepts optional `MAX_SHORTLINK_AVG_MS`, `MAX_SHORTLINK_P95_MS`, `MAX_ADMIN_AVG_MS`, and `MAX_ADMIN_P95_MS` guards; the default `0` keeps observation-only behavior.
- Learning value: interview docs now describe the current short-link path as Redis-first, async queued, and batch-written, and the code evidence paths match the actual `com/wuxing/persona` package layout.

### Phase 38

- Observability priority: make async event drops visible when using the performance smoke script as low-latency evidence.
- Backend change: added an admin-protected visit-event runtime endpoint that reports async queue size, capacity, drain limit, cumulative dropped events, and worker liveness.
- Tooling value: `scripts/performance-smoke-test.sh` now prints `asyncQueueSize`, `asyncDroppedEvents`, and `asyncWorkerAlive`, so a fast short-link result can be checked against event queue health.

### Phase 39

- Visual evidence priority: make the showcase screenshot workflow useful for product, ordinary-user, and art-manager review rather than a single mobile viewport.
- Tooling change: the Playwright showcase spec now captures the full product flow for iPhone SE and wide Android mobile viewports, plus the desktop admin overview.
- Documentation value: the promotion kit, role matrix, and completion audit now distinguish "multi-viewport screenshot script exists" from "real generated screenshot artifacts still need to be archived."

### Phase 40

- Reproducibility priority: make mobile E2E and showcase screenshot commands available from a clean frontend install.
- Tooling change: added `@playwright/test` to frontend dev dependencies and exposed `npm run e2e:mobile` / `npm run e2e:showcase`, with root scripts delegating to those commands.
- Documentation value: README and historical scorecards now distinguish "Playwright dependency is present" from the remaining CI/browser-install and screenshot-archive work.

### Phase 41

- CI priority: turn mobile interaction checks and showcase screenshots into a repeatable GitHub Actions signal.
- Workflow plan: drafted a `browser-e2e` job that starts the backend in local H2 mode, starts Vite against that backend, installs Chromium, runs mobile E2E, captures showcase screenshots, and uploads logs plus screenshots as artifacts.
- Permission note: pushing `.github/workflows/quality-gate.yml` was blocked because the current GitHub credential lacks `workflow` scope, so the ready-to-apply job is documented in `docs/ci-browser-e2e-plan.md`.
- Documentation value: README, role matrix, quality scorecard, audit, and learning manual now distinguish prepared CI browser coverage from the remaining workflow-scope, real-device, WeChat, online pressure-test, and alerting evidence gaps.

### Phase 42

- Visual evidence priority: replace "screenshot script exists" with actual archived multi-viewport assets.
- Tooling fix: `scripts/capture-showcase-screenshots.sh` now resolves relative `SHOWCASE_SCREENSHOT_DIR` values from the repository root before changing into `frontend`.
- Verification value: local H2 backend plus Vite frontend passed `scripts/mobile-e2e.sh` and `scripts/capture-showcase-screenshots.sh`; 11 showcase screenshots were generated under `docs/screenshots/showcase/`.

### Phase 43

- Promotion priority: turn the screenshot archive into a direct portfolio artifact.
- Documentation change: added `docs-site/showcase.html`, a zero-dependency portfolio page combining real screenshots, product story, backend hot-path explanation, and the five-minute interview thread.
- Learning value: the interview manual now includes task cards with exact file paths, verification commands, and the one-sentence explanation each module should support in an interview.

### Phase 44

- Backend evidence priority: make "high peak, low latency" claims auditable instead of vague.
- Documentation change: added `docs/production-load-alert-runbook.md` with layered pressure-test order, performance smoke thresholds, async queue/runtime evidence, alert drills, and a fixed report template.
- Interview value: README, quality scorecard, and learning manual now point to the runbook and explicitly separate smoke evidence from unverified production QPS claims.

### Phase 45

- Orchestration priority: turn the requested multi-role eight-hour workflow into live, auditable agent assignments.
- Documentation change: updated `docs/agent-workflow-orchestration.md` with the current role allocation, each role's input scope, expected output, and eight-hour timeboxes.
- Workflow value: role agents stay read-only while the main thread owns integration, verification, commits, and honest release notes, reducing the risk of parallel edits colliding.

### Phase 46

- Visual priority: make the result card's primary/secondary ratio bar reflect the actual five-elements identity instead of a fixed two-color bar.
- Backend priority: keep high-frequency result page reads from waiting on non-critical analytics writes.
- Code change: `ElementRatioCard` now uses the real primary/secondary element colors passed by `PersonaCard`, and `ResultService#getByResultId` records `RESULT_VIEW` through the async visit-event queue.

### Phase 47

- Visitor priority: make the first test step feel less blocked by controls on small mobile screens.
- Frontend change: the birth-info step now keeps the action panel in normal document flow on mobile, clarifies privacy and default-year copy, adds horizontal-scroll hints, and disables the primary action until both year and month are selected.
- Browser value: verified `/test` locally at a 375px mobile viewport; the birth action panel is static after the birth card, `flowPaddingBottom=0px`, and the primary action changes from "选择月份后继续" to "进入第 1 题" after month selection.

### Phase 48

- Result-page priority: reduce duplicated share and retest actions so the user has one clear next step.
- Frontend change: own result pages now promote "保存分享图" near the persona card while `ShareLinkBox` owns copy/native share; shared-result pages use the banner and tail CTA for "我也测一张" instead of repeating save/share buttons.
- Verification value: updated the mobile E2E shared-result click target to the new `result-banner` campaign and passed `scripts/mobile-e2e.sh`.

### Phase 49

- Documentation-site priority: make "立即体验 H5" behave like an experience entry instead of sending visitors back into README.
- Docs-site change: added a dedicated `#experience` section with local H5 URL, showcase page, and startup command links; expanded the materials screenshot wall into a five-step story from homepage to birth info, question card, result page, and admin metrics.
- Verification value: static reference check confirmed all `href` and `src` targets in `docs-site/index.html` resolve.

### Phase 50

- Backend pressure-test priority: make the async visit-event queue and short-link hot-row touch interval tunable without code changes.
- Code change: `VISIT_EVENT_ASYNC_QUEUE_CAPACITY`, `VISIT_EVENT_ASYNC_DRAIN_LIMIT`, and `SHORT_LINK_LAST_VISIT_TOUCH_INTERVAL_SECONDS` now bind through application config with the same conservative defaults.
- Documentation value: the production load runbook names the tuning knobs, and the interview manual now describes queue capacity and batch size as explicit pressure-test tradeoffs.

### Phase 51

- Admin observability priority: make short-link list statistics explain whether numbers came from real-time events, daily aggregates, or an external short-link provider.
- Backend resilience change: internal short-code creation now relies on the `uk_short_code` unique key and retries on duplicate-key collisions, removing the old `count + insert` race.
- Contract change: `ShortLinkListItemVO` now returns `metricSource`, CSV export includes the same field, and the admin table shows a human-readable "口径" column.
- Interview value: the backend story can separate low-latency hot-path choices from admin query cost, and short-code conflict handling now has code and test evidence instead of a future-only explanation.

### Phase 52

- UX priority: give mobile users control after selecting an answer instead of auto-advancing the card.
- Frontend change: answer selection now only marks the chosen option; the sticky primary action is disabled until an answer exists, then advances explicitly with "下一题" or submits on the last question.
- Verification value: mobile E2E and showcase screenshot flows now click "下一题" between answers, so the automated path matches the more deliberate interaction.

### Phase 53

- Architecture priority: make external short-link consistency boundaries explicit instead of hiding cross-system failures behind optimistic fallback language.
- Backend change: external short-code duplicate-key collisions now reuse the existing fallback path, while a successful external create followed by local binding write failure returns an explicit 500 requiring manual or compensating cleanup.
- Interview value: the project can now explain which external failures are safe to degrade and which ones need a production compensation mechanism such as delete/disable API, outbox, or reconciliation.

### Phase 54

- Reliability priority: prove the async visit-event queue does not hide low-latency success behind invisible event drops.
- Test change: `VisitEventServiceTest` now forces a queue capacity of one and blocks the worker so the third async event must increment `droppedAsyncEvents`.
- Tooling change: `scripts/performance-smoke-test.sh` now requires the async worker to be alive, prints queue threshold settings, and supports optional `MAX_ASYNC_QUEUE_SIZE` / `MAX_ASYNC_DROPPED_EVENTS` guards.

### Phase 55

- Product priority: make the docs-site "try the H5" path direct enough for portfolio visitors.
- Docs-site change: the experience section now links to the local `/test` route, names the H2 local profile path, and shows the backend/frontend preview commands in the same panel.
- Verification value: the documentation site no longer sends a visitor through README first just to find the working local demo URL.

### Phase 56

- Interview priority: turn the admin-token boundary from a one-endpoint assertion into a broader sensitive-endpoint guarantee.
- Test change: `MvpFlowIntegrationTest` now parameterizes unauthorized checks across overview, short-link list, CSV export, visit details, external status, visit-event runtime, and manual analytics aggregation.
- Documentation value: the interview materials now explicitly say `X-Admin-Token` is MVP admin protection, not a full RBAC system.

### Phase 57

- Interview priority: make the database migration boundary explicit instead of letting `schema.sql` look like production migration governance.
- Documentation change: added `docs/db-migration-plan.md` with current init-script limits, a Flyway-style version split, rollout checks, rollback rules, and interview wording.
- Risk value: the project can now admit that Flyway/Liquibase is not implemented while still showing a concrete productionization path.

### Phase 58

- UX priority: remove the small dead end after a visitor clicks "先看样例" on the home page.
- Frontend change: the sample persona card now includes a full-width "生成我的人格卡" action that routes to `/test` and reuses the start-click tracking.
- Validation value: the real-user checklist now observes whether users can continue from the sample card without scrolling back to the hero CTA.

### Phase 59

- Visual evidence priority: keep long showcase screenshots inspectable without making the portfolio page unwieldy.
- Docs-site change: result and admin screenshots now sit inside fixed-height scroll frames, so the full Playwright long screenshots remain available instead of being cropped by `object-fit: cover`.
- Portfolio value: the result-page sharing actions and admin evidence are no longer hidden below a hard crop.

### Phase 60

- Observability priority: prove the async visit-event worker is draining, not merely alive.
- Backend change: visit-event runtime now reports `totalFlushedEvents`, `lastFlushAt`, `lastBatchSize`, and `batchWriteFailures` alongside queue size, drops, and worker liveness.
- Tooling change: performance smoke prints the new worker-drain fields and supports `MAX_ASYNC_BATCH_FAILURES` as an optional hard threshold.

### Phase 61

- UX copy priority: keep technical "short link" language out of the ordinary visitor flow.
- Frontend change: homepage, result loading state, share link box, and generated share-card copy now say "分享链接" instead of "短链/短链接".
- Validation value: the real-user checklist now asks users to find the share image or share-link entry, while backend/admin docs keep the engineering short-link terminology.

### Phase 62

- UX action priority: make the result-page share buttons describe the action without relying on surrounding helper text.
- Frontend change: the share box now says "复制分享链接" and "系统分享" instead of the shorter "复制" and "分享".
- Validation value: ordinary visitors can distinguish copying a private-chat link from invoking the browser or WeChat system share sheet.

### Phase 63

- Documentation priority: keep public-facing project language aligned with the frontend copy cleanup.
- Docs change: README, the promotion kit, docs-site copy, completion audit, and interview manual now describe user actions as "分享链接" while preserving "短链接系统" for backend architecture.
- Portfolio value: ordinary readers see product language first, and interviewers can still trace the short-link engineering implementation.

### Phase 64

- Launch priority: start the five-hour real-domain workflow with a strict pre-domain self-audit instead of jumping straight to DNS changes.
- Tooling change: added `scripts/domain-bind-preflight.sh` to verify DNS resolution, expected server IP, public health endpoints, and optional admin overview access.
- Documentation change: added `docs/domain-launch-self-audit.md` and `docs/five-hour-domain-workflow.md`, then linked the domain workflow from README, deploy docs, and the interview learning manual.
- Boundary value: first real-domain launch should use the main domain with internal short links; HTTPS, `APP_BASE_URL`, strong secrets, DNS access, and server SSH remain explicit preconditions.

### Phase 65

- Launch priority: make the HTTPS/domain step executable on a real server instead of leaving it as an abstract checklist.
- Deploy change: added `deploy/host-nginx-domain-tls.example.conf`, a host-level Nginx TLS template that forwards public `80/443` traffic to the Compose nginx on `127.0.0.1:8088`.
- Documentation change: added `docs/domain-server-runbook.md` with DNS, security group, `.env`, Compose, Certbot, smoke, performance smoke, browser verification, and rollback steps.
- Learning value: the interview manual now explains the split between DNS, host Nginx/TLS, container Nginx, `APP_BASE_URL`, production smoke, and performance smoke.

### Phase 66

- Workflow boundary: after completing real-domain prework, stop and wait for the user's external domain/DNS/SSH/certificate information instead of guessing or continuing business changes.
- Documentation change: added `docs/domain-launch-info-template.md`, a fill-in checklist for main domain, server IP, SSH, DNS provider, ICP/access status, HTTPS plan, shortlink subdomain decision, and sensitive secret handling.
- Handoff value: README, the five-hour workflow, the self-audit, the server runbook, and the interview learning manual now all point to the same pre-domain information template before server execution.

### Phase 67

- Architecture priority: use the domain-audit waiting period to tighten the public input contract instead of adding unrelated features.
- Backend change: `ElementCalculateService` now rejects future birth months, future birth dates, and impossible calendar dates such as non-leap-year February 29.
- Frontend change: `TestPage.vue` now derives available days from the selected year/month and disables future months when the selected year is the current year.
- Test value: focused service and integration tests cover impossible birth dates, while the frontend production build verifies the tightened Vue state flow.

### Phase 68

- Product priority: move from one-person result sharing into a short-code-triggered two-person matching loop.
- Self-audit change: added `docs/dual-match-self-audit.md` to record why the first version uses pure clipboard short codes, backend-computed matches, refreshable match routes, and no new match table.
- Backend change: added `/api/matches/candidates/{shortCode}`, `POST /api/matches`, and `/api/matches/{partnerShortCode}/{currentShortCode}` around `MatchService`.
- Frontend change: homepage detects valid clipboard short codes, asks whether to start matching, test submission routes to `/match/{partnerShortCode}/{currentShortCode}`, and result sharing now prioritizes copying the match short code.
- Learning value: the interview manual now includes the dual-match sequence diagram, code-reading task card, and answer for why matching is computed from two short codes instead of immediately persisted.

### Phase 69

- Completion-audit priority: make the short-code matching entry more faithful to the original "enter homepage, detect clipboard" requirement.
- Frontend change: homepage now attempts clipboard reading directly in real browsers, while keeping the no-interruption fallback path for denied clipboard access.
- Analytics change: manual short-code entry now emits `MATCH_SHORT_CODE_ENTERED`, separate from `MATCH_CLIPBOARD_DETECTED`, so automatic detection and fallback usage can be distinguished later.
- Learning value: the interview manual now explicitly explains why clipboard auto-detection needs a user-triggered fallback in production browsers.

### Phase 70

- Workflow priority: resume the eight-hour loop with explicit self-check, implementation, verification, and record-keeping instead of treating the dashboard as finished after one pass.
- Self-check finding: the load-test data created abnormal operating ratios such as completion rate above `100%` and short-link return strength above `1000%`; raw data was useful, but the dashboard could mislead a human reviewer if those values looked like ordinary healthy conversion.
- Frontend change: admin conversion displays now use a shared rate formatter, compact very large ratios as `>1000%`, and mark funnel-table ratios with normal/watch/danger visual chips while preserving the underlying event counts.
- Validation: `npm run build` passed in `frontend`, `git diff --check` passed, and browser verification on `/admin` showed focus cards and funnel rows using the new compact abnormal-rate display.

### Phase 71

- Performance priority: turn the pressure-test tool from a single fixed mixed workload into a reusable scenario tester.
- Tooling change: `scripts/performance-limit-test.sh` now accepts `WORKLOAD=mixed|shortlink|result|admin|health`; single-path runs make it easier to isolate whether the bottleneck is short-link redirect, result reads, admin overview, or global service health.
- Documentation change: the load-test record now lists scenario commands and recommends the sequence `health -> shortlink/result/admin -> mixed` for future production-style tests.
- Validation: `bash -n scripts/performance-limit-test.sh` passed, a lightweight `WORKLOAD=health` run generated `docs/performance-reports/workflow-health-verify/report.md`, and the report includes `Workload: health`.

### Phase 72

- Delivery priority: make the showcase PPT asset easy to find and regenerate after the long workflow ends.
- Documentation change: added `docs/artifacts/presentations/README.md` with direct links to the PPT, contact sheet, slide sources, preview images, layout JSON, build manifest, and the exact rebuild command.
- Handoff value: the presentation is now an asset package rather than a lone binary file, so future edits can start from the slide modules instead of recreating the deck from scratch.

### Phase 73

- Data-integrity priority: keep repeated pressure tests from distorting the default admin data center.
- Backend change: admin overview, short-link list, and CSV export now default to excluding `channel=perf-test`; `includeSynthetic=true` restores the full view for performance-review scenarios.
- Frontend change: the admin filter bar now exposes “包含测试流量” with an inline note that the default view has already excluded pressure-test and health-check traffic.
- Tooling change: `scripts/performance-limit-test.sh` now adds `X-Perf-Run-Id`, uses run-specific client IDs, writes `RUN_ID` into CSV/report/summary, and refuses non-loopback targets unless `ALLOW_PUBLIC_LOAD_TEST=1` is explicitly set.
- Documentation change: `docs/admin-data-center-guide.md` now includes a daily reading SOP, synthetic-traffic scope, metric-source caveat, and common abnormal-ratio diagnosis; `docs/performance-load-test-record-20260614.md` now explains the safety gate and run tracing.
- Self-check result: backend architecture review confirmed this is a practical default-view isolation, not entity-level strong isolation; the next stronger design would add `is_synthetic` or source fields to `user_result` and `short_link`.
- Validation: `mvn -q test`, `npm run build`, `bash -n scripts/performance-limit-test.sh`, `git diff --check`, and the non-loopback safety-gate check all passed.

### Phase 74

- Product priority: make the synthetic-traffic isolation visible, not just implicit in backend query parameters.
- Frontend change: admin overview now fetches the opposite metric-scope overview in the background and shows a “口径差异” diagnostic band with PV、结果、分享链接、回流访问的测试增量。
- Operator value: when pressure tests create abnormal ratios, the dashboard can show how much came from `perf-test` instead of forcing the user to mentally compare two API responses.
- Documentation change: `docs/admin-data-center-guide.md` now explains the “口径差异” panel and how to interpret synthetic increments.
- Validation: `npm run build` and `git diff --check` passed; direct API verification on a temporary `18081` backend confirmed default overview returned `syntheticTrafficExcluded=true` with 1 organic result while `includeSynthetic=true` returned 2 total results including `perf-test`.
- Residual note: full browser data verification on the existing `5175` app was limited because the default `8080` backend port was occupied by an old/unreachable process; temporary services were stopped after API verification.

### Phase 75

- Performance-report priority: make every pressure-test report explain its code and runtime context before showing numbers.
- Tooling change: `scripts/performance-limit-test.sh` now writes an environment card into `summary.json` and `report.md`, including `Run ID`, `Git SHA`, dirty/clean state, Java version, Python version, host name, base URL, and public-target authorization.
- Documentation change: `docs/performance-load-test-record-20260614.md` now explains how to read the environment card and why local H2 reports must not be compared directly with production MySQL/Nginx reports.
- Safety value: the public-target gate remains active; a deliberate `BASE_URL=http://example.com` check exits before sending traffic unless `ALLOW_PUBLIC_LOAD_TEST=1` is set.
- Validation: `bash -n scripts/performance-limit-test.sh` passed, `git diff --check` passed, and the non-loopback safety-gate check returned the expected refusal message.

### Phase 76

- Architecture priority: decide whether synthetic traffic isolation should move from event-view filtering into entity-level fields immediately.
- Self-check finding: `ResultService.create` can read request attribution, but `user_result` and `short_link` currently have no source fields; implementing strong isolation now would require schema changes, mapper changes, short-link creation signature changes, aggregation changes, and historical backfill.
- Decision: keep the current event-level default-view isolation for this workflow stage, and document entity-level `source_channel/source_campaign/synthetic` as the next stabilization step instead of adding a risky schema migration amid many active changes.
- Documentation change: added `docs/synthetic-traffic-isolation-design.md`, linked it from `docs/performance-optimization-plan.md`, and added an interview-ready explanation to `docs/interview-learning-manual.md`.
- Validation: `git diff --check` passed for the documentation-only decision record.

### Phase 77

- Performance-evidence priority: make the growing set of load-test artifacts searchable and reviewable instead of leaving them as loose run directories.
- Documentation change: added `docs/performance-reports/README.md` with a report table, legacy metadata caveat, report-reading flowchart, key conclusions, new-report requirements, and the recommended next-run sequence.
- Navigation change: linked the report index from README quick navigation and `docs/performance-load-test-record-20260614.md`.
- Handoff value: future production-style pressure tests now have a stable landing page for report discovery, while older reports remain traceable to their raw `report.md`, CSV, and `summary.json`.

### Phase 78

- Presentation-handoff priority: make the 12-page showcase PPT easier to actually speak through in a portfolio review or interview.
- Documentation change: added `docs/artifacts/presentations/wuxing-showcase-speaker-notes.md` with a 5-minute story spine, evidence-chain diagram, slide-by-slide talk track, source links, overclaim warnings, and likely interviewer questions.
- Navigation change: linked the speaker notes from the PPT asset README, README quick navigation, and the promotion kit asset list.
- Interview value: the PPT now has a companion script that ties each claim to repository evidence and keeps local-pressure-test, RocketMQ, domain-launch, and synthetic-traffic boundaries honest.

### Phase 79

- Data-center self-check priority: make the desktop admin dashboard easier to interpret before adding more charts.
- Documentation change: added `docs/admin-metric-dictionary.md` with a dashboard reading order, core metric dictionary, funnel interpretation, source/口径 rules, runtime-field definitions, anomaly diagnosis matrix, and interview wording.
- Navigation change: linked the metric dictionary from `docs/admin-data-center-guide.md` and README quick navigation.
- Product value: future dashboard enhancements now have a stable metric contract, so new cards or charts can be judged by whether they improve diagnosis instead of merely increasing visual density.

### Phase 80

- Pressure-test evidence priority: verify that the updated load-test script writes the new environment card and synthetic-trace metadata into a real report.
- Runtime action: started a temporary local Spring Boot backend on `127.0.0.1:18082` after sandbox port binding required controlled escalation.
- Verification run: executed `WORKLOAD=health`, `STEPS=1,2`, `REQUESTS_PER_STAGE=12`, `RUN_ID=workflow-health-env-card` against the temporary backend.
- Report result: `docs/performance-reports/workflow-health-env-card/report.md` completed all stages with P95 `1ms`, error rate `0.00%`, runtime health `ok`, and environment fields for Git SHA, dirty state, Java/Python, host, base URL, synthetic channel, campaign, and public-target authorization.
- Boundary value: this is a script/report-format validation run, not a business-chain or production-capacity conclusion.

### Phase 81

- Performance-plan priority: turn the optimization plan from broad recommendations into a report-driven action map.
- Documentation change: added a “从报告到行动” section to `docs/performance-optimization-plan.md`, mapping `health`/`shortlink`/`result`/`admin` P95 changes, error-rate increases, queue growth, event drops, batch-write failures, and synthetic data pollution to concrete next actions and verification paths.
- Navigation change: linked the optimization plan from README, the load-test record, and the performance report index.
- Engineering value: future pressure-test results now have a decision table before any tuning, which reduces the chance of prematurely adding infrastructure when the bottleneck is actually date range, cache miss, or test-data口径.

### Phase 82

- Delivery-index priority: give the user one place to inspect the current eight-hour workflow outcomes.
- Documentation change: added `docs/eight-hour-performance-showcase-delivery.md`, tying together the PPT asset package, slide speaker notes, data-center guide, metric dictionary, pressure-test report index, new environment-card verification report, load-test record, and optimization plan.
- Navigation change: linked the delivery index from README and the older eight-hour completion audit.
- Handoff value: the workflow now has an executive-style index that says where each artifact is, how to read it, what it proves, and what must not be overstated.

### Phase 83

- Quality-gate priority: re-run the hard project checks after the latest PPT, data-center, pressure-test, and delivery-index documentation changes.
- Verification: `mvn -q test` passed in `backend`; the warning/error log lines came from intentional failure-path and degradation tests.
- Verification: `npm run build` passed in `frontend`, producing the current Vite production bundle.
- Verification: `git diff --check` passed after the latest documentation and report-index changes.
- Handoff value: this phase confirms the current workflow artifacts are backed by backend tests, frontend type/build checks, and whitespace validation.

### Phase 84

- Multi-agent self-check priority: use the SRE review feedback to close a real mismatch between pressure-test wording and script behavior.
- Finding: the report wording said `WORKLOAD=shortlink` verifies `301/302` and `Location`, but the script only checked redirect status codes.
- Tooling change: `scripts/performance-limit-test.sh` now captures the `Location` header, marks shortlink requests failed when a redirect has no `Location`, and writes `location` into each stage CSV.
- Verification: ran `bash -n scripts/performance-limit-test.sh`, then executed `WORKLOAD=shortlink`, `STEPS=1`, `REQUESTS_PER_STAGE=6`, `RUN_ID=workflow-shortlink-location-verify` against a temporary `127.0.0.1:18083` backend.
- Report result: `docs/performance-reports/workflow-shortlink-location-verify/stage-c1.csv` contains a `location` column and all 6 shortlink responses returned `302` with `/result/...` Location; temporary service was stopped after verification.

### Phase 85

- Multi-agent self-check priority: close the data-center wording gap found by the product/data review.
- Frontend change: `/admin` now separates real share actions (`SHORT_LINK_COPY`、`SAVE_SHARE_IMAGE_SUCCESS`、`NATIVE_SHARE_SUCCESS`) from system short-link creation, and reports return strength as visits per short link instead of a misleading percent.
- Documentation change: `docs/admin-data-center-guide.md` and `docs/admin-metric-dictionary.md` now state that `shortLinkCreated` is a system binding metric, not user sharing; related learning/interview documents were aligned to the same wording.
- Presentation change: fixed stale evidence links in the PPT speaker notes and strengthened the slide 11 boundary that local H2 pressure-test numbers are not production QPS promises.
- Verification: `npm run build` passed in `frontend`, stale wording/link scan returned no targeted matches, and `git diff --check` passed after this correction batch.
- Browser verification: `http://127.0.0.1:5175/admin` shows “分享入口”“回流强度”“当前页统计来源结构”, includes “系统短链” and “次/链接”, and no longer shows the old “分享率/回流率” labels.

### Phase 86

- Pressure-test storytelling priority: turn raw report evidence into a visual brief that can be used in PPT, interview review, and performance retrospectives.
- Documentation change: added `docs/performance-visual-brief.md` with a capacity story line, P95 red-line chart, 768-concurrency bottleneck reasoning diagram, report evidence chain, interview wording, and the next public-chain pressure-test matrix.
- Navigation change: linked the visual brief from README, the performance report index, the eight-hour performance showcase delivery index, and the PPT asset package.
- Presentation handoff: updated the showcase speaker notes so slide 8 distinguishes true share actions from system short-link binding, and slide 11 points to the new visual brief.
- Boundary value: the brief explicitly says the current numbers are local `Spring Boot + H2 + local async queue` evidence, not production QPS.
- Verification: `git diff --check` passed; targeted scan confirmed the new brief is linked and still carries the production-QPS boundary, `perf-test` isolation, and `Location` verification notes.

### Phase 87

- Multi-agent review priority: resolve the high-priority issues found by product/data, SRE, and interview-review agents.
- Backend change: `AdminStatService` funnel steps now include `SAVE_SHARE_IMAGE_SUCCESS` and `NATIVE_SHARE_SUCCESS`, so `/admin` can actually support the frontend's “真实分享动作” metric instead of only counting copied short links.
- Frontend wording change: `SHARE_PANEL_VIEW` is now presented as “分享区曝光” rather than “打开分享面板”, matching the current component-mounted tracking behavior.
- Performance tooling change: `scripts/performance-limit-test.sh` now has runtime stop gates for `healthStatus=danger`, queue usage threshold, async event drops, and batch write failures, in addition to P95 and error-rate stops.
- Documentation change: pressure-test docs now describe `512/768` as configured concurrency stages, clarify that legacy mixed reports should not inherit the newer Location check, and add public-test safeguards, cooldowns, runtime gates, and server-side collection requirements.
- Interview/PPT change: fixed stale `ExternalShortLinkStatsAdapter` evidence paths, refreshed the pressure-test Q&A, and tightened RocketMQ wording to “optional async channel / future complete consumer path” rather than completed production takeover.
- Verification: full `mvn -q test` passed in `backend`; `npm run build` passed in `frontend`; `bash -n scripts/performance-limit-test.sh` passed; `git diff --check` passed.

### Phase 88

- Production-load readiness priority: turn SRE feedback into an executable observability checklist before any public pressure test.
- Documentation change: added `docs/production-load-observability-checklist.md` with pre-test authorization checks, per-stage collection items, command templates, runtime stop conditions, public-test cadence, report supplement fields, and interview wording.
- Navigation change: linked the checklist from README, the performance visual brief, the performance report index, the optimization plan, and the eight-hour performance showcase delivery index.
- Boundary value: the checklist explicitly requires备案、授权、回滚和最大并发 before public load testing, keeping it as future execution rather than completed production evidence.

### Phase 89

- Self-check priority: remove a flaky full-suite failure in the backend growth-funnel integration test before continuing feature work.
- Finding: `MvpFlowIntegrationTest` passed when run alone, but full `mvn -q test` could fail while waiting for the third `/api/events` async write; the failure came from test orchestration depending too heavily on background queue timing.
- Test hardening: the main admin-flow test now keeps one `/api/events` call to cover the event API path, then seeds the remaining funnel events deterministically so the assertion focus stays on admin aggregation, share-action funnel coverage, attribution, and runtime reporting.
- Schema alignment: the Spring Boot integration test now points to the existing `classpath:db/schema-local.sql` instead of the stale `schema-h2.sql` path.
- Coverage value: the funnel assertions now include `SHARE_PANEL_VIEW` as “分享区曝光” plus `SHORT_LINK_COPY`, `SAVE_SHARE_IMAGE_SUCCESS`, and `NATIVE_SHARE_SUCCESS`, matching the data-center definition of true share actions.
- Verification: full `mvn -q test` passed in `backend`; `npm run build` passed in `frontend`; `bash -n scripts/performance-limit-test.sh` passed; `git diff --check` passed.

### Phase 90

- Data-center priority: improve the desktop admin dashboard's ability to tell the operator what to handle first, not just show more charts.
- Frontend change: added a “风险与行动建议” section to `/admin` that combines visit-event runtime health, completion-rate anomalies, share-action gaps, short-link return strength, synthetic-traffic impact, and external short-link reachability into `P0/P1/P2/OK` action rows.
- Product boundary: the section is a rule-based summary of existing metrics, not a new data source or AI diagnosis; every suggestion maps back to visible dashboard fields.
- Documentation change: updated `docs/admin-data-center-guide.md`, `docs/admin-metric-dictionary.md`, and the eight-hour delivery index to explain the new action layer and its verification boundary.
- Verification: full `mvn -q test` passed in `backend`; `npm run build` passed in `frontend`; `bash -n scripts/performance-limit-test.sh` passed; `git diff --check` passed. Local Playwright verification was not available in the current Node REPL, so browser screenshot validation remains a follow-up.

### Phase 91

- Multi-agent self-check priority: absorb the highest-risk review findings from product/data, SRE, and interview-review roles.
- Backend/API change: `AdminOverviewVO` now exposes `syntheticIsolationLevel` and `syntheticIsolationNote`, and `AdminStatService` marks the current default as `event_channel` rather than implying entity-level strong isolation.
- Frontend change: `/admin` now says “按事件 channel 排除” for the default data口径, and the synthetic impact band explicitly notes that this is not entity-level strong isolation.
- Test change: `MvpFlowIntegrationTest.shouldExcludeSyntheticTrafficFromDefaultAdminViews` asserts both the isolation level and the explanatory note for default and all-traffic views.
- Performance tooling change: `scripts/performance-limit-test.sh` now records `DEPLOYMENT_PROFILE`, rejects public targets when `REQUESTS_PER_STAGE < max(STEPS) * 2`, checks RocketMQ shadow/fallback state before public runs, and evaluates runtime stop gates before P95/error-rate stop reasons.
- Documentation change: added the pressure-test evidence version stamp, RocketMQ three-stage takeover diagram, and a big-tech interview Q&A clarifying that dashboard samples are not automatically real-user growth conclusions.
- Verification: targeted synthetic-isolation integration test passed; full `mvn -q test` passed in `backend`; `npm run build` passed in `frontend`; `bash -n scripts/performance-limit-test.sh` passed; `git diff --check` passed.

### Phase 92

- Presentation priority: bring the PPT itself in line with the latest interview and SRE boundary fixes instead of leaving them only in speaker notes.
- PPT change: rebuilt `docs/artifacts/presentations/wuxing-persona-project-showcase.pptx` after updating slide 11 with a pressure-test evidence version stamp and slide 12 with a RocketMQ local/shadow/takeover three-stage mini-flow.
- Visual QA: regenerated the 12-slide preview and contact sheet under `outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase/preview`; the contact sheet shows the new slide 11/12 content without obvious overlap.
- Verification: full `mvn -q test` passed in `backend`; `npm run build` passed in `frontend`; `bash -n scripts/performance-limit-test.sh` passed; `git diff --check` passed.

### Phase 93

- Tooling self-check priority: verify that the new public-load safety gate fails before sending any real traffic.
- Verification run: `BASE_URL=http://example.com ALLOW_PUBLIC_LOAD_TEST=1 STEPS=4 REQUESTS_PER_STAGE=4 scripts/performance-limit-test.sh` refused execution because the sample count was lower than `max(STEPS) * 2`.
- Tooling fix: moved output directory creation behind the Python preflight gates, so rejected public runs no longer create empty report directories.
- Verification: repeated the refusal with a temporary `OUT_DIR` and confirmed no directory was created; `bash -n scripts/performance-limit-test.sh` and `git diff --check` passed.

### Phase 94

- Backend self-check priority: harden the admin statistics query path before the data center accumulates larger traffic history.
- Finding: the default dashboard口径 now correctly labels synthetic isolation as event-channel-level, but result and short-link exclusion relies on `visit_event`反查; without targeted indexes this can become the first slow-query path under larger datasets.
- Database change: added `idx_visit_event_result_event_channel(result_id, event_type, channel)` and `idx_visit_event_event_short_created_channel(event_type, short_code, created_at, channel)` to production, local, and H2 schemas.
- Documentation change: updated the performance optimization plan, database schema notes, and big-tech interview Q&A with the actual index reasoning.
- Test hardening: `MvpFlowIntegrationTest.shouldCompleteResultShortLinkAndAdminFlow` now keeps `/api/events` as an API smoke call but seeds the asserted funnel event synchronously, so the admin aggregation assertions no longer depend on async worker timing.
- Verification: targeted `mvn -q -Dtest=MvpFlowIntegrationTest test` passed, then full `mvn -q test`, `npm run build`, `bash -n scripts/performance-limit-test.sh`, and `git diff --check` all passed after the backend/schema patch set.

### Phase 95

- Data-center self-check priority: verify that the richer admin dashboard remains a desktop-oriented operations surface.
- Browser check: reloaded `http://127.0.0.1:5175/admin` and confirmed the page renders the admin hero, filter bar, focus metrics, and “风险与行动建议” content.
- Layout finding: the Codex browser viewport was narrow, but the admin CSS intentionally keeps a `min-width` desktop canvas and horizontal scrolling, instead of collapsing the data center into a phone-style vertical dashboard.
- Documentation change: updated the eight-hour delivery index and admin data-center guide with the desktop-layout verification and narrow-viewport behavior.

### Phase 96

- Artifact consistency priority: keep the PPT talk track and learning material aligned with the backend statistics hardening.
- Presentation note change: slide 9 speaker notes now mention the two targeted `visit_event` indexes used for synthetic-traffic exclusion and short-link batch statistics.
- Learning material change: `docs/interview-learning-manual.md` now explains the newer index set instead of the older generic event/date/shortCode wording.

### Phase 97

- Pressure-tool self-check priority: make rejected or failed preflight runs leave less report-directory noise.
- Finding: the script created `OUT_DIR` after public-target sample gates, but before health/runtime/RocketMQ preflight and seed-result checks.
- Tooling change: `scripts/performance-limit-test.sh` now creates the output directory only after health, questions, runtime preflight, RocketMQ public safety gate, and seed result all pass.
- Verification: `bash -n scripts/performance-limit-test.sh` passed; a local unreachable-target run failed during preflight and `test ! -e /private/tmp/wuxing-preflight-dir-test` confirmed no report directory was created; `git diff --check` passed.

### Phase 98

- SRE agent feedback absorbed: public pressure tests should fail closed if visit-event runtime cannot be observed, and public reports should not keep the default `local-h2` deployment profile.
- Tooling change: public targets now reject `DEPLOYMENT_PROFILE=local-h2/local/dev`, require observable runtime keys, reject dirty dropped/batch-failure baselines, and stop a stage if runtime before/after becomes unavailable.
- Report change: generated Markdown reports now expose preflight async mode, runtime health, RocketMQ availability, consumer readiness, local fallback, queue usage percent, and batch-failure delta.
- Documentation change: updated the report index and production observability checklist with the stricter public-runtime and deployment-profile gates.
- Verification: `bash -n scripts/performance-limit-test.sh` passed; `BASE_URL=http://example.com ALLOW_PUBLIC_LOAD_TEST=1 DEPLOYMENT_PROFILE=local-h2 STEPS=4 REQUESTS_PER_STAGE=8 ...` refused before network traffic and no output directory was created; `git diff --check` passed.

### Phase 99

- Product/data and interview-review feedback absorbed: dashboard and docs now avoid implying that every short-link visit is a confirmed friend visit, and use the more precise “按 `perf-test` 事件过滤后的运营口径” wording.
- Frontend change: `/admin` keeps the same desktop data-center layout, but the synthetic-toggle helper and short-link metric note now describe the actual event-filter and回流口径 more accurately.
- Learning/interview change: the RocketMQ diagram now shows current local queue persistence, optional shadow publishing, and future consumer takeover as separate stages; the quick-answer table no longer says MQ is only a future idea.
- Presentation note change: slide 11 now explicitly separates legacy mixed 512/768 local H2 capacity samples from newer script format/safety-gate verification.
- Verification: `npm run build` passed in `frontend`; `bash -n scripts/performance-limit-test.sh` passed; `git diff --check` passed.

### Phase 100

- Artifact self-check priority: verify that the eight-hour deliverables are not only referenced by docs, but actually present and visually usable.
- Delivery-index check: confirmed the PPT, speaker notes, pressure-test visual brief, optimization plan, health env-card report, short-link Location report, summary files, and CSV evidence exist at the paths referenced by `docs/eight-hour-performance-showcase-delivery.md`.
- Visual QA: opened `outputs/019eb62f-8502-73e2-866e-af2130879105/presentations/wuxing-showcase/preview/contact-sheet.png`; the 12-slide deck preview is readable, with slide 11/12 showing the pressure-test evidence boundary and RocketMQ staged roadmap.

### Phase 101

- Runtime self-check priority: keep the local preview usable while continuing the eight-hour workflow.
- Finding: plain sandbox startup could not bind the Spring Boot port and reported `Operation not permitted`; after port-bind approval, the backend started with local H2 profile on `48081`.
- Frontend check: an existing Vite process is serving `http://127.0.0.1:5175/`; a temporary extra server on `5176` was cleaned up to avoid mixed previews.
- Browser verification: `http://127.0.0.1:5175/admin` rendered with title “五行人格卡” and DOM markers for 数据中台、运营观察台、包含测试流量、风险与行动建议、短链访问. Screenshot capture timed out, so the recorded evidence is DOM-level rather than visual screenshot.

### Phase 102

- Pressure-tool self-check priority: verify the limit-test script still executes end-to-end against the restored local backend, not just passes shell syntax.
- Local smoke run: executed `WORKLOAD=health`, `STEPS=1`, `REQUESTS_PER_STAGE=4`, `BASE_URL=http://127.0.0.1:48081`, outputting to `/private/tmp/wuxing-health-smoke`.
- Report verification: the generated report includes environment card fields, `DEPLOYMENT_PROFILE=local-h2`, `SYNTHETIC_CHANNEL=perf-test`, preflight runtime health, RocketMQ availability/consumer state, and `stopReason=completed all stages`.

### Phase 103

- Data-center contract self-check: reviewed `AdminOverviewVO`, `VisitEventRuntimeVO`, `frontend/src/api/types.ts`, and `frontend/src/api/admin.ts` for field drift after the dashboard expansion.
- Runtime verification: local `/api/admin/overview` returns synthetic-isolation fields, metric source, daily trends, funnel steps, and empty recent lists in the expected shape; `/api/admin/visit-events/runtime` returns queue, dropped/batch failure, RocketMQ, and health fields with `healthStatus=ok`.

### Phase 104

- Statistics-query self-check: re-read `AdminStatService`, `VisitEventMapper`, `ShortLinkMapper`, `UserResultMapper`, and the schema indexes to confirm the newer dashboard queries stay batch-oriented.
- Finding: short-link list rendering first pages links, then batch-loads result summaries and local stats; it does not issue one stats query per short code.
- Index check: synthetic exclusion for `user_result` / `short_link` uses `NOT EXISTS` on `visit_event(result_id, event_type, channel)`, and short-link batch stats use `event_type + short_code + created_at + channel`, matching the schema indexes added in Phase 94.
- Remaining boundary: `channel != perf-test` filtering still belongs to the event-view layer and remains less strong than entity-level `synthetic` fields, which is already called out in the synthetic isolation design.

### Phase 105

- Result-copy runtime self-check: generated a 2005-12 火/土倾向 sample and confirmed the response follows the requested structure:判定依据、元素逐项解释、互动总览.
- Finding: the first restored backend used the local default `APP_BASE_URL=http://localhost:5173`, so generated `shortUrl` did not match the active `5175` preview.
- Runtime fix: restarted the backend with `APP_BASE_URL=http://127.0.0.1:5175`, then restarted the `5175` Vite server with `BACKEND_PROXY_TARGET=http://127.0.0.1:48081` because the old `5175` process was proxying to the wrong backend target.
- Verification: regenerated a sample result and got `shortUrl=http://127.0.0.1:5175/s/V06l3T`; `GET http://127.0.0.1:5175/s/V06l3T` returned `302` to `/result/R20260614044549542942631?sc=V06l3T`; browser DOM verification of `http://127.0.0.1:5175/admin` still passed.

### Phase 106

- Learning-doc self-check: searched the learning/interview/persona docs for stale “simple year number” wording and confirmed the standard/sample docs already use干支、纳音、节令/月令口径.
- Documentation change: added a “结果判定与文案链路” section to `docs/interview-learning-manual.md`, connecting `WuxingCalendarTerms`, `ElementCalculateService`, and `ResultTextService` so the user can explain why a result becomes主火次土 instead of only saying “后端算分”.
- Verification: `mvn -q -Dtest=ResultTextServiceTest,ResultServiceTest test` passed; the logged result-id collision warning is the expected retry branch covered by tests.

### Phase 107

- Change-scope self-check: reviewed `git status -sb`, tracked `git diff --name-only`, and untracked files after the eight-hour workflow changes.
- Finding: the working tree is intentionally broad across backend statistics/runtime, frontend data center/mobile experience, pressure-test tooling, PPT assets, reports, and learning docs; no unrelated deletion was observed in the current status snapshot.
- Commit-risk note: when publishing later, this should be split by topic if possible: backend/runtime, frontend/data-center, pressure-test artifacts, and learning/PPT docs are each reviewable units.

### Phase 108

- Stage gate: ran a full local quality close-out after the runtime proxy fix and learning-doc update.
- Verification: full `mvn -q test` passed in `backend`; `npm run build` passed in `frontend`; `bash -n scripts/performance-limit-test.sh` passed; final `git diff --check` passed.
- Note: test warnings are expected branch coverage for result-id collision retry, async queue overflow, RocketMQ fallback/disabled behavior, external short-link fallback, and cache/DB failure handling.

### Phase 109

- Local-preview reliability priority: turn the `APP_BASE_URL` / `BACKEND_PROXY_TARGET` mismatch found in Phase 105 into a reusable gate.
- Tooling change: added `scripts/local-preview-smoke-test.sh`, which creates a local test result, verifies `shortUrl == FRONTEND_URL/s/{shortCode}`, checks `GET /s/{code}` returns `301/302` to `/result/{resultId}?sc={code}`, confirms `/admin` returns the SPA shell, and checks visit-event runtime health.
- Safety change: the script defaults to `X-Channel=perf-test` and `X-Campaign=local-preview-smoke`, and refuses public URLs unless `ALLOW_PUBLIC_PREVIEW_SMOKE=1` is explicitly set.
- Documentation change: added `docs/local-preview-runbook.md`, linked it from README quick navigation and the eight-hour delivery index, and documented the recommended `5175` / `48081` startup pair plus LAN-phone preview variant.
- Verification: shell syntax checks passed; public URL refusal was verified; live local run passed with `shortUrl=http://127.0.0.1:5175/s/xvAg5K`, redirecting to `/result/R20260614045901813697779?sc=xvAg5K`, runtime health `ok`, and `syntheticChannel=perf-test`.

### Phase 110

- Artifact integrity priority: add a machine-checkable gate for the eight-hour deliverable pack so PPT/report/doc paths do not silently rot.
- Tooling change: added `scripts/verify-eight-hour-artifacts.sh`, which verifies the delivery index, data-center docs, local-preview runbook, pressure-test docs, PPT file, speaker notes, contact sheet, and six report directories with `report.md` + `summary.json`.
- Compatibility fix: the verifier treats older `legacy mixed` summaries as legacy format while requiring `runId`, `workload`, and `syntheticChannel` for newer `workflow-*` reports.
- Documentation change: linked the verifier from `docs/eight-hour-performance-showcase-delivery.md` and added its shell syntax check to `scripts/quality-check.sh`.
- Verification: `scripts/verify-eight-hour-artifacts.sh` passed, finding the current PPT contact sheet and checking 6 report directories.

### Phase 111

- Delivery-usability priority: make the eight-hour outcome easier to inspect without relying on chat memory.
- Multi-agent review absorbed: existing sidecar reviewers found no P0 issue; their P1/P2 feedback focused on honest wording for synthetic traffic, short-link visits, RocketMQ shadow boundaries, public-load-test protection, and PPT/interview evidence stamps.
- Documentation change: expanded `docs/eight-hour-performance-showcase-delivery.md` with a “按目的找成果” table and “本地复测入口” commands, covering PPT, data center, pressure-test records, interview materials, local preview smoke, artifact verification, and the full quality gate.
- Boundary statement: the delivery index now says public pressure tests must wait for备案、授权、production observability, and stop conditions; local format checks must not be presented as production capacity.

### Phase 112

- Admin UX clarity priority: reduce confusion when the desktop data center is opened on narrow screens or during pressure-test review.
- Frontend copy change: the `/admin` hero now describes the page as a “电脑端运营看板” and says narrow screens keep the horizontal desktop canvas; the synthetic traffic toggle helper now says “未勾选则排除 perf-test”.
- Verification: `npm run build` passed in `frontend`; `git diff --check` passed; browser DOM verification on `http://127.0.0.1:5175/admin` found “数据中台”, “电脑端运营看板”, “未勾选则排除 perf-test”, and “运营观察台”.

### Phase 113

- Synthetic-isolation regression priority: protect the default short-link list from accidentally reintroducing `perf-test` rows after closed-date aggregation.
- Test change: extended `MvpFlowIntegrationTest.shouldExcludeSyntheticTrafficFromDefaultAdminViews` so after moving organic and synthetic results to yesterday and running `/api/admin/analytics/aggregate`, the default `/api/admin/short-links` still shows only organic rows, while `includeSynthetic=true` shows both organic and `perf-test` rows.
- Verification: targeted `mvn -q -f backend/pom.xml -Dtest=MvpFlowIntegrationTest#shouldExcludeSyntheticTrafficFromDefaultAdminViews test` passed.

### Phase 114

- Quality reset: reran the full project gate after the delivery-index, admin-copy, and synthetic-isolation test changes.
- Verification: `scripts/quality-check.sh` passed, including `git diff --check`, shell syntax checks, generated-artifact guard, forbidden-claim scan, full `mvn -q -f backend/pom.xml test`, frontend `npm run build`, and Docker Compose config checks.
- Note: the Maven warnings in the output are expected branch coverage for collision retry, async queue overflow, RocketMQ fallback/disabled behavior, external short-link fallback, and cache/DB failure handling.

### Phase 115

- Runtime preview self-check: reran the local preview smoke after the admin-copy and test changes to make sure the active `5175` frontend and `48081` backend still agree on short-link URLs.
- Verification: `scripts/local-preview-smoke-test.sh` passed with `shortUrl=http://127.0.0.1:5175/s/reIfO0`, redirecting to `/result/R20260614051402405564947?sc=reIfO0`; visit-event runtime health was `ok` and traffic was tagged `syntheticChannel=perf-test`, `syntheticCampaign=local-preview-smoke`.

### Phase 116

- Artifact-regression priority: make the delivery index's new “按目的找成果” and “本地复测入口” sections machine-checkable.
- Tooling change: extended `scripts/verify-eight-hour-artifacts.sh` to require those delivery-index sections in addition to PPT, local-preview, performance-report, RocketMQ, and `perf-test` references.
- Verification: `bash -n scripts/verify-eight-hour-artifacts.sh`, `scripts/verify-eight-hour-artifacts.sh`, and `git diff --check` passed.

### Phase 117

- Visual-evidence attempt: tried to capture a fresh `/admin` desktop screenshot from the in-app browser after logging in with the local `dev-token`.
- Result: DOM verification remained usable, but full-page, viewport, and clipped screenshot capture all timed out at the browser capture layer.
- Integrity note: no partial `docs/screenshots/eight-hour-admin-data-center-desktop.png` file was created; current visual evidence remains the previously verified PPT contact sheet plus DOM-level admin markers.

### Phase 118

- Pressure-test traceability priority: make performance-test batches visible in both generated reports and the data center's Campaign dimension without adding schema.
- Tooling change: `scripts/performance-limit-test.sh` now derives an effective campaign as `SYNTHETIC_CAMPAIGN:RUN_ID` for request headers, keeps the base campaign in the report, and writes `effectiveSyntheticCampaign` into `summary.json`.
- Documentation change: updated the data-center guide, load-test record, report index, visual brief, and synthetic-traffic isolation design to explain that `campaign` carries the run id for pressure-test review.
- Verification: `bash -n scripts/performance-limit-test.sh` and `git diff --check` passed; a temporary `WORKLOAD=health` run with `RUN_ID=campaign-trace-check` wrote `Effective Synthetic Campaign: performance-limit-test:campaign-trace-check` to `/private/tmp/wuxing-campaign-trace-check/report.md` and `effectiveSyntheticCampaign` to its `summary.json`.

### Phase 119

- Publish-readiness priority: keep the broad eight-hour working tree reviewable when it later becomes commits or a PR.
- Documentation change: added a “建议提交拆分” section to `docs/eight-hour-performance-showcase-delivery.md`, grouping the work into admin analytics/synthetic isolation, admin/mobile frontend polish, performance/preview verification gates, and showcase/learning docs.
- Review boundary: the split explicitly calls out SQL/index/cache review, desktop/narrow-screen UX review, public-load-test safety review, and PPT/interview evidence-boundary review.

### Phase 120

- Delivery-index guard: extended `scripts/verify-eight-hour-artifacts.sh` so the “建议提交拆分” section is also protected by the artifact verifier.
- Verification: `scripts/verify-eight-hour-artifacts.sh`, `bash -n scripts/verify-eight-hour-artifacts.sh`, and `git diff --check` passed.

### Phase 121

- Learning/interview sync priority: keep the speaking materials aligned with the new pressure-test batch tracing behavior.
- Documentation change: updated the PPT speaker notes, big-tech interviewer Q&A, and interview learning manual so they mention `RUN_ID` / effective campaign tracing alongside `perf-test`, `Location`, runtime stop gates, and environment cards.
- Verification: `git diff --check` passed, and a repository text search confirmed the new campaign-tracing wording appears in the data-center guide, performance record, report index, visual brief, Q&A, learning manual, and speaker notes.

### Phase 122

- Quality reset: reran the full project gate after pressure-test campaign tracing and learning/interview wording changes.
- Verification: `scripts/quality-check.sh` passed, including diff checks, script syntax checks, generated-artifact guard, forbidden-claim scan, full backend tests, frontend production build, and Docker Compose config checks.
- Note: the warning lines in Maven output are expected negative-path test coverage for collision retry, async overflow, RocketMQ fallback/disabled branches, external short-link fallback, and cache/DB failure handling.

### Phase 123

- Showcase evidence priority: recover the automated screenshot archive after the admin/data-center and mobile result-page changes.
- Finding: `scripts/capture-showcase-screenshots.sh` initially failed because the screenshot spec waited for “进入第 1 题” before selecting a month; the current UI correctly requires month selection first.
- Test fix: updated `frontend/e2e/showcase-screenshots.spec.mjs` to wait for the “先选出生年月” heading, capture the birth card, select year/month, then enter the first question.
- Verification: `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh` passed with 3 Playwright tests, regenerating 11 screenshots under `docs/screenshots/showcase/`.
- Visual QA: inspected `desktop-06-admin-overview.png` and `iphone-se-04-result.png`; desktop data center renders as a wide operations panel, and the mobile result page includes the new explanation structure.
- Artifact guard: extended `scripts/verify-eight-hour-artifacts.sh` to require all 11 showcase screenshots; verifier, `git diff --check`, and frontend `npm run build` passed.

### Phase 124

- Delivery-index sync: added the restored showcase screenshot run to the verification table in `docs/eight-hour-performance-showcase-delivery.md`.
- Verification: `scripts/verify-eight-hour-artifacts.sh` and `git diff --check` passed.

### Phase 125

- Workspace hygiene: removed the transient Playwright `frontend/test-results/.last-run.json` file produced by the failed screenshot attempt.
- Verification: `git status -sb frontend/test-results` no longer reports untracked test-result files.

### Phase 126

- Quality reset: reran the full project gate after the showcase screenshot spec fix, screenshot regeneration, artifact-verifier update, and temporary test-results cleanup.
- Verification: `scripts/quality-check.sh` passed, including diff checks, script syntax checks, generated-artifact guard, forbidden-claim scan, full backend tests, frontend production build, and Docker Compose config checks.
- Screenshot note: the actual showcase screenshot command was verified separately in Phase 123 with 3/3 Playwright tests passing.

### Phase 127

- Backend self-review priority: inspect whether the data-center short-link source filter is exact enough for desktop operations, especially after the list grows beyond the first screen.
- Finding: `statSource=local/external` previously filtered after scanning only the newest 500 short links, so a larger date range could undercount older matching rows and make CSV/export totals misleading.
- Backend fix: `AdminStatService.listShortLinksByComputedSource` now scans the complete filtered date range in database pages, applies the computed source filter, and then paginates the matching result set.
- Test guard: added `MvpFlowIntegrationTest.shouldScanAllShortLinksWhenFilteringByComputedSource`, seeding 501 local short links and verifying page 26 still reports total `501` with one record.
- Documentation change: updated the data-center guide and metric dictionary to explain that `statSource` filtering is complete within the selected range, while still being an admin diagnostic rather than long-term BI.
- Verification: `mvn -q -f backend/pom.xml -Dtest=MvpFlowIntegrationTest#shouldScanAllShortLinksWhenFilteringByComputedSource test` passed.

### Phase 128

- Backend self-review priority: check whether manual daily aggregation can leave `/api/admin/overview` showing a stale 45-second cached result.
- Finding: overview caching is useful for refresh performance, but after a successful manual aggregation the next admin read should immediately observe the new `daily_metric`口径.
- Backend fix: `RedisCacheService` now uses a versioned `admin:overview:v{version}:{range}` cache key, and `AnalyticsAggregationService.aggregate` advances the overview cache version after aggregation succeeds.
- Engineering note: this avoids Redis key scanning while still invalidating all overview ranges; result detail and short-link resolution caches are untouched.
- Test guard: added cache-version verification in `RedisCacheServiceTest`, and the aggregation integration test now verifies the overview cache version is advanced on aggregation.
- Documentation change: the data-center guide now explains the 45-second overview cache and the post-aggregation version switch.
- Verification: targeted Maven tests for aggregation and Redis cache versioning passed.

### Phase 129

- Runtime self-review priority: make visit-event runtime counters match their operational meaning under failure.
- Finding: `totalFlushedEvents` was incremented before a batch insert actually succeeded, so a batch failure followed by failed single-row degrade could overstate successful persistence.
- Backend fix: `VisitEventService.flushBatch` now increments `totalFlushedEvents` after batch success, or after counting successful single-row degraded writes when the batch path fails.
- Test guard: added `VisitEventServiceTest.recordAsyncShouldNotCountFlushWhenBatchAndFallbackInsertFail`, keeping `totalFlushedEvents=0` when both batch and fallback writes fail.
- Verification: targeted VisitEventService tests for normal async insert, batch-failure fallback, and batch+fallback failure passed.

### Phase 130

- Quality reset: reran the eight-hour artifact verifier, `git diff --check`, focused backend tests for the three new backend fixes, and the full `scripts/quality-check.sh`.
- Verification result: all gates passed; Maven warning lines were the expected negative-path tests for collision retry, async overflow, RocketMQ fallback/disabled branches, external-shortlink fallback, cache degradation, and visit-event write failures.
- Runtime refresh: stopped the stale `48081` backend and restarted Spring Boot local profile with `APP_BASE_URL=http://127.0.0.1:5175` and `--server.port=48081`.
- Local preview smoke: `scripts/local-preview-smoke-test.sh` passed against frontend `5175` and backend `48081`, creating `shortUrl=http://127.0.0.1:5175/s/uh9pYl` and redirecting to `/result/R20260614055000462705042?sc=uh9pYl`.
- Runtime check: `/api/admin/visit-events/runtime` returned `healthStatus=ok`, `queueSize=0`, `droppedAsyncEvents=0`, `batchWriteFailures=0`, and `totalFlushedEvents=1`.

### Phase 131

- Consistency self-review priority: align the frontend/admin wording and learning materials with the backend fixes from Phases 127-129.
- Frontend check: `/admin` already labels `totalFlushedEvents` as “已落库”, which matches the corrected success-count semantics.
- Documentation sync: updated the learning manual, big-tech interviewer Q&A, PPT speaker notes, and API spec to explain versioned overview cache invalidation after aggregation.
- API sync: clarified that `totalFlushedEvents` counts successful batch writes or successful single-row degraded writes, not failed attempts.

### Phase 132

- Performance-report priority: make generated pressure-test reports easier to read without changing the core request workload.
- Tooling change: `scripts/performance-limit-test.sh` now writes an “自动结论与下一步” section and a `summary.analysis` array covering stop reason, slowest endpoint kind, runtime risk, workload scope, and local/public extrapolation boundary.
- Evidence run: generated `docs/performance-reports/workflow-health-analysis-section/` with `WORKLOAD=health`, `STEPS=1`, `REQUESTS_PER_STAGE=4`, and `BASE_URL=http://127.0.0.1:48081`.
- Documentation change: added the new report to `docs/performance-reports/README.md`, added it to the eight-hour delivery verification table, and extended the artifact verifier to require its `analysis` output.
- Boundary note: this is a report-format and runtime-capture verification, not a business-chain capacity result.

### Phase 133

- Data-center UI self-review priority: verify the desktop `/admin` page against the new backend source-filter behavior.
- Finding: the page still said “来源筛选最多扫描最近 500 条短链”, which contradicted the Phase 127 backend fix.
- Frontend fix: updated the short-link list helper text to say the source filter scans the complete selected range in pages and is intended for operations diagnosis.
- Browser verification: Playwright DOM check against `http://127.0.0.1:5175/admin` found `数据中台`, `电脑端运营看板`, `测算完成`, `分享入口`, `回流强度`, `风险与行动建议`, `访问事件运行态`, `短链列表`, and the new source-filter text; desktop viewport `1440x1000` had no horizontal overflow.
- Verification: `npm --prefix frontend run build` passed.

### Phase 134

- Optimization-plan sync priority: keep the follow-up performance plan aligned with the new backend and report behavior.
- Documentation change: `docs/performance-optimization-plan.md` now mentions automatic report conclusions, versioned overview cache invalidation after aggregation, `totalFlushedEvents` as successful persistence, and complete-range `statSource` scanning.
- Documentation change: `docs/production-load-observability-checklist.md` now asks production reviews to record the script's automatic conclusion, including slowest endpoint, runtime risk, and extrapolation boundary.

### Phase 135

- Eight-hour self-check priority: remove stale delivery wording that could make the data-center and performance story sound less precise than the current implementation.
- Documentation fix: updated older quality/release notes that still described `statSource` filtering as scanning only the newest 500 short links; they now say the backend scans the complete selected range in pages while remaining an admin diagnostic rather than long-term BI.
- PPT claim hygiene: changed slide 11 from “512 并发健康” to “512 阶梯健康” and rebuilt the PPT, slide previews, layout JSON, build manifest, and contact sheet so the formal deck no longer sounds like a production QPS promise.
- Verification: `scripts/verify-eight-hour-artifacts.sh` and `git diff --check` passed after the documentation cleanup and PPT rebuild.

### Phase 136

- Data-center improvement priority: make the desktop admin page easier to scan as an operations console, not just a dense metric table.
- Frontend change: added an “运营雷达” section to `/admin`, deriving four 0-100 observation values from existing overview data: 完成力、分享意愿、回流热度、口径可信.
- Frontend polish: downgraded raw external-shortlink and visit-event runtime fetch failures from visible `system error` text to operator-friendly “不影响核心数据” notices, so debug failures do not make the data center look broken during demos.
- Screenshot refresh: reran `scripts/capture-showcase-screenshots.sh`, updating the desktop admin showcase image with the radar module and cleaner runtime notices.
- Documentation sync: updated the data-center guide, metric dictionary, eight-hour delivery index, and PPT speaker notes to explain the radar as a derived observation layer rather than a new backend fact source.
- Verification: `npm --prefix frontend run build`, `scripts/verify-eight-hour-artifacts.sh`, and `git diff --check` passed.

### Phase 137

- SRE self-review priority: check whether the pressure-test workflow is executable later on a real public domain without relying on memory or manual caveats.
- Finding: the production checklist required 30-60 seconds of cooldown between public stages, but `scripts/performance-limit-test.sh` did not expose or enforce a cooldown parameter.
- Tooling change: added `STAGE_COOLDOWN_SECONDS`; loopback runs may keep it at `0`, while public multi-stage runs fail closed unless it is at least `30`.
- Report change: generated reports now include `stageCooldownSeconds` in `summary.json` and “Stage cooldown seconds” in the environment card.
- Evidence run: regenerated `docs/performance-reports/workflow-health-analysis-section/` with the new field after a tiny `WORKLOAD=health` local run.
- Documentation sync: updated the pressure-test report index, production observability checklist, and performance optimization plan to mention the public-stage cooldown guard.
- Verification: `bash -n` for pressure-test and artifact-verifier scripts, `scripts/verify-eight-hour-artifacts.sh`, and `git diff --check` passed.

### Phase 138

- Delivery-entry self-review priority: make sure the growing eight-hour artifact pack is discoverable from the static docs site, not only from deep repository paths.
- Docs-site update: added links to the project showcase PPT asset pack, eight-hour performance delivery index, performance visual brief, data-center guide, metric dictionary, report index, observability checklist, and optimization plan.
- Showcase wording update: the desktop admin screenshot caption now calls out risk suggestions, the operations radar, trends, distributions, and short-link lists.
- Docs-site README update: clarified that the static site now serves as a portfolio-style entry point for H5 experience, architecture proof, eight-hour workflow, PPT, data center, and pressure-test materials.
- Verification: local relative-link check for `docs-site/index.html`, `scripts/verify-eight-hour-artifacts.sh`, and `git diff --check` passed.

### Phase 139

- Performance evidence priority: bridge the older legacy 512/768 local capacity samples and the newer report-format checks with a current-code mixed sanity run.
- Evidence run: generated `docs/performance-reports/workflow-mixed-current-sanity/` using `WORKLOAD=mixed`, `STEPS=1,2,4,8,16,32`, `REQUESTS_PER_STAGE=64`, and local backend `127.0.0.1:48081`.
- Result: all stages completed; final 32-step stage P95 was `104ms`, error rate was `0%`, runtime queue stayed at `0`, and the slowest endpoint kind in the highest stage was `admin` with P95 `108ms`.
- Boundary: this is a current local H2 mixed regression and report-format proof, not a public production capacity claim.
- Documentation sync: added the report to the performance report index, eight-hour delivery index, performance visual brief, and artifact verifier.
- Verifier fix: corrected a backtick pattern in `scripts/verify-eight-hour-artifacts.sh` that briefly triggered shell command substitution while checking `Workload: \`mixed\``.
- Verification: `bash -n`, `scripts/verify-eight-hour-artifacts.sh`, and `git diff --check` passed; verifier now reports `reportsChecked=8`.

### Phase 140

- Quality checkpoint: reran the full `scripts/quality-check.sh` after the mixed sanity report, docs-site navigation, pressure-test cooldown guard, artifact-verifier update, and data-center radar changes.
- Verification result: quality gate passed, including diff checks, shell syntax checks, generated-artifact guard, forbidden-claim scan, full Maven tests, frontend production build, and Docker Compose config checks.
- Expected test logs: Maven warnings/errors came from negative-path tests for collision retry, queue overflow, batch-write fallback failure, RocketMQ unavailable/fallback branches, external-shortlink fallback, cache degradation, and database-busy paths.

### Phase 141

- Eight-hour self-check priority: align the artifact verifier with the pressure-test report index and make the delivery index easier to use as a live checkpoint.
- Verifier change: added `workflow-health-verify` to `scripts/verify-eight-hour-artifacts.sh`; because it is an older workflow report, the verifier checks its required files and workload marker without forcing newer environment-card keys.
- Delivery-index change: added a “阶段自检卡” covering artifact completeness, full quality gate, data-center demo readiness, pressure-test evidence, public-load-test preconditions, and interview boundary wording.
- Verification: `bash -n scripts/verify-eight-hour-artifacts.sh`, `git diff --check`, and `scripts/verify-eight-hour-artifacts.sh` passed; verifier now reports `reportsChecked=9`.
- Quality checkpoint: reran `scripts/quality-check.sh`; the full gate passed after the verifier and delivery-index updates.

### Phase 142

- Data-center improvement priority: make the desktop admin page show where the conversion chain loses users, not only the raw funnel counts.
- Frontend change: added “转化链路诊断” to `/admin`, deriving adjacent-step retention, drop count, inverted-data warnings, and short diagnostic notes from the existing `funnelSteps` array.
- Documentation sync: updated the data-center guide, metric dictionary, docs-site screenshot caption, eight-hour delivery index, and artifact verifier to include the new diagnostic layer.
- Screenshot refresh: reran `scripts/capture-showcase-screenshots.sh` against `127.0.0.1:5175`; all 3 Playwright showcase tests passed and the desktop admin screenshot now includes the conversion-chain section.
- Verification: `npm --prefix frontend run build` and `git diff --check` passed before screenshot refresh.

### Phase 143

- Showcase consistency priority: keep the formal PPT aligned with the updated data-center page.
- PPT source change: updated slide 8 to mention “运营雷达、转化链路诊断、趋势图、来源结构和五行分布”, and changed the right-side proof node from generic visualization to “链路诊断”.
- Speaker-notes sync: slide 8 now explains that the operations radar handles 0-100 directional scanning while conversion-chain diagnosis explains adjacent retention, drop counts, and inverted-data warnings.
- Rebuild: rebuilt the 12-slide PPTX, per-slide previews, final layout JSON, and contact sheet through artifact-tool.
- Visual QA: inspected `slide-08.png`; the updated copy renders without visible overflow.

### Phase 144

- Performance evidence priority: add a focused backend report for the admin overview path after the data-center UI gained more operational emphasis.
- Runtime finding: the previous local backend process was alive but its H2 schema had become empty; the first admin report attempt failed during seed result creation with `Table "user_result" not found`. Stopped that process and restarted the local backend on `127.0.0.1:48081` so schema initialization ran cleanly.
- Evidence run: generated `docs/performance-reports/workflow-admin-current-sanity/` using `WORKLOAD=admin`, `STEPS=1,2,4,8,16,32,64`, `REQUESTS_PER_STAGE=96`, and local backend `127.0.0.1:48081`.
- Result: all stages completed; final 64-step stage P95 was `216ms`, error rate was `0%`, runtime queue stayed at `0`, and no dropped events or batch-write failures were observed.
- Boundary: this is a current local H2 admin-query regression and report-format proof, not a public production capacity claim.
- Documentation sync: added the report to the performance report index, eight-hour delivery index, performance visual brief, performance optimization plan, and artifact verifier.
- Verification: `bash -n scripts/verify-eight-hour-artifacts.sh`, `scripts/verify-eight-hour-artifacts.sh`, `git diff --check`, and the full `scripts/quality-check.sh` passed; verifier now reports `reportsChecked=10`.

### Phase 145

- Backend/SRE hardening priority: prevent future pressure-test and preview workflows from treating a live process as ready when the database schema is missing.
- Backend change: added `GET /api/readiness`, which checks the core tables `user_result`, `short_link`, `visit_event`, `site_daily_metric`, and `short_link_daily_metric`; missing schema now returns HTTP `503` with `data.status=DOWN`.
- Tooling change: production health, production smoke, Docker smoke, domain bind preflight, local preview smoke, performance smoke, and performance limit scripts now check readiness before continuing.
- Evidence run: restarted the local backend on `127.0.0.1:48081`; `/api/readiness` returned all five core tables as `ok`.
- Local preview smoke: `scripts/local-preview-smoke-test.sh` passed with `readinessStatus=UP`, `shortUrl=http://127.0.0.1:5175/s/1b7R3H`, and redirect `/result/R20260614072110338772274?sc=1b7R3H`.
- Pressure-report check: a tiny `WORKLOAD=health` run wrote `Readiness status: UP` into `/private/tmp/wuxing-readiness-preflight-check/report.md`.
- Verification: `mvn -q -f backend/pom.xml test`, targeted shell syntax checks, and `git diff --check` passed.

### Phase 146

- Multi-agent review absorbed: the SRE reviewer flagged smoke data pollution risk, and the data-center reviewer prioritized desktop quick filters, short-link pagination, and clearer口径 state.
- Smoke isolation change: `scripts/production-smoke-test.sh` and `scripts/docker-smoke-test.sh` now default to `X-Channel=perf-test`, carry synthetic campaign labels, and validate admin overview with `includeSynthetic=true` so smoke samples remain visible to the check but excluded from daily default operations.
- Smoke verification: both scripts passed against local backend `127.0.0.1:48081`, outputting `readinessStatus=UP` and the expected synthetic channel/campaign markers.
- Frontend change: `/admin` now has quick date filters, a current口径 status strip, short-link page-size controls, and first/previous/next/last pagination for desktop operations.
- Documentation sync: updated the data-center guide, domain self-audit, and performance optimization plan so the docs match the new smoke isolation and admin pagination behavior.
- Visual verification: `npm --prefix frontend run build` passed, and `scripts/capture-showcase-screenshots.sh` refreshed the desktop admin screenshot showing the quick filters,口径 strip, conversion-chain diagnosis, and paginated short-link list.

### Phase 147

- SRE follow-up priority: reduce false negatives in production smoke caused by eventually consistent visit-event persistence.
- Tooling change: `scripts/production-smoke-test.sh` now polls admin overview for up to `SMOKE_OBSERVE_TIMEOUT_SECONDS` while checking the synthetic smoke sample, instead of requiring the short-link visit to appear immediately after the redirect.
- Smoke verification: local run passed with `SMOKE_OBSERVE_TIMEOUT_SECONDS=6`, `readinessStatus=UP`, `syntheticChannel=perf-test`, and `syntheticCampaign=production-smoke`.
- Data-center improvement: the `/admin` daily trend table now shows day-over-day `ΔPV`, `Δ结果`, and `Δ回流` labels derived from existing `dailyTrends`, helping operators find the first day a metric changed.
- Visual verification: `npm --prefix frontend run build` passed, and `scripts/capture-showcase-screenshots.sh` refreshed the desktop admin screenshot with daily trend deltas and short-link pagination.

### Phase 148

- Performance evidence priority: add a current-code single-path report for result-page reads, so the evidence set covers mixed, admin, health, shortlink-format, and result paths.
- Evidence run: generated `docs/performance-reports/workflow-result-current-sanity/` using `WORKLOAD=result`, `STEPS=1,2,4,8,16,32,64`, `REQUESTS_PER_STAGE=96`, and local backend `127.0.0.1:48081`.
- Result: all stages completed; final 64-step stage P95 was `112ms`, P99 was `114ms`, error rate was `0%`, runtime health stayed `ok`, and no dropped events or batch-write failures were observed.
- Boundary: this is a current local H2 result-read regression and report-format proof, not a public production capacity claim.
- Documentation sync: added the report to the performance report index, eight-hour delivery index, performance visual brief, performance optimization plan, and artifact verifier.

### Phase 149

- Performance evidence priority: add a current-code single-path report for the short-link 302 hot path, because it is the most user-visible part of the share/return loop.
- Evidence run: generated `docs/performance-reports/workflow-shortlink-current-sanity/` using `WORKLOAD=shortlink`, `STEPS=1,2,4,8,16,32,64`, `REQUESTS_PER_STAGE=96`, and local backend `127.0.0.1:48081`.
- Result: all stages completed; final 64-step stage P95 was `185ms`, P99 was `192ms`, error rate was `0%`, runtime health stayed `ok`, and no dropped events or batch-write failures were observed.
- Boundary: this is a current local H2 short-link regression and report-format proof, not a public production capacity claim.
- Documentation sync: added the report to the performance report index, eight-hour delivery index, performance visual brief, performance optimization plan, and artifact verifier.

### Phase 150

- Workspace hygiene priority: keep Playwright screenshot runs from leaving repeated untracked local noise.
- Tooling hygiene change: added `frontend/test-results/` to `.gitignore`, matching the existing generated-artifact policy for `frontend/dist/` and `backend/target/`.
- Verification: `git status -sb --ignored=matching frontend/test-results .gitignore` now reports the Playwright output directory as ignored rather than untracked.

### Phase 151

- Backend observability priority: tighten readiness semantics and reduce noisy error logs from unrelated missing static-resource probes.
- Readiness contract change: `/api/readiness` now returns `scope=core_schema`, making clear that the endpoint verifies core table availability rather than claiming Redis, RocketMQ, and every write path are fully healthy.
- Test change: added unit assertions for readiness scope and a MockMvc integration check for `/api/readiness` JSON shape across all five core tables.
- Error handling change: `NoResourceFoundException` now returns API-style `404 not found` instead of falling through to the generic 500 handler and logging `Unexpected server error`.
- Runtime verification: restarted backend on `127.0.0.1:48081`; `/api/readiness` returned `scope=core_schema`, unknown `/admin-api/...` returned `{"code":404,"message":"not found"}`, and `scripts/local-preview-smoke-test.sh` passed with `readinessStatus=UP`.

### Phase 152

- Study-material priority: make the latest performance and readiness evidence easier to explain under interview pressure.
- Learning-manual change: `docs/interview-learning-manual.md` now distinguishes performance smoke from stepped load reports, records the current-code mixed/admin/result/shortlink P95 evidence, and explicitly states that local H2 results are regression proof rather than production QPS proof.
- Interview-manual change: `docs/big-tech-interviewer-qa.md` now includes the current small-step regression numbers and the `/api/readiness` `core_schema` scope, so answers can show engineering honesty instead of overstating capacity.
- Presentation-note sync: `docs/artifacts/presentations/wuxing-showcase-speaker-notes.md` already carries the same current-report numbers for the performance slide speaking track.

### Phase 153

- Data-center UX priority: make the desktop admin page produce a concise operational readout instead of forcing the owner to manually assemble numbers from many panels.
- Frontend change: added a “复盘摘要” section to `/admin`, derived from the current filters, core metrics, share actions, latest trend deltas, source structure, top action item, and visit-event runtime.
- Interaction change: added a “复制摘要” action with clipboard fallback, so current-scope conclusions can be pasted into daily notes, pressure-test reviews, or interview rehearsal notes.
- Documentation change: `docs/admin-data-center-guide.md` now explains the review-summary use cases and its boundary as a derived readout rather than a new metric source.

### Phase 154

- Multi-agent self-audit priority: absorb the SRE and data-center reviewer findings before moving to another large feature slice.
- SRE fix: `scripts/performance-smoke-test.sh` now defaults to `SYNTHETIC_CHANNEL=perf-test`, generates a `RUN_ID`, sends `X-Channel` / `X-Campaign` on result, result-read, and short-link requests, validates admin overview with `includeSynthetic=true`, and prints the synthetic markers in the output.
- Data-center fix: short-link detail links now carry `includeSynthetic`, keyword, and stat-source query context; the detail page reads `includeSynthetic` and shows the inherited口径 before querying visits.
- Documentation sync: `docs/admin-data-center-guide.md` now notes that short-link details inherit the date and synthetic-traffic口径.
- Verification: restarted a clean local backend on `127.0.0.1:48082` because `48081` was occupied by an unusable old listener; `scripts/performance-smoke-test.sh` passed with `syntheticChannel=perf-test`, `syntheticCampaign=performance-smoke:smoke-20260614081350`, `runtimeHealthStatus=ok`, `asyncDroppedEvents=0`, `asyncBatchWriteFailures=0`, and `readinessStatus=UP`.

### Phase 155

- Presentation quality priority: address the reviewer feedback that the data-center slide needed stronger visual proof at thumbnail size and the pressure-test slide needed a clearer boundary marker.
- Slide change: rebuilt slide 8 from a plain admin screenshot plus text cards into a讲解图 layout with desktop screenshot, three callouts for复盘摘要、口径保护、运行态排障, and a four-step口径-判断-定位-行动 rail.
- Boundary change: added a visible “LOCAL METHOD EVIDENCE · NOT PRODUCTION QPS” stamp to slide 11, matching the speaker-note boundary language.
- Artifact rebuild: regenerated `docs/artifacts/presentations/wuxing-persona-project-showcase.pptx`, all slide previews, layout JSON, and the contact sheet through the recorded artifact-tool command.
- Visual QA: inspected `slide-08.png`, `slide-11.png`, and `contact-sheet.png`; the updated slides render without obvious overlap and the contact sheet keeps a distinct rhythm.

### Phase 156

- Performance-explanation priority: make it harder to confuse readiness, smoke, and stepped load reports during review or interviews.
- Documentation change: `docs/performance-optimization-plan.md` now has a three-tool matrix explaining what `/api/readiness`, `scripts/performance-smoke-test.sh`, and `scripts/performance-limit-test.sh` each prove and do not prove.
- Study sync: `docs/interview-learning-manual.md` and `docs/big-tech-interviewer-qa.md` now explicitly frame readiness as dependency preflight, performance smoke as a regression gate, and performance limit as stepped exploration rather than production-QPS proof.

### Phase 157

- SRE hardening priority: prevent future local stepped-load reports from accidentally treating missing runtime observation as proof of no runtime risk.
- Tooling change: `scripts/performance-limit-test.sh` now supports `STRICT_RUNTIME_OBSERVATION=1`; when enabled, loopback runs also require visit-event runtime to be observable during preflight and before/after each stage.
- Report change: generated reports now include `Strict runtime observation` in the environment card, and the automatic analysis distinguishes incomplete runtime observation from zero drops/failures.
- Documentation sync: `docs/performance-optimization-plan.md`, `docs/performance-reports/README.md`, and `docs/interview-learning-manual.md` now recommend strict runtime observation for local reports that will be沉淀 as evidence.
- Verification: `OUT_DIR=/private/tmp/wuxing-strict-runtime-check WORKLOAD=health STEPS=1 REQUESTS_PER_STAGE=4 STRICT_RUNTIME_OBSERVATION=1 scripts/performance-limit-test.sh` passed against `127.0.0.1:48082`; the report contains `Strict runtime observation: True`, `Readiness status: UP`, and `Stop reason: completed all stages`.

### Phase 158

- Data-center workflow priority: make the desktop admin page easier to use during live review, so a risk conclusion can be traced to the exact evidence panel without manual scrolling.
- Frontend change: added a “证据索引” strip to `/admin`, summarizing conversion, trend, attribution, short-link, and runtime evidence and jumping to the matching section.
- Interaction change: each “风险与行动建议” row now includes a native anchor-style `定位...` action that jumps to the most relevant evidence section, such as转化链路、口径差异、来源排行、短链明细、访问事件运行态 or外部短链调试信息.
- Documentation sync: `docs/admin-data-center-guide.md` now explains the evidence index and how the risk row jump targets should be used during daily operations, pressure-test review, and interview demos.
- Verification: `npm --prefix frontend run build` passed, and a 1440px Playwright check confirmed clicking `定位转化链路` changed the URL to `#journey-section`, moved scroll position from `0` to `2482`, and placed the target section about `18px` below the viewport top.

### Phase 159

- Data-center review priority: add a time anchor to copied operational summaries, because a dashboard number without refresh time is hard to use in reports or interviews.
- Frontend change: `/admin` now records `lastLoadedAt` after successful overview/list loading and after manual aggregation refresh.
- UI change: the top口径条 now includes `刷新于 yyyy-MM-dd HH:mm:ss`, and the “复盘摘要”口径 line carries the same refresh time.
- Documentation sync: `docs/admin-data-center-guide.md` now tells the operator to preserve the refresh-time line when copying summaries outward.
- Verification: `npm --prefix frontend run build` passed, and browser DOM verification found `刷新于 2026-06-14 08:47:45` in both the口径条 and复盘摘要.

### Phase 160

- SRE review priority: turn performance smoke from “链路跑通” into a visible low-cost threshold gate for every small change.
- Verification run: `BASE_URL=http://127.0.0.1:48082 ADMIN_TOKEN=dev-token SHORTLINK_HITS=8 ADMIN_HITS=2 MAX_SHORTLINK_P95_MS=220 MAX_ADMIN_P95_MS=500 MAX_ASYNC_QUEUE_SIZE=0 MAX_ASYNC_DROPPED_EVENTS=0 MAX_ASYNC_BATCH_FAILURES=0 scripts/performance-smoke-test.sh` passed.
- Result: short-link P95 was `27ms`, admin overview P95 was `36ms`, runtime health was `ok`, queue size was `0`, dropped events were `0`, batch-write failures were `0`, and readiness was `UP`.
- Documentation sync: `docs/performance-optimization-plan.md`, `docs/performance-reports/README.md`, and `docs/eight-hour-performance-showcase-delivery.md` now include this threshold-smoke record and its boundary as a regression gate rather than a capacity claim.

### Phase 161

- Data-center traceability priority: keep short-link investigation context intact after drilling from the list into a single short-link detail page.
- Frontend change: `/admin` now reads `startDate`, `endDate`, `includeSynthetic`, `keyword`, and `statSource` from route query on initialization, so returning from a detail page can restore the same filter context.
- Detail-page change: `/admin/short-links/{code}` now supports visit pagination with page-size controls, first/previous/next/last navigation, and a “带筛选返回后台” link that preserves inherited query context.
- Documentation sync: `docs/admin-data-center-guide.md` now notes that short-link detail visit records are paginated and that the context-aware back link should be used during investigation.
- Verification: `npm --prefix frontend run build` passed; a 1440px Playwright check with local `dev-token` confirmed the detail page renders pagination, inherits `includeSynthetic=true`, and exposes `/admin?includeSynthetic=true` as the context-aware back link.

### Phase 162

- SRE self-check priority: acknowledge the remaining gap that current local reports do not yet include a strict-runtime mixed stepped run.
- Execution boundary: attempted to schedule a local `STRICT_RUNTIME_OBSERVATION=1 WORKLOAD=mixed` small-step run against `127.0.0.1:48082`, but sandbox escalation was rejected by the automatic reviewer; the workflow did not try to bypass that decision.
- Documentation change: `docs/performance-reports/README.md` now includes the exact next-run strict mixed template and explains that it is only a local `local-h2` runtime-observability gate, not a production capacity claim.

### Phase 163

- Presentation sync priority: keep the showcase deck aligned with the latest data-center UI rather than shipping a stale screenshot.
- Screenshot refresh: `E2E_BASE_URL=http://127.0.0.1:5175 E2E_ADMIN_TOKEN=dev-token scripts/capture-showcase-screenshots.sh` passed and regenerated the mobile and desktop showcase screenshots.
- Deck rebuild: regenerated `docs/artifacts/presentations/wuxing-persona-project-showcase.pptx`, 12 slide previews, layout JSON, and the contact sheet through the artifact-tool deck build command.
- Speaker-note sync: slide 8 notes now mention证据索引、刷新时间、短链详情分页 and context-aware evidence jumps; the presentation README also updates slide 8's topic.
- Visual QA: inspected `slide-08.png` and `contact-sheet.png`; the updated data-center slide remains readable and the overall deck rhythm is intact.

### Phase 164

- Performance-story priority: make the pressure-test visual brief distinguish regression gates from capacity exploration.
- Documentation change: `docs/performance-visual-brief.md` now adds a “回归门和极限摸底不是一回事” section with a readiness -> smoke -> limit -> public retest flow.
- Evidence sync: the visual brief now records the threshold-smoke sample (`shortlinkP95Ms=27`, `adminP95Ms=36`, runtime `ok`) and the strict-runtime mixed template as a next-run gate rather than completed capacity proof.
- Interview wording update: the brief's interview answer now frames readiness, performance smoke, and performance limit as three different layers, and explicitly says formal local reports should enable strict runtime observation.

### Phase 165

- Handoff priority: make the final delivery page usable as the first stop after the eight-hour workflow, instead of forcing the owner to search across PPT, reports, scripts, and guides.
- Documentation change: `docs/eight-hour-performance-showcase-delivery.md` now has a “最终自检入口” table that points to the PPT, contact sheet, data center, pressure-test report index, and optimization plan.
- Consistency fix: aligned the local retest examples with the current verified `5175` frontend and `48082` backend session, while noting that future default restarts may use `48081`.
- Evidence sync: updated the artifact-verifier wording from older 9/10 report counts to the current 12 checked report directories.

### Phase 166

- SRE feedback priority: close the small evidence gap where `performance-smoke-test.sh` only printed stdout and did not leave a reusable artifact.
- Tooling change: `scripts/performance-smoke-test.sh` now accepts `SMOKE_OUT_DIR`; when set, it writes `smoke-output.txt` and `summary.json` with the result id, short code, synthetic markers, P95 numbers, readiness, runtime health, async queue, dropped events, batch failures, and RocketMQ shadow fields.
- Documentation change: `docs/performance-reports/README.md` and `docs/performance-optimization-plan.md` now show the `SMOKE_OUT_DIR` option for threshold smoke evidence.
- Production-readiness change: `docs/production-load-observability-checklist.md` now includes copyable public-load templates for health, shortlink, result, admin, and mixed workloads, using the actual `STOP_P95_MS` and `STOP_ERROR_RATE` script variables.
- Wording fix: `docs/performance-visual-brief.md` now says “默认运营视图隔离能力” instead of implying entity-level strong isolation.
- Verification: `SMOKE_OUT_DIR=/private/tmp/wuxing-smoke-artifact-check` passed against `127.0.0.1:48082`; generated `smoke-output.txt` and `summary.json`, with short-link P95 `28ms`, admin P95 `36ms`, readiness `UP`, queue `0`, dropped `0`, and batch-write failures `0`.

### Phase 167

- Product-handoff priority: make the final artifacts easier to find after the working session ends.
- Artifact sync: copied the presentation contact sheet into `docs/artifacts/presentations/contact-sheet.png`, so the deliverable package no longer depends on the transient `outputs/` path for the 12-slide overview image.
- Verification sync: `scripts/verify-eight-hour-artifacts.sh` now requires the archived contact sheet.
- Documentation change: `docs/artifacts/presentations/README.md` now separates “交付成品” from “构建与调试材料”.
- Documentation change: `docs/admin-data-center-guide.md` now has an “打开入口” table with the local admin URL, token, default口径, and metric dictionary.
- Documentation change: `docs/eight-hour-performance-showcase-delivery.md` now points to the stable contact sheet, showcase screenshots, and a final two-command delivery check.
- Worklog usability: this file now has `Start date`, `Latest update`, and a direct final-delivery link at the top.

### Phase 168

- Verification-usability priority: make the artifact verifier output explain which contact sheet is the stable deliverable and which one is the build cache.
- Tooling change: `scripts/verify-eight-hour-artifacts.sh` now prints both `archivedContactSheet=docs/artifacts/presentations/contact-sheet.png` and `buildContactSheet=outputs/.../contact-sheet.png`.
- Verification: `scripts/verify-eight-hour-artifacts.sh` passed with `reportsChecked=12`.

### Phase 169

- README accuracy priority: avoid pointing readers to the old feature branch after the eight-hour workflow moved back onto `main`.
- Documentation change: `README.md` now says the current development state is the `main` working tree carrying the eight-hour performance-showcase workflow changes, with release-time commit splitting recommended through the delivery index.

### Phase 170

- Regression-protection priority: make the artifact verifier guard the newest handoff details, not just older PPT and report files.
- Tooling change: `scripts/verify-eight-hour-artifacts.sh` now checks for the smoke evidence landing record, `SMOKE_OUT_DIR` support, `SMOKE_OUT_DIR` documentation, and the public per-workload pressure-test command template.
- Verification: `scripts/verify-eight-hour-artifacts.sh` passed with `reportsChecked=12`.

### Phase 171

- Visual-delivery priority: inspect the archived presentation overview image after moving it into the deliverable directory.
- Visual QA: opened `docs/artifacts/presentations/contact-sheet.png`; all 12 slide thumbnails render, including the data-center slide, local pressure-test boundary slide, and roadmap slide.

### Phase 172

- Safety-template priority: prevent the public pressure-test command examples from being copied with the local development admin token.
- Documentation change: `docs/production-load-observability-checklist.md` now uses `ADMIN_TOKEN=replace-with-production-admin-token` in all public workload templates instead of `dev-token` or shell-redirection-style placeholders.

### Phase 173

- Local-run clarity priority: avoid confusing the current `48082` validation port with older default local examples.
- Documentation change: `README.md` now says `BACKEND_URL` should follow the actual backend port; this workflow verified `48082`, while the default snippet may still use `48081`.

### Phase 174

- Presentation-manifest priority: make the archived presentation overview image discoverable from machine-readable build metadata.
- Artifact change: `docs/artifacts/presentations/artifact-build-manifest.json` now includes `archivedContactSheet` pointing to `docs/artifacts/presentations/contact-sheet.png`.
- Verification sync: `scripts/verify-eight-hour-artifacts.sh` now requires the `archivedContactSheet` field.

### Phase 175

- Final quality priority: rerun the full quality gate after all smoke, handoff, verifier, README, and presentation-manifest changes.
- Verification: `scripts/quality-check.sh` passed, including diff check, shell syntax checks, artifact verifier, Maven tests, frontend production build, and Docker Compose config validation.

### Phase 176

- SRE audit priority: leave evidence when `performance-limit-test.sh` refuses to run for safety reasons, so a missing report is explainable.
- Tooling change: `scripts/performance-limit-test.sh` now writes `preflight-failed.json` to `OUT_DIR` before exiting on public-safety refusal, readiness failure, runtime-unobservable refusal, public runtime baseline refusal, or RocketMQ shadow/fallback refusal.
- Documentation sync: `docs/performance-reports/README.md`, `docs/performance-optimization-plan.md`, and `docs/production-load-observability-checklist.md` now explain that `preflight-failed.json` is a refusal audit artifact, not a capacity report.
- Verification: `BASE_URL=https://example.com OUT_DIR=/private/tmp/wuxing-preflight-fail-check WORKLOAD=health scripts/performance-limit-test.sh` refused before load and wrote `/private/tmp/wuxing-preflight-fail-check/preflight-failed.json` with phase `public-safety`.

### Phase 177

- Success-path priority: confirm the new refusal-audit changes did not break normal `performance-limit-test.sh` report generation.
- Verification: `BASE_URL=http://127.0.0.1:48082 ADMIN_TOKEN=dev-token OUT_DIR=/private/tmp/wuxing-limit-success-check WORKLOAD=health STEPS=1 REQUESTS_PER_STAGE=4 STRICT_RUNTIME_OBSERVATION=1 scripts/performance-limit-test.sh` completed all stages and generated `report.md` plus `summary.json`.
- Result: readiness was `UP`, strict runtime observation was `True`, async runtime health was `ok`, and stop reason was `completed all stages`.

### Phase 178

- Contract-audit priority: make `scripts/external-shortlink-smoke-test.sh` fail on wrong visit-detail source instead of only printing totals.
- Tooling change: the smoke script now asserts visit page shape, verifies explicit `statSource` detail records, and allows `includeSynthetic=true` to return either local or external records according to the running short-link mode.
- Contract fix: split bare `/s/{shortCode}` redirect from attributed `/s/{shortCode}?channel=share&campaign=result-card`; the bare compatibility entry must preserve `sc`, while the attributed entry must preserve `sc`, `channel`, and `campaign`.
- Verification: `WUXING_BASE_URL=http://127.0.0.1:48082 ADMIN_TOKEN=dev-token scripts/external-shortlink-smoke-test.sh` passed with local `firstSource=local`, `redirect=/result/R20260619035327376105854?sc=KUZT4X`, and `sharedRedirect=/result/R20260619035327376105854?sc=KUZT4X&channel=share&campaign=result-card`.

### Phase 179

- Review-closeout priority: address the P2 findings from the visual and contract review agents without touching old `outputs/` artifacts or staging files.
- Tooling change: `scripts/external-shortlink-smoke-test.sh` now creates a real `channel=perf-test` short-link visit and, for local stat source, asserts `includeSynthetic=true` returns more visit records than the default excluded view.
- Tooling change: `scripts/quality-check.sh` now reports untracked quality-gate scripts locally and fails in CI or `REQUIRE_TRACKED_QUALITY_SCRIPTS=1`, matching the CI fresh-checkout guard while keeping dirty-tree iteration possible.
- Frontend polish: the result card mobile identity row tightens element mark sizing, the match hero title is slightly more restrained, `ElementLegend` no longer gives the fifth mobile item full-width emphasis, and admin short-link detail links have a 44px touch target.
- Verification: frontend build, frontend contract verifier, `git diff --check`, external shortlink smoke, mobile E2E 9/9, showcase screenshots 11/11, and eight-hour artifact verifier all passed on the `48082` backend plus `5176` frontend session.

### Phase 180

- Interaction-trust priority: remove the last automatic clipboard-read attempt from the home page while preserving the explicit "检测剪贴板" and manual short-code flows.
- Frontend polish: `ElementMark` stays text-only and no sketch graphics are reintroduced; default, compact, legend, and mobile result-card glyph sizes are reduced so element words read as labels instead of decoration.
- Contract sync: `scripts/verify-frontend-contracts.mjs` now asserts that home-page clipboard detection requires an explicit click and that text-only element glyphs remain visually restrained.
- Documentation sync: `docs/frontend-qa-record-20260619.md` records the current 31-screenshot showcase artifact口径 and keeps the older 25/29-screenshot lines as historical stage notes.
- Verification: frontend build, frontend contract verifier, `git diff --check`, live in-app browser DOM check, mobile E2E 9/9, showcase screenshots 11/11, eight-hour artifact verifier, full `scripts/quality-check.sh`, and live external shortlink smoke all passed after the interaction polish; the local quality gate still warns that 9 quality scripts must be tracked before CI/release.

### Phase 181

- Release-readiness priority: turn the known Git-tracking risk into a reproducible strict-gate result.
- Verification: `REQUIRE_TRACKED_QUALITY_SCRIPTS=1 scripts/quality-check.sh` failed immediately as expected because 9 required quality scripts are still untracked; this confirms the CI/release blocker is version-control scope, not app behavior.
- Live-gate verification: `scripts/frontend-live-gate.sh` passed against `http://127.0.0.1:5176` and `http://127.0.0.1:48082`, including local preview smoke, mobile E2E 9/9, showcase screenshots 11/11, and artifact verification.
- Browser QA: the in-app browser homepage DOM check confirmed no horizontal overflow, no default match invite, hidden empty clipboard status, 44px control targets, and restrained text-only element marks; a viewport screenshot also rendered the home hero cleanly.
- Boundary note: at this phase the deployment contract remained same-origin Nginx proxy; independent API-domain CORS/OPTIONS preflight was still a documented future boundary.
- CI doc sync: `docs/ci-browser-e2e-plan.md` now matches the current workflow by showing `scripts/verify-eight-hour-artifacts.sh` and uploading `frontend/test-results/` plus `frontend/playwright-report/`.

### Phase 182

- Live-polish priority: add one more visual QA pass over the real in-app browser and the latest Playwright screenshots after the text-only element-mark cleanup.
- Browser QA: current `http://127.0.0.1:5176/#preview` home page has no horizontal overflow, no visible text overlap, no console warnings/errors, and all visible controls meet the 44px touch-target floor.
- Result QA: the live-gate result `R20260619120518688988937` renders both shared and direct result variants without error state, horizontal overflow, small controls, or graphic descendants inside `.element-mark`.
- Screenshot QA: reviewed the refreshed mobile result and admin screenshots; the result page now reads as restrained text-first cards, while the mobile admin remains dense but usable and free of obvious clipping.
- Evidence sync: `docs/frontend-qa-record-20260619.md` now records this in-app browser pass, the direct-result sharing-box check, the screenshot-review paths, and the screenshot API timeout boundary.

### Phase 183

- Visual-review closeout priority: remove the last desktop-table feel from the mobile admin short-link list called out by the review agent.
- Frontend change: `#shortlink-section` now keeps the wide table for desktop and switches to `admin-shortlink-mobile-list` cards on mobile, with PV/UV/UIP chips, source/scope metadata, and a 44px detail entry on each card.
- E2E guard: the showcase screenshot flow now asserts that the mobile short-link card list is visible and the wide table is hidden before taking the expanded mobile admin screenshot.
- Verification: frontend build, frontend contract verifier, `git diff --check`, mobile E2E 9/9, showcase screenshots 11/11, and eight-hour artifact verifier all passed after the card-list change.
- Visual QA: reviewed `iphone-se-10-admin-report-expanded.png`, `android-wide-10-admin-report-expanded.png`, `iphone-se-08-admin-overview.png`, and `desktop-06-admin-overview.png`; mobile short links now read as native cards, while desktop remains a full operations table.

### Phase 184

- Release-risk priority: close the conditional independent API-domain CORS blocker without changing the default same-origin deployment behavior.
- Backend change: added `CorsWebConfig` plus `app.cors.allowed-origins` / `app.cors.max-age-seconds`; the default empty whitelist keeps CORS closed, while configured origins allow `GET`, `POST`, `OPTIONS`, the frontend attribution/admin headers, and expose `Content-Disposition` / `Location`.
- Test change: `MvpFlowIntegrationTest` now verifies configured-origin preflight succeeds and unconfigured-origin preflight is rejected.
- Documentation sync: `docs/api-spec.md`, `docs/production-operations-runbook.md`, `deploy/.env.example`, and `deploy/.env.external.example` now describe same-origin as the recommended path and CORS as an explicit independent-domain option.
- Verification: targeted CORS MockMvc tests passed. The remaining hard release risk is Git tracking for the required quality scripts; no `git add` was performed in this phase.
