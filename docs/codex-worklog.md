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
