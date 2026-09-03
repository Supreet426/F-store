import { startUsersService, stopUsersService, fetchAllUsersFromFirestore, getUnifiedUsersList } from './user-service.js';
import { initUserUI, updateUserKPIs, filterAndRenderUsersTable } from './user-ui.js';

// Global exports for backwards compatibility with HTML inline scripts
window.startUsersService = startUsersService;
window.stopUsersService = stopUsersService;
window.initUsersListener = startUsersService;
window.filterUsersByRole = function(role) {
    if (window.setFilterPill) {
        window.setFilterPill(role);
    }
};

document.addEventListener("DOMContentLoaded", () => {
    initUserUI();
});
