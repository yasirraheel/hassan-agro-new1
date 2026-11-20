#!/usr/bin/env bash
set -euo pipefail

# Run on the Hassan Agro Hostinger account. The Git checkout stays outside
# public_html, and sparse checkout downloads only backend/.
REPOSITORY="https://github.com/yasirraheel/hassan-agro-new1.git"
BRANCH="main"
BASE="$HOME/.local/share/hassan-agro-ledger"
SOURCE="$BASE/source"
TARGET="$HOME/domains/hassanagro.com/public_html/ledger"
BACKUPS="$BASE/backups"

mkdir -p "$BASE" "$BACKUPS" "$TARGET"
chmod 700 "$BASE" "$BACKUPS"
if [[ ! -d "$SOURCE/.git" ]]; then
    git clone --filter=blob:none --sparse --branch "$BRANCH" "$REPOSITORY" "$SOURCE"
fi
git -C "$SOURCE" sparse-checkout set --no-cone /backend/
git -C "$SOURCE" pull --ff-only origin "$BRANCH"

if [[ ! -f "$TARGET/.env" ]]; then
    echo "Missing $TARGET/.env; configure the production environment first." >&2
    exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP="$BACKUPS/$STAMP"
mkdir -p "$BACKUP"
chmod 700 "$BACKUP"
rsync -a --exclude='/hassan-agro-new1/' --exclude='/vendor/' "$TARGET/" "$BACKUP/"

# The old full repository clone accidentally exposed Android files in the web folder.
if [[ -d "$TARGET/hassan-agro-new1" ]]; then
    mv "$TARGET/hassan-agro-new1" "$BACKUP/old-repository"
fi

rsync -a --delete \
    --exclude='/.env' \
    --exclude='/vendor/' \
    --exclude='/storage/' \
    --exclude='/bootstrap/cache/' \
    --exclude='/scripts/' \
    "$SOURCE/backend/" "$TARGET/"

mkdir -p "$TARGET/bootstrap/cache" \
    "$TARGET/storage/framework/cache" \
    "$TARGET/storage/framework/cache/data" \
    "$TARGET/storage/framework/sessions" \
    "$TARGET/storage/framework/views" \
    "$TARGET/storage/logs"

cd "$TARGET"
# Hostinger disables proc_open, which Composer needs for script subprocesses.
composer install --no-dev --prefer-dist --no-interaction --optimize-autoloader --no-scripts
php artisan package:discover --ansi
if ! grep -Eq '^APP_KEY=.+$' .env; then
    php artisan key:generate --force
fi
php artisan migrate --force
php artisan config:cache
chmod -R u+rwX,g+rwX storage bootstrap/cache

printf 'Deployed backend commit %s\n' "$(git -C "$SOURCE" rev-parse --short HEAD)"
printf 'Previous files backed up at %s\n' "$BACKUP"
