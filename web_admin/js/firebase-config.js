// Firebase v10 Modular SDK Initialization for Flash-O Portal
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { getStorage } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-storage.js";

const firebaseConfig = {
    apiKey: "AIzaSyA5TLXs7Nmdl9Qp27v7l0Brog2pMdD_3a4",
    authDomain: "fryumm-5c865.firebaseapp.com",
    projectId: "fryumm-5c865",
    storageBucket: "fryumm-5c865.firebasestorage.app",
    messagingSenderId: "216011846150",
    appId: "1:216011846150:android:456ee4edba2fd71fc78987"
};

// Initialize Firebase App
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);
const storage = getStorage(app);

console.log("🔥 Firebase initialized for project fryumm-5c865");

export { app, auth, db, storage, firebaseConfig };
