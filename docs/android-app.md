# CELLS Android app (Capacitor)

The Android app is the same Vue app, packaged with Capacitor. The website on Netlify is not affected: it keeps
its cookie login and its `/api` proxy. The app is built separately (`npm run build:native`) and calls the backend directly.

App ID: `com.cells.app` (permanent once published on Google Play). App name: `CELLS`.

## Status (24 November 2026)

| Done and tested on a real phone | Not done yet |
| --- | --- |
| Starts, shows login, logs in against the Render backend (native sign-in mode) | `android:allowBackup="false"` (still `true` in `AndroidManifest.xml`) |
| Saved login survives closing the app | Rate-limiter test on the live backend (section 8) |
| Phone registers its Firebase token with the backend | Launcher icon and splash screen (section 6), transparent notification icon |
| Real push notification arrives (Firebase sender on Render, `PUSH_ENABLED=true`) | Signed release bundle, Play Console account, privacy policy (section 7) |
| | Real SMTP email (needs your own domain), Android App Links for invitation links |

If you are picking this project up in a new session: the repo, `CELLS - Development Roadmap`, and the product documentation
in the Claude Project are the source of truth. This file is the Android and push part.

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

## Before you deploy: check CORS on your own computer

Run the backend locally with the rate limiter switched on, then run these checks. Use `curl.exe` in PowerShell
(plain `curl` there is a different command). Replace `http://localhost:8080` with your Render address to repeat
them after deploying.

```powershell
$env:APP_RATE_LIMIT_ENABLED = "true"   # then start the backend from the same window
```

Create a file `login.json` containing `{"email":"nobody@example.com","password":"wrong-password"}`.

| # | Check | Command | You should see |
| --- | --- | --- | --- |
| 1 | The app may call the API | `curl.exe -i -X OPTIONS http://localhost:8080/api/v1/auth/login -H "Origin: https://localhost" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: content-type,x-client-type"` | 200, `Access-Control-Allow-Origin: https://localhost`, and `x-client-type` in `Access-Control-Allow-Headers`. No `Access-Control-Allow-Credentials` |
| 2 | Other sites may not | the same command with `-H "Origin: https://evil.example"` | 403 `Invalid CORS request` |
| 3 | The website cannot ask for the refresh token | `curl.exe -i -X POST http://localhost:8080/api/v1/auth/login -H "Origin: https://cells-alliance.netlify.app" -H "X-Client-Type: native" -H "Content-Type: application/json" --data "@login.json"` with a REAL account in the file | `Set-Cookie` present and no `refreshToken` in the body |
| 4 | The app does get it | the same with `-H "Origin: https://localhost"` | `refreshToken` in the body and no `Set-Cookie` |
| 5 | Too many attempts is readable by the app | send check 4's request with the wrong password 11 times in one minute | the 11th is 429 and has `Access-Control-Allow-Origin: https://localhost`, `Retry-After`, and `Access-Control-Expose-Headers: Retry-After` |

Check 5 is the one that failed before this fix: the 429 had no CORS headers, so the app showed a vague network error
instead of "Too many attempts. Please wait N seconds."

## 2. Install and create the Android project (already done in this repo)

The Android project is already committed in `frontend/android/`. On a new machine you only need to run `npm install`
inside `frontend`, then continue with step 3 (Firebase file) and step 4 (Run). Do not run `npx cap add android` again:
it complains that the folder exists.

Only if you ever have to recreate the project from nothing, run in PowerShell, inside the `frontend` folder:

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

Push has two halves and **two separate switches**:

| Half | Switch | Where |
| --- | --- | --- |
| The phone asks for permission and registers with Firebase | `VITE_PUSH_ENABLED=true` at build time | `frontend/.env.native.local` (ignored by Git) |
| The server sends pushes | `PUSH_ENABLED=true` and `FIREBASE_CREDENTIALS_BASE64` | Render environment |

### The phone side

1. Open the Firebase console, create a project (this one is called `cells`).
2. Add an **Android app** to it. The package name must be exactly `com.cells.app`.
3. Download `google-services.json` and put it in `frontend/android/app/`. It is ignored by Git, so on a new machine
   download it again (Project settings, Your apps). Keep it out of public screenshots.
4. Put `VITE_PUSH_ENABLED=true` in `frontend/.env.native.local`.
5. Run `npm run cap:sync`, then Run in Android Studio.

Turning the switch on **without** `google-services.json` crashes the app right after login. With the switch off (the default)
the app never asks for notification permission and never registers a phone.

### The server side

1. Firebase console, Project settings, Service accounts, **Generate new private key**. Save the `.json` file outside the
   project folder (for example `C:\Users\<you>\Secrets\`). It is a password: anyone who has it can send notifications as the app.
2. Turn it into one line and copy it (PowerShell): `Set-Clipboard ([Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\key.json")))`
3. In Render, Environment: `FIREBASE_CREDENTIALS_BASE64` = the pasted text, and `PUSH_ENABLED` = `true`. Save (this redeploys).
4. In the Render log after the deploy look for `Firebase push sender is ready.` If you see
   `Firebase credentials could not be loaded` the variable was pasted wrongly. The backend still starts, but only logs pushes.
5. To check on your own computer first: set `$env:FIREBASE_CREDENTIALS_BASE64` in PowerShell, run the backend, and look for the same line.
   Remove the variable from that window afterwards (`Remove-Item Env:FIREBASE_CREDENTIALS_BASE64`).

Never paste the key, its base64 text, or the file contents into a chat, an issue, or Git.

### Testing a push

1. Log in on the phone as a member of a room, allow notifications, then close the app.
2. On the website, log in as that room's leader and create a devotion dated today. The members get a "new devotion" push.
3. If nothing arrives:

```sql
select type, status, attempts, push_sent_at, last_error from notifications order by id desc limit 5;
select user_id, platform, left(token, 12) as token_start, last_seen_at from device_tokens order by id desc;
```

No `device_tokens` row: the phone did not register (log out and in again; check `VITE_PUSH_ENABLED` and `google-services.json`).
A row stuck at `PENDING` or `FAILED`: read `last_error` and the Render log. "no Firebase sender is installed" means the
credentials variable was not read. "Firebase could not deliver one push: <code>" is Firebase's own reason.

Known limit: if a person has two phones and one push fails temporarily, the retry sends to both again, so the phone that worked
gets a duplicate. Fixing it needs per-phone delivery tracking.

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
3. Only if the phone was built with `VITE_PUSH_ENABLED=true` (section 3): allow notifications when Android asks.
4. Same condition: in the database, confirm the phone registered:

```sql
select user_id, platform, left(token, 12) as token_start, last_seen_at from device_tokens order by id desc;
```

5. Log out in the app. That row should disappear.

If login fails with "Cannot reach the server": that text appears whenever the browser engine gets no usable answer at all,
which means a failed CORS check, DNS, TLS or no connection. It does **not** mean the server is down, and a real server
answer (401, 403, 429, 500) would show the server's own message instead. Work through this:

1. The backend on Render is older than the CORS change. Run the preflight check from "Before you deploy" against the Render
   address. If the answer has no `Access-Control-Allow-Origin`, the deployed backend is out of date.
2. `VITE_API_BASE_URL` is wrong (check `frontend/.env.native`, then `npm run cap:sync`).
3. Read the real error: connect the phone by USB, open `chrome://inspect/#devices` in Chrome, click **inspect** on the CELLS
   WebView, and look at the Console ("blocked by CORS policy ...") and the Network tab (the `OPTIONS` request and its status).

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

## 8. Production checklist

### Backend on Render
- Required variables: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET` (long and random),
  `APP_FRONTEND_URL`, and `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` for the first administrator.
- Leave `CORS_ALLOWED_ORIGINS` unset, or set it to exactly `https://localhost,capacitor://localhost` (the production default).
  Never use a wildcard. If you ever give the app a custom host, change it here and in `capacitor.config.json` together.
- Push: `PUSH_ENABLED=true` and `FIREBASE_CREDENTIALS_BASE64` (section 3). `MAIL_ENABLED` stays `false` until a real SMTP provider
  and your own domain exist.
- **Check how the rate limiter sees visitors.** Logins and invitation accepts are limited to 10 per minute per client address.
  If Render (and, for the website, Netlify's proxy) hides the real address, everyone shares one budget and ten people
  logging in within a minute would lock the others out. To test: from two different networks (for example your phone on mobile
  data and your computer on Wi-Fi) send about six wrong logins each within a minute. If the second network gets a 429 early,
  the server is seeing a shared address; the fix is `APP_RATE_LIMIT_CLIENT_IP_HEADER` set to the header your host
  documents for the real client address. I could not verify this from here, so test it once after deploying.

### Android project (edit these files after `npx cap add android`)
1. **Not done yet.** `android/app/src/main/AndroidManifest.xml`, in the `<application ...>` tag: change `android:allowBackup="true"` to `"false"`.
   Android would otherwise copy the app's stored data to Google Drive backups, and a restored copy cannot decrypt
   the saved login on a new phone.
2. Do not add `android:usesCleartextTraffic="true"` and keep `server.url` and `server.cleartext` out of
   `capacitor.config.json` (they are not there now). The release app must only talk HTTPS.
3. `android/app/build.gradle`: leave `minifyEnabled false` for the first release (turning on code shrinking can break plugins
   unless you add rules). Raise `versionCode` for every upload and set `versionName`.
4. Check that `android/app/google-services.json` was downloaded for package `com.cells.app`.
5. Test the real thing before you publish: build the signed bundle, upload it to a Play **Internal testing** track,
   install it from the Play Store on a phone, and log in, restart, log out, and receive a notification there.
6. Keep the upload keystore and its passwords in a password manager, and never in the repository.

### Later, not blocking the first release
- Invitation emails open in the phone's browser. Opening them directly in the app needs Android App Links
  (an `assetlinks.json` file on your own domain), so it waits for your custom domain.
- A transparent notification icon (without it Android shows a white square for the small icon).
- A refresh-token reuse alert and audit log (Phase 10 of the roadmap).
- Per-phone push delivery tracking (see "Known limit" in section 3).
- A GitHub Dependabot alert (1 moderate) is open on the default branch. Check it before the first release.

## 9. Troubleshooting: white screen on the phone

A white screen means the page loaded but the Vue app did not start. Since the startup fix, the app shows
"CELLS could not start" with the reason on screen instead. If you still see only white, work through this in order.

1. **Read the real error (fastest).** Connect the phone by USB with USB debugging on. Open `chrome://inspect/#devices` in
   Chrome on your computer, find the CELLS WebView under your phone, click **inspect**, and open the **Console** tab.
   The red error there names the cause. Also check the **Network** tab for red requests.
2. **Rebuild and re-sync.** The phone runs the copy of the website stored inside the Android project, not your latest code.
   After any code change, run `npm run cap:sync`, then Run again in Android Studio. Confirm that
   `frontend/android/app/src/main/assets/public/index.html` exists and was just updated.
3. **Use the native build.** `npm run build` (the website build) calls `/api` on the phone itself and cannot work.
   `npm run cap:sync` already runs `build:native`.
4. **Old WebView.** The app needs a reasonably recent "Android System WebView" and Chrome. On an old phone or an
   emulator image, update both in the Play Store (or use an emulator image with the Google Play logo). A script the
   engine cannot read gives exactly this blank screen.
5. **Slow server.** With a saved login the app waits up to 6 seconds for the server (Render's free tier can need a
   minute to wake), then shows the login page and finishes in the background.
6. **Cleared by a fresh start.** Uninstall the app from the phone and run it again from Android Studio, to rule out
   leftovers from an earlier install.
