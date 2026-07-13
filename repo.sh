#!/bin/bash
set -e

git add -A && git commit -m "${1:-update}" && git push

# Remote server git pull
SSH_HOST="194.233.76.175"
SSH_USER="webbycms"
SSH_PASS="Quidents64"
REMOTE_DIR="/var/www/webbypage/just-friends.webbypage.com"

echo ""
echo "Pulling latest code on production server..."
sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no "$SSH_USER@$SSH_HOST" "
  echo '$SSH_PASS' | sudo -S -u www-data git -c safe.directory=$REMOTE_DIR -C $REMOTE_DIR pull origin main 2>&1
  echo 'Server git pull complete'
"
