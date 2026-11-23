# CELLS Android app (Capacitor)

The Android app is the same Vue app, packaged with Capacitor. The website on Netlify is not affected: it keeps
its cookie login and its `/api` proxy. The app is built separately (`npm run build:native`) and calls the backend directly.

App ID: `com.cells.app` (permanent once published on Google Play). App name: `CELLS`.

## How the app differs from the website

| | Website (Netlify) | Android app |
| --- | --- | --- |
| Pages come from | Netlify | Bundled inside the app (`https://localhost`) |
| API address | `/api` on the same site | `VITE_API_BASE_URL` in `frontend/.env.native` (the Render backend) |
| Refresh token | httpOnly cookie | Android Keystore (secure storage), sent in the request body |
| Request header | none | `X-Client-Type: native` |
| Push notifications | none | Firebase Cloud Messaging |

## 1. Prerequisites (Windows)

- Node.js 22 or newer (you already have it).
- Android Studio, version 2025.2.1 or newer. In its SDK Manager install an SDK Platform for API 24 or later
  (and the latest stable one for building). Android Studio brings its own JDK.
- Deploy the backend changes first (CORS and native sign-in) to Render. The app cannot log in against an older backend.

## 2. Install and create the Android project (once)

Run in PowerShell, inside the `frontend` folder:

```powershell
npm install @capacitor/core @capacitor/android @capacitor/push-notifications @aparajita/capacitor-secure-storage
npm install -D @capacitor/cli
npm run build:native
npx cap add android
npx cap sync android
```

`npm install` also updates `package.json` and `package-lock.json`: commit both. Commit the new `frontend/android/`
folder too (Capacitor adds its own `.gitignore` inside it).

## 3. Firebase (needed for push notifications)

1. Open the Firebase console, create a project (for example `cells`).
2. Add an **Android app** to it. The package name must be exactly `com.cells.app`.
3. Download `google-services.json` and put it in `frontend/android/app/`.
   It is not a password, but keep it out of public screenshots. Do not put the service-account key anywhere in the repo.
4. Run `npx cap sync android` again.

The backend needs a separate service-account key (Project settings, Service accounts) to send pushes. That comes
with the next step (the Firebase sender).

## 4. Run it

```powershell
npm run cap:sync
npm run cap:open
```

In Android Studio press Run. Use either:
- a real phone with USB debugging turned on, or
- an emulator using a **Google Play** system image (needed later for push).

Android Studio's Gradle sync can take several minutes the first time.

After every change to the Vue code run `npm run cap:sync` again, then Run.

## 5. Check that it works

1. Log in on the phone with a member account. You should land on the member home.
2. Close the app completely and open it again. You should still be logged in (the saved refresh token).
3. Allow notifications when Android asks.
4. In the database, confirm the phone registered:

```sql
select user_id, platform, left(token, 12) as token_start, last_seen_at from device_tokens order by id desc;
```

5. Log out in the app. That row should disappear.

If login fails with "Cannot reach the server": the backend is not deployed with the CORS change, or
`VITE_API_BASE_URL` is wrong (check `frontend/.env.native`, then `npm run cap:sync`).

## 6. App icon and splash screen

Capacitor needs one square icon of 1024 x 1024 pixels (your current icons stop at 512, so export a larger one).

```powershell
npm install -D @capacitor/assets
# put icon-only.png (1024x1024), icon-foreground.png, icon-background.png and splash.png (2732x2732) in frontend/assets/
npx capacitor-assets generate --android
```

Use the navy `#0E2250` as the background colour and keep the gold dot or wordmark inside the centre 66% of the foreground
(Android crops icons into circles and rounded squares).

## 7. Publishing to Google Play (overview)

Check each point in the Play Console, because Google changes these rules.

1. Create a Google Play developer account (one-time registration fee).
2. Build a signed **App Bundle (.aab)**: Android Studio, Build, Generate Signed App Bundle. Create the upload keystore once,
   **back it up outside the repo and never lose it**, and never commit it.
3. Raise `versionCode` in `frontend/android/app/build.gradle` for every upload.
4. Fill in the store listing, content rating, and the Data safety form. CELLS stores names, emails, and members' written
   reflections, so say so.
5. A public **privacy policy URL** is required. Say what you store, who can read it (leader and room members, not pastors or
   admins by default), and how a person can ask for deletion.
6. New personal developer accounts have had to run a closed test with a minimum number of testers for a minimum period before
   production access. Check the current requirement in the Play Console.
7. Make sure the app's target API level meets Google's current minimum for new apps.
