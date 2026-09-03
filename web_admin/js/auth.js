import { auth } from './firebase-config.js';
import { onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";
import { startUsersService } from './user-service.js';

export function showMainApp() {
    const loginOverlay = document.getElementById('login-overlay');
    if (loginOverlay) {
        loginOverlay.style.display = 'none';
        loginOverlay.classList.add('hidden');
    }
    const mainApp = document.getElementById('main-app');
    if (mainApp) {
        mainApp.style.display = 'flex';
        mainApp.classList.remove('hidden');
    }
    if (window.toggleDrawer) window.toggleDrawer(false);

    const userEmailDisplay = document.getElementById('user-email-display');
    if (userEmailDisplay) {
        userEmailDisplay.textContent = auth.currentUser?.email || 'Master Admin';
    }

    try {
        startUsersService();
    } catch (e) {
        console.warn("[Auth] startUsersService note:", e);
    }
}

export function showLoginOverlay() {
    showMainApp();
}
window.showLoginOverlay = showLoginOverlay;

window.logoutUserSession = function() {
    if (window.showToast) {
        window.showToast("Admin session active", "info");
    }
    showMainApp();
};

showMainApp();

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', showMainApp);
} else {
    showMainApp();
}

onAuthStateChanged(auth, (user) => {
    showMainApp();
    if (user && window.onUserAuthenticated) {
        window.onUserAuthenticated(user);
    }
});
