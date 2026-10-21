#!/system/bin/sh
# Start the local Android DNS compatibility tunnel once per phone boot.
stepneo_root=/data/local/tmp/stepneo-codex
umask 077
(
  /system/bin/flock -x 9 || exit 1
  if ! /system/bin/nc -z -w 1 127.0.0.1 19092 >/dev/null 2>&1; then
    /system/bin/nohup /system/bin/nc -L -s 127.0.0.1 -p 19092 \
      /system/bin/sh "$stepneo_root/provider-connect.sh" \
      9>&- </dev/null >/dev/null 2>"$stepneo_root/tmp/provider-connect.err" &
    echo "$!" > "$stepneo_root/tmp/provider-connect.pid"
    stepneo_attempt=0
    while ! /system/bin/nc -z -w 1 127.0.0.1 19092 >/dev/null 2>&1; do
      stepneo_attempt=$((stepneo_attempt + 1))
      [ "$stepneo_attempt" -lt 20 ] || exit 1
      /system/bin/sleep 0.1
    done
  fi
  cat /apex/com.android.conscrypt/cacerts/* > "$stepneo_root/config/android-ca-bundle.pem.tmp" || exit 1
  mv "$stepneo_root/config/android-ca-bundle.pem.tmp" "$stepneo_root/config/android-ca-bundle.pem"
) 9>"$stepneo_root/tmp/provider-connect.lock"
