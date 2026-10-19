#!/usr/bin/env sh
set -eu
if command -v swipl >/dev/null 2>&1; then
    exec swipl "$@"
elif command -v flatpak >/dev/null 2>&1 && flatpak info org.swi_prolog.swipl >/dev/null 2>&1; then
    exec flatpak run --command=swipl org.swi_prolog.swipl "$@"
fi
echo 'Instale SWI-Prolog (swipl) o el Flatpak org.swi_prolog.swipl.' >&2
exit 127
