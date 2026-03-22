// Admin Dashboard JavaScript Functions

// Global variables
let currentTab = 'overview';
let refreshInterval;

// Initialize dashboard
document.addEventListener('DOMContentLoaded', function() {
    console.log('Dashboard initialized');
    initializeCharts();
    setupEventListeners();
    setupSidebar();
    startAutoRefresh();

    // Set initial tab based on URL parameter
    const urlParams = new URLSearchParams(window.location.search);
    const tab = urlParams.get('tab');
    if (tab) {
        switchTab(tab);
    } else {
        // If no tab parameter, default to overview and highlight it
        switchTab('overview');
    }

    // User management is now handled server-side

    // DISABLED: Simple user rows protection - now using CSS-only filtering
    // setTimeout(() => {
    //     console.log('Running user rows protection...');
    //     forceShowAllRows();
    // }, 1000);
});

// Sidebar functionality
function setupSidebar() {
    const sidebar = document.getElementById('sidebar');
    const sidebarToggle = document.getElementById('sidebar-toggle');
    const sidebarClose = document.getElementById('sidebar-close');
    const sidebarOverlay = document.getElementById('sidebar-overlay');

    // Toggle sidebar on mobile
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', function() {
            openSidebar();
        });
    }

    // Close sidebar
    if (sidebarClose) {
        sidebarClose.addEventListener('click', function() {
            closeSidebar();
        });
    }

    // Close sidebar when clicking overlay
    if (sidebarOverlay) {
        sidebarOverlay.addEventListener('click', function() {
            closeSidebar();
        });
    }

    // Close sidebar on escape key
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            closeSidebar();
        }
    });

    // Handle window resize
    window.addEventListener('resize', function() {
        if (window.innerWidth >= 1024) {
            closeSidebar();
        }
    });

    // Modal handling removed - using server-side forms
}

function openSidebar() {
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebar-overlay');

    if (sidebar && sidebarOverlay) {
        sidebar.classList.add('sidebar-open');
        sidebarOverlay.classList.remove('hidden');
        document.body.style.overflow = 'hidden';
    }
}

function closeSidebar() {
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebar-overlay');

    if (sidebar && sidebarOverlay) {
        sidebar.classList.remove('sidebar-open');
        sidebarOverlay.classList.add('hidden');
        document.body.style.overflow = '';
    }
}

// Tab switching functionality
function switchTab(tabName, event) {
    console.log('Switching to tab:', tabName);

    // Hide all tab contents
    const tabContents = document.querySelectorAll('.tab-content');
    console.log('Found', tabContents.length, 'tab contents');
    tabContents.forEach(content => {
        content.classList.remove('active');
        console.log('Removed active from:', content.id);
    });

    // !!!!!!!!!!!! SỬA LỖI TẠI ĐÂY !!!!!!!!!!!!
    // ĐÃ BÌNH LUẬN LẠI KHỐI CODE GÂY LỖI
    // Lý do: Khối code này xóa class 'active' mà Thymeleaf đã render đúng
    /*
    // Remove active class from all sidebar items
    const sidebarItems = document.querySelectorAll('.sidebar-item');
    sidebarItems.forEach(item => {
        item.classList.remove('active');
    });
    */
    // !!!!!!!!!!!! KẾT THÚC SỬA LỖI !!!!!!!!!!!!

    // Show selected tab content
    const targetTab = document.getElementById(tabName);
    console.log('Target tab element:', targetTab);
    if (targetTab) {
        targetTab.classList.add('active');
        console.log('Tab', tabName, 'is now active');
    } else {
        console.error('Tab', tabName, 'not found!');
        // List all available tabs for debugging
        const allTabs = document.querySelectorAll('[id]');
        console.log('Available elements with IDs:', Array.from(allTabs).map(el => el.id));
    }

    // !!!!!!!!!!!! SỬA LỖI TẠI ĐÂY !!!!!!!!!!!!
    // ĐÃ BÌNH LUẬN LẠI KHỐI CODE GÂY LỖI
    // Lý do: Khối code này không cần thiết vì ta không dùng SPA
    /*
    // Add active class to clicked sidebar item (if called from click event)
    if (event && event.target && event.target.closest) {
        const clickedButton = event.target.closest('.sidebar-item');
        if (clickedButton) {
            clickedButton.classList.add('active');
        }
    } else {
        // If no event, find the sidebar item by data attribute and activate it
        const sidebarItem = document.querySelector(`[data-tab="${tabName}"]`);
        if (sidebarItem) {
            sidebarItem.classList.add('active');
        }
    }
    */
    // !!!!!!!!!!!! KẾT THÚC SỬA LỖI !!!!!!!!!!!!

    currentTab = tabName;

    // Show/hide dashboard statistics cards
    const dashboardStats = document.getElementById('dashboard-stats');
    const dashboardStats2 = document.getElementById('dashboard-stats-2');
    if (dashboardStats && dashboardStats2) {
        if (tabName === 'overview') {
            dashboardStats.style.display = 'grid';
            dashboardStats2.style.display = 'grid';
        } else {
            dashboardStats.style.display = 'none';
        }
    }

    // Dispatch tabChanged event for tab-specific scripts
    const event = new CustomEvent('tabChanged', { detail: tabName });
    window.dispatchEvent(event);
    console.log('Dispatched tabChanged event for tab:', tabName);

    // Close sidebar on mobile after selection
    if (window.innerWidth < 1024) {
        closeSidebar();
    }

    // Update URL without page reload
    // !!!!!!!!!!!! SỬA LỖI TẠI ĐÂY !!!!!!!!!!!!
    // ĐÃ BÌNH LUẬN LẠI KHỐI CODE GÂY LỖI
    // Lý do: Không cần pushState vì trang đang dùng cơ chế tải lại (th:href)
    /*
    const url = new URL(window.location);
    url.searchParams.set('tab', tabName);
    window.history.pushState({}, '', url);
    */
}

// Setup event listeners
function setupEventListeners() {
    // Search functionality - COMPLETELY DISABLED
    const searchInputs = document.querySelectorAll('input[placeholder*="Search"]');
    searchInputs.forEach(input => {
        input.addEventListener('input', function(e) {
            console.log('Search input changed but functionality is disabled');
            // Do nothing - completely disabled
        });
    });

    // Filter functionality - COMPLETELY DISABLED
    const filterSelects = document.querySelectorAll('select');
    filterSelects.forEach(select => {
        select.addEventListener('change', function(e) {
            console.log('Filter changed but functionality is disabled');
            // Do nothing - completely disabled
        });
    });

    // Refresh button
    const refreshButton = document.querySelector('[data-refresh]');
    if (refreshButton) {
        refreshButton.addEventListener('click', refreshData);
    }

    // DISABLED: Reset all rows - now using CSS-only filtering
    // setTimeout(() => {
    //     resetAllRows();
    //     forceShowAllRows();
    // }, 1000);

    // DISABLED: Force show all rows - now using CSS-only filtering
    // forceShowAllRows();

    // Action buttons with data attributes
    document.addEventListener('click', function(event) {
        const button = event.target.closest('button[data-action]');
        if (!button) return;

        const action = button.getAttribute('data-action');

        switch(action) {
            case 'suspend':
                const suspendUserId = button.getAttribute('data-user-id');
                suspendUser(suspendUserId);
                break;
            case 'activate':
                const activateUserId = button.getAttribute('data-user-id');
                activateUser(activateUserId);
                break;
            case 'vehicle-status':
                const vehicleId = button.getAttribute('data-vehicle-id');
                const vehicleStatus = button.getAttribute('data-status');
                updateVehicleStatus(vehicleId, vehicleStatus);
                break;
            case 'booking-status':
                const bookingId = button.getAttribute('data-booking-id');
                const bookingStatus = button.getAttribute('data-status');
                updateBookingStatus(bookingId, bookingStatus);
                break;
            case 'contract-status':
                const contractId = button.getAttribute('data-contract-id');
                const contractStatus = button.getAttribute('data-status');
                updateContractStatus(contractId, contractStatus);
                break;
            case 'refund':
                const paymentId = button.getAttribute('data-payment-id');
                refundPayment(paymentId);
                break;
            case 'edit-discount':
                const editDiscountId = button.getAttribute('data-discount-id');
                editDiscount(editDiscountId);
                break;
            case 'delete-discount':
                const deleteDiscountId = button.getAttribute('data-discount-id');
                deleteDiscount(deleteDiscountId);
                break;
            case 'edit-insurance':
                const editInsuranceId = button.getAttribute('data-insurance-id');
                editInsurance(editInsuranceId);
                break;
            case 'delete-insurance':
                const deleteInsuranceId = button.getAttribute('data-insurance-id');
                deleteInsurance(deleteInsuranceId);
                break;
            case 'delete-notification':
                const notificationId = button.getAttribute('data-notification-id');
                deleteNotification(notificationId);
                break;
        }
    });
}

// Search functionality - TEMPORARILY DISABLED FOR DEBUGGING
function handleSearch(event) {
    console.log('Search called with term:', event.target.value);
    console.log('Search functionality temporarily disabled for debugging');

    // TEMPORARILY DISABLED - Just show all rows
    const table = event.target.closest('.bg-white').querySelector('table');
    const rows = table.querySelectorAll('tbody tr');

    console.log('Found', rows.length, 'rows in table');

    rows.forEach((row, index) => {
        row.style.display = '';
        console.log('Row', index, 'forced to visible');
    });
}

// Filter functionality
function handleFilter(event) {
    console.log('Filter called with value:', event.target.value);
    console.log('Filter functionality temporarily disabled for debugging');

    // TEMPORARILY DISABLED - Just show all rows
    const table = event.target.closest('.bg-white')?.querySelector('table');
    if (!table) {
        console.log('Table not found for filter');
        return;
    }
    const rows = table.querySelectorAll('tbody tr');

    rows.forEach(row => {
        row.style.display = '';
        console.log('Row forced to visible by filter');
    });
}

// Force show all rows - simple approach
function forceShowAllRows() {
    console.log('Force showing all rows...');

    // Find all tables in the page
    const tables = document.querySelectorAll('table');
    tables.forEach((table, tableIndex) => {
        const rows = table.querySelectorAll('tbody tr');
        console.log('Force show - Table', tableIndex, 'has', rows.length, 'rows');

        rows.forEach((row, rowIndex) => {
            // Skip debug rows
            if (row.textContent.includes('DEBUG:')) {
                console.log('Force show - Skipping debug row', rowIndex);
                return;
            }

            // Simple force show
            row.style.display = '';
            row.style.visibility = 'visible';
            row.style.opacity = '1';
            row.classList.remove('hidden', 'd-none');
            row.removeAttribute('hidden');
            console.log('Force show - Row', rowIndex, 'forced to visible');
        });
    });
}

// Reset all rows to visible
function resetAllRows() {
    console.log('Resetting all rows to visible...');

    // Find all tables in the page
    const tables = document.querySelectorAll('table');
    tables.forEach((table, tableIndex) => {
        const rows = table.querySelectorAll('tbody tr');
        console.log('Table', tableIndex, 'has', rows.length, 'rows');

        rows.forEach((row, rowIndex) => {
            // Skip debug rows
            if (row.textContent.includes('DEBUG:')) {
                console.log('Skipping debug row', rowIndex);
                return;
            }

            row.style.display = '';
            console.log('Row', rowIndex, 'reset to visible');
        });
    });
}

// Debounce function
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// API Helper Functions
async function makeApiCall(url, method = 'GET', data = null) {
    try {
        console.log('Making API call to:', url, 'with method:', method);
        console.log('Data:', data);

        // Get CSRF token
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'same-origin' // Include cookies for session
        };

        // Add CSRF token if available
        if (csrfToken && csrfHeader) {
            options.headers[csrfHeader] = csrfToken;
            console.log('CSRF token added:', csrfToken);
        } else {
            console.log('CSRF token not found');
        }

        if (data) {
            options.body = new URLSearchParams(data);
            console.log('Request body:', options.body.toString());
        }

        const response = await fetch(url, options);
        console.log('Response status:', response.status);
        console.log('Response headers:', response.headers);

        if (!response.ok) {
            // Try to get error message from response
            let errorMessage = `HTTP error! status: ${response.status}`;
            try {
                const errorData = await response.json();
                console.log('Error response data:', errorData);
                if (errorData.error) {
                    errorMessage = errorData.error;
                }
            } catch (e) {
                console.log('Could not parse error response as JSON');
            }

            if (response.status === 403) {
                throw new Error('Bạn không có quyền thực hiện hành động này. Vui lòng đăng nhập lại.');
            } else if (response.status === 401) {
                throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            } else {
                throw new Error(errorMessage);
            }
        }

        const result = await response.json();
        console.log('Response data:', result);
        return result;
    } catch (error) {
        console.error('API Error:', error);
        showNotification('Lỗi: ' + error.message, 'error');
        throw error;
    }
}

// Notification system
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `fixed top-4 right-4 p-4 rounded-lg shadow-lg z-50 ${
        type === 'error' ? 'bg-red-500 text-white' :
            type === 'success' ? 'bg-green-500 text-white' :
                'bg-blue-500 text-white'
    }`;
    notification.textContent = message;

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, 5000);
}

// User Management Functions
function suspendUser(userId) {
    if (confirm('Are you sure you want to suspend this user?')) {
        makeApiCall('/admin/api/users/suspend', 'POST', { userId })
            .then(data => {
                if (data.success) {
                    showNotification('User suspended successfully', 'success');
                    location.reload();
                }
            })
            .catch(error => {
                showNotification('Failed to suspend user: ' + error.message, 'error');
            });
    }
}

function activateUser(userId) {
    if (confirm('Are you sure you want to activate this user?')) {
        makeApiCall('/admin/api/users/activate', 'POST', { userId })
            .then(data => {
                if (data.success) {
                    showNotification('User activated successfully', 'success');
                    location.reload();
                }
            })
            .catch(error => {
                showNotification('Failed to activate user: ' + error.message, 'error');
            });
    }
}

// Vehicle Management Functions
function updateVehicleStatus(vehicleId, status) {
    if (confirm(`Are you sure you want to change this vehicle status to ${status}?`)) {
        makeApiCall('/admin/api/vehicles/status', 'POST', { vehicleId, status })
            .then(data => {
                if (data.success) {
                    showNotification('Vehicle status updated successfully', 'success');
                    location.reload();
                }
            })
            .catch(error => {
                showNotification('Failed to update vehicle status: ' + error.message, 'error');
            });
    }
}

// Booking Management Functions
function updateBookingStatus(bookingId, status) {
    if (confirm(`Are you sure you want to change this booking status to ${status}?`)) {
        makeApiCall('/admin/api/bookings/status', 'POST', { bookingId, status })
            .then(data => {
                if (data.success) {
                    showNotification('Booking status updated successfully', 'success');
                    location.reload();
                }
            })
            .catch(error => {
                showNotification('Failed to update booking status: ' + error.message, 'error');
            });
    }
}

// Contract Management Functions
function updateContractStatus(contractId, status) {
    if (confirm(`Are you sure you want to change this contract status to ${status}?`)) {
        makeApiCall('/admin/api/contracts/status', 'POST', { contractId, status })
            .then(data => {
                if (data.success) {
                    showNotification('Contract status updated successfully', 'success');
                    location.reload();
                }
            })
            .catch(error => {
                showNotification('Failed to update contract status: ' + error.message, 'error');
            });
    }
}

// Payment Management Functions
function refundPayment(paymentId) {
    const reason = prompt('Please enter refund reason (optional):');
    if (confirm('Are you sure you want to refund this payment?')) {
        makeApiCall('/admin/api/payments/refund', 'POST', { paymentId, reason: reason || '' })
            .then(data => {
                if (data.success) {
                    showNotification('Payment refunded successfully', 'success');
                    location.reload();
                }
            })
            .catch(error => {
                showNotification('Failed to refund payment: ' + error.message, 'error');
            });
    }
}

// Discount Management Functions
function showAddDiscountModal() {
    // TODO: Implement modal for adding discount
    showNotification('Add Discount feature coming soon', 'info');
}

function editDiscount(discountId) {
    // TODO: Implement modal for editing discount
    showNotification('Edit Discount feature coming soon', 'info');
}

function deleteDiscount(discountId) {
    if (confirm('Are you sure you want to delete this discount?')) {
        makeApiCall('/admin/api/discounts/delete', 'POST', { discountId })
            .then(data => {
                if (data.success) {
                    showNotification('Discount deleted successfully', 'success');
                    location.reload();
                }
            })
            .catch(error => {
                showNotification('Failed to delete discount: ' + error.message, 'error');
            });
    }
}

// Insurance Management Functions
function showAddInsuranceModal() {
    // TODO: Implement modal for adding insurance
    showNotification('Add Insurance feature coming soon', 'info');
}

function editInsurance(insuranceId) {
    // TODO: Implement modal for editing insurance
    showNotification('Edit Insurance feature coming soon', 'info');
}

function deleteInsurance(insuranceId) {
    if (confirm('Are you sure you want to delete this insurance policy?')) {
        makeApiCall('/admin/api/insurance/delete', 'POST', { insuranceId })
            .then(data => {
                if (data.success) {
                    showNotification('Insurance deleted successfully', 'success');
                    location.reload();
                }
            })
            .catch(error => {
                showNotification('Failed to delete insurance: ' + error.message, 'error');
            });
    }
}

// Notification Management Functions
function showSendNotificationModal() {
    // TODO: Implement modal for sending notification
    showNotification('Send Notification feature coming soon', 'info');
}

function deleteNotification(notificationId) {
    if (confirm('Are you sure you want to delete this notification?')) {
        makeApiCall('/admin/api/notifications/delete', 'POST', { notificationId })
            .then(data => {
                if (data.success) {
                    showNotification('Notification deleted successfully', 'success');
                    location.reload();
                }
            })
            .catch(error => {
                showNotification('Failed to delete notification: ' + error.message, 'error');
            });
    }
}

// Chart initialization
function initializeCharts() {
    // Revenue Chart
    const revenueCtx = document.getElementById('revenueChart');
    if (revenueCtx) {
        new Chart(revenueCtx.getContext('2d'), {
            type: 'line',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                datasets: [{
                    label: 'Revenue (VND)',
                    data: [12000000, 19000000, 15000000, 25000000, 22000000, 30000000],
                    borderColor: 'rgb(59, 130, 246)',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    tension: 0.4,
                    fill: true,
                    pointRadius: 4,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                aspectRatio: 2,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return (value / 1000000).toFixed(0) + 'M VND';
                            },
                            font: {
                                size: 11
                            }
                        },
                        grid: {
                            color: 'rgba(0, 0, 0, 0.1)'
                        }
                    },
                    x: {
                        ticks: {
                            font: {
                                size: 11
                            }
                        },
                        grid: {
                            color: 'rgba(0, 0, 0, 0.1)'
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                        labels: {
                            font: {
                                size: 12
                            }
                        }
                    }
                }
            }
        });
    }

    // Booking Trends Chart
    const bookingTrendsCtx = document.getElementById('bookingTrendsChart');
    if (bookingTrendsCtx) {
        new Chart(bookingTrendsCtx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
                datasets: [{
                    label: 'Bookings',
                    data: [12, 19, 15, 25, 22, 30, 18],
                    backgroundColor: 'rgba(34, 197, 94, 0.8)',
                    borderColor: 'rgb(34, 197, 94)',
                    borderWidth: 1,
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                aspectRatio: 2,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            font: {
                                size: 11
                            }
                        },
                        grid: {
                            color: 'rgba(0, 0, 0, 0.1)'
                        }
                    },
                    x: {
                        ticks: {
                            font: {
                                size: 11
                            }
                        },
                        grid: {
                            color: 'rgba(0, 0, 0, 0.1)'
                        }
                    }
                },
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                        labels: {
                            font: {
                                size: 12
                            }
                        }
                    }
                }
            }
        });
    }

    // Vehicle Status Chart
    const vehicleStatusCtx = document.getElementById('vehicleStatusChart');
    if (vehicleStatusCtx) {
        new Chart(vehicleStatusCtx.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: ['Available', 'In Use', 'Maintenance'],
                datasets: [{
                    data: [15, 8, 2],
                    backgroundColor: [
                        'rgba(34, 197, 94, 0.8)',
                        'rgba(59, 130, 246, 0.8)',
                        'rgba(239, 68, 68, 0.8)'
                    ],
                    borderColor: [
                        'rgb(34, 197, 94)',
                        'rgb(59, 130, 246)',
                        'rgb(239, 68, 68)'
                    ],
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                aspectRatio: 1.5,
                plugins: {
                    legend: {
                        display: true,
                        position: 'bottom',
                        labels: {
                            font: {
                                size: 11
                            },
                            padding: 15
                        }
                    }
                }
            }
        });
    }
}

// Auto-refresh functionality
function startAutoRefresh() {
    refreshInterval = setInterval(refreshData, 30000); // Refresh every 30 seconds
}

function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
}

function refreshData() {
    fetch('/admin/api/analytics')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.text(); // Get as text first
        })
        .then(text => {
            try {
                const data = JSON.parse(text);
                console.log('Updated analytics:', data);
                updateDashboardData(data);
            } catch (parseError) {
                console.error('Error parsing analytics JSON:', parseError);
                console.log('Response text:', text.substring(0, 200) + '...');
            }
        })
        .catch(error => {
            console.error('Error updating analytics:', error);
        });
}

function updateDashboardData(data) {
    // Update statistics cards
    const totalUsers = document.querySelector('[data-stat="users"]');
    const totalVehicles = document.querySelector('[data-stat="vehicles"]');
    const totalBookings = document.querySelector('[data-stat="bookings"]');
    const totalRevenue = document.querySelector('[data-stat="revenue"]');

    if (totalUsers && data.totalUsers) totalUsers.textContent = data.totalUsers;
    if (totalVehicles && data.totalVehicles) totalVehicles.textContent = data.totalVehicles;
    if (totalBookings && data.totalBookings) totalBookings.textContent = data.totalBookings;
    if (totalRevenue && data.totalRevenue) totalRevenue.textContent = data.totalRevenue;
}

// User management functions removed - now handled server-side

// Logout functionality
function logout() {
    if (confirm('Bạn có chắc chắn muốn đăng xuất?')) {
        // Show loading state
        showNotification('Đang đăng xuất...', 'info');

        // Create a form to submit logout request
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/logout';

        // Add CSRF token if available
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        if (csrfToken) {
            const csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = '_csrf';
            csrfInput.value = csrfToken.getAttribute('content');
            form.appendChild(csrfInput);
        }

        document.body.appendChild(form);
        form.submit();
    }
}


// Test function
function testModal() {
    console.log('Test function called');
    alert('JavaScript đang hoạt động!');

    // Test simple modal first
    const testModal = document.getElementById('testModal');
    if (testModal) {
        console.log('Test modal found, showing...');
        testModal.classList.remove('hidden');
        testModal.style.display = 'flex';
    } else {
        console.error('Test modal not found!');
    }
}

function closeTestModal() {
    const testModal = document.getElementById('testModal');
    if (testModal) {
        testModal.classList.add('hidden');
        testModal.style.display = 'none';
    }
}

// Export functions for global access
window.switchTab = switchTab;
window.openSidebar = openSidebar;
window.closeSidebar = closeSidebar;
window.logout = logout;
window.suspendUser = suspendUser;
window.activateUser = activateUser;
window.updateVehicleStatus = updateVehicleStatus;
window.updateBookingStatus = updateBookingStatus;
window.updateContractStatus = updateContractStatus;
window.refundPayment = refundPayment;
window.showAddDiscountModal = showAddDiscountModal;
window.editDiscount = editDiscount;
window.deleteDiscount = deleteDiscount;
window.showAddInsuranceModal = showAddInsuranceModal;
window.editInsurance = editInsurance;
window.deleteInsurance = deleteInsurance;
window.showSendNotificationModal = showSendNotificationModal;
window.deleteNotification = deleteNotification;
window.testModal = testModal;
window.closeTestModal = closeTestModal;
window.resetAllRows = resetAllRows;
window.forceShowAllRows = forceShowAllRows;