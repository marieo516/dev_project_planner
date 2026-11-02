# Dev Project Planner

Dev Project Planner is an Android app for organizing software projects from the first idea through delivery. Keep project details, dates, sprints, technologies, libraries, ideas, and tasks together in one place.

## Screenshots

| Projects | Project overview |
| --- | --- |
| ![Projects list](demo_images/Screenshot%20From%202026-11-02%2007-57-24.png) | ![Project overview](demo_images/Screenshot%20From%202026-11-02%2007-58-28.png) |

| Sprint planning | Tasks |
| --- | --- |
| ![Sprint planning](demo_images/Screenshot%20From%202026-11-02%2008-00-14.png) | ![Project tasks](demo_images/Screenshot%20From%202026-11-02%2008-00-16.png) |

| Settings | Settings (dark theme) |
| --- | --- |
| ![Settings](demo_images/Screenshot%20From%202026-11-02%2007-58-06.png) | ![Settings in dark theme](demo_images/Screenshot%20From%202026-11-02%2007-58-17.png) |

## Why

I enjoy staying organized, but I hadn’t found a planning app that fit the way I manage software projects. I already wanted to build one, so I used the idea for a school project. The assignment required Firebase, which I added as an optional cloud backup. The app still works fully locally for people who prefer to keep their project data on their device.

## Features

- Create and manage multiple projects, with dates and labels.
- Find projects using the search bar, filters, and labels.
- Export a project overview to a file on your device (example if you need to use it in a meeting).
- Plan work in sprints and track tasks by completion status.
- Store project technologies, libraries, brainstorming notes, and ideas.
- Switch between English and French, and choose light, dark, or system appearance.
- Use the app locally, with optional Firebase backup and restore.

## Build and run

Open the project in Android Studio, sync Gradle, and run the `app` configuration on an emulator or Android device. The minimum Android version is Android 7.0 (API 24).

Firebase authentication and Firestore are used for cloud backup. To configure them, follow [FIREBASE_SETUP.md](FIREBASE_SETUP.md).

## Tech stack

Kotlin · AndroidX · Material Components · Firebase Authentication · Cloud Firestore

## License

See [LICENSE](LICENSE).
