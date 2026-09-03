#!/usr/bin/env bash
set -euo pipefail

backup_dir=${FENXIAO_BACKUP_DIR:-/var/backups/fenxiao/mysql}
container=${FENXIAO_MYSQL_CONTAINER:-deploy-mysql-1}
retention_days=${FENXIAO_BACKUP_RETENTION_DAYS:-30}
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
final_file="$backup_dir/fenxiao-$timestamp.sql.gz"

install -d -o root -g root -m 0700 "$backup_dir"
exec 9>"$backup_dir/.backup.lock"
flock -n 9 || exit 0

tmp_file=$(mktemp "$backup_dir/.fenxiao-$timestamp.XXXXXX.sql.gz")
trap 'rm -f -- "$tmp_file"' EXIT

if [ "$(docker inspect --format '{{.State.Running}}' "$container")" != "true" ]; then
  echo "BANDEIRA MySQL container is not running" >&2
  exit 1
fi

docker exec "$container" sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --quick --routines --triggers --events --hex-blob "$MYSQL_DATABASE"' \
  | gzip -9 >"$tmp_file"

gzip -t "$tmp_file"
chmod 0600 "$tmp_file"
mv "$tmp_file" "$final_file"
sha256sum "$final_file" >"$final_file.sha256"
chmod 0600 "$final_file.sha256"

find "$backup_dir" -maxdepth 1 -type f \
  \( -name 'fenxiao-*.sql.gz' -o -name 'fenxiao-*.sql.gz.sha256' \) \
  -mtime "+$retention_days" -delete

echo "BANDEIRA MySQL backup verified: $final_file"
