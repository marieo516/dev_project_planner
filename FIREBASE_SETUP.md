# Firebase setup

The checked-in `app/google-services.json` uses a placeholder API key. To enable
Firebase, replace it with the `google-services.json` downloaded from your own
Firebase project.

1. In Firebase Console, open **Build → Authentication → Sign-in method** and
   enable **Email/Password**.
2. Open **Build → Firestore Database** and create a database for the project used
   by `app/google-services.json`.
3. In **Firestore Database → Rules**, replace the rules with the contents of
   `firestore.rules` and click **Publish**.

The app stores one account backup at `backups/{uid}` and one document per
attached image in the top-level `images` collection. The rules allow authenticated
users to access only their own documents. Images are gzip-compressed; Firestore's
document limit still applies, so the app rejects an image that remains too large
after compression.
