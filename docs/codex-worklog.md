# Codex 8-Hour Worklog

Date: 2026-06-11

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
