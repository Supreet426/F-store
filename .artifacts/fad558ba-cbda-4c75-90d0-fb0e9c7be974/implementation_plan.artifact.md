# Dark Store Enhancement Plan

This plan outlines the changes required to enhance Dark Store management in both the Android Admin app and the Web Admin panel. We will add location-setting features, detailed manager/bank information, and operational radius controls.

## User Review Required

> [!IMPORTANT]
> - The new fields (Aadhar, Bank Details) will be stored in Firestore. Ensure that your database security rules are updated to protect this sensitive information.
> - "Set Current Location" in the Android app will require fine location permissions.
> - The delivery/visibility radius enforcement logic will need to be applied in the customer-facing app and ordering logic, which is outside the scope of this specific admin-only request, but I will prepare the data structure for it.

## Proposed Changes

### Data Model

#### [MODIFY] [DarkStore.kt](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/app/src/main/java/com/fryumm/store/data/model/DarkStore.kt)
- Add fields: `managerName`, `managerMobile`, `aadharNumber`, `bankName`, `bankAccountNumber`, `bankIfsc`, `visibilityRadiusKm`.
- Default `visibilityRadiusKm` to 5.0.

---

### Android Admin App

#### [MODIFY] [DarkStoreManagementScreen.kt](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/app/src/main/java/com/fryumm/store/ui/screens/DarkStoreManagementScreen.kt)
- Update `EditDarkStoreDialog` to include input fields for all new properties.
- Add a "📍 Use Current Location" button next to Latitude/Longitude inputs.
- Implement location fetching using `FusedLocationProviderClient`.
- Add a section to display and manage `assignedMerchantIds`.

#### [MODIFY] [StoreViewModel.kt](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/app/src/main/java/com/fryumm/store/ui/viewmodels/StoreViewModel.kt)
- Ensure `updateDarkStore` correctly handles all new fields.
- Add logic to fetch all merchants to allow selecting them for a specific Dark Store.

---

### Web Admin Panel

#### [MODIFY] [index.html](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/web_admin/index.html)
- Add new input fields to `#modal-dark-store`:
    - Manager Details (Name, Mobile, Aadhar)
    - Bank Details (Name, Account No, IFSC)
    - Operational Radius (Booking/Visibility)
- Add a "📍 Current Location" button in the modal.

#### [MODIFY] [darkstores.js](file:///C:/Users/supre/AndroidStudioProjects/Flash-O/F-Store/web_admin/js/darkstores.js)
- Update `editDarkStore` to populate new fields from Firestore data.
- Update `renderDarkStoresGrid` to display Manager and Radius info.
- Implement `getCurrentLocation()` using `navigator.geolocation` API.
- Update the form submission event listener to include new fields in the `storeData` object.

## Verification Plan

### Automated Tests
- I will verify the Kotlin model compilation.
- I will check the JSON structure of the Firestore updates.

### Manual Verification
- Deploy to Android device:
    - Open Dark Store Hubs.
    - Click Add/Edit.
    - Test "Use Current Location" button (verify lat/lng update).
    - Fill in new details and save.
    - Verify details persist after reload.
- Open Web Admin:
    - Open Dark Store Hubs tab.
    - Click Add/Edit.
    - Test "Current Location" button.
    - Save and verify Firestore data.
