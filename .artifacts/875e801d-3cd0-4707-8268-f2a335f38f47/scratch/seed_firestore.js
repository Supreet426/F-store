const admin = require('firebase-admin');
admin.initializeApp({
    projectId: 'fryumm-5c865'
});

const db = admin.firestore();

async function seedDatabase() {
    console.log("Starting Firebase Firestore Seeding for project: fryumm-5c865");

    try {
        // 1. Master Admins
        const masterAdmins = [
            {
                uid: 'master_admin_ranjana',
                name: 'Ranjana Mishra',
                email: 'ranjanamishra1117@gmail.com',
                role: 'ADMIN',
                isActive: true,
                createdAt: Date.now()
            },
            {
                uid: 'master_admin_supreet',
                name: 'Supreet Mishra',
                email: 'supreet.mishr@gmail.com',
                role: 'ADMIN',
                isActive: true,
                createdAt: Date.now()
            }
        ];

        for (const adminUser of masterAdmins) {
            await db.collection('users').doc(adminUser.uid).set(adminUser, { merge: true });
            await db.collection('admins').doc(adminUser.uid).set(adminUser, { merge: true });
            console.log(`Seeded admin: ${adminUser.email}`);
        }

        // 2. Default Dark Stores
        const darkStores = [
            {
                id: 'hub_central',
                name: 'Flash-O Central Dark Store',
                address: 'Sector 18, Noida, UP',
                latitude: 28.5700,
                longitude: 77.3200,
                visibilityRadiusKm: 5.0,
                managerName: 'Rahul Sharma',
                managerMobile: '+919876543210',
                isActive: true,
                assignedMerchantIds: [],
                createdAt: Date.now()
            },
            {
                id: 'hub_north',
                name: 'Flash-O North Hub',
                address: 'Connaught Place, New Delhi',
                latitude: 28.6280,
                longitude: 77.2090,
                visibilityRadiusKm: 6.0,
                managerName: 'Amit Verma',
                managerMobile: '+919876543211',
                isActive: true,
                assignedMerchantIds: [],
                createdAt: Date.now()
            }
        ];

        for (const store of darkStores) {
            await db.collection('dark_stores').doc(store.id).set(store, { merge: true });
            console.log(`Seeded Dark Store: ${store.name}`);
        }

        // 3. Settings / Store Config
        await db.collection('settings').doc('store_config').set({
            storeName: 'Flash-O Store',
            currency: '₹',
            taxPercentage: 5.0,
            deliveryFee: 25.0,
            freeDeliveryAbove: 499.0,
            isAcceptingOrders: true,
            updatedAt: Date.now()
        }, { merge: true });

        await db.collection('settings').doc('dynamic_app_config').set({
            minAppVersion: '1.0.0',
            forceUpdate: false,
            maintenanceMode: false,
            announcementBanner: 'Welcome to Flash-O! Fast delivery in 10 minutes.',
            updatedAt: Date.now()
        }, { merge: true });

        console.log("Seeding completed successfully!");
    } catch (error) {
        console.error("Error seeding Firestore:", error);
    }
}

seedDatabase();
