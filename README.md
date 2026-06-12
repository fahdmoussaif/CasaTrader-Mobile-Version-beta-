# CasaTrader

CasaTrader is a modern Android application for trading and managing a portfolio of stocks listed on the **Casablanca Stock Exchange (BVC)**. It pairs a polished Material 3 dark interface with a Supabase backend that handles authentication, market data, and atomic trade execution.


## 🚀 Key Features

*   **Live BVC Stock Data**: Browse all 113 listed stocks with current prices, daily change, and full quote details (open/high/low/volume/market cap).
*   **Interactive Price Chart**: Year-long historical price chart on every stock detail screen, powered by MPAndroidChart.
*   **Simulated Trading with Realistic Fees**: Execute BUY and SELL orders against a 100,000 MAD starting balance with a 1% commission per side, mirroring real Moroccan brokerage costs.
*   **Atomic Server-Side Execution**: Trades are processed by a Postgres RPC function that validates funds, updates holdings, debits cash, and logs the transaction in a single transaction — no client-side race conditions.
*   **Portfolio Dashboard**: Total portfolio value, cash vs. holdings split, and per-position unrealized P/L.
*   **Watchlist**: Save stocks of interest with one-tap toggle from the detail screen.
*   **Transaction History**: Chronological log of every trade with cash impact and timezone-aware timestamps.
*   **Secure Authentication**: Full email/password signup and login flow via Supabase Auth, with JWTs stored in `EncryptedSharedPreferences` and silent token refresh on expiry.
*   **Material 3 Design**: Custom dark theme with charcoal surfaces and indigo accents, designed for an evening-trading-session feel.

## 🛠️ Tech Stack

*   **Language**: Java
*   **Architecture**: MVVM with `LiveData`
*   **Backend**: Supabase (PostgreSQL · PostgREST · GoTrue Auth)
*   **Networking**: Retrofit, OkHttp, Gson
*   **UI**: Material 3, View Binding, AndroidX Navigation Component (single-activity + fragments + bottom nav)
*   **Charts**: MPAndroidChart
*   **Security**: `EncryptedSharedPreferences` for session storage; dual-mode auth interceptor (anon key when logged out, JWT when logged in) with automatic refresh on 401

## 🏛️ Architecture Highlights

*   **Single-source-of-truth trading**: `buy_stock` and `sell_stock` Postgres functions execute the entire trade — fund check, holdings upsert with weighted-average cost basis, cash debit, transaction log — atomically. The client only displays results.
*   **Row-Level Security**: Every user-scoped table (`profiles`, `holdings`, `transactions`, `watchlist`) is protected by RLS policies that filter by `auth.uid()`. The app never sends a `user_id` filter; the database enforces it.
*   **Resilient JWT lifecycle**: An OkHttp interceptor catches 401 responses on `/rest/v1/`, refreshes the token via the bare auth client, and transparently retries the original request. Concurrent 401s share a single refresh via a synchronized lock to prevent refresh-token churn.
*   **Offline-tolerant search**: The stock list fetches all 113 stocks once and filters client-side as the user types — zero network latency per keystroke.

## 📦 Backend

The Supabase backend is a separate repository: [Casatrader_backend](https://github.com/fahdmoussaif/Casatrader_backend). It includes the daily scraper (GitHub Actions cron) that ingests BVC market data into the `stocks` and `price_snapshots` tables. SQL migrations for the schema, RLS policies, and `buy_stock`/`sell_stock` RPCs are versioned there.

## 🏗️ Building from Source

1.  Clone the repository.
2.  Open the project in **Android Studio** (Hedgehog or newer).
3.  Create a `local.properties` file in the project root with your Supabase credentials:
```properties
    sdk.dir=/path/to/Android/Sdk
    SUPABASE_URL=https://your-project-ref.supabase.co
    SUPABASE_ANON_KEY=your-anon-key
```
4.  Sync Gradle, then build with `./gradlew assembleDebug` or run on an emulator (API 24+).

## 📦 Download APK

The latest debug APK is available in the [GitHub Releases](https://github.com/fahdmoussaif/casatrader/releases) section.

## 📜 License

This project is for educational and portfolio purposes.
