import { auth, db } from './firebase-config.js';
import {
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword,
    signInWithPopup,
    GoogleAuthProvider,
    sendPasswordResetEmail,
    signOut,
    onAuthStateChanged
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-auth.js";
import { doc, getDoc, setDoc } from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { startUsersService, stopUsersService } from './user-service.js';

const MASTER_ADMIN_EMAILS = [
    "ranjanamishra1117@gmail.com",
    "supreet.mishr@gmail.com"
];

function isMasterAdminEmail(emailStr) {
    if (!emailStr) return false;
    return MASTER_ADMIN_EMAILS.includes(String(emailStr).toLowerCase().trim());
}

// UI Display Control
export function showMainApp(user) {
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

    const activeUser = user || auth.currentUser;
    const activeEmail = activeUser?.email || 'Master Admin';
    const activeName = activeUser?.displayName || activeUser?.email?.split('@')[0] || 'Master Admin';

    const userEmailDisplay = document.getElementById('user-email-display');
    if (userEmailDisplay) {
        userEmailDisplay.textContent = activeEmail;
    }
    const sidebarEmail = document.getElementById('sidebar-user-email');
    if (sidebarEmail) {
        sidebarEmail.textContent = activeEmail;
    }

    try {
        startUsersService();
    } catch (e) {
        console.warn("[Auth] startUsersService note:", e);
    }
}
window.showMainApp = showMainApp;

export async function showLoginOverlay(errorMsg = '') {
    const mainApp = document.getElementById('main-app');
    if (mainApp) {
        mainApp.style.display = 'none';
        mainApp.classList.add('hidden');
    }

    let loginOverlay = document.getElementById('login-overlay');
    if (!loginOverlay && window.loadAllComponents) {
        try {
            await window.loadAllComponents();
            loginOverlay = document.getElementById('login-overlay');
        } catch (_) {}
    }

    if (loginOverlay) {
        loginOverlay.style.display = 'flex';
        loginOverlay.classList.remove('hidden');
    }

    if (errorMsg) {
        showAuthError(errorMsg);
    } else {
        hideAuthError();
    }

    try {
        stopUsersService();
    } catch (e) {}
}
window.showLoginOverlay = showLoginOverlay;

function showAuthError(message) {
    const errBox = document.getElementById('login-error-msg');
    const errText = document.getElementById('login-error-text');
    if (errBox && errText) {
        errText.textContent = message;
        errBox.classList.remove('hidden');
    }
}

function hideAuthError() {
    const errBox = document.getElementById('login-error-msg');
    if (errBox) {
        errBox.classList.add('hidden');
    }
}

// User Authorization Verification
async function verifyAndAuthorizeUser(user) {
    if (!user) {
        showLoginOverlay();
        return;
    }

    const email = user.email ? user.email.toLowerCase().trim() : '';

    // Master Admins are automatically whitelisted
    if (isMasterAdminEmail(email)) {
        showMainApp(user);
        return;
    }

    // Verify role in Firestore
    try {
        let role = '';

        // Check users collection
        const userDocRef = doc(db, 'users', user.uid);
        const docSnap = await getDoc(userDocRef);

        if (docSnap.exists()) {
            const data = docSnap.data();
            role = String(data.role || data.userRole || data.type || '').toUpperCase();
        } else {
            // Check admins or sellers collection
            const adminDocRef = doc(db, 'admins', user.uid);
            const adminSnap = await getDoc(adminDocRef);
            if (adminSnap.exists()) {
                role = 'ADMIN';
            } else {
                const sellerDocRef = doc(db, 'sellers', user.uid);
                const sellerSnap = await getDoc(sellerDocRef);
                if (sellerSnap.exists()) {
                    role = 'MERCHANT';
                }
            }
        }

        const isAuthorized = ['ADMIN', 'MERCHANT', 'STORE_OWNER', 'SELLER', 'STAFF', 'OTHER', 'WORKER'].some(r => role.includes(r));

        if (isAuthorized) {
            showMainApp(user);
        } else {
            // Customer / Unauthorized User
            await signOut(auth);
            showLoginOverlay('Access Denied: Only Admins, Merchants, and Staff can access this Control Portal.');
        }
    } catch (err) {
        console.warn('[Auth] Authorization check fallback:', err);
        // Fallback: If logged in with valid Firebase Auth, allow access
        showMainApp(user);
    }
}

// Global Authentication Functions for login-overlay
window.handleAdminLogin = async function(event) {
    if (event) event.preventDefault();
    hideAuthError();

    const emailInput = document.getElementById('login-email');
    const passwordInput = document.getElementById('login-password');
    const submitBtn = document.getElementById('login-submit-btn');
    const spinner = document.getElementById('login-spinner');
    const btnIcon = document.getElementById('login-btn-icon');
    const btnText = document.getElementById('login-btn-text');

    const email = emailInput?.value?.trim();
    const password = passwordInput?.value;

    if (!email || !password) {
        showAuthError("Please enter both email and password.");
        return false;
    }

    try {
        if (spinner) spinner.classList.remove('hidden');
        if (btnIcon) btnIcon.classList.add('hidden');
        if (submitBtn) submitBtn.disabled = true;
        if (btnText) btnText.textContent = "Verifying Credentials...";

        await signInWithEmailAndPassword(auth, email, password);
        // onAuthStateChanged handles verification and UI toggle
    } catch (error) {
        console.error("[Auth] Login error:", error);
        let msg = "Authentication failed. Please check your credentials.";
        if (error.code === 'auth/invalid-credential' || error.code === 'auth/wrong-password' || error.code === 'auth/user-not-found') {
            msg = "Invalid email or password. Please try again.";
        } else if (error.code === 'auth/too-many-requests') {
            msg = "Too many failed attempts. Please try again later.";
        } else if (error.message) {
            msg = error.message;
        }
        showAuthError(msg);
    } finally {
        if (spinner) spinner.classList.add('hidden');
        if (btnIcon) btnIcon.classList.remove('hidden');
        if (submitBtn) submitBtn.disabled = false;
        if (btnText) btnText.textContent = "Sign In to Dashboard";
    }
    return false;
};

window.handleAdminRegister = async function(event) {
    if (event) event.preventDefault();
    hideAuthError();

    const nameInput = document.getElementById('reg-name');
    const phoneInput = document.getElementById('reg-phone');
    const emailInput = document.getElementById('reg-email');
    const passwordInput = document.getElementById('reg-password');
    const submitBtn = document.getElementById('reg-submit-btn');
    const spinner = document.getElementById('reg-spinner');
    const btnIcon = document.getElementById('reg-btn-icon');
    const btnText = document.getElementById('reg-btn-text');

    const name = nameInput?.value?.trim();
    const phone = phoneInput?.value?.trim();
    const email = emailInput?.value?.trim();
    const password = passwordInput?.value;

    if (!email || !password) {
        showAuthError("Please enter email and password.");
        return false;
    }
    if (password.length < 6) {
        showAuthError("Password must be at least 6 characters.");
        return false;
    }

    try {
        if (spinner) spinner.classList.remove('hidden');
        if (btnIcon) btnIcon.classList.add('hidden');
        if (submitBtn) submitBtn.disabled = true;
        if (btnText) btnText.textContent = "Creating Partner Account...";

        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;

        const role = isMasterAdminEmail(email) ? "ADMIN" : "MERCHANT";

        await setDoc(doc(db, "users", user.uid), {
            uid: user.uid,
            name: name || email.split('@')[0],
            email: email,
            phone: phone || "",
            role: role,
            createdAt: new Date().toISOString(),
            isActive: true
        }, { merge: true });

        if (window.showToast) {
            window.showToast("Account created successfully!", "success");
        }
    } catch (error) {
        console.error("[Auth] Registration error:", error);
        let msg = "Failed to create account.";
        if (error.code === 'auth/email-already-in-use') {
            msg = "An account with this email address already exists.";
        } else if (error.code === 'auth/invalid-email') {
            msg = "Please enter a valid email address.";
        } else if (error.code === 'auth/weak-password') {
            msg = "Password should be at least 6 characters.";
        } else if (error.message) {
            msg = error.message;
        }
        showAuthError(msg);
    } finally {
        if (spinner) spinner.classList.add('hidden');
        if (btnIcon) btnIcon.classList.remove('hidden');
        if (submitBtn) submitBtn.disabled = false;
        if (btnText) btnText.textContent = "Create Partner Account";
    }
    return false;
};

window.handleGoogleLogin = async function() {
    hideAuthError();
    const provider = new GoogleAuthProvider();
    try {
        const result = await signInWithPopup(auth, provider);
        const user = result.user;
        const email = user.email ? user.email.toLowerCase().trim() : '';

        if (isMasterAdminEmail(email)) {
            await setDoc(doc(db, "users", user.uid), {
                uid: user.uid,
                name: user.displayName || email.split('@')[0],
                email: email,
                role: "ADMIN",
                isActive: true
            }, { merge: true });
        }
    } catch (error) {
        console.error("[Auth] Google Login error:", error);
        if (error.code !== 'auth/popup-closed-by-user') {
            showAuthError(error.message || "Google sign-in failed.");
        }
    }
};

window.handleForgotPassword = async function() {
    hideAuthError();
    const emailInput = document.getElementById('login-email');
    let email = emailInput?.value?.trim();

    if (!email) {
        email = prompt("Enter your business email address to receive password reset link:");
    }

    if (!email) {
        showAuthError("Please enter your email address.");
        return;
    }

    try {
        await sendPasswordResetEmail(auth, email);
        if (window.showToast) {
            window.showToast("Password reset email sent! Check your inbox.", "success");
        } else {
            alert("Password reset email sent! Check your inbox.");
        }
    } catch (error) {
        console.error("[Auth] Reset password error:", error);
        showAuthError(error.message || "Failed to send reset email.");
    }
};

window.switchAuthTab = function(mode) {
    hideAuthError();
    const loginForm = document.getElementById('login-form');
    const regForm = document.getElementById('register-form');
    const tabLoginBtn = document.getElementById('tab-login-btn');
    const tabRegBtn = document.getElementById('tab-register-btn');
    const subtitle = document.getElementById('auth-subtitle');

    if (mode === 'login') {
        if (loginForm) loginForm.classList.remove('hidden');
        if (regForm) regForm.classList.add('hidden');
        if (tabLoginBtn) {
            tabLoginBtn.className = "flex-1 py-2 text-xs font-semibold rounded-xl transition-all bg-rose-600 text-white shadow-md shadow-rose-600/20";
        }
        if (tabRegBtn) {
            tabRegBtn.className = "flex-1 py-2 text-xs font-semibold rounded-xl transition-all text-slate-400 hover:text-white";
        }
        if (subtitle) subtitle.textContent = "Sign in for Admins, Store Owners & Staff Members";
    } else {
        if (loginForm) loginForm.classList.add('hidden');
        if (regForm) regForm.classList.remove('hidden');
        if (tabRegBtn) {
            tabRegBtn.className = "flex-1 py-2 text-xs font-semibold rounded-xl transition-all bg-rose-600 text-white shadow-md shadow-rose-600/20";
        }
        if (tabLoginBtn) {
            tabLoginBtn.className = "flex-1 py-2 text-xs font-semibold rounded-xl transition-all text-slate-400 hover:text-white";
        }
        if (subtitle) subtitle.textContent = "Register a new Partner / Store Account";
    }
};

window.togglePasswordVisibility = function(inputId, eyeId) {
    const input = document.getElementById(inputId);
    const eye = document.getElementById(eyeId);
    if (input) {
        if (input.type === 'password') {
            input.type = 'text';
            if (eye) {
                eye.classList.remove('fa-eye');
                eye.classList.add('fa-eye-slash');
            }
        } else {
            input.type = 'password';
            if (eye) {
                eye.classList.remove('fa-eye-slash');
                eye.classList.add('fa-eye');
            }
        }
    }
};

window.logoutUserSession = async function() {
    try {
        await signOut(auth);
        if (window.showToast) {
            window.showToast("Signed out successfully", "info");
        }
    } catch (e) {
        console.error("[Auth] Logout error:", e);
    }
    showLoginOverlay();
};

// Listen for Firebase Auth state changes
onAuthStateChanged(auth, (user) => {
    if (user) {
        verifyAndAuthorizeUser(user);
        if (window.onUserAuthenticated) {
            window.onUserAuthenticated(user);
        }
    } else {
        showLoginOverlay();
    }
});
