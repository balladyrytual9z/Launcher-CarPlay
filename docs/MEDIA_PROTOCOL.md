# Media Protocol

## Snapshot fields

- version, session_id, package, title, artist, album
- duration_ms, position_ms, playing
- can_play, can_pause, can_seek, can_next, can_previous, can_shuffle, can_repeat
- artwork_uri, updated_at, source

## Commands

All control methods require a trusted caller (or same package).

- media_play / media_pause / media_toggle
- media_next / media_previous
- media_seek (extras: position_ms)
- media_select_source (extras: package)

## Security notes

- Prefer signature permission + UID verification before production multi-APK split.
- Validate arguments and reject oversized artwork payloads.
- Do not treat package name alone as authentication.




