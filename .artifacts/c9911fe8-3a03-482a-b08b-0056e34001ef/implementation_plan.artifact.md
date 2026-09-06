# Cashfree Payment Integration Plan

Implement Cashfree Payment Gateway for automated customer payments and verification in the Flash-O ecosystem.

## User Review Required

> [!IMPORTANT]
> You will need to provide your **Cashfree App ID** and **Secret Key**. These should be stored in Firebase Functions config or Environment Variables. DO NOT hardcode them in the app.
> You also need to set the **Webhook URL** in your Cashfree Dashboard to: `https://<your-region>-<your-project-id>.cloudfunctions.net/cashfreeWebhook`

## Proposed Changes

### [Backend] Firebase Cloud Functions

#### [MODIFY] [index.js](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/functions/index.js)
- Add `createCashfreeOrder` HTTPS callable function.
- Add `cashfreeWebhook` HTTPS trigger to handle payment success/failure notifications.

---

### [Android] Flash-O (Customer App)

#### [MODIFY] [libs.versions.toml](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/Flash-O/gradle/libs.versions.toml)
- Add Cashfree SDK dependency.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/Flash-O/app/build.gradle.kts)
- Include Cashfree dependency in the app module.

#### [MODIFY] [PaymentScreen.kt](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/Flash-O/app/src/main/java/com/flasho/ui/features/cart/PaymentScreen.kt)
- Integrate Cashfree Drop-in SDK.
- Call `createCashfreeOrder` Cloud Function to initiate payment.
- Handle Cashfree callbacks to update local UI.

---

### [Android] F-Store (Merchant App)

#### [MODIFY] [Order.kt](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/app/src/main/java/com/fryumm/store/data/model/Order.kt)
- Add `cashfreeOrderId` and `paymentId` fields for tracking.

## Verification Plan

### Automated Tests
- Mock the Cloud Function response in `PaymentScreen`.
- Verify that `Cashfree.launch()` is called with correct parameters.

### Manual Verification
- Deploy Cloud Functions.
- Run the Flash-O app, go to checkout, select "Cashfree" (or update the UPI flow).
- Complete a test transaction using Cashfree Test Cards/UPI.
- Check Firestore to see if the order status is updated to `PAID` via webhook.
