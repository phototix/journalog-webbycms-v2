#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APK_PUBLIC_DIR="$PROJECT_DIR/public/apk"
VERSION_FILE="$PROJECT_DIR/storage/app/apk-version.json"
VERSION_TXT="$SCRIPT_DIR/app-version.txt"
APP_NAME="journalog"

LARAVEL_APP_URL="${APP_URL:-https://journalog.webbypage.com}"

echo "========================================"
echo "  Journalog APK Build Script"
echo "========================================"

# 1. Bump version
echo ""
echo "[1/4] Bumping version..."

DATE_TAG=$(date +%Y-%m-%d)

if [ -f "$VERSION_TXT" ]; then
    CURRENT=$(cat "$VERSION_TXT")
    CURRENT_BUILD=$(echo "$CURRENT" | sed -n 's/.*-BUILD-\([0-9]*\)/\1/p')
    NEXT_BUILD=$((CURRENT_BUILD + 1))
else
    NEXT_BUILD=1
fi

VERSION_STR="${DATE_TAG}-BUILD-$(printf '%03d' $NEXT_BUILD)"
echo "version:${VERSION_STR}" > "$VERSION_TXT"
echo "  → version:${VERSION_STR}"

# 2. Build APK using Docker
echo ""
echo "[2/4] Building APK (Docker)..."

docker build \
  -f "$SCRIPT_DIR/Dockerfile.build" \
  -t journalog-build \
  "$SCRIPT_DIR" 2>&1 | tail -5

# 3. Extract APK from Docker image
echo ""
echo "[3/4] Extracting APK..."

mkdir -p "$APK_PUBLIC_DIR"

CONTAINER_ID=$(docker container create journalog-build 2>/dev/null)
docker cp "$CONTAINER_ID:/project/app/build/outputs/apk/debug/app-debug.apk" \
  "$APK_PUBLIC_DIR/${APP_NAME}-v${VERSION_STR}.apk"
docker rm "$CONTAINER_ID" > /dev/null 2>&1

echo "  → ${APP_NAME}-v${VERSION_STR}.apk ($(du -h "$APK_PUBLIC_DIR/${APP_NAME}-v${VERSION_STR}.apk" | cut -f1))"

# Clean up old versions (keep last 5)
ls -t "$APK_PUBLIC_DIR"/*.apk 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true

# 4. Write version metadata
echo ""
echo "[4/4] Writing version metadata..."

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

echo ""
echo "========================================"
echo "  Build complete!"
echo "  Version : ${VERSION_STR}"          
echo "  APK     : ${APP_NAME}-v${VERSION_STR}.apk"
echo "  URL     : ${DOWNLOAD_URL}"
echo "========================================"
