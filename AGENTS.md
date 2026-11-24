# Release policy

Every app update must ship to all supported versions. Today that includes the Android APK and the web app used in browsers and from the iPhone Home Screen.

Build both from the same release source. Deliver the signed Android update, deploy the production web build to the existing GitHub Pages site, and verify both before marking the release complete. Check the APK version and signing certificate, then confirm that the live site serves the new bundle and that a returning web user can receive the service worker update. If a platform is blocked, report it as pending.
