import { db } from './firebase-config.js';
import {
    collection,
    query,
    orderBy,
    onSnapshot,
    doc,
    setDoc,
    updateDoc,
    addDoc,
    where,
    limit
} from "https://www.gstatic.com/firebasejs/10.8.0/firebase-firestore.js";
import { showToast } from './ui.js';

let currentSessionId = null;
let currentSessionUnsubscribe = null;
let sessionsUnsubscribe = null;
let currentFilter = 'ALL';
let allSessions = [];

export function initSupportChat() {
    const sessionsList = document.getElementById('support-sessions-list');

    // 1. Observe active sessions
    const sessionsQuery = query(
        collection(db, 'support_chats'),
        where('isActive', '==', true),
        orderBy('lastTimestamp', 'desc')
    );

    sessionsUnsubscribe = onSnapshot(sessionsQuery, (snapshot) => {
        allSessions = snapshot.docs;
        applyFilter();
        updateUnreadBadge(snapshot.docs);
    });

    // 2. Form submission for reply
    const replyForm = document.getElementById('form-support-reply');
    replyForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const input = document.getElementById('support-reply-input');
        const text = input.value.trim();

        if (text && currentSessionId) {
            await sendReply(currentSessionId, text);
            input.value = '';
        }
    });
}

window.filterSupport = function(role) {
    currentFilter = role;

    // Update button styles
    ['all', 'merchants', 'customers'].forEach(id => {
        const btn = document.getElementById(`filter-${id}`);
        if (btn) {
            btn.classList.remove('bg-brand-600', 'text-white');
            btn.classList.add('bg-slate-800', 'text-slate-400');
        }
    });

    const activeBtn = document.getElementById(`filter-${role.toLowerCase()}s`) || document.getElementById('filter-all');
    activeBtn.classList.remove('bg-slate-800', 'text-slate-400');
    activeBtn.classList.add('bg-brand-600', 'text-white');

    applyFilter();
};

function applyFilter() {
    const filtered = currentFilter === 'ALL'
        ? allSessions
        : allSessions.filter(doc => doc.data().userRole === currentFilter);

    renderSessions(filtered);
}

function renderSessions(sessionDocs) {
    const list = document.getElementById('support-sessions-list');
    if (sessionDocs.length === 0) {
        list.innerHTML = `
            <div class="p-8 text-center text-slate-500">
                <i class="fa-solid fa-moon text-xl mb-2"></i>
                <p class="text-xs">No active sessions</p>
            </div>
        `;
        return;
    }

    list.innerHTML = sessionDocs.map(docSnap => {
        const session = docSnap.data();
        const isActive = docSnap.id === currentSessionId;
        const time = new Date(session.lastTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        const roleBadge = session.userRole === 'MERCHANT'
            ? '<span class="text-[8px] bg-purple-500/10 text-purple-400 px-1.5 py-0.5 rounded border border-purple-500/20">MERCHANT</span>'
            : '<span class="text-[8px] bg-blue-500/10 text-blue-400 px-1.5 py-0.5 rounded border border-blue-500/20">CUSTOMER</span>';

        return `
            <div onclick="selectSession('${docSnap.id}')"
                 class="p-4 cursor-pointer transition-all border-l-4 ${isActive ? 'bg-slate-800/50 border-brand-500' : 'hover:bg-slate-800/30 border-transparent'}">
                <div class="flex justify-between items-start mb-1">
                    <div class="flex flex-col gap-1">
                        <h5 class="font-bold text-white text-xs truncate max-w-[140px]">${session.userName}</h5>
                        ${roleBadge}
                    </div>
                    <span class="text-[9px] text-slate-500">${time}</span>
                </div>
                <div class="flex justify-between items-center mt-2">
                    <p class="text-[10px] text-slate-400 truncate max-w-[180px]">${session.lastMessage || 'Starting chat...'}</p>
                    ${session.isUnreadByAdmin ? '<span class="w-2 h-2 bg-brand-500 rounded-full"></span>' : ''}
                </div>
            </div>
        `;
    }).join('');
}

window.selectSession = async function(sessionId) {
    if (currentSessionId === sessionId) return;

    currentSessionId = sessionId;

    // UI Updates
    document.getElementById('support-reply-input').disabled = false;
    document.getElementById('btn-send-support').disabled = false;

    // Clear previous listener
    if (currentSessionUnsubscribe) currentSessionUnsubscribe();

    // Fetch session details
    const sessionRef = doc(db, 'support_chats', sessionId);
    onSnapshot(sessionRef, (snap) => {
        if (snap.exists()) {
            const data = snap.data();
            document.getElementById('chat-user-name').textContent = data.userName;
            document.getElementById('chat-user-status').textContent = data.userEmail;

            // Mark as read
            if (data.isUnreadByAdmin) {
                updateDoc(sessionRef, { isUnreadByAdmin: false });
            }
        }
    });

    // Observe messages
    const messagesQuery = query(
        collection(db, 'support_chats', sessionId, 'messages'),
        orderBy('timestamp', 'asc')
    );

    currentSessionUnsubscribe = onSnapshot(messagesQuery, (snapshot) => {
        renderMessages(snapshot.docs);
    });

    // Highlight selected session in list
    initSupportChat(); // Re-render sessions to update active state (could be optimized)
};

function renderMessages(messageDocs) {
    const container = document.getElementById('support-messages-container');
    container.innerHTML = messageDocs.map(docSnap => {
        const msg = docSnap.data();
        const isMe = msg.isFromAdmin;
        const time = new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        return `
            <div class="flex ${isMe ? 'justify-end' : 'justify-start'}">
                <div class="max-w-[75%] rounded-2xl p-3 ${isMe ? 'bg-brand-600 text-white rounded-tr-none' : 'bg-slate-800 text-slate-200 rounded-tl-none'}">
                    <p class="text-sm">${msg.message}</p>
                    <p class="text-[9px] mt-1 opacity-60 text-right">${time}</p>
                </div>
            </div>
        `;
    }).join('');

    container.scrollTop = container.scrollHeight;
}

async function sendReply(sessionId, text) {
    const messageId = Math.random().toString(36).substring(7);
    const message = {
        id: messageId,
        senderId: 'admin',
        senderName: 'Flash-O Admin',
        message: text,
        timestamp: Date.now(),
        isFromAdmin: true,
        type: 'TEXT'
    };

    try {
        await setDoc(doc(db, 'support_chats', sessionId, 'messages', messageId), message);
        await updateDoc(doc(db, 'support_chats', sessionId), {
            lastMessage: text,
            lastTimestamp: Date.now(),
            isUnreadByAdmin: false
        });
    } catch (err) {
        console.error("Error sending reply:", err);
        showToast("Failed to send message", "error");
    }
}

window.resolveCurrentSession = async function() {
    if (!currentSessionId) return;

    if (confirm("Are you sure you want to resolve and close this session?")) {
        try {
            await updateDoc(doc(db, 'support_chats', currentSessionId), {
                isActive: false
            });
            showToast("Session marked as resolved");
            currentSessionId = null;
            document.getElementById('support-messages-container').innerHTML = '';
            document.getElementById('chat-user-name').textContent = 'Select a session';
            document.getElementById('support-reply-input').disabled = true;
        } catch (err) {
            showToast("Failed to resolve session", "error");
        }
    }
};

function updateUnreadBadge(sessionDocs) {
    const unreadCount = sessionDocs.filter(d => d.data().isUnreadByAdmin).length;
    const badge = document.getElementById('badge-support-unread');
    if (badge) {
        if (unreadCount > 0) {
            badge.textContent = unreadCount;
            badge.classList.remove('hidden');
        } else {
            badge.classList.add('hidden');
        }
    }
}
