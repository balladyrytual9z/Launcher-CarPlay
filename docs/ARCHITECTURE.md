

# Dock 2.0 Architecture

## Modules (current deliverable)

Single Gradle module `:app` (package `pl.dock.widget`) for build reliability on the provided CI workflow.

Logical packages:

- `pl.dock.widget` – launcher, dock, widgets, MediaHub, NowPlayingCard
- `pl.dock.widget.media` – MediaSnapshot / Command / Capabilities
- `pl.dock.widget.ui` – design system
- `pl.dock.widget.core` – task manager

## Future multi-module (planned)

```
media-core/   (library)
dock/         (application pl.dock.widget)
media-center/ (application pl.dock.media)
```

Both APKs share the same signing certificate so signature-level permissions work for ContentProvider commands.

## Data flow (music)

```
DiPlay / Zlink / player
        │
        ▼
Android MediaSession
        │
        ▼
DockNotificationListener → MediaHub → MediaSnapshot
        │                      │
        │                      ├── NowPlayingCard (home)
        │                      └── MediaCenterActivity
        ▼
DockStateProvider.call()  ←── trusted external clients
```

Dock owns the Media Core. Media Center does not create a second independent controller.




