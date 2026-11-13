# Gold Alert — MVP source

This is a working calculation/API prototype, **not yet an installable APK or a 24/7 push notification system**.

## Backend
Python 3.10+:
```
cd backend
pip install -r requirements.txt
export TWELVE_DATA_API_KEY=your_key
uvicorn server:app --host 0.0.0.0 --port 8000
```
Open `http://localhost:8000/signals?interval=5min`.

Twelve Data's XAU/USD access and update frequency must be verified for your plan before relying on live signals. No trading orders are placed.

## Next production milestones
1. Confirm permitted reliable XAU/USD OHLC provider and candle timestamp semantics.
2. Persist signal IDs to avoid duplicate alerts and use a scheduled server-side candle-close worker.
3. Add Firebase Cloud Messaging, Android notification permissions, and a Kotlin UI with device registration.
4. Deploy on an always-on host; test against broker chart candles and replay historical data.

Current API endpoint is deliberately on-demand and has no background scheduler. Protect the endpoint with authentication before public deployment. Do not expose your provider API key in the Android app.

## Android app source (POCO F6 Pro / HyperOS 3)
Open `android/` in Android Studio (JDK 17, Android SDK 35), sync Gradle and choose **Build > Build APK(s)**. This project does not include a precompiled APK or Gradle wrapper; an Android SDK and Gradle download are required.

In the app, enter the public **HTTPS** URL of the backend (not `localhost`), choose an interval, and tap Start. The foreground service polls `/signals` once per minute and deduplicates signals. The first observed signal is suppressed to avoid replaying a historical signal. The backend API key stays on the server.

**Important limitations:** Android 15+ foreground `dataSync` services have time limits; HyperOS can terminate the service. This phone-side prototype is **not guaranteed 24/7** and does not implement server-side FCM push. Production should move scheduling and deduplication to a hosted worker and deliver via FCM. There is no guarantee that Twelve Data's free tier supports XAU/USD or 1-minute refresh. No trades are executed. Test on demo data first.
