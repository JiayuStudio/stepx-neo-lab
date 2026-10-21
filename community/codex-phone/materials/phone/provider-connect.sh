#!/system/bin/sh
# Local compatibility tunnel: Android resolves the fixed provider hostname.
# TLS remains end to end between Codex and the configured provider.
IFS= read -r stepneo_request || exit 1
case "$stepneo_request" in
  'CONNECT api.example.com:443 HTTP/1.1'*) ;;
  *) printf 'HTTP/1.1 403 Forbidden\r\nConnection: close\r\n\r\n'; exit 1 ;;
esac
stepneo_cr=$(printf '\r')
stepneo_headers=0
while IFS= read -r stepneo_line; do
  [ -z "$stepneo_line" ] || [ "$stepneo_line" = "$stepneo_cr" ] || {
    stepneo_headers=$((stepneo_headers + 1))
    [ "$stepneo_headers" -lt 64 ] || exit 1
    continue
  }
  printf 'HTTP/1.1 200 Connection established\r\n\r\n'
  exec /system/bin/nc -w 15 -W 180 api.example.com 443
done
exit 1
