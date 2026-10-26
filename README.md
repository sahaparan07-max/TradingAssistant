# Trading Assistant Android (starter)

## What works in this starter
- Native Android screen
- Public Binance ticker lookup for BTCUSDT, ETHUSDT, SOLUSDT
- Basic paper-trade risk checks: 0.5% account risk, previous-day high/low stop placement with a 0.1% illustrative buffer, 3-loss pause, no adding to a losing trade, and position-value cap
- Simple offline Assamese help for RSI / risk / stop-loss

## Important limitations
- This is a starter prototype, not a broker-connected trading app. It never places orders.
- Price data is a public ticker quote, not candle history. You must manually enter yesterday's high/low, entry, and target.
- Position size assumes the entered price and unit are directly comparable; futures contract multipliers, fees, slippage, funding and exchange-specific rules are not included. Therefore, test only in paper mode.
- The “TAKE” verdict is only a rule-check result, not a prediction or recommendation to risk real money.
- Full conversational AI needs a backend server and an API key stored on that server, never embedded in the APK. This starter's chat is deliberately basic/offline.

## Build APK with GitHub Actions
1. Create a GitHub repository and upload all files in this folder.
2. Push the files to the `main` branch.
3. Open the repository's **Actions** tab.
4. Select **Build Android APK** and tap **Run workflow** (or wait for the push workflow).
5. Open the completed workflow run and download the `TradingAssistant-debug-apk` artifact.
6. Extract the ZIP and install `app-debug.apk` on your Android phone. Android may ask you to allow installs from that source.

## Build locally (Android Studio)
Open this folder in Android Studio with JDK 17 and Android SDK Platform 35, then run `gradle assembleDebug` or use Android Studio's Build APK action.

## Risk logic note
A production-quality reviewer should fail closed if any required candle data is absent, calculate risk in the instrument's actual P&L units, and validate stop/target direction. This starter is for UI and workflow testing only.
