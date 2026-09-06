# Unified Notification and Popup Broadcast Management System

Implement a centralized Notification Center in the Admin Website to manage both Push Notifications (FCM) and In-App Popup Alerts for the Customer Apps and the website.

## User Review Required

> [!IMPORTANT]
> The implementation uses the `notifications` Firestore collection for push broadcasts and `app_config/config_app` / `app_config/config_web` for popup alerts. Ensure that the Cloud Functions are deployed to handle the `notifications` collection triggers.

## Proposed Changes

### Admin Website (Web Admin)

#### [MODIFY] [index.html](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/web_admin/index.html)
- Add "Notifications" link to the sidebar navigation.
- Add a new section `view-notifications` with a dashboard for:
    - Sending Push Notifications (Title, Body, Image, Action).
    - Configuring Global App Popup (Title, Message, Image, Action, Active Toggle).
    - Configuring Web Portal Popup.
    - Displaying a history of sent notifications.

#### [NEW] [notifications.js](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/web_admin/js/notifications.js)
- Implement logic to handle:
    - Sending FCM broadcasts by adding documents to the `notifications` collection.
    - Updating app/web configurations for popups in `app_config` collection.
    - Fetching and rendering notification history.
    - UI interactions for the notification center.

#### [MODIFY] [ui.js](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/web_admin/js/ui.js) (or similar)
- Wire up the new notification tab in the `switchTab` logic.

### Android Customer App

#### [MODIFY] [MainActivity.kt](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/app/src/main/java/com/fryumm/store/MainActivity.kt)
- Ensure the Global Popup is responsive to real-time updates (already implemented, but verify logic).

#### [MODIFY] [MyFirebaseMessagingService.kt](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/app/src/main/java/com/fryumm/store/MyFirebaseMessagingService.kt)
- Update `sendGeneralNotification` to handle `imageUrl` in the data payload for richer notifications.

## Verification Plan

### Automated Tests
- N/A (Manual UI verification preferred for this feature).

### Manual Verification
1.  **Admin Web**: Navigate to the "Notifications" tab.
2.  **Push Notification**: Send a test notification. Verify it appears in the `notifications` collection and is received on a physical/emulated device.
3.  **App Popup**: Update the Global Popup settings. Verify the Customer App shows the popup on launch or refresh.
4.  **Web Popup**: Update the Web Popup settings. Verify the Admin Website (or customer website if applicable) shows the alert.
