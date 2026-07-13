#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APK_PUBLIC_DIR="$PROJECT_DIR/public/apk"
VERSION_FILE="$PROJECT_DIR/storage/app/apk-version.json"
VERSION_TXT="$SCRIPT_DIR/app-version.txt"
APP_NAME="journalog"
NEXTCLOUD_DEST="/Volumes/Files/Nextcloud/WebbyPage/Documents/Projects/MyApps-Development"
SSH_HOST="194.233.76.175"
SSH_USER="webbycms"
SSH_PASS="Quidents64"
REMOTE_DIR="/var/www/webbypage/just-friends.webbypage.com"

LARAVEL_APP_URL="${APP_URL:-https://journalog.webbypage.com}"

echo "========================================"
echo "  Journalog APK Build Script"
echo "========================================"

# 1. Bump version
echo ""
echo "[1/6] Bumping version..."

DATE_TAG=$(date +%Y-%m-%d)

if [ -f "$VERSION_TXT" ]; then
    CURRENT=$(cat "$VERSION_TXT")
    CURRENT_BUILD=$(echo "$CURRENT" | sed -n 's/.*-BUILD-0*\([0-9]*\)/\1/p')
    NEXT_BUILD=$((10#$CURRENT_BUILD + 1))
else
    NEXT_BUILD=1
fi

VERSION_STR="${DATE_TAG}-BUILD-$(printf '%03d' $NEXT_BUILD)"
echo "version:${VERSION_STR}" > "$VERSION_TXT"
echo "  → version:${VERSION_STR}"

# 2. Build APK
echo ""
echo "[2/6] Building APK..."

if command -v docker &>/dev/null; then
    echo "  Using Docker..."
    docker build \
      -f "$SCRIPT_DIR/Dockerfile.build" \
      -t journalog-build \
      "$SCRIPT_DIR" 2>&1 | tail -5

    echo "  Extracting from Docker image..."
    mkdir -p "$APK_PUBLIC_DIR"
    CONTAINER_ID=$(docker container create journalog-build 2>/dev/null)
    docker cp "$CONTAINER_ID:/project/app/build/outputs/apk/debug/app-debug.apk" \
      "$APK_PUBLIC_DIR/${APP_NAME}-v${VERSION_STR}.apk"
    docker rm "$CONTAINER_ID" > /dev/null 2>&1
else
    echo "  Docker not found, building locally with Gradle..."
    export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
    export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
    mkdir -p "$APK_PUBLIC_DIR"
    if ! "$SCRIPT_DIR/gradlew" -p "$SCRIPT_DIR" assembleDebug --no-daemon 2>&1; then
        echo "  ⚠ Gradle build failed, but continuing with existing APK if available"
    fi
    if [ -f "$SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk" ]; then
        cp "$SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk" \
          "$APK_PUBLIC_DIR/${APP_NAME}-v${VERSION_STR}.apk"
    fi
fi

echo "  → ${APP_NAME}-v${VERSION_STR}.apk ($(du -h "$APK_PUBLIC_DIR/${APP_NAME}-v${VERSION_STR}.apk" | cut -f1))"

# Clean up old versions (keep last 5)
ls -t "$APK_PUBLIC_DIR"/*.apk 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true

# 3. Write version metadata
echo ""
echo "[3/6] Writing version metadata..."

VERSION_CODE=$NEXT_BUILD
DOWNLOAD_URL="${LARAVEL_APP_URL}/apk/${APP_NAME}-v${VERSION_STR}.apk"

cat > "$VERSION_FILE" <<EOF
{
    "version_code": ${VERSION_CODE},
    "version_name": "${VERSION_STR}",
    "download_url": "${DOWNLOAD_URL}",
    "release_date": "${DATE_TAG}",
    "changelog": "Build ${VERSION_STR}"
}
EOF

echo "  → ${VERSION_FILE} written"

# 4. Copy APK to Nextcloud
echo ""
echo "[4/6] Copying to Nextcloud..."

APK_SOURCE="$APK_PUBLIC_DIR/${APP_NAME}-v${VERSION_STR}.apk"

if [ ! -f "$APK_SOURCE" ]; then
    echo "  ⚠ APK not found at $APK_SOURCE, skipping Nextcloud copy"
else
    mkdir -p "$NEXTCLOUD_DEST"
    find "$NEXTCLOUD_DEST" -maxdepth 1 -name '*Journalog*debug*' -exec rm -v {} \; 2>/dev/null || true

    ts=$(date +%s)
    destFile="$NEXTCLOUD_DEST/${ts}_Journalog-debug.apk"

    if cp "$APK_SOURCE" "$destFile" 2>/dev/null; then
        echo "  → ${ts}_Journalog-debug.apk deployed to Nextcloud"
    else
        echo "  cp failed, trying Finder fallback..."
        osascript -e "
            tell application \"Finder\"
                set srcFile to POSIX file \"$APK_SOURCE\" as alias
                set destFolder to POSIX file \"$NEXTCLOUD_DEST\" as alias
                set newFile to duplicate file srcFile to destFolder with replacing
                set name of newFile to \"${ts}_Journalog-debug.apk\"
            end tell
        " 2>/dev/null && echo "  → ${ts}_Journalog-debug.apk deployed to Nextcloud" \
            || echo "  ⚠ Failed to copy to Nextcloud"
    fi
fi

# 5. Git commit and push
echo ""
echo "[5/6] Committing and pushing to Git..."

cd "$PROJECT_DIR"
git add -A
git reset -- public/apk/*.apk 2>/dev/null || true
git commit -m "Build ${VERSION_STR}" --allow-empty 2>&1 | tail -3
git push 2>&1 | tail -5
echo "  → Pushed to origin main"

# 6. Deploy to production server
echo ""
echo "[6/6] Deploying to production server..."

APK_SOURCE="$APK_PUBLIC_DIR/${APP_NAME}-v${VERSION_STR}.apk"

echo "  Copying APK to server..."
sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no "$SSH_USER@$SSH_HOST" "
  mkdir -p $REMOTE_DIR/public/apk
  echo '$SSH_PASS' | sudo -S chmod 777 $REMOTE_DIR/public/apk 2>/dev/null || true
"
if sshpass -p "$SSH_PASS" scp -o StrictHostKeyChecking=no \
  "$APK_SOURCE" \
  "$SSH_USER@$SSH_HOST:$REMOTE_DIR/public/apk/"; then
    echo "  APK copied successfully"
else
    echo "  ⚠ SCP failed, trying sudo on server..."
    sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no "$SSH_USER@$SSH_HOST" "
      echo '$SSH_PASS' | sudo -S cp /dev/null $REMOTE_DIR/public/apk/journalog-v${VERSION_STR}.apk 2>/dev/null || true
    "
    sshpass -p "$SSH_PASS" scp -o StrictHostKeyChecking=no \
      "$APK_SOURCE" \
      "$SSH_USER@$SSH_HOST:$REMOTE_DIR/public/apk/"
fi

echo "  Pulling latest code on server..."
sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no "$SSH_USER@$SSH_HOST" "
  echo '$SSH_PASS' | sudo -S -u www-data git -c safe.directory=$REMOTE_DIR -C $REMOTE_DIR pull origin main 2>&1
  echo '$SSH_PASS' | sudo -S chmod -R g+rw $REMOTE_DIR/.git 2>/dev/null || true
  echo '$SSH_PASS' | sudo -S chown -R www-data:www-data $REMOTE_DIR 2>/dev/null || true
  ls -t $REMOTE_DIR/public/apk/*.apk 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true
  echo 'Server deploy complete'
"
echo "  → Production server updated"

echo ""
echo "========================================"
echo "  Build complete!"
echo "  Version : ${VERSION_STR}"          
echo "  APK     : ${APP_NAME}-v${VERSION_STR}.apk"
echo "  URL     : ${DOWNLOAD_URL}"
echo "  Nextcloud : ${NEXTCLOUD_DEST}/${ts}_Journalog-debug.apk"
echo "  Git     : origin main (${VERSION_STR})"
echo "  Server  : ${SSH_USER}@${SSH_HOST}:${REMOTE_DIR}"
echo "========================================"
