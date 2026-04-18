# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (Spring Boot, runs on :8080)
```bash
cd backend
./mvnw spring-boot:run          # Start dev server
./mvnw test                     # Run all tests
./mvnw test -Dtest=ClassName    # Run a single test class
./mvnw clean package -DskipTests # Build JAR without tests
```

### Frontend (React + Vite, runs on :5173)
```bash
cd frontend
npm run dev     # Start dev server (proxies /api and /ws to :8080)
npm run build   # TypeScript check + production build
npm run lint    # ESLint (zero warnings enforced)
```

### Local infrastructure
```bash
docker-compose up -d   # Start PostgreSQL (5432) + Redis (6379)
```

## Architecture

PocketFolio is a personal finance tracker. The backend is Spring Boot 3 / Java 17, the frontend is React 18 / TypeScript. They communicate via REST and WebSocket (STOMP over SockJS).

### Backend layers
```
controller → service → repository → entity
```
DTOs are used at the API boundary (separate `*Request` / `*Response` classes). Services never return entities directly.

**Key services:**
- `AuthService` — registration (auto-creates 11 default categories + 4 accounts), login, JWT
- `PriceService` — price fetch orchestrator; calls CoinGeckoService or YahooFinanceService, saves to DB, pushes WebSocket message
- `KnownAssetSyncService` — syncs tradable asset lists: TWSE (`.TW`), TPEX (`.TWO`), CoinGecko top-200 by market cap. Called on startup (if empty) and nightly at 2 AM. Uses `@Retryable` + `@Transactional` with a sanity-check threshold before delete+saveAll.
- `WebSocketService` — wraps `SimpMessagingTemplate`; broadcasts to `/topic/price-updates` and sends to `/queue/alerts` per user.

**Scheduling** (all in `scheduler/`):
- Every 5 min — update all asset prices + push WebSocket
- 1 AM daily — create portfolio snapshots
- 2 AM daily — sync asset lists from exchanges
- 3 AM daily — clear price cache

**Caching:** `@Cacheable(value="prices")` on `CoinGeckoService.getPrice` and `YahooFinanceService.getPrice`. Cache key format: `crypto:<coinGeckoId>` / `stock:<SYMBOL>`. TTL is 5 minutes via Redis config.

**Security:** `JwtAuthenticationFilter` extracts Bearer token and populates `SecurityContext`. `SecurityUtil.getCurrentUserId()` retrieves the authenticated user's UUID anywhere in the service layer.

### Frontend layers
- `src/api/` — one file per domain, all using the shared `axios.ts` instance (auto-injects Bearer token, handles 401 → logout)
- `src/store/` — Zustand: `authStore` (token + user, persisted to localStorage), `websocketStore` (last price update timestamp)
- `src/pages/` — page-level components, self-contained with local state
- `src/router.tsx` — React Router v6 with `PrivateRoute` / `PublicRoute` guards

### Asset symbol conventions
| Type | `asset.symbol` stored | Used for |
|---|---|---|
| TWSE stock | `2330.TW` | Yahoo Finance API |
| TPEX stock | `6547.TWO` | Yahoo Finance API |
| Crypto | `bitcoin` (CoinGecko ID, lowercase) | CoinGecko API |

CoinGecko IDs must be lowercase. `CoinGeckoService.getPrice` normalizes to lowercase before the API call to handle legacy data.

### WebSocket flow
Client connects to `/ws` (SockJS). On successful price update, `PriceService` calls `WebSocketService.broadcastPriceUpdate()` → `/topic/price-updates`. Frontend `websocketStore` updates `lastPriceUpdateAt`, triggering a `useEffect` reload in `AssetList`.

### Deployment
- **Backend** → GCP Cloud Run; CI triggered by changes under `backend/`. Cloud SQL connected via Unix socket (no public IP).
- **Frontend** → Firebase Hosting; CI triggered by changes under `frontend/`. `VITE_API_BASE_URL` and `VITE_WS_URL` are injected at build time from GitHub Secrets.
- **Local vs prod config**: `application.yaml` (localhost DB/Redis) vs `application-prod.yaml` (Cloud SQL + Upstash Redis TLS).
