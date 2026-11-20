# Hassan Agro

This repository contains two projects:

- `android-app/` — Android Studio and Gradle project. Open this folder in Android Studio.
- `backend/` — Laravel API deployed to `https://ledger.hassanagro.com/`.

The repository uses one `main` branch. A normal `git pull --ff-only` in this
checkout updates both folders in their own paths. Git commands run inside either
folder still operate on this same repository.

The Hostinger server uses a sparse checkout that contains only `backend/`. To
pull and deploy backend changes there, run:

```bash
bash ~/.local/share/hassan-agro-ledger/source/backend/scripts/deploy-hostinger.sh
```

To make a separate Android-only checkout on another machine:

```bash
git clone --filter=blob:none --sparse https://github.com/yasirraheel/hassan-agro-new1.git hassan-agro-android
cd hassan-agro-android
git sparse-checkout set --no-cone /android-app/
git pull --ff-only
```

That checkout will contain `android-app/` and no backend files.
