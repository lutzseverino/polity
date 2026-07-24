#!/bin/sh

set -eu

LC_ALL=C
export LC_ALL

fail() {
  printf 'Invalid %s: %s\n' "$1" "$2" >&2
  exit 1
}

validate_scheme() {
  name=$1
  value=$2

  case "$value" in
    http | https) ;;
    *) fail "$name" 'expected exactly "http" or "https"' ;;
  esac
}

validate_port() {
  name=$1
  value=$2

  case "$value" in
    '' | *[!0-9]*)
      fail "$name" 'expected decimal digits with a value from 1 through 65535'
      ;;
  esac

  normalized=$value
  while [ "${normalized#0}" != "$normalized" ]; do
    normalized=${normalized#0}
  done
  if [ -z "$normalized" ]; then
    normalized=0
  fi

  if [ "${#normalized}" -gt 5 ] ||
    [ "$normalized" -lt 1 ] ||
    [ "$normalized" -gt 65535 ]; then
    fail "$name" 'expected decimal digits with a value from 1 through 65535'
  fi
}

is_ipv4_address() {
  value=$1
  case "$value" in
    .* | *. | *..*) return 1 ;;
  esac

  previous_ifs=$IFS
  IFS=.
  # The caller has already limited value to decimal digits and dots.
  # shellcheck disable=SC2086
  set -- $value
  IFS=$previous_ifs

  [ "$#" -eq 4 ] || return 1
  for octet do
    case "$octet" in
      0 | [1-9] | [1-9][0-9] | [1-9][0-9][0-9]) ;;
      *) return 1 ;;
    esac
    [ "$octet" -le 255 ] || return 1
  done
}

is_dns_hostname() {
  value=$1

  [ "${#value}" -le 253 ] || return 1
  case "$value" in
    *[!A-Za-z0-9.-]* | *[!A-Za-z0-9])
      return 1
      ;;
    *[A-Za-z]*) ;;
    *) return 1 ;;
  esac

  printf '%s\n' "$value" |
    grep -Eq '^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$'
}

validate_host() {
  name=$1
  value=$2

  case "$value" in
    '')
      fail "$name" 'expected an ASCII DNS hostname or canonical dotted-decimal IPv4 address'
      ;;
    *[!0-9.]*)
      if ! is_dns_hostname "$value"; then
        fail "$name" 'expected an ASCII DNS hostname or canonical dotted-decimal IPv4 address'
      fi
      ;;
    *)
      if ! is_ipv4_address "$value"; then
        fail "$name" 'expected an ASCII DNS hostname or canonical dotted-decimal IPv4 address'
      fi
      ;;
  esac
}

validate_upstream() {
  name=$1
  value=$2

  case "$value" in
    *:*)
      host=${value%:*}
      port=${value##*:}
      ;;
    *)
      fail "$name" 'expected <DNS-or-IPv4-host>:<port>'
      ;;
  esac

  case "$host" in
    *:*) fail "$name" 'expected <DNS-or-IPv4-host>:<port>' ;;
  esac

  validate_host "$name host" "$host"
  validate_port "$name port" "$port"
}

validate_scheme POLITY_EXTERNAL_SCHEME "${POLITY_EXTERNAL_SCHEME-}"
validate_host POLITY_EXTERNAL_HOST "${POLITY_EXTERNAL_HOST-}"
validate_port POLITY_EXTERNAL_PORT "${POLITY_EXTERNAL_PORT-}"
validate_port POLITY_WEB_PORT "${POLITY_WEB_PORT-}"
validate_upstream CARDO_IDENTITY_UPSTREAM "${CARDO_IDENTITY_UPSTREAM-}"
validate_upstream POLITY_SERVICE_UPSTREAM "${POLITY_SERVICE_UPSTREAM-}"
