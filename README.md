![App Logo](markdown-resource/Logo.svg)

# UniteRide

This project was undertaken as a group assignment for the Mobile Application Development course (COMP S313F) at Hong Kong Metropolitan University in 2025, build with Java.

## Contributor
[@randombytebit](https://github.com/randombytebit)

[@Chung1045](https://github.com/Chung1045)

## Introduction
UniteRide is an Android bus tracking application that provides real-time information about bus locations and schedules from Hong Kong Government's Open Data Platform. Designed to help users plan their journeys more efficiently by providing accurate and up-to-date information.

UniteRide provides route information and estimated arrival time for Kowloon Motor Bus (KMB), Citybus (CTB) and Green Mini buses (GMB), unified at one place.

This application was initially called "9 Rush To Bus" when in development which uses cantonese idioms to describe the action to rush towards the bus. The name was changed to "UniteRide" to reflect the application's purpose and functionality better.

## Features
- Real-time bus tracking
- Route information
- Estimated arrival time
- Bus Route Searching
- Nearby Bus Route Discovery
- Multi-language support (Traditional Chinese, Simplified Chinese, English)
- Google Maps integration
- Dark mode support
- RTHK Traffic News integration (Available in Cantonese only)
- Notification for bus tracking

## Install Requirement
OS: Android

Minimum: Android 12+

Target: Android 15

## Build your own
> [!NOTE]
> You need to provide your own Google Map API Key in order to build and use Google Maps SDK for Android’s feature, if you don’t have one, apply for one at Google Cloud.

### What you need
- Computer with modern OS (macOS / Linux / Windows)
- Android Studio (Or other IDE that supports Android development)
- Your own Google Map API Key
- git installed

### Steps to build
1. Clone the repository, either from Command Line Interface or from the IDE
```
git clone https://github.com/Chung1045/UniteRide.git
```
2. Open the cloned project in the IDE of your choice
3. Create two files in the root directory of the project:
   - `local.properties` (if not already present)
   - `gradle.properties` (if not already present)
4. Add the following lines to `local.properties`:
```
MAPS_API_KEY=YOUR_API_KEY
```
This would be used to reference the project's Google Maps API key.

5. In your `secret.properties` file, add the following lines:
```
MAPS_API_KEY=<Your-Google-Maps-API-Key>
```
Replace `<Your-Google-Maps-API-Key>` with your actual Google Maps API key.

6. Build and run the application using your IDE

## Permissions
- Networking
- Location (Optional)
- Notification (Optional)

Location data would be processed locally and not be sent to any server. However, while using the Android Location Service, your location data may be sent to Google for processing. Please refer to the [Google Privacy Policy](https://policies.google.com/privacy) for more information.

## Libraries
- JSoup
- OkHttp
- Google Maps SDK for Android
- SQLite

