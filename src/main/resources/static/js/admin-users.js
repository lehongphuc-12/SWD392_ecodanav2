/**
 * =============================================
 * USER MANAGEMENT JAVASCRIPT
 * Modern, accessible user administration
 * =============================================
 */

(function() {
    'use strict';
    
    // =============================================
    // GLOBAL VARIABLES
    // =============================================
    var users = [];
    var roles = [];
    var currentEditingUserId = null;
    var csrfToken = '';
    var csrfHeader = '';
    
    // Debounce timer
    var searchDebounceTimer = null;
    var DEBOUNCE_DELAY = 300;
    
    // =============================================
    // INITIALIZATION
    // =============================================
    document.addEventListener('DOMContentLoaded', function() {
        initializeCSRF();
        initializeEventListeners();
        loadRoles();
        // loadUsers(); // COMMENTED OUT - Using server-side rendering with Thymeleaf instead
    });
    
    /**
     * Initialize CSRF token from meta tags
     */
    function initializeCSRF() {
        var tokenMeta = document.querySelector('meta[name="_csrf"]');
        var headerMeta = document.querySelector('meta[name="_csrf_header"]');
        
        if (tokenMeta && headerMeta) {
            csrfToken = tokenMeta.getAttribute('content');
            csrfHeader = headerMeta.getAttribute('content');
        }
    }
    
    /**
     * Initialize all event listeners
     */
    function initializeEventListeners() {
        // Search and filter removed - using server-side filtering
        
        // Action buttons
        var addUserBtn = document.getElementById('addUserBtn');
        if (addUserBtn) {
            addUserBtn.addEventListener('click', openAddUserModal);
        }
        
        var refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', function() {
                window.location.reload();
                showToast('User list refreshed', 'success');
            });
        }
        
        // Form submission
        var userForm = document.getElementById('userForm');
        if (userForm) {
            userForm.addEventListener('submit', handleFormSubmit);
        }
        
        // Modal close on background click
        var modal = document.getElementById('userModal');
        if (modal) {
            modal.addEventListener('click', function(e) {
                if (e.target === modal) {
                    closeUserModal();
                }
            });
        }
        
        // Keyboard shortcuts
        document.addEventListener('keydown', function(e) {
            // ESC to close modal
            if (e.key === 'Escape') {
                closeUserModal();
                closeUserDetailModal();
                closeBanModal();
                hideToast();
            }
            // Ctrl/Cmd + K to focus search
            if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                e.preventDefault();
                var searchInput = document.getElementById('searchInput');
                if (searchInput) {
                    searchInput.focus();
                }
            }
        });
        
        // Close modals when clicking outside
        var userDetailModal = document.getElementById('userDetailModal');
        if (userDetailModal) {
            userDetailModal.addEventListener('click', function(e) {
                if (e.target === userDetailModal) {
                    closeUserDetailModal();
                }
            });
        }
        
        var banConfirmModal = document.getElementById('banConfirmModal');
        if (banConfirmModal) {
            banConfirmModal.addEventListener('click', function(e) {
                if (e.target === banConfirmModal) {
                    closeBanModal();
                }
            });
        }
    }
    
    // =============================================
    // DATA LOADING FUNCTIONS
    // =============================================
    
    /**
     * Load all users from API
     */
    function loadUsers() {
        showLoadingState();
        
        fetch('/admin/users/api/list', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin'
        })
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Failed to load users');
            }
            return response.json();
        })
        .then(function(data) {
            if (data.success) {
                users = data.users || [];
                renderUsers(users);
            } else {
                throw new Error(data.message || 'Failed to load users');
            }
        })
        .catch(function(error) {
            console.error('Error loading users:', error);
            showErrorState('Failed to load users. Please try again.');
            showToast('Error loading users: ' + error.message, 'error');
        });
    }
    
    /**
     * Load all roles from API
     */
    function loadRoles() {
        fetch('/admin/users/api/roles', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin'
        })
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Failed to load roles');
            }
            return response.json();
        })
        .then(function(data) {
            if (data.success) {
                roles = data.roles || [];
            }
        })
        .catch(function(error) {
            console.error('Error loading roles:', error);
        });
    }
    
    // Filter functions removed - using server-side filtering with page reload
    
    // =============================================
    // RENDERING FUNCTIONS
    // =============================================
    
    /**
     * Render users table
     */
    function renderUsers(usersToRender) {
        var tbody = document.getElementById('usersTableBody');
        if (!tbody) return;
        
        if (!usersToRender || usersToRender.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="px-6 py-12 text-center text-gray-500">' +
                '<div class="empty-state">' +
                '<i class="fas fa-users empty-state-icon"></i>' +
                '<p class="empty-state-text">No users found</p>' +
                '</div></td></tr>';
            return;
        }
        
        var html = '';
        usersToRender.forEach(function(user) {
            html += renderUserRow(user);
        });
        
        tbody.innerHTML = html;
        attachRowEventListeners();
    }
    
    /**
     * Render a single user row
     */
    function renderUserRow(user) {
        var avatar = user.avatarUrl ? 
            '<img src="' + escapeHtml(user.avatarUrl) + '" alt="' + escapeHtml(user.username) + '" class="user-avatar">' :
            '<div class="user-avatar-placeholder">' + getInitials(user.firstName, user.lastName) + '</div>';
        
        var statusClass = 'status-' + (user.status || 'inactive').toLowerCase();
        var roleClass = 'role-' + (user.roleName || 'customer').toLowerCase();
        
        var createdDate = user.createdDate ? formatDate(user.createdDate) : '-';
        
        return '<tr class="fade-in" data-user-id="' + escapeHtml(user.id) + '">' +
            '<td class="px-6 py-4 whitespace-nowrap">' +
                '<div class="flex items-center">' +
                    '<div class="flex-shrink-0 h-10 w-10">' + avatar + '</div>' +
                    '<div class="ml-4">' +
                        '<div class="text-sm font-medium text-gray-900">' + escapeHtml(user.username || '-') + '</div>' +
                        '<div class="text-sm text-gray-500">' + escapeHtml(user.firstName || '') + ' ' + escapeHtml(user.lastName || '') + '</div>' +
                    '</div>' +
                '</div>' +
            '</td>' +
            '<td class="px-6 py-4 whitespace-nowrap">' +
                '<div class="text-sm text-gray-900">' + escapeHtml(user.email || '-') + '</div>' +
                '<div class="text-sm text-gray-500">' + escapeHtml(user.phoneNumber || '-') + '</div>' +
            '</td>' +
            '<td class="px-6 py-4 whitespace-nowrap">' +
                '<span class="role-badge ' + roleClass + '">' + escapeHtml(user.roleName || 'Customer') + '</span>' +
            '</td>' +
            '<td class="px-6 py-4 whitespace-nowrap">' +
                '<span class="status-badge ' + statusClass + '">' + escapeHtml(user.status || 'Inactive') + '</span>' +
            '</td>' +
            '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">' + createdDate + '</td>' +
            '<td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">' +
                '<a href="/admin/users/detail/' + escapeHtml(user.id) + '" class="action-btn action-btn-view mr-2" title="View details" aria-label="View details">' +
                    '<i class="fas fa-eye"></i>' +
                '</a>' +
                '<button class="action-btn action-btn-edit mr-2" onclick="editUser(\'' + escapeHtml(user.id) + '\')" title="Edit user" aria-label="Edit user">' +
                    '<i class="fas fa-edit"></i>' +
                '</button>' +
                '<button class="action-btn action-btn-delete" onclick="deleteUser(\'' + escapeHtml(user.id) + '\')" title="Delete user" aria-label="Delete user">' +
                    '<i class="fas fa-trash"></i>' +
                '</button>' +
            '</td>' +
        '</tr>';
    }
    
    /**
     * Show loading state
     */
    function showLoadingState() {
        var tbody = document.getElementById('usersTableBody');
        if (!tbody) return;
        
        tbody.innerHTML = '<tr><td colspan="6" class="px-6 py-12 text-center">' +
            '<div class="flex flex-col items-center justify-center">' +
            '<i class="fas fa-spinner fa-spin text-4xl text-gray-400 mb-4"></i>' +
            '<p class="text-gray-500">Loading users...</p>' +
            '</div></td></tr>';
    }
    
    /**
     * Show error state
     */
    function showErrorState(message) {
        var tbody = document.getElementById('usersTableBody');
        if (!tbody) return;
        
        tbody.innerHTML = '<tr><td colspan="6" class="px-6 py-12 text-center text-red-500">' +
            '<div class="empty-state">' +
            '<i class="fas fa-exclamation-triangle empty-state-icon"></i>' +
            '<p class="empty-state-text">' + escapeHtml(message) + '</p>' +
            '</div></td></tr>';
    }
    
    /**
     * Attach event listeners to row buttons
     */
    function attachRowEventListeners() {
        // Event delegation is handled by onclick attributes in the HTML
        // This function is kept for potential future enhancements
    }
    
    // =============================================
    // MODAL FUNCTIONS
    // =============================================
    
    /**
     * Open modal for adding new user
     */
    function openAddUserModal() {
        currentEditingUserId = null;
        document.getElementById('modalTitle').textContent = 'Add New User';
        document.getElementById('passwordRequired').style.display = 'inline';
        document.getElementById('password').required = true;
        resetForm();
        showModal();
    }
    window.openAddUserModal = openAddUserModal;
    
    /**
     * View user detail in modal
     */
    function viewUserDetail(userId) {
        var modal = document.getElementById('userDetailModal');
        var content = document.getElementById('userDetailContent');
        
        if (!modal || !content) return;
        
        // Show modal
        modal.style.display = 'block';
        modal.classList.remove('hidden');
        
        // Show loading state
        content.innerHTML = '<div class="flex items-center justify-center py-8">' +
            '<i class="fas fa-spinner fa-spin text-3xl text-gray-400"></i>' +
            '</div>';
        
        // Fetch user details
        fetch('/admin/users/api/' + userId, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin'
        })
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Failed to load user details');
            }
            return response.json();
        })
        .then(function(data) {
            if (data.success && data.user) {
                renderUserDetail(data.user);
            } else {
                throw new Error(data.message || 'Failed to load user details');
            }
        })
        .catch(function(error) {
            console.error('Error loading user details:', error);
            content.innerHTML = '<div class="text-center text-red-500 py-8">' +
                '<i class="fas fa-exclamation-triangle text-3xl mb-3"></i>' +
                '<p>Error loading user details: ' + escapeHtml(error.message) + '</p>' +
                '</div>';
        });
    }
    window.viewUserDetail = viewUserDetail;
    
    /**
     * Render user detail in modal
     */
    function renderUserDetail(user) {
        var content = document.getElementById('userDetailContent');
        if (!content) return;
        
        var avatar = user.avatarUrl ? 
            '<img src="' + escapeHtml(user.avatarUrl) + '" alt="' + escapeHtml(user.username) + '" class="w-24 h-24 rounded-full object-cover">' :
            '<div class="w-24 h-24 rounded-full bg-gray-200 flex items-center justify-center text-2xl text-gray-500">' +
            '<i class="fas fa-user"></i></div>';
        
        var statusClass = user.status === 'Active' ? 'bg-green-100 text-green-800' : 
                         (user.status === 'Banned' ? 'bg-red-100 text-red-800' : 'bg-yellow-100 text-yellow-800');
        
        var roleClass = user.roleName === 'Admin' ? 'bg-purple-100 text-purple-800' : 
                       (user.roleName === 'Owner' ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800');
        
        content.innerHTML = 
            '<div class="space-y-6">' +
                '<!-- User Avatar and Basic Info -->' +
                '<div class="flex items-center space-x-4 pb-4 border-b">' +
                    '<div class="flex-shrink-0">' + avatar + '</div>' +
                    '<div class="flex-1">' +
                        '<h4 class="text-xl font-semibold text-gray-900">' + escapeHtml(user.username || '-') + '</h4>' +
                        '<p class="text-gray-600">' + escapeHtml((user.firstName || '') + ' ' + (user.lastName || '')) + '</p>' +
                        '<div class="flex items-center space-x-2 mt-2">' +
                            '<span class="px-2 py-1 text-xs font-semibold rounded-full ' + roleClass + '">' + 
                                escapeHtml(user.roleName || 'Customer') + 
                            '</span>' +
                            '<span class="px-2 py-1 text-xs font-semibold rounded-full ' + statusClass + '">' + 
                                escapeHtml(user.status || 'Unknown') + 
                            '</span>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
                
                '<!-- Contact Information -->' +
                '<div>' +
                    '<h5 class="text-sm font-semibold text-gray-700 mb-3 flex items-center">' +
                        '<i class="fas fa-address-card mr-2"></i> Contact Information' +
                    '</h5>' +
                    '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">' +
                        '<div>' +
                            '<p class="text-xs text-gray-500">Email</p>' +
                            '<p class="text-sm font-medium text-gray-900">' + escapeHtml(user.email || '-') + '</p>' +
                        '</div>' +
                        '<div>' +
                            '<p class="text-xs text-gray-500">Phone Number</p>' +
                            '<p class="text-sm font-medium text-gray-900">' + escapeHtml(user.phoneNumber || 'N/A') + '</p>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
                
                '<!-- Personal Information -->' +
                '<div>' +
                    '<h5 class="text-sm font-semibold text-gray-700 mb-3 flex items-center">' +
                        '<i class="fas fa-user-circle mr-2"></i> Personal Information' +
                    '</h5>' +
                    '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">' +
                        '<div>' +
                            '<p class="text-xs text-gray-500">Date of Birth</p>' +
                            '<p class="text-sm font-medium text-gray-900">' + 
                                (user.userDOB ? formatDate(user.userDOB) : 'N/A') + 
                            '</p>' +
                        '</div>' +
                        '<div>' +
                            '<p class="text-xs text-gray-500">Gender</p>' +
                            '<p class="text-sm font-medium text-gray-900">' + escapeHtml(user.gender || 'N/A') + '</p>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
                
                '<!-- Account Information -->' +
                '<div>' +
                    '<h5 class="text-sm font-semibold text-gray-700 mb-3 flex items-center">' +
                        '<i class="fas fa-shield-alt mr-2"></i> Account Information' +
                    '</h5>' +
                    '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">' +
                        '<div>' +
                            '<p class="text-xs text-gray-500">Email Verified</p>' +
                            '<p class="text-sm font-medium">' + 
                                (user.emailVerified ? 
                                    '<span class="text-green-600"><i class="fas fa-check-circle"></i> Yes</span>' : 
                                    '<span class="text-red-600"><i class="fas fa-times-circle"></i> No</span>') +
                            '</p>' +
                        '</div>' +
                        '<div>' +
                            '<p class="text-xs text-gray-500">Two Factor Enabled</p>' +
                            '<p class="text-sm font-medium">' + 
                                (user.twoFactorEnabled ? 
                                    '<span class="text-green-600"><i class="fas fa-check-circle"></i> Yes</span>' : 
                                    '<span class="text-gray-600"><i class="fas fa-times-circle"></i> No</span>') +
                            '</p>' +
                        '</div>' +
                        '<div>' +
                            '<p class="text-xs text-gray-500">Lockout Enabled</p>' +
                            '<p class="text-sm font-medium">' + 
                                (user.lockoutEnabled ? 
                                    '<span class="text-yellow-600"><i class="fas fa-lock"></i> Yes</span>' : 
                                    '<span class="text-gray-600"><i class="fas fa-lock-open"></i> No</span>') +
                            '</p>' +
                        '</div>' +
                        '<div>' +
                            '<p class="text-xs text-gray-500">Created Date</p>' +
                            '<p class="text-sm font-medium text-gray-900">' + 
                                (user.createdDate ? formatDate(user.createdDate) : 'N/A') + 
                            '</p>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
    }
    
    /**
     * Close user detail modal
     */
    function closeUserDetailModal() {
        var modal = document.getElementById('userDetailModal');
        if (modal) {
            modal.style.display = 'none';
            modal.classList.add('hidden');
        }
    }
    window.closeUserDetailModal = closeUserDetailModal;
    
    // =============================================
    // BAN/UNBAN FUNCTIONS
    // =============================================
    
    var currentBanAction = null; // { action: 'ban'|'unban', userId: string, username: string }
    
    /**
     * Show ban confirmation modal
     */
    function banUser(userId, username) {
        currentBanAction = {
            action: 'ban',
            userId: userId,
            username: username
        };
        
        var modal = document.getElementById('banConfirmModal');
        var title = document.getElementById('banModalTitle');
        var message = document.getElementById('banModalMessage');
        var icon = document.getElementById('banModalIcon');
        var confirmBtn = document.getElementById('banConfirmBtn');
        
        if (!modal || !title || !message || !icon || !confirmBtn) return;
        
        title.textContent = 'Ban User';
        message.innerHTML = 'Bạn có chắc chắn muốn ban user <strong>' + escapeHtml(username) + '</strong>?<br>' +
            '<span class="text-sm text-gray-500">User sẽ không thể đăng nhập vào hệ thống.</span>';
        icon.className = 'flex-shrink-0 w-12 h-12 rounded-full flex items-center justify-center bg-red-100';
        icon.innerHTML = '<i class="fas fa-ban text-2xl text-red-600"></i>';
        confirmBtn.className = 'px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors';
        confirmBtn.textContent = 'Ban User';
        
        modal.style.display = 'block';
        modal.classList.remove('hidden');
    }
    window.banUser = banUser;
    
    /**
     * Show unban confirmation modal
     */
    function unbanUser(userId, username) {
        currentBanAction = {
            action: 'unban',
            userId: userId,
            username: username
        };
        
        var modal = document.getElementById('banConfirmModal');
        var title = document.getElementById('banModalTitle');
        var message = document.getElementById('banModalMessage');
        var icon = document.getElementById('banModalIcon');
        var confirmBtn = document.getElementById('banConfirmBtn');
        
        if (!modal || !title || !message || !icon || !confirmBtn) return;
        
        title.textContent = 'Unban User';
        message.innerHTML = 'Bạn có chắc chắn muốn unban user <strong>' + escapeHtml(username) + '</strong>?<br>' +
            '<span class="text-sm text-gray-500">User sẽ có thể đăng nhập lại vào hệ thống.</span>';
        icon.className = 'flex-shrink-0 w-12 h-12 rounded-full flex items-center justify-center bg-green-100';
        icon.innerHTML = '<i class="fas fa-user-check text-2xl text-green-600"></i>';
        confirmBtn.className = 'px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors';
        confirmBtn.textContent = 'Unban User';
        
        modal.style.display = 'block';
        modal.classList.remove('hidden');
    }
    window.unbanUser = unbanUser;
    
    /**
     * Close ban confirmation modal
     */
    function closeBanModal() {
        var modal = document.getElementById('banConfirmModal');
        if (modal) {
            modal.style.display = 'none';
            modal.classList.add('hidden');
        }
        currentBanAction = null;
    }
    window.closeBanModal = closeBanModal;
    
    /**
     * Confirm ban/unban action
     */
    function confirmBanAction() {
        if (!currentBanAction) return;
        
        var action = currentBanAction.action;
        var userId = currentBanAction.userId;
        var endpoint = action === 'ban' ? '/admin/users/api/ban' : '/admin/users/api/unban';
        
        console.log('Attempting to ' + action + ' user:', userId);
        console.log('Endpoint:', endpoint);
        
        // Get CSRF token
        var tokenMeta = document.querySelector('meta[name="_csrf"]');
        var headerMeta = document.querySelector('meta[name="_csrf_header"]');
        var csrfToken = tokenMeta ? tokenMeta.getAttribute('content') : '';
        var csrfHeader = headerMeta ? headerMeta.getAttribute('content') : '';
        
        console.log('CSRF Token:', csrfToken);
        console.log('CSRF Header:', csrfHeader);
        
        // Create form data
        var formData = new URLSearchParams();
        formData.append('userId', userId);
        
        // Send request
        var headers = {
            'Content-Type': 'application/x-www-form-urlencoded'
        };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        
        console.log('Sending request with headers:', headers);
        console.log('Request body:', formData.toString());
        
        fetch(endpoint, {
            method: 'POST',
            headers: headers,
            body: formData,
            credentials: 'same-origin'
        })
        .then(function(response) {
            console.log('Response status:', response.status);
            console.log('Response ok:', response.ok);
            
            if (!response.ok) {
                return response.text().then(function(text) {
                    console.error('Error response:', text);
                    throw new Error('Failed to ' + action + ' user: ' + response.status);
                });
            }
            return response.json();
        })
        .then(function(data) {
            console.log('Response data:', data);
            
            if (data.success) {
                closeBanModal();
                showToast('User ' + action + 'ned successfully', 'success');
                
                // Update UI immediately without reload
                updateUserRowStatus(userId, action);
            } else {
                throw new Error(data.message || 'Failed to ' + action + ' user');
            }
        })
        .catch(function(error) {
            console.error('Error ' + action + 'ning user:', error);
            closeBanModal();
            showToast('Error: ' + error.message, 'error');
        });
    }
    window.confirmBanAction = confirmBanAction;
    
    /**
     * Update user row status after ban/unban
     */
    function updateUserRowStatus(userId, action) {
        console.log('Updating UI for user:', userId, 'action:', action);
        
        // Find the user row
        var rows = document.querySelectorAll('#usersTableBody tr');
        var userRow = null;
        
        console.log('Total rows found:', rows.length);
        
        for (var i = 0; i < rows.length; i++) {
            var actionButtons = rows[i].querySelectorAll('button[onclick*="' + userId + '"]');
            console.log('Row', i, 'has', actionButtons.length, 'matching buttons');
            if (actionButtons.length > 0) {
                userRow = rows[i];
                console.log('Found user row at index:', i);
                break;
            }
        }
        
        if (!userRow) {
            console.error('Could not find user row for userId:', userId);
            return;
        }
        
        console.log('User row found, applying', action, 'styling');
        
        if (action === 'ban') {
            // Add banned styling
            userRow.classList.add('banned-user');
            userRow.style.opacity = '0.5';
            userRow.style.backgroundColor = '#fee2e2';
            
            // Update status badge
            var statusBadge = userRow.querySelector('td:nth-child(4) span');
            if (statusBadge) {
                statusBadge.className = 'px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800';
                statusBadge.textContent = 'Banned';
            }
            
            // Add BANNED overlay badge
            var userCell = userRow.querySelector('td:first-child');
            if (userCell) {
                var existingBadge = userCell.querySelector('.banned-badge');
                if (!existingBadge) {
                    var bannedBadge = document.createElement('div');
                    bannedBadge.className = 'banned-badge';
                    bannedBadge.innerHTML = '<span class="inline-flex items-center px-2 py-1 text-xs font-bold text-white bg-red-600 rounded-full">' +
                        '<i class="fas fa-ban mr-1"></i> BANNED</span>';
                    userCell.querySelector('.flex').appendChild(bannedBadge);
                }
            }
            
            // Replace ban button with unban button
            var actionsCell = userRow.querySelector('td:last-child .flex');
            if (actionsCell) {
                var banBtn = actionsCell.querySelector('button[onclick*="banUser"]');
                if (banBtn) {
                    var username = currentBanAction ? currentBanAction.username : 'User';
                    banBtn.outerHTML = '<button type="button" onclick="unbanUser(\'' + userId + '\', \'' + username + '\')" ' +
                        'class="action-btn action-btn-edit p-2 rounded-md transition-colors text-green-600 hover:bg-green-50" title="Unban User">' +
                        '<i class="fas fa-user-check text-lg"></i></button>';
                }
            }
            
        } else if (action === 'unban') {
            // Remove banned styling
            userRow.classList.remove('banned-user');
            userRow.style.opacity = '1';
            userRow.style.backgroundColor = '';
            
            // Update status badge
            var statusBadge = userRow.querySelector('td:nth-child(4) span');
            if (statusBadge) {
                statusBadge.className = 'px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800';
                statusBadge.textContent = 'Active';
            }
            
            // Remove BANNED badge
            var bannedBadge = userRow.querySelector('.banned-badge');
            if (bannedBadge) {
                bannedBadge.remove();
            }
            
            // Replace unban button with ban button
            var actionsCell = userRow.querySelector('td:last-child .flex');
            if (actionsCell) {
                var unbanBtn = actionsCell.querySelector('button[onclick*="unbanUser"]');
                if (unbanBtn) {
                    var username = currentBanAction ? currentBanAction.username : 'User';
                    unbanBtn.outerHTML = '<button type="button" onclick="banUser(\'' + userId + '\', \'' + username + '\')" ' +
                        'class="action-btn action-btn-delete p-2 rounded-md transition-colors" title="Ban User">' +
                        '<i class="fas fa-ban text-lg"></i></button>';
                }
            }
        }
        
        // Add animation
        userRow.style.transition = 'all 0.3s ease-in-out';
    }
    
    /**
     * Navigate to edit page
     */
    function editUser(userId) {
        window.location.href = '/admin/users/edit/' + userId;
        return;
        
        // OLD MODAL CODE (DEPRECATED)
        currentEditingUserId = userId;
        document.getElementById('modalTitle').textContent = 'Edit User';
        document.getElementById('passwordRequired').style.display = 'none';
        document.getElementById('password').required = false;
        
        // Find user data
        var user = users.find(function(u) { return u.id === userId; });
        if (!user) {
            showToast('User not found', 'error');
            return;
        }
        
        // Populate form
        document.getElementById('userId').value = user.id;
        document.getElementById('username').value = user.username || '';
        document.getElementById('email').value = user.email || '';
        document.getElementById('firstName').value = user.firstName || '';
        document.getElementById('lastName').value = user.lastName || '';
        document.getElementById('phoneNumber').value = user.phoneNumber || '';
        document.getElementById('userDOB').value = user.userDOB || '';
        document.getElementById('gender').value = user.gender || '';
        document.getElementById('roleId').value = user.roleId || '';
        document.getElementById('status').value = user.status || 'Active';
        document.getElementById('avatarUrl').value = user.avatarUrl || '';
        document.getElementById('emailVerified').checked = user.emailVerified || false;
        document.getElementById('twoFactorEnabled').checked = user.twoFactorEnabled || false;
        document.getElementById('lockoutEnabled').checked = user.lockoutEnabled || false;
        
        showModal();
    }
    window.editUser = editUser;
    
    /**
     * Show modal
     */
    function showModal() {
        var modal = document.getElementById('userModal');
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('show');
            document.body.style.overflow = 'hidden';
            
            // Focus first input
            setTimeout(function() {
                var firstInput = modal.querySelector('input:not([type="hidden"])');
                if (firstInput) {
                    firstInput.focus();
                }
            }, 100);
        }
    }
    
    /**
     * Close modal
     */
    function closeUserModal() {
        var modal = document.getElementById('userModal');
        if (modal) {
            modal.classList.add('hidden');
            modal.classList.remove('show');
            document.body.style.overflow = '';
            resetForm();
            currentEditingUserId = null;
        }
    }
    window.closeUserModal = closeUserModal;
    
    /**
     * Reset form
     */
    function resetForm() {
        var form = document.getElementById('userForm');
        if (form) {
            form.reset();
            document.getElementById('userId').value = '';
        }
    }
    
    // =============================================
    // FORM SUBMISSION
    // =============================================
    
    /**
     * Handle form submission
     */
    function handleFormSubmit(e) {
        e.preventDefault();
        
        var formData = {
            username: document.getElementById('username').value,
            email: document.getElementById('email').value,
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            phoneNumber: document.getElementById('phoneNumber').value,
            userDOB: document.getElementById('userDOB').value || null,
            gender: document.getElementById('gender').value || null,
            roleId: document.getElementById('roleId').value,
            status: document.getElementById('status').value,
            avatarUrl: document.getElementById('avatarUrl').value || null,
            emailVerified: document.getElementById('emailVerified').checked,
            twoFactorEnabled: document.getElementById('twoFactorEnabled').checked,
            lockoutEnabled: document.getElementById('lockoutEnabled').checked,
            password: document.getElementById('password').value || null
        };
        
        if (currentEditingUserId) {
            updateUser(currentEditingUserId, formData);
        } else {
            createUser(formData);
        }
    }
    
    /**
     * Create new user
     */
    function createUser(userData) {
        var headers = {
            'Content-Type': 'application/json'
        };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        
        fetch('/admin/users/api/create', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(userData),
            credentials: 'same-origin'
        })
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            if (data.success) {
                showToast('User created successfully', 'success');
                closeUserModal();
                loadUsers();
            } else {
                showToast(data.message || 'Failed to create user', 'error');
            }
        })
        .catch(function(error) {
            console.error('Error creating user:', error);
            showToast('Error creating user: ' + error.message, 'error');
        });
    }
    
    /**
     * Update existing user
     */
    function updateUser(userId, userData) {
        var headers = {
            'Content-Type': 'application/json'
        };
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        
        fetch('/admin/users/api/update/' + userId, {
            method: 'PUT',
            headers: headers,
            body: JSON.stringify(userData),
            credentials: 'same-origin'
        })
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            if (data.success) {
                showToast('User updated successfully', 'success');
                closeUserModal();
                loadUsers();
            } else {
                showToast(data.message || 'Failed to update user', 'error');
            }
        })
        .catch(function(error) {
            console.error('Error updating user:', error);
            showToast('Error updating user: ' + error.message, 'error');
        });
    }
    
    /**
     * Delete user
     */
    function deleteUser(userId) {
        if (!confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
            return;
        }
        
        var headers = {};
        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }
        
        fetch('/admin/users/api/delete/' + userId, {
            method: 'DELETE',
            headers: headers,
            credentials: 'same-origin'
        })
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            if (data.success) {
                showToast('User deleted successfully', 'success');
                loadUsers();
            } else {
                showToast(data.message || 'Failed to delete user', 'error');
            }
        })
        .catch(function(error) {
            console.error('Error deleting user:', error);
            showToast('Error deleting user: ' + error.message, 'error');
        });
    }
    window.deleteUser = deleteUser;
    
    // =============================================
    // TOAST NOTIFICATION
    // =============================================
    
    /**
     * Show toast notification
     */
    function showToast(message, type) {
        type = type || 'info';
        
        var toast = document.getElementById('toast');
        var toastMessage = document.getElementById('toastMessage');
        var toastIcon = document.getElementById('toastIcon');
        
        if (!toast || !toastMessage || !toastIcon) return;
        
        // Set icon based on type
        var iconClass = '';
        var iconColor = '';
        switch(type) {
            case 'success':
                iconClass = 'fas fa-check-circle';
                iconColor = 'text-green-500';
                break;
            case 'error':
                iconClass = 'fas fa-times-circle';
                iconColor = 'text-red-500';
                break;
            case 'warning':
                iconClass = 'fas fa-exclamation-triangle';
                iconColor = 'text-yellow-500';
                break;
            default:
                iconClass = 'fas fa-info-circle';
                iconColor = 'text-blue-500';
        }
        
        toastIcon.innerHTML = '<i class="' + iconClass + ' text-2xl ' + iconColor + '"></i>';
        toastMessage.textContent = message;
        
        toast.classList.remove('hidden');
        toast.classList.add('show');
        
        // Auto hide after 5 seconds
        setTimeout(function() {
            hideToast();
        }, 5000);
    }
    
    /**
     * Hide toast notification
     */
    function hideToast() {
        var toast = document.getElementById('toast');
        if (toast) {
            toast.classList.add('hidden');
            toast.classList.remove('show');
        }
    }
    window.hideToast = hideToast;
    
    // =============================================
    // UTILITY FUNCTIONS
    // =============================================
    
    /**
     * Escape HTML to prevent XSS
     */
    function escapeHtml(text) {
        if (!text) return '';
        var map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return String(text).replace(/[&<>"']/g, function(m) { return map[m]; });
    }
    
    /**
     * Get initials from name
     */
    function getInitials(firstName, lastName) {
        var initials = '';
        if (firstName) initials += firstName.charAt(0).toUpperCase();
        if (lastName) initials += lastName.charAt(0).toUpperCase();
        return initials || '?';
    }
    
    /**
     * Format date
     */
    function formatDate(dateString) {
        if (!dateString) return '-';
        try {
            var date = new Date(dateString);
            var day = ('0' + date.getDate()).slice(-2);
            var month = ('0' + (date.getMonth() + 1)).slice(-2);
            var year = date.getFullYear();
            return day + '/' + month + '/' + year;
        } catch (e) {
            return dateString;
        }
    }
    
})();
