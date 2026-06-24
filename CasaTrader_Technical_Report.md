# CasaTrader — Comprehensive Technical Report

A complete walkthrough of the CasaTrader Android application: every class, every design choice, every layout, and the reasoning behind them. This document is structured so each section answers two questions: **what is it?** and **why was it built this way?**

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack and Justifications](#2-tech-stack-and-justifications)
3. [Backend — Supabase](#3-backend--supabase)
4. [Android Project Structure](#4-android-project-structure)
5. [Data Layer](#5-data-layer)
6. [Network Layer](#6-network-layer)
7. [Authentication and JWT Lifecycle](#7-authentication-and-jwt-lifecycle)
8. [UI Layer — Screens, Fragments, ViewModels](#8-ui-layer--screens-fragments-viewmodels)
9. [Design System and Resources](#9-design-system-and-resources)
10. [End-to-End User Flows](#10-end-to-end-user-flows)
11. [Likely Examination Questions and Model Answers](#11-likely-examination-questions-and-model-answers)

---

## 1. Project Overview

CasaTrader is an Android application that simulates trading stocks listed on the **Casablanca Stock Exchange (BVC)**. Users register, browse 113 listed stocks, view price charts and quote details, execute buy/sell orders against a 100,000 MAD simulated cash balance with a 1% commission per side, and track their portfolio's unrealized profit/loss.

### High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                     Android Client (Java)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────┐     │
│  │  UI Layer    │→ │  ViewModels  │→ │   Repositories      │     │
│  │ (Fragments)  │← │  (LiveData)  │← │ (business logic)    │     │
│  └──────────────┘  └──────────────┘  └──────────┬──────────┘     │
│                                                  ↓                │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  Retrofit + OkHttp + Interceptors (Auth + Refresh)       │    │
│  └──────────────────────────┬───────────────────────────────┘    │
└─────────────────────────────┼────────────────────────────────────┘
                              │ HTTPS (JWT)
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                          Supabase                                │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────┐     │
│  │   GoTrue     │  │   PostgREST  │  │   PostgreSQL        │     │
│  │ (Auth API)   │  │ (REST + RPC) │  │   + RLS + Triggers  │     │
│  └──────────────┘  └──────────────┘  └─────────────────────┘     │
└──────────────────────────────────────────────────────────────────┘
                              ↑
                              │ Daily cron (15:30 UTC)
┌──────────────────────────────────────────────────────────────────┐
│      Python scraper (separate repo: bvc-scraper + Casatrader_backend)  │
│      Pulls live BVC data and writes to stocks + price_snapshots  │
└──────────────────────────────────────────────────────────────────┘
```

### Repository Structure (Android side)

```
app/src/main/
├── AndroidManifest.xml
├── java/com/fahd/casatrader/
│   ├── CasaTraderApp.java          (Application class — initializes singletons)
│   ├── MainActivity.java           (Hosts bottom-nav + fragments)
│   ├── data/
│   │   ├── model/                  (POJOs matching DB schema)
│   │   ├── remote/                 (Retrofit interfaces, interceptors, ApiClient)
│   │   └── repo/                   (Repository classes wrapping API + caching logic)
│   ├── ui/
│   │   ├── auth/                   (Login, signup, AuthViewModel)
│   │   ├── stocks/                 (Stock list)
│   │   ├── detail/                 (Stock detail + buy/sell dialog)
│   │   ├── portfolio/              (Holdings + cash overview)
│   │   ├── transactions/           (Trade history)
│   │   └── watchlist/              (Saved stocks)
│   └── util/
│       └── TokenStore.java         (Singleton wrapping EncryptedSharedPreferences)
└── res/
    ├── layout/                     (Activity, fragment, dialog, item layouts)
    ├── menu/                       (Bottom nav, overflow menus)
    ├── navigation/                 (nav_main.xml — Navigation graph)
    ├── drawable/                   (Vector icons, pill backgrounds)
    ├── values/ + values-night/     (Colors, themes, styles for light + dark mode)
    └── mipmap-*/                   (Launcher icons at multiple densities)
```

---

## 2. Tech Stack and Justifications

| Choice | Justification |
|---|---|
| **Java** | Required by the project specification. Strong static typing, mature Android tooling, well-documented for academic review. |
| **MVVM** | Standard Android architecture. Cleanly separates view (UI), state (ViewModel), and data (Repository). Survives configuration changes (rotation) thanks to `ViewModel`'s lifecycle, which outlives the Activity/Fragment. |
| **LiveData** | Lifecycle-aware observable. Stops emitting when the observer's lifecycle is paused, preventing memory leaks. Pairs naturally with ViewModel. |
| **Retrofit + OkHttp** | Industry-standard HTTP client for Android. Retrofit provides declarative interfaces; OkHttp handles connections, interceptors, and logging. The interceptor model allows adding the auth header in one place rather than per-call. |
| **Gson** | Reflection-based JSON parser that works out-of-the-box for Java POJOs. Simpler setup than Moshi, which is optimized for Kotlin. Adequate for this project's payload shapes. |
| **Supabase** | Open-source Firebase alternative. Provides Postgres + REST + Auth + RLS without writing backend code. PostgREST automatically exposes tables and functions over HTTP, which dramatically reduces backend boilerplate for an academic project. |
| **Material 3** | Latest Material Design system. Provides modern token-based theming (`colorSurfaceContainer*`, `colorOutlineVariant`, etc.) and works correctly with light/dark mode out of the box. |
| **View Binding** | Generates type-safe binding classes from layouts at compile time. Replaces `findViewById()`, eliminates a class of runtime errors (wrong cast, missing view), and adds compile-time guarantees. |
| **AndroidX Navigation Component** | Official solution for in-app navigation. Provides a declarative navigation graph and integrates with `BottomNavigationView` via `NavigationUI` to handle tab switching and toolbar title updates automatically. |
| **MPAndroidChart** | The de facto charting library for Android. Mature, customizable, supports line/bar/candlestick. Used here for the year-long price history line chart on the stock detail screen. |
| **EncryptedSharedPreferences** | AndroidX Security Crypto component that transparently encrypts shared prefs values using the Android Keystore. Used to store JWTs at rest so a rooted device can't trivially read them. |

### Why MVVM Specifically (and Not MVC or MVP)?

* **MVC** mixes view and controller logic in the Activity, which makes Activities huge and hard to test. Configuration changes (rotation) destroy the Activity, taking the controller state with it.
* **MVP** separates the view but keeps presenter lifecycle tied to the view, requiring manual handling of configuration changes.
* **MVVM** with `ViewModel` is **lifecycle-aware**: the ViewModel survives rotation, so cached data and in-flight requests aren't lost. `LiveData` ensures the UI always reflects the current state without manual subscription management.

### Why Server-Side Trade Logic (RPC) Instead of Client-Side?

A buy operation involves four steps that must succeed or fail together:
1. Verify cash balance covers cost
2. Insert/update the holding row (with weighted-average cost basis recalculation)
3. Debit cash from the profile
4. Insert a transaction record

If these run client-side with separate REST calls, a network failure between steps could leave the database in an inconsistent state (e.g., cash debited but holding not updated). By wrapping the whole sequence in a single Postgres function (`buy_stock` and `sell_stock`), the entire operation runs in **one database transaction** — atomic by construction. This is the same pattern used by real fintech systems.

---

## 3. Backend — Supabase

The backend has three concerns: storing market data, storing user data, and executing trades atomically. All three live in PostgreSQL, exposed via PostgREST (auto-generated REST API) and GoTrue (auth).

### 3.1 Database Schema

#### `public.stocks`
Stores all 113 BVC-listed stocks. Updated daily by the Python scraper.

```sql
CREATE TABLE public.stocks (
  ticker text PRIMARY KEY,            -- e.g. 'ATW'
  symbol_id integer,
  name text NOT NULL,                 -- e.g. 'ATTIJARIWAFA BANK'
  sector text,
  status text,
  price double precision,             -- last traded price
  open, high, low, previous_close double precision,
  change_percent double precision,
  volume double precision,
  shares_traded bigint,
  trades_count integer,
  market_cap double precision,
  best_bid, best_ask double precision,
  best_bid_size, best_ask_size integer,
  updated_at timestamptz DEFAULT now()
);
```

**Why `ticker` as primary key instead of an integer id?** Tickers are stable, unique, and the natural way users identify stocks. Using `ticker` as the FK simplifies queries and makes the schema self-documenting.

#### `public.price_snapshots`
Daily OHLCV history for charting. One row per ticker per trading day. Backfilled with one year of data.

```sql
CREATE TABLE public.price_snapshots (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ticker text NOT NULL REFERENCES stocks(ticker),
  trade_date date NOT NULL,
  price double precision NOT NULL,    -- closing price
  open, high, low double precision,
  volume double precision,
  change_percent double precision,
  recorded_at timestamptz DEFAULT now()
);
```

#### `public.profiles`
One row per user, created automatically on signup by the `handle_new_user()` trigger. Stores cash balance.

```sql
CREATE TABLE public.profiles (
  id uuid PRIMARY KEY REFERENCES auth.users(id),
  username text NOT NULL UNIQUE,
  email text,
  cash_balance double precision NOT NULL DEFAULT 100000.00,
  created_at timestamptz DEFAULT now()
);
```

**Why a separate `profiles` table when Supabase already has `auth.users`?** The `auth.users` table is owned by GoTrue and stores authentication-only data (email, password hash). Application data (username, cash balance) belongs in a separate table that the app can read/write directly. Linking via `id REFERENCES auth.users(id)` ensures the profile is automatically deleted if the auth user is deleted (referential integrity).

#### `public.holdings`
Active positions: how many shares the user owns of each stock and at what average price.

```sql
CREATE TABLE public.holdings (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES profiles(id),
  ticker text NOT NULL REFERENCES stocks(ticker),
  shares integer NOT NULL CHECK (shares > 0),
  avg_buy_price double precision NOT NULL,
  updated_at timestamptz DEFAULT now(),
  UNIQUE (user_id, ticker)            -- enforces one row per user+ticker
);
```

**The `UNIQUE (user_id, ticker)` constraint** is critical. It guarantees a user has at most one row per stock, which is what the `ON CONFLICT (user_id, ticker)` upsert in `buy_stock` relies on.

**`avg_buy_price` is the weighted-average effective cost per share** — including the 1% buy fee. This makes the displayed P/L accurate without separate fee tracking.

#### `public.transactions`
Append-only log of every buy and sell.

```sql
CREATE TABLE public.transactions (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES profiles(id),
  ticker text NOT NULL REFERENCES stocks(ticker),
  type text NOT NULL CHECK (type IN ('BUY', 'SELL')),
  shares integer NOT NULL CHECK (shares > 0),
  price double precision NOT NULL,    -- clean execution price (no fee)
  total double precision NOT NULL,    -- actual cash impact (with fee)
  created_at timestamptz DEFAULT now()
);
```

**Why store both `price` and `total`?** `price` records the clean execution price for transparency ("you bought at 540.50 MAD"). `total` records the actual cash that moved (`shares * price * 1.01` for a buy, `shares * price * 0.99` for a sell). Storing both lets the UI show either depending on context.

#### `public.watchlist`
Saved stocks the user wants to track without owning.

```sql
CREATE TABLE public.watchlist (
  user_id uuid NOT NULL REFERENCES profiles(id),
  ticker text NOT NULL REFERENCES stocks(ticker),
  added_at timestamptz DEFAULT now(),
  PRIMARY KEY (user_id, ticker)       -- composite PK prevents duplicates
);
```

**No surrogate `id` column** because the natural key (user + ticker) is sufficient. The composite primary key both indexes and enforces uniqueness.

### 3.2 Row-Level Security (RLS)

RLS is a PostgreSQL feature that filters rows based on the current session's role and identity. With RLS enabled on a table, every query is automatically rewritten to add a `WHERE` clause derived from the active policy. The Android app cannot bypass this — it's enforced at the database level.

**Public tables** (`stocks`, `price_snapshots`): SELECT policy allows the `anon` and `authenticated` roles to read all rows. No INSERT/UPDATE/DELETE policy exists, so the scraper (using `service_role`) is the only thing that can write.

**User-scoped tables** (`profiles`, `holdings`, `transactions`, `watchlist`):
```sql
CREATE POLICY "user owns own row"
ON public.holdings
FOR ALL
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);
```

`auth.uid()` is a PostgreSQL function exposed by Supabase that returns the user ID extracted from the JWT in the request. Two consequences:

1. **The Android app never sends a `user_id` filter.** It just queries `/rest/v1/holdings?select=*` and the database returns only the rows where `auth.uid() = user_id`.
2. **A malicious client cannot read another user's data**, even with a forged request, because the JWT signature is verified by Supabase and `auth.uid()` returns the verified UUID.

### 3.3 The `handle_new_user()` Trigger

When a user signs up, GoTrue inserts a row into `auth.users`. A trigger fires after that insert and creates the corresponding `profiles` row.

```sql
CREATE OR REPLACE FUNCTION public.handle_new_user()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path = public
AS $$
BEGIN
    INSERT INTO public.profiles (id, username, email, cash_balance)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username',
                 'user_' || LEFT(NEW.id::text, 8)),
        NEW.email,
        100000.00
    );
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE LOG 'handle_new_user failed for %: % (sqlstate %)',
              NEW.email, SQLERRM, SQLSTATE;
    RAISE;
END;
$$;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user();
```

Key points:

* **`SECURITY DEFINER`**: the function runs with the privileges of its owner (a privileged role), not the calling role. Without this, the trigger would fail because the anon role doesn't have INSERT permission on `profiles`.
* **`SET search_path = public`**: defends against search-path manipulation attacks on `SECURITY DEFINER` functions. A Supabase lint warning otherwise.
* **`COALESCE` fallback for username**: if metadata doesn't include a username (e.g., a user created via the dashboard), generate one from the user ID prefix. Prevents NOT NULL violations.
* **Default `cash_balance = 100000.00`**: every new user starts with 100,000 MAD simulated cash.
* **Exception handler that logs + re-raises**: if the insert fails, the error appears in Postgres logs with context, and the entire signup transaction rolls back (so no orphan auth.users row exists).

### 3.4 The `buy_stock` RPC

The complete buy logic, executed as a single Postgres function. The Android client calls `POST /rest/v1/rpc/buy_stock` with `{p_ticker, p_shares, p_price}` in the body.

```sql
CREATE OR REPLACE FUNCTION public.buy_stock(
    p_ticker text,
    p_shares integer,
    p_price double precision
) RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_user_id uuid;
    v_fee_rate constant double precision := 0.01;
    v_gross, v_fee, v_total_cost, v_effective_price double precision;
    v_cash_balance double precision;
    v_existing_shares integer;
    v_existing_avg, v_new_avg double precision;
    v_new_shares integer;
    v_transaction_id bigint;
BEGIN
    -- 1. Authentication
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'not_authenticated' USING ERRCODE = '28000';
    END IF;

    -- 2. Validate inputs
    IF p_shares IS NULL OR p_shares <= 0 THEN
        RAISE EXCEPTION 'invalid_shares' USING ERRCODE = '22000';
    END IF;
    IF p_price IS NULL OR p_price <= 0 THEN
        RAISE EXCEPTION 'invalid_price' USING ERRCODE = '22000';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.stocks WHERE ticker = p_ticker) THEN
        RAISE EXCEPTION 'unknown_ticker: %', p_ticker USING ERRCODE = '22000';
    END IF;

    -- 3. Compute totals
    v_gross := p_shares * p_price;
    v_fee := v_gross * v_fee_rate;
    v_total_cost := v_gross + v_fee;
    v_effective_price := p_price * (1.0 + v_fee_rate);

    -- 4. Lock profile, verify cash
    SELECT cash_balance INTO v_cash_balance
    FROM public.profiles WHERE id = v_user_id FOR UPDATE;

    IF v_cash_balance < v_total_cost THEN
        RAISE EXCEPTION 'insufficient_funds: have %, need %',
            v_cash_balance, v_total_cost USING ERRCODE = '22000';
    END IF;

    -- 5. Lock existing holding (prevents race on concurrent buys)
    SELECT shares, avg_buy_price INTO v_existing_shares, v_existing_avg
    FROM public.holdings
    WHERE user_id = v_user_id AND ticker = p_ticker FOR UPDATE;

    -- 6. Compute new weighted-average cost
    IF v_existing_shares IS NULL THEN
        v_new_shares := p_shares;
        v_new_avg := v_effective_price;
    ELSE
        v_new_shares := v_existing_shares + p_shares;
        v_new_avg := ((v_existing_shares * v_existing_avg)
                      + (p_shares * v_effective_price)) / v_new_shares;
    END IF;

    -- 7. Upsert holding
    INSERT INTO public.holdings (user_id, ticker, shares, avg_buy_price, updated_at)
    VALUES (v_user_id, p_ticker, v_new_shares, v_new_avg, now())
    ON CONFLICT (user_id, ticker) DO UPDATE
        SET shares = EXCLUDED.shares,
            avg_buy_price = EXCLUDED.avg_buy_price,
            updated_at = now();

    -- 8. Debit cash
    UPDATE public.profiles
    SET cash_balance = cash_balance - v_total_cost
    WHERE id = v_user_id;

    -- 9. Log transaction
    INSERT INTO public.transactions (user_id, ticker, type, shares, price, total)
    VALUES (v_user_id, p_ticker, 'BUY', p_shares, p_price, v_total_cost)
    RETURNING id INTO v_transaction_id;

    -- 10. Return JSON for the client
    RETURN jsonb_build_object(
        'transaction_id', v_transaction_id,
        'ticker', p_ticker, 'type', 'BUY',
        'shares', p_shares, 'price', p_price,
        'gross', v_gross, 'fee', v_fee, 'total_cost', v_total_cost,
        'new_cash_balance', v_cash_balance - v_total_cost,
        'new_holding_shares', v_new_shares,
        'new_avg_buy_price', v_new_avg
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.buy_stock(text, integer, double precision)
    TO authenticated;
```

**Key concepts to be ready to explain:**

* **`auth.uid()` instead of accepting a `user_id` parameter**: a malicious client can't buy on behalf of another user.
* **`FOR UPDATE` row locks**: prevent two concurrent buys from the same user from racing. If Buy A reads cash=1000 and Buy B reads cash=1000 simultaneously, both could pass the funds check; the lock serializes them.
* **Weighted-average cost basis**: when adding to an existing position, the new average is `(old_shares * old_avg + new_shares * new_price) / total_shares`. This is standard accounting — it gives the correct break-even price.
* **Implicit transaction**: every PL/pgSQL function runs in a single transaction by default. If any step fails (e.g., a constraint violation), all preceding writes are rolled back automatically.
* **Custom SQLSTATEs**: `28000` = invalid authorization, `22000` = data exception. PostgREST surfaces these in the HTTP response, allowing the client to map them to user-friendly messages.

### 3.5 The `sell_stock` RPC

Mirror of `buy_stock` with key differences:

* Verifies the user owns at least the requested shares
* Computes net proceeds as `gross - fee` (fee deducted from proceeds, not added on top)
* Cost basis on remaining shares is unchanged (selling doesn't change `avg_buy_price`)
* If selling all shares (`new_shares = 0`), deletes the holding row entirely instead of updating
* Returns realized P/L: `net_proceeds - (shares * existing_avg)`

---

## 4. Android Project Structure

### 4.1 `build.gradle.kts` (app module)

Defines minimum SDK 24 (Android 7.0 — covers ~98% of devices), target SDK 35 (latest), Java 11 source/target compatibility, and **core library desugaring** (which backports Java 8+ APIs like `java.time.OffsetDateTime` to API 24). Dependencies are pinned to specific versions for reproducible builds.

A particularly important pattern is the use of `BuildConfig` fields fed from `local.properties`:

```kotlin
buildConfigField(
    "String", "SUPABASE_URL",
    "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
)
```

This injects the Supabase URL and anon key at compile time, so they never appear in source-controlled files. `local.properties` is gitignored.

### 4.2 `AndroidManifest.xml`

Declares:
* `INTERNET` permission (required for HTTP calls).
* `CasaTraderApp` as the `android:name` of the application — this is our subclass of `Application`.
* `LoginActivity` as the LAUNCHER activity (the entry point when the app is opened from the home screen).
* `MainActivity`, `SignupActivity`, and `StockDetailActivity` as non-exported activities (only this app can start them).

### 4.3 `CasaTraderApp.java` — the Application class

```java
public class CasaTraderApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TokenStore tokenStore = TokenStore.getInstance(this);
        ApiClient.getInstance(tokenStore);
    }
}
```

**Why a custom Application class?** Two reasons:
1. **Pre-warm singletons**. Initializing `TokenStore` and `ApiClient` in `Application.onCreate()` (called once when the process starts) means the first activity doesn't pay the cost of decrypting EncryptedSharedPreferences or building the OkHttp client. Reduces first-screen latency.
2. **Provides an `Application` context** for any code that needs a `Context` not tied to an Activity (e.g., Repositories shouldn't hold an Activity reference because they outlive Activities).

---

## 5. Data Layer

### 5.1 Models (`data/model/`)

POJOs that mirror the database schema. Every field has `@SerializedName` annotations so Gson can map snake_case JSON to camelCase Java fields. **Nullable numeric columns use boxed types** (`Double`, `Integer`, `Long`) instead of primitives, because `null` would otherwise silently become `0.0` — corrupting data display.

#### `Stock.java`
Mirror of the `stocks` row. All financial fields nullable.

#### `PriceSnapshot.java`
A single OHLCV row. Used by the chart to plot history.

#### `Profile.java`
Current user's profile. `id` is the UUID from `auth.users`.

#### `Holding.java`
A position. Nullable fields handle the "no holding" case gracefully.

#### `HoldingWithStock.java`
A position **with the embedded current stock data** (joined via PostgREST foreign-key embedding). Includes computed methods:
```java
public double currentValue()      { return stock.price * shares; }
public double costBasis()         { return avgBuyPrice * shares; }
public double unrealizedPl()      { return currentValue() - costBasis(); }
public double unrealizedPlPercent() { return (unrealizedPl() / costBasis()) * 100.0; }
```

**Why a separate class instead of putting these methods on `Holding`?** Separation of concerns: `Holding` mirrors exactly what the database has. `HoldingWithStock` represents an enriched view used only on the portfolio screen. Mixing the two would create confusion about what's real data and what's computed.

#### `Transaction.java`
A trade record. `type` is "BUY" or "SELL". `total` is the cash impact (positive value).

#### `WatchlistEntry.java`
A composite of (user_id, ticker, added_at) plus an embedded `Stock` for displaying current price in the watchlist UI.

#### `AuthDtos.java`
Static inner classes for the GoTrue auth API:
* `EmailPasswordRequest`: body for signup/login (`email`, `password`, optional `data` metadata)
* `AuthSession`: response from login/refresh (`access_token`, `refresh_token`, `expires_at`, `user`)
* `AuthUser`: the user sub-object inside `AuthSession`
* `RefreshRequest`: body for refresh (`refresh_token`)

#### `WriteDtos.java`
Static inner classes for write operations:
* `BuyRequest`, `SellRequest`: bodies for the buy/sell RPCs (parameters prefixed `p_` to match the Postgres signature)
* `TradeResult`: response from the RPCs (transaction id, fee breakdown, new balance)
* `TransactionInsert`, `HoldingUpsert`, `CashBalanceUpdate`, `WatchlistInsert`: payload shapes for direct table writes

**Why separate read models and write DTOs?** When inserting into `holdings`, the client doesn't send `id` (auto-generated) or `updated_at` (default). Mixing read and write into one class would require either sending unwanted fields or fragile field-skipping logic. The separation makes intent explicit.

### 5.2 Why Public Fields on POJOs?

Standard Java style says to use private fields with getters/setters. These DTOs use public fields. The reasoning:

* DTOs are **inert data containers** — no behavior, no invariants to protect. Encapsulation isn't adding safety, just ceremony.
* Gson reads/writes via reflection regardless of access modifier.
* The Android style guide and *Effective Java* both carve out "data classes with no behavior" as fine for public fields.

If asked: *"They're inert DTOs, not domain objects with invariants. Public fields are pragmatic; in production with stricter style requirements I'd add Lombok or write the boilerplate."*

---

## 6. Network Layer

### 6.1 `ApiClient.java`

Singleton that owns the Retrofit instance and exposes typed API interfaces. Two key responsibilities:

1. **Build the OkHttp client with the interceptor stack:**
   - `AuthInterceptor` — adds the `apikey` and `Authorization: Bearer <token>` headers
   - `TokenRefreshInterceptor` — catches 401s on `/rest/v1/`, refreshes the JWT, retries
   - `HttpLoggingInterceptor` — logs requests/responses on debug builds only

2. **Provide a `refreshSync()` method** that synchronously calls `/auth/v1/token?grant_type=refresh_token`, updates `TokenStore`, and returns success/failure. Used by `TokenRefreshInterceptor` and by `LoginActivity` for proactive refresh on app start.

**The dual-client design:** The class actually builds **two** Retrofit instances. The "main" one has the full interceptor stack. The "bare" one has only `AuthInterceptor` and is used exclusively for refresh calls. Reason: if a refresh request itself returned a 401 and was caught by `TokenRefreshInterceptor`, it would recursively try to refresh — infinite loop. The bare client physically cannot recurse because it doesn't have the refresh interceptor.

### 6.2 `AuthInterceptor.java`

Always adds `apikey: <anon_key>`. Adds `Authorization: Bearer <jwt or anon_key>` based on whether `TokenStore` has a user JWT.

```java
String userToken = tokenStore.getAccessToken();
String bearer    = userToken != null ? userToken : anonKey;
Request authenticated = original.newBuilder()
        .header("apikey", anonKey)
        .header("Authorization", "Bearer " + bearer)
        .build();
```

**Why both `apikey` and `Authorization`?** Supabase requires the `apikey` header on every request to identify the project. The `Authorization` header carries either:
* The anon key (when the user is not logged in) — gives access to public tables like `stocks`
* The user JWT (after login) — `auth.uid()` returns the user's UUID, enabling RLS-scoped queries

This dual-mode design means **the app uses the same Retrofit instance before and after login**. The interceptor automatically picks up the new token from `TokenStore` after `saveSession()` is called, with zero changes to API code.

### 6.3 `TokenRefreshInterceptor.java`

The 401-recovery mechanism. When any `/rest/v1/` call returns 401:

1. Capture the token that was used for the failing request.
2. Acquire a lock (`synchronized (refreshLock)`).
3. Check if `TokenStore.getAccessToken()` is still the same token. If a different thread already refreshed while we were on the wire, just retry.
4. Otherwise, call `refresher.refresh()` (which calls `ApiClient.refreshSync()` via a method reference).
5. If refresh succeeded, retry the original request. The `AuthInterceptor` will pick up the new token.
6. If refresh failed, clear `TokenStore` (forces re-login on next interaction) and let the original 401 propagate.

**Key design: the lock prevents concurrent refresh storms.** If 5 requests fire simultaneously and all get 401, only one refresh runs; the other 4 see that the token has changed and retry directly. Without this, you'd hit the `auth.refresh_tokens` table 5 times concurrently, and 4 of them would fail because each refresh consumes (rotates) the previous refresh token.

### 6.4 `AuthApi.java`

Retrofit interface for `/auth/v1/`. Three endpoints:
```java
@POST("auth/v1/signup") Call<AuthSession> signup(@Body EmailPasswordRequest body);
@POST("auth/v1/token")  Call<AuthSession> login(@Query("grant_type") String, @Body EmailPasswordRequest);
@POST("auth/v1/token")  Call<AuthSession> refresh(@Query("grant_type") String, @Body RefreshRequest);
```

### 6.5 `SupabaseApi.java`

Retrofit interface for `/rest/v1/` and `/rest/v1/rpc/`. Methods include:
* `getAllStocks` — list stocks ordered by name
* `getStockSingle` — one stock with `Accept: application/vnd.pgrst.object+json` (returns object, not array)
* `getPriceHistory` — snapshots for one ticker
* `getMyProfile`, `getMyProfileSingle` — current user's profile (RLS-scoped)
* `getMyHoldingsWithStock` — holdings with embedded stock data (PostgREST FK embedding)
* `getMyHolding` — single holding by ticker
* `getMyTransactions` — transaction history
* `getMyWatchlist`, `getWatchlistEntry` — watchlist queries
* `addToWatchlist`, `removeFromWatchlist` — watchlist mutations
* `buyStock`, `sellStock` — RPC calls

**PostgREST embedding** is worth explaining. Calling:
```
GET /rest/v1/holdings?select=*,stocks(ticker,name,price,change_percent)
```
makes PostgREST detect the foreign key `holdings_ticker_fkey` and embed the matching `stocks` row inline. This means the portfolio screen gets holdings + current prices in one round-trip instead of N+1 queries.

### 6.6 Repositories (`data/repo/`)

A repository wraps API calls and exposes a clean callback interface to the ViewModel. It hides Retrofit, JSON, and HTTP details.

#### `AuthRepository.java`
Methods: `signup(email, password, username, callback)`, `login(email, password, callback)`, `logout()`. Internally calls `AuthApi`, parses the response, persists tokens to `TokenStore` on success, surfaces user-friendly errors on failure.

#### `StockRepository.java`
`getAllStocks(callback)`, `getStock(ticker, callback)`, `getPriceHistory(ticker, days, callback)`. Used by `StockListViewModel` and `StockDetailViewModel`.

#### `TradeRepository.java`
`buy(ticker, shares, price, callback)`, `sell(ticker, shares, price, callback)`, `getProfile(callback)`, `getHolding(ticker, callback)`. Includes `parsePostgresError()` which extracts the `message` field from PostgREST error JSON and `humanize()` which maps SQL error codes to UI strings (e.g., `"insufficient_funds"` → `"Not enough cash for this purchase."`).

#### `PortfolioRepository.java`
`getProfile(callback)`, `getHoldings(callback)`. The holdings call uses the embedded select to pull current stock prices in one request.

#### `TransactionsRepository.java`
`getAll(callback)`. Returns transactions ordered by `created_at` descending.

#### `WatchlistRepository.java`
`getAll(callback)`, `isWatched(ticker, callback)`, `add(ticker, callback)`, `remove(ticker, callback)`. The remove uses a DELETE with `?ticker=eq.X` filter — PostgREST refuses unbounded deletes for safety.

**Why repositories?** They serve three purposes:
1. **Single source of truth** for a given data type. The portfolio screen and the trade dialog both fetch the user's profile; both go through `getProfile()`. If the API path changes, one place to update.
2. **Hide implementation details** from the ViewModel. The ViewModel doesn't know whether data comes from network, cache, or DB — just calls a method and gets a callback.
3. **Testability**. A repository can be mocked in unit tests; ViewModels can be tested without spinning up a real HTTP stack.

---

## 7. Authentication and JWT Lifecycle

This is one of the most architecturally interesting parts of the app and a likely topic of examination.

### 7.1 The `TokenStore.java` Singleton

Wraps `EncryptedSharedPreferences`. Provides methods:
* `saveSession(accessToken, refreshToken, expiresAt, userId)` — writes everything atomically
* `getAccessToken()`, `getRefreshToken()`, `getUserId()`, `getExpiresAt()`
* `isLoggedIn()` — returns true if an access token is stored
* `isAccessTokenExpired()` — returns true if `now >= expiresAt - 30 seconds` (30-second safety margin to avoid mid-flight expiry)
* `clear()` — wipes everything (logout)

**Why EncryptedSharedPreferences?** Plain SharedPreferences stores values in cleartext XML on disk. On a rooted device (or with physical access), an attacker can read JWTs. EncryptedSharedPreferences encrypts both keys and values using AES, with the encryption key itself stored in the Android Keystore (hardware-backed on most modern devices).

**Why singleton with double-checked locking?** EncryptedSharedPreferences initialization is expensive (involves Keystore round-trips). One instance per process, lazily initialized, thread-safe.

### 7.2 The Signup Flow

1. User enters email, password, username on `SignupActivity`.
2. `AuthViewModel.signup()` validates inputs, switches state to LOADING.
3. `AuthRepository.signup()` calls `AuthApi.signup()` with `EmailPasswordRequest{email, password, data: {username}}`.
4. GoTrue creates a row in `auth.users`. The `on_auth_user_created` trigger fires `handle_new_user()`, which inserts into `profiles`.
5. GoTrue returns the `AuthSession` (access token, refresh token, user object).
6. Repository writes tokens to `TokenStore`.
7. ViewModel emits SUCCESS state.
8. Activity observes the state and navigates to `MainActivity`.

### 7.3 The Login Flow

Same as signup but calls `/auth/v1/token?grant_type=password`. No trigger involvement (the user already exists).

### 7.4 The Refresh Flow

Two triggers cause a refresh:

**Reactive (mid-session expiry):**
1. App makes any `/rest/v1/` call with the current token.
2. Server returns 401 because the token expired.
3. `TokenRefreshInterceptor` catches the 401.
4. Calls `ApiClient.refreshSync()` → `/auth/v1/token?grant_type=refresh_token`.
5. Server returns a new access token + new refresh token (refresh tokens are rotated for security).
6. Repository writes new tokens to `TokenStore`.
7. Interceptor retries the original request, which now succeeds.
8. **The user sees nothing.** No flicker, no error, no re-login.

**Proactive (cold start with expired token):**
1. App launches, `LoginActivity.onCreate()` runs.
2. Checks `TokenStore.isLoggedIn()` and `isAccessTokenExpired()`.
3. If logged in but expired, shows a brief spinner and calls `refreshSync()` on a background thread.
4. On success, navigates straight to `MainActivity`. On failure, shows the login form.

### 7.5 The Logout Flow

1. User taps "Log out" on the portfolio screen.
2. Confirmation dialog appears ("Are you sure?").
3. On confirm: `TokenStore.clear()` wipes encrypted prefs.
4. Start `LoginActivity` with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` to clear the back stack.
5. Finish current activity.

---

## 8. UI Layer — Screens, Fragments, ViewModels

The app uses a **single-activity architecture**: `MainActivity` hosts a `NavHostFragment` that swaps four primary fragments based on the bottom-nav selection. Two activities exist outside this host: `LoginActivity` (entry point) and `StockDetailActivity` (secondary destination, opened from anywhere a stock is tappable).

### 8.1 `MainActivity` and the Navigation Component

**Layout** (`activity_main.xml`):
```
ConstraintLayout
├── MaterialToolbar (top)
├── FragmentContainerView with app:navGraph + app:defaultNavHost (middle)
└── BottomNavigationView (bottom)
```

**Code** wires three things together:
```java
NavController nav = navHost.getNavController();
AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
    R.id.stockListFragment, R.id.watchlistFragment,
    R.id.portfolioFragment, R.id.transactionsFragment).build();
NavigationUI.setupWithNavController(binding.toolbar, nav, appBarConfig);
NavigationUI.setupWithNavController(binding.bottomNav, nav);
```

`NavigationUI.setupWithNavController(toolbar, …)` makes the toolbar title automatically reflect the current destination's `android:label`. `NavigationUI.setupWithNavController(bottomNav, …)` connects bottom-nav clicks to navigation actions, **provided the menu item IDs match the fragment IDs in the nav graph exactly**. This is a strict requirement and a common bug source.

**Navigation graph** (`res/navigation/nav_main.xml`): declares the four fragment destinations with `app:label`.

**Bottom nav menu** (`res/menu/menu_bottom_nav.xml`): the four tabs with their icons.

**Why single-activity?** Modern Android best practice. Fragments share the host activity's lifecycle, which means they survive better through configuration changes, and the nav component can manage their back stack coherently. Multi-activity navigation requires manual intent flag management and back-stack mental gymnastics.

### 8.2 The Auth Screens

#### `LoginActivity.java` and `activity_login.xml`

Layout: vertically centered form with logo + title at top, email/password TextInputLayouts in the middle, primary Login button, and a "Don't have an account? Sign up" text button at the bottom.

Logic:
* On create, checks `TokenStore.isLoggedIn()` to potentially skip the form (auto-login).
* If token expired but refresh exists, attempts a silent refresh.
* If not logged in, displays the form.
* On Login button click, delegates to `AuthViewModel.login()`.
* Observes UI state: LOADING shows progress, SUCCESS navigates to MainActivity, ERROR shows a Toast.

#### `SignupActivity.java` and `activity_signup.xml`

Same structure with a username field added. On success, navigates to MainActivity (skipping login since signup auto-logs-in).

#### `AuthViewModel.java`

State machine with four states (`IDLE`, `LOADING`, `SUCCESS`, `ERROR`) wrapped in a `UiState` object. Exposes `LiveData<UiState>`. Methods: `login(email, password)`, `signup(email, password, username)`, `resetState()`.

**Why a shared ViewModel for both login and signup?** Both flows have identical state shape (idle/loading/success/error). Two separate ViewModels would duplicate code with no benefit.

#### `AuthViewModelFactory.java`

Implements `ViewModelProvider.Factory` because `AuthViewModel`'s constructor takes an `AuthRepository`. Without a custom factory, `ViewModelProvider` would try to invoke a no-arg constructor and fail.

### 8.3 Stock List

#### `StockListFragment.java` and `fragment_stock_list.xml`

Layout: a search TextInputLayout at top, a SwipeRefreshLayout wrapping a RecyclerView in the middle, with overlapping ProgressBar and "no results" TextView for loading and empty states.

Logic:
* On view created, builds the adapter with a click listener that opens `StockDetailActivity`.
* Wires search box → `viewModel.setSearchQuery()` (filters in-memory, no network).
* Wires SwipeRefresh → `viewModel.load()`.
* Observes UI state: LOADING shows progress, SUCCESS submits filtered list to adapter, ERROR shows Toast.
* On `onResume`, no auto-reload (data doesn't change frequently and the cache is fine).

#### `StockListViewModel.java`

Owns:
* `MutableLiveData<UiState>` exposed as `LiveData<UiState>`
* `List<Stock> allStocks` — the unfiltered cached list
* `String currentQuery` — current search filter

Methods:
* `load()` — fetches from repository, populates `allStocks`, emits filtered list
* `setSearchQuery(query)` — re-filters cached list, emits new state. **Critical: no network call on each keystroke.**

#### `StockListAdapter.java`

Extends `ListAdapter<Stock, VH>` with a `DiffUtil.ItemCallback`. `submitList(newList)` triggers the framework to compute the minimum set of insertions, deletions, and item changes, then animates the transitions automatically. Better than `notifyDataSetChanged()` because it avoids redrawing unchanged rows.

Renders each row with ticker (bold), name (secondary), price (bold right-aligned), change percent (color-coded green/red/gray).

### 8.4 Stock Detail

#### `StockDetailActivity.java` and `activity_stock_detail.xml`

Stays an Activity (not a fragment) because it's a secondary destination and benefits from its own back-stack entry.

Layout in a NestedScrollView:
* Header (ticker, name, price, change%)
* MaterialCardView containing the LineChart
* MaterialCardView with quote details (8 fields in a 2x4 grid using nested LinearLayouts)
* Buy + Sell buttons row
* "Add to watchlist" text button with star icon

Logic:
* `newIntent(ctx, ticker)` factory creates the Intent with the ticker as an extra. Avoids string-key bugs at call sites.
* On create, builds `StockDetailViewModel`, configures the chart, loads watchlist state.
* Observes `stockState` and `historyState` independently (two parallel loads).
* Buy/Sell buttons open `TradeDialog` as a `BottomSheetDialogFragment`.
* Watchlist button toggles state with optimistic UI update and rollback on error.

#### `StockDetailViewModel.java`

Two parallel LiveData streams: `stockState` (the live stock data) and `historyState` (the year of price snapshots). Exposed independently because the chart can render before the header finishes loading and vice-versa.

#### `TradeDialog.java` and `dialog_trade.xml`

A `BottomSheetDialogFragment` reused for both buy and sell. Mode is passed as a fragment argument.

Layout:
* Header ("Buy ATW" / "Sell ATW")
* Subheader ("Current price 540.50 MAD")
* "Available: X MAD" (cash for buy) or "You own: N shares" (for sell)
* Shares input (TextInputLayout with number input)
* Breakdown card: gross / fee (1%) / total
* Inline error TextView (hidden by default)
* Confirm button (disabled until valid input)
* Loading ProgressBar (during submit)

Logic:
* On view created, parses arguments (mode, ticker, price), wires text watcher on shares input.
* Loads context (profile for buy, holding for sell) to populate the "Available" line.
* `revalidate()` runs on every keystroke — checks cash/share constraints, enables/disables confirm button, shows inline error.
* On submit, calls `repo.buy()` or `repo.sell()`, dismisses on success and propagates `TradeResult` to the host activity via the `OnTradeCompletedListener` interface.

**Why a BottomSheet instead of a full activity?** Modal modal-on-top patterns work better for short interactions. The user keeps the chart visible behind the sheet, and dismissing is a swipe rather than a back button. Standard Material 3 pattern for "configure and confirm" flows.

### 8.5 Portfolio

#### `PortfolioFragment.java` and `fragment_portfolio.xml`

Layout in a NestedScrollView:
* Greeting ("Hello, fahd")
* Summary card: total value (display-text size), total P/L colored, cash + holdings split
* "Holdings" section header
* MaterialCardView wrapping a RecyclerView of holdings (with `nestedScrollingEnabled=false` so it scrolls with the parent NestedScrollView)
* Logout text button at the bottom in error red

Logic:
* On view created, builds ViewModel, adapter (with click listener opening StockDetailActivity).
* Observes UiState; on SUCCESS, calls `renderSummary()` and `submitList()`.
* `renderSummary()` computes total value (cash + sum of holding values), total P/L (totalValue - 100,000), formats currency, applies green/red coloring.
* On `onResume`, reloads — covers the case of returning from a buy/sell on the detail screen.
* Logout button shows MaterialAlertDialog confirmation, then clears tokens and starts LoginActivity.

#### `PortfolioViewModel.java`

Has the most interesting state logic in the app: **two parallel loads merged into one UiState.**

Methods:
* `load()` — kicks off both `getProfile()` and `getHoldings()` concurrently.

Internal coordination:
* `profileLoaded` and `holdingsLoaded` flags
* `tryEmit()` — only emits SUCCESS when both flags are true; if either errored, emits ERROR

This pattern is sometimes called a "barrier" — wait for N async operations before continuing.

#### `HoldingsAdapter.java`

Renders each holding as: ticker (bold), "N shares · avg X.XX" (secondary), current value (bold right-aligned), unrealized P/L (color-coded). Uses computed methods on `HoldingWithStock` for value and P/L calculations.

### 8.6 Transactions

#### `TransactionsFragment.java` and `fragment_transactions.xml`

Simple list screen: SwipeRefreshLayout wrapping RecyclerView, with overlapping loading/empty states.

#### `TransactionsViewModel.java`

Single `load()` that calls the repository, emits LOADING / SUCCESS / ERROR.

#### `TransactionsAdapter.java`

Each row:
* BUY/SELL pill (colored background, white text, rounded full corners)
* Ticker
* "10 shares @ 540.50 MAD" subtitle
* Date in local timezone (`MMM d, HH:mm` format using `OffsetDateTime` parsing)
* Cash impact: "−1,234.56" (red) for buys, "+9,876.54" (green) for sells

The date parsing uses `OffsetDateTime.parse()` (handles the ISO-8601 with offset format Postgres returns) and `.atZoneSameInstant(ZoneId.systemDefault())` to convert to the device's timezone before formatting. This works on minSdk 24 because of core library desugaring.

### 8.7 Watchlist

#### `WatchlistFragment.java` and `fragment_watchlist.xml`

Same shape as the transactions screen but with an X button per row to remove, and a different empty state message.

#### `WatchlistViewModel.java`

Has a notable feature: **optimistic remove**. When the user taps X:
1. Remove the entry from the local list immediately and emit the new state (UI updates without waiting).
2. Send the DELETE to the server.
3. On error, refetch (rolls back the optimistic change).

This makes the UI feel instant. The same pattern is used on the detail screen for the watchlist toggle.

#### `WatchlistAdapter.java`

Renders ticker, name, price, change% (like the stock list) plus a small icon button on the right with the close icon. Two click listeners on each row: one for opening the detail, one for removing.

---

## 9. Design System and Resources

### 9.1 Color Palette

Custom Material 3 palette defined in `res/values/colors.xml` (light) and `res/values-night/colors.xml` (dark). Charcoal surfaces with sky-blue/indigo accents for primary actions, with green/red flipped between modes for proper contrast.

The palette uses Material 3 token names: `md_theme_primary`, `md_theme_surface`, `md_theme_surfaceContainerLowest`, etc. Themes (`themes.xml`) map these tokens to the standard MaterialComponents attributes (`colorPrimary`, `colorSurface`, etc.) so they're accessible as `?attr/colorPrimary` in any layout.

App-specific colors:
* `gain_green` — positive P/L
* `loss_red` — negative P/L  
* `neutral_gray` — zero/no data state

These have separate values in `values-night/` so they remain readable on both backgrounds.

### 9.2 Themes

`Theme.CasaTrader` extends `Theme.Material3.DayNight.NoActionBar`:
* `DayNight` automatically picks the light or dark colors based on the system setting.
* `NoActionBar` because the toolbar is added manually inside layouts, not provided by the theme. Allows transparent or custom toolbar styling.

Status bar is transparent with `windowLightStatusBar` set to true in light mode and false in dark mode (controls icon color).

### 9.3 Drawables

Vector drawables for icons (so they scale to any density without bitmap variants):
* `ic_arrow_back.xml`, `ic_close.xml`, `ic_history.xml`, `ic_portfolio.xml`, `ic_search.xml`, `ic_star_filled.xml`, `ic_star_outline.xml`, `ic_trending_up.xml`

Shape drawables for backgrounds:
* `bg_pill_buy.xml`, `bg_pill_sell.xml` — rounded pill backgrounds for transaction type indicators

### 9.4 Styles

`res/values/styles.xml` defines reusable styles:
* `QuoteRow`, `QuoteCell`, `QuoteLabel`, `QuoteValue` — used in the quote details grid on the stock detail screen and the cash/holdings split on the portfolio screen.
* `TradeBreakdownRow`, `TradeBreakdownLabel`, `TradeBreakdownValue` — used in the trade dialog's price breakdown.

**Why styles instead of inlining the attributes?** DRY. Eight quote cells with identical layout params would repeat 8 lines × ~4 attributes each. Pulled into a style, the row becomes one line.

---

## 10. End-to-End User Flows

### 10.1 Cold Start with Valid Session

1. Process starts → `CasaTraderApp.onCreate()` initializes `TokenStore` and `ApiClient`.
2. Android starts `LoginActivity` (the LAUNCHER activity).
3. `LoginActivity.onCreate()` checks `tokenStore.isLoggedIn()` → true.
4. Checks `tokenStore.isAccessTokenExpired()` → false (still valid).
5. Calls `goHome()` → starts `MainActivity` with NEW_TASK | CLEAR_TASK flags, finishes self.
6. `MainActivity` shows the bottom nav with Stocks tab selected.
7. `StockListFragment.onViewCreated()` calls `viewModel.load()`.
8. ViewModel calls `StockRepository.getAllStocks()`.
9. Retrofit fires GET `/rest/v1/stocks?select=*&order=name.asc`.
10. `AuthInterceptor` adds `apikey: <key>` and `Authorization: Bearer <jwt>`.
11. Supabase returns the 113 stocks; RLS allows the read because the public-read policy is in place.
12. Gson deserializes into `List<Stock>`.
13. Repository invokes the callback with the list.
14. ViewModel emits SUCCESS with the list.
15. Fragment's observer receives the state and calls `adapter.submitList()`.
16. RecyclerView renders the rows.

### 10.2 Buy Trade

1. User on stock detail screen for ATW. Taps **Buy**.
2. `openTradeDialog(BUY)` reads the current price from `viewModel.getStockState().getValue().stock.price`.
3. `TradeDialog.newInstance(BUY, "ATW", 540.50)` shows the BottomSheet.
4. Dialog calls `repo.getProfile()` to populate "Available cash".
5. User types `10` in the shares field.
6. Text watcher computes gross (5,405), fee (54.05), total (5,459.05) and updates the breakdown.
7. Validation passes (cash > total). Confirm button enables.
8. User taps **Confirm purchase**.
9. `repo.buy("ATW", 10, 540.50)` calls `POST /rest/v1/rpc/buy_stock` with body `{p_ticker, p_shares, p_price}`.
10. PostgREST invokes `buy_stock(...)`. The function:
    - Verifies `auth.uid()` returns the user's UUID
    - Validates inputs
    - Locks the profile row, checks cash >= 5,459.05
    - Locks any existing ATW holding (none in this case)
    - Computes new shares = 10, new avg = 540.50 × 1.01 = 545.905
    - Inserts holding row
    - Decrements cash from 100,000 to 94,540.95
    - Inserts a transaction row with `type=BUY, total=5459.05`
    - Returns a JSON object with the trade summary
11. PostgREST returns 200 with the JSON body.
12. Repository invokes `cb.onSuccess(tradeResult)`.
13. Dialog shows a Toast ("Bought 10 ATW for 5,459.05 MAD") and dismisses.
14. The `OnTradeCompletedListener` callback fires on `StockDetailActivity`, which calls `viewModel.loadStock()` to refresh the displayed price.

### 10.3 Token Refresh

1. User has been logged in for over an hour. Taps a tab.
2. Tab fragment fires GET `/rest/v1/...`.
3. Server returns 401 because the JWT expired.
4. `TokenRefreshInterceptor` catches the 401.
5. Acquires the refresh lock.
6. Calls `ApiClient.refreshSync()`, which fires POST `/auth/v1/token?grant_type=refresh_token` via the **bare** client (no refresh interceptor — can't recurse).
7. Server returns 200 with new tokens; `TokenStore.saveSession()` persists them.
8. Refresh succeeds; interceptor releases the lock and retries the original request via `chain.proceed(request.newBuilder().build())`.
9. New request goes through `AuthInterceptor`, which reads the **new** token from `TokenStore`.
10. Request succeeds with 200. User sees the data with no indication anything happened.

---

## 11. Likely Examination Questions and Model Answers

### Architecture

**Q: Why MVVM and not MVC or MVP?**
MVVM with `ViewModel` is lifecycle-aware. The ViewModel survives configuration changes (rotation, dark mode toggle), so cached data and in-flight requests aren't lost. `LiveData` automatically stops emitting when an observer's lifecycle is paused, preventing memory leaks. MVC mixes view and controller in the Activity, making Activities huge. MVP separates concerns but doesn't solve the configuration-change problem.

**Q: Why Fragments instead of Activities for the bottom nav?**
Single-activity architecture is the modern Android best practice. Fragments share the host activity's lifecycle, the `NavController` manages a coherent back stack, and tab switching is smooth (the previous fragment's view is destroyed but the ViewModel survives, so re-selecting a tab is instant). Multi-activity navigation requires manual intent flag management and breaks animations between tabs.

**Q: Why is `StockDetailActivity` still an Activity?**
It's a secondary destination, not part of the primary navigation. Keeping it as an Activity means:
1. It gets its own back-stack entry, so the system back button returns to whichever fragment opened it (StockList, Watchlist, Portfolio, or History).
2. It can be opened from anywhere a stock is tappable, with a clean `Intent` API (`StockDetailActivity.newIntent(context, ticker)`).
3. Its lifecycle is independent of the bottom-nav tabs.

### Data Flow

**Q: How does data flow from the database to the screen?**
`Database → PostgREST → OkHttp → Retrofit → Gson → Repository → ViewModel → LiveData → Fragment observer → Adapter → RecyclerView`. Each layer has a single responsibility: PostgREST exposes tables, Retrofit declares typed HTTP interfaces, Repository hides networking from ViewModel, ViewModel exposes immutable UI state, Fragment renders.

**Q: What is `LiveData` and why use it?**
LiveData is an observable data holder that's lifecycle-aware. When the observer's `Lifecycle` is in STARTED or RESUMED state, LiveData delivers updates; when the lifecycle is destroyed, LiveData stops emitting and releases the observer. This eliminates a whole class of memory leaks and the "callback fired on a destroyed Activity" crash.

**Q: What is `DiffUtil`?**
A utility that computes the minimal set of insertions, deletions, and item changes between two lists. `ListAdapter.submitList(newList)` uses DiffUtil to update the RecyclerView with proper animations, instead of redrawing every row via `notifyDataSetChanged()`.

### Networking and Auth

**Q: Why two HTTP headers (`apikey` and `Authorization`)?**
Supabase requires both. `apikey` identifies the project. `Authorization: Bearer <token>` carries the user's identity. Before login, both contain the anon key (read-only public access). After login, `apikey` stays the anon key, `Authorization` becomes the user JWT, which RLS uses via `auth.uid()` to scope queries.

**Q: How does JWT refresh work?**
When any `/rest/v1/` call returns 401, the `TokenRefreshInterceptor` synchronously calls `/auth/v1/token?grant_type=refresh_token` with the stored refresh token, gets new tokens, persists them, and retries the original request. Concurrent 401s are coalesced by a synchronized lock so only one refresh runs at a time. On refresh failure, tokens are cleared and the user is forced back to login.

**Q: Why a separate "bare" client for the refresh call?**
To prevent infinite loops. If the refresh call itself were intercepted by `TokenRefreshInterceptor`, a 401 from the refresh would trigger another refresh, and so on. The bare client physically lacks the refresh interceptor.

**Q: Why EncryptedSharedPreferences?**
Plain SharedPreferences stores values in cleartext XML on disk. On a rooted device, an attacker could read JWTs and impersonate the user. EncryptedSharedPreferences encrypts both keys and values using AES, with the encryption key in the Android Keystore (hardware-backed on most modern devices).

### Backend

**Q: What is Row-Level Security (RLS)?**
A PostgreSQL feature that filters rows based on the active session's identity. With RLS enabled and a policy like `USING (auth.uid() = user_id)`, every query on the table is automatically rewritten to add `WHERE auth.uid() = user_id`. The Android app cannot bypass this — it's enforced at the database level. Even if a malicious client sends `?user_id=eq.<other_user>`, RLS still filters the result to only the JWT's user.

**Q: Why server-side trade logic in a Postgres function?**
Atomicity. A trade involves four writes: cash debit, holding upsert, transaction insert, plus a fund check. If these run as separate REST calls from the client, a network failure between steps could leave the database inconsistent (e.g., cash debited but holding not updated). A Postgres function runs in a single transaction — all writes succeed or all are rolled back. Plus, putting `auth.uid()` checks server-side prevents a malicious client from buying on behalf of another user.

**Q: What is `SECURITY DEFINER`?**
A function modifier that makes the function run with the privileges of its owner instead of the calling role. The `handle_new_user()` trigger needs SECURITY DEFINER because it inserts into `profiles`, but the calling role during signup (`anon`) doesn't have INSERT permission on that table. Without it, the trigger fails. Same reason applies to `buy_stock` and `sell_stock` — they need to read/write `holdings`, `profiles`, and `transactions` regardless of the calling role's specific permissions.

**Q: What is `FOR UPDATE` and why is it used in `buy_stock`?**
A row-level lock that prevents concurrent transactions from reading the same row. If the user fires two buys simultaneously, without `FOR UPDATE` both transactions could read the same cash balance and both pass the funds check, then both subtract the cost — overdrawing the account. `FOR UPDATE` serializes them: the second transaction waits until the first commits, then sees the updated balance.

**Q: What is the weighted-average cost basis and how is it computed?**
When adding to an existing position, the new average cost per share is `(old_shares × old_avg + new_shares × new_price) / total_shares`. This is the standard accounting formula for cost basis after multiple purchases at different prices. It gives the correct break-even price: selling all shares at exactly the avg returns exactly the total amount paid (excluding fees).

### Specific Features

**Q: How does the app handle the 1% fee?**
On a buy, `total_cost = shares × price × 1.01`. The user's cash decreases by `total_cost`; the holding's `avg_buy_price` is set to the effective rate (`price × 1.01`). On a sell, `net_proceeds = shares × price × 0.99`. The user's cash increases by `net_proceeds`. The fee is therefore deducted from the proceeds, not added separately. Round-trip cost is `1.01 × 1.01 ≈ 2.02%`, meaning a stock has to gain ~2% before the user breaks even.

**Q: How does the search work? Does it hit the server on every keystroke?**
No. The stock list fetches all 113 stocks once on screen entry and caches them in the ViewModel. The search input is wired to `viewModel.setSearchQuery(query)`, which filters the cached list in memory and emits a new `UiState` with the filtered subset. Zero network roundtrips during typing.

**Q: How is the chart on the stock detail screen rendered?**
Uses MPAndroidChart's `LineChart`. The repository fetches up to 365 `price_snapshots` rows for the ticker, ordered by date ascending. The activity converts them into `Entry(x_index, price)` data points, builds a `LineDataSet` with cubic Bezier interpolation, and applies a fill gradient. The X-axis labels are formatted as `MM-DD` via a custom `ValueFormatter`. The line color flips green/red based on whether the year ends higher or lower than it started.

**Q: What happens if a user signs up with an email that already exists?**
GoTrue returns a 422 with `{"msg": "User already registered"}`. The repository's `parseError` method extracts and displays the message. The trigger never fires, no `profiles` row is inserted, no orphan data is created.

**Q: What's the "optimistic UI" pattern in the watchlist?**
When the user toggles the watchlist button or removes an item, the UI updates immediately (before the network call completes). If the call eventually succeeds, nothing changes — the user already saw the result. If it fails, the change is rolled back (the button reverts, or the list is re-fetched). This makes the app feel instant while still being correct in failure cases.

### Resources

**Q: What is View Binding and why use it instead of `findViewById`?**
View Binding generates a binding class for each layout at compile time, with typed fields for every `android:id` in the layout. Instead of `(TextView) findViewById(R.id.tickerTv)`, you write `binding.tickerTv` — fully typed, null-safe, and a compile error if the ID changes. Eliminates an entire class of runtime crashes (`ClassCastException`, `NullPointerException`).

**Q: What is `core library desugaring`?**
A Gradle feature that backports Java 8+ APIs (like `java.time.OffsetDateTime`, `java.util.stream`) to older Android versions. Without it, those APIs would be unavailable on minSdk < 26. This app uses minSdk 24 with desugaring so the code can use `OffsetDateTime.parse()` for transaction date parsing.

**Q: Why are there separate `values/` and `values-night/` resource folders?**
Android automatically selects the appropriate folder based on the system's day/night setting. Light-mode colors live in `values/colors.xml`; dark-mode overrides live in `values-night/colors.xml`. Same pattern works for any resource type (drawables, themes, strings), enabling proper light/dark theming with no runtime branching.

---

## Final Notes

The app is approximately **40 Java files**, **15 layout files**, and **10 resource files** of XML configuration, plus **3 SQL migrations** for the backend (the `handle_new_user` trigger, `buy_stock`, `sell_stock`).

The most intellectually dense parts of the project, in order of likelihood to come up in examination:

1. **The trade RPC functions** — atomicity, RLS via `auth.uid()`, weighted-average cost basis, row locks.
2. **The interceptor stack** — dual-mode auth, refresh logic, concurrency lock, the dual-client trick.
3. **MVVM with LiveData** — why this pattern, configuration-change survival, observer lifecycle awareness.
4. **Single-activity architecture** — why fragments, NavigationUI, the difference between primary destinations (in the bottom nav) and secondary (StockDetailActivity).

If a question is asked about something in the code that this document doesn't cover, the answer is almost always one of:
* "Convention/standard pattern from the official Android documentation."
* "Avoiding a common bug class (memory leak, race condition, configuration loss)."
* "Reducing latency or network roundtrips."

Reading the source alongside this report should be sufficient to answer essentially any question about the project.
