/* =============================================
   ADMIN BOOKINGS MANAGEMENT - JAVASCRIPT
   Modern, Accessible, Performance-Optimized
   ============================================= */

(function() {
    'use strict';

    // State Management
    var state = {
        bookings: [],
        filteredBookings: [],
        currentFilters: {
            status: '',
            rentalType: '',
            search: ''
        }
    };

    // DOM Elements
    var elements = {
        searchInput: null,
        statusFilter: null,
        rentalTypeFilter: null,
        bookingsTableBody: null,
        refreshBtn: null,
        exportBtn: null,
        emptyState: null,
        totalBookings: null,
        pendingBookings: null,
        approvedBookings: null,
        ongoingBookings: null
    };

    // Initialize
    function init() {
        cacheDOMElements();
        attachEventListeners();
        loadBookings();
    }

    // Cache DOM Elements
    function cacheDOMElements() {
        elements.searchInput = document.getElementById('searchInput');
        elements.statusFilter = document.getElementById('statusFilter');
        elements.rentalTypeFilter = document.getElementById('rentalTypeFilter');
        elements.bookingsTableBody = document.getElementById('bookingsTableBody');
        elements.refreshBtn = document.getElementById('refreshBtn');
        elements.exportBtn = document.getElementById('exportBtn');
        elements.emptyState = document.getElementById('emptyState');
        elements.totalBookings = document.getElementById('totalBookings');
        elements.pendingBookings = document.getElementById('pendingBookings');
        elements.approvedBookings = document.getElementById('approvedBookings');
        elements.ongoingBookings = document.getElementById('ongoingBookings');
    }

    // Attach Event Listeners
    function attachEventListeners() {
        if (elements.searchInput) {
            elements.searchInput.addEventListener('input', debounce(handleSearch, 300));
        }
        if (elements.statusFilter) {
            elements.statusFilter.addEventListener('change', handleStatusFilter);
        }
        if (elements.rentalTypeFilter) {
            elements.rentalTypeFilter.addEventListener('change', handleRentalTypeFilter);
        }
        if (elements.refreshBtn) {
            elements.refreshBtn.addEventListener('click', handleRefresh);
        }
        if (elements.exportBtn) {
            elements.exportBtn.addEventListener('click', handleExport);
        }
    }

    // Debounce Function
    function debounce(func, wait) {
        var timeout;
        return function executedFunction() {
            var context = this;
            var args = arguments;
            var later = function() {
                timeout = null;
                func.apply(context, args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // Load Bookings
    function loadBookings() {
        showLoading();
        
        var url = '/admin/api/bookings';
        var params = [];
        
        if (state.currentFilters.status) {
            params.push('status=' + encodeURIComponent(state.currentFilters.status));
        }
        if (state.currentFilters.search) {
            params.push('search=' + encodeURIComponent(state.currentFilters.search));
        }
        
        if (params.length > 0) {
            url += '?' + params.join('&');
        }

        fetch(url)
            .then(function(response) {
                console.log('Response status:', response.status);
                if (!response.ok) {
                    throw new Error('Network response was not ok: ' + response.status);
                }
                return response.json();
            })
            .then(function(data) {
                console.log('Received bookings data:', data);
                state.bookings = data || [];
                console.log('Total bookings:', state.bookings.length);
                applyFilters();
                renderBookings();
                updateStatistics();
            })
            .catch(function(error) {
                console.error('Error loading bookings:', error);
                showError('Failed to load bookings: ' + error.message);
                showEmptyState();
            });
    }

    // Apply Filters
    function applyFilters() {
        state.filteredBookings = state.bookings.filter(function(booking) {
            var matchesRentalType = !state.currentFilters.rentalType || 
                                   booking.rentalType === state.currentFilters.rentalType;
            return matchesRentalType;
        });
    }

    // Render Bookings
    function renderBookings() {
        if (!elements.bookingsTableBody) return;

        if (state.filteredBookings.length === 0) {
            showEmptyState();
            return;
        }

        hideEmptyState();
        
        var html = '';
        state.filteredBookings.forEach(function(booking) {
            html += createBookingRow(booking);
        });
        
        elements.bookingsTableBody.innerHTML = html;
        attachRowEventListeners();
    }

    // Create Booking Row
    function createBookingRow(booking) {
        console.log('Creating row for booking:', booking);
        var statusClass = getStatusClass(booking.status);
        var rentalTypeClass = getRentalTypeClass(booking.rentalType);
        
        var pickupDate = formatDateTime(booking.pickupDateTime);
        var returnDate = formatDateTime(booking.returnDateTime);
        var amount = formatCurrency(booking.totalAmount);
        
        var html = '<tr class="table-row-hover">';
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<div class="text-sm font-medium text-gray-900">' + escapeHtml(booking.bookingCode || '-') + '</div>';
        html += '<div class="text-xs text-gray-500">' + escapeHtml(booking.rentalType || 'daily') + '</div>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<div class="text-sm font-medium text-gray-900">' + escapeHtml(booking.userName || '-') + '</div>';
        html += '<div class="text-xs text-gray-500">' + escapeHtml(booking.userEmail || '-') + '</div>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<div class="text-sm font-medium text-gray-900">' + escapeHtml(booking.vehicleModel || '-') + '</div>';
        html += '<div class="text-xs text-gray-500">' + escapeHtml(booking.licensePlate || '-') + '</div>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<div class="text-sm text-gray-900">' + pickupDate + '</div>';
        html += '<div class="text-xs text-gray-500">' + returnDate + '</div>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<span class="status-badge ' + statusClass + '">' + escapeHtml(booking.status) + '</span>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">';
        html += amount;
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap text-sm font-medium">';
        html += '<button class="action-button action-button-view mr-3" data-action="view" data-id="' + booking.bookingId + '">';
        html += '<i class="fas fa-eye mr-1"></i>View';
        html += '</button>';
        
        if (booking.status === 'Pending') {
            html += '<button class="action-button action-button-approve mr-3" data-action="approve" data-id="' + booking.bookingId + '">';
            html += '<i class="fas fa-check mr-1"></i>Approve';
            html += '</button>';
            html += '<button class="action-button action-button-reject" data-action="reject" data-id="' + booking.bookingId + '">';
            html += '<i class="fas fa-times mr-1"></i>Reject';
            html += '</button>';
        } else if (booking.status === 'Approved') {
            html += '<button class="action-button action-button-complete" data-action="complete" data-id="' + booking.bookingId + '">';
            html += '<i class="fas fa-check-circle mr-1"></i>Complete';
            html += '</button>';
        }
        
        html += '</td>';
        html += '</tr>';
        
        return html;
    }

    // Attach Row Event Listeners
    function attachRowEventListeners() {
        var actionButtons = document.querySelectorAll('[data-action]');
        actionButtons.forEach(function(button) {
            button.addEventListener('click', handleAction);
        });
    }

    // Handle Action
    function handleAction(event) {
        var button = event.currentTarget;
        var action = button.getAttribute('data-action');
        var bookingId = button.getAttribute('data-id');
        
        switch(action) {
            case 'view':
                viewBookingDetail(bookingId);
                break;
            case 'approve':
                updateBookingStatus(bookingId, 'Approved');
                break;
            case 'reject':
                showStatusUpdateModal(bookingId, 'Rejected');
                break;
            case 'complete':
                updateBookingStatus(bookingId, 'Completed');
                break;
        }
    }

    // View Booking Detail
    function viewBookingDetail(bookingId) {
        fetch('/admin/api/bookings/' + bookingId)
            .then(function(response) {
                if (!response.ok) throw new Error('Failed to load booking details');
                return response.json();
            })
            .then(function(booking) {
                showBookingDetailModal(booking);
            })
            .catch(function(error) {
                console.error('Error:', error);
                alert('Failed to load booking details');
            });
    }

    // Show Booking Detail Modal
    function showBookingDetailModal(booking) {
        var modal = document.getElementById('bookingDetailModal');
        var content = document.getElementById('bookingDetailContent');
        
        if (!modal || !content) return;
        
        var html = '<div class="detail-grid">';
        html += createDetailItem('Booking Code', booking.bookingCode);
        html += createDetailItem('Status', '<span class="status-badge ' + getStatusClass(booking.status) + '">' + booking.status + '</span>');
        html += createDetailItem('Customer', booking.userName);
        html += createDetailItem('Email', booking.userEmail);
        html += createDetailItem('Phone', booking.userPhone || '-');
        html += createDetailItem('Vehicle', booking.vehicleModel);
        html += createDetailItem('License Plate', booking.licensePlate);
        html += createDetailItem('Rental Type', booking.rentalType);
        html += createDetailItem('Pickup Date', formatDateTime(booking.pickupDateTime));
        html += createDetailItem('Return Date', formatDateTime(booking.returnDateTime));
        html += createDetailItem('Total Amount', formatCurrency(booking.totalAmount));
        html += createDetailItem('Payment Method', booking.expectedPaymentMethod || '-');
        html += createDetailItem('Created Date', formatDateTime(booking.createdDate));
        
        if (booking.cancelReason) {
            html += '<div class="col-span-2">';
            html += createDetailItem('Cancel Reason', booking.cancelReason);
            html += '</div>';
        }
        
        html += '</div>';
        
        content.innerHTML = html;
        modal.classList.remove('hidden');
    }

    // Create Detail Item
    function createDetailItem(label, value) {
        return '<div class="detail-item">' +
               '<div class="detail-label">' + escapeHtml(label) + '</div>' +
               '<div class="detail-value">' + value + '</div>' +
               '</div>';
    }

    // Update Booking Status
    function updateBookingStatus(bookingId, status, reason) {
        var data = { status: status };
        if (reason) {
            data.reason = reason;
        }
        
        fetch('/admin/api/bookings/' + bookingId + '/status', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        })
        .then(function(response) {
            if (!response.ok) throw new Error('Failed to update status');
            return response.json();
        })
        .then(function(result) {
            if (result.success) {
                showSuccess('Booking status updated successfully');
                loadBookings();
            } else {
                throw new Error(result.message || 'Update failed');
            }
        })
        .catch(function(error) {
            console.error('Error:', error);
            showError('Failed to update booking status');
        });
    }

    // Show Status Update Modal
    function showStatusUpdateModal(bookingId, status) {
        var modal = document.getElementById('statusUpdateModal');
        var form = document.getElementById('statusUpdateForm');
        var bookingIdInput = document.getElementById('updateBookingId');
        var statusSelect = document.getElementById('updateStatus');
        var reasonField = document.getElementById('reasonField');
        
        if (!modal || !form) return;
        
        bookingIdInput.value = bookingId;
        statusSelect.value = status;
        
        if (status === 'Rejected' || status === 'Cancelled') {
            reasonField.classList.remove('hidden');
        } else {
            reasonField.classList.add('hidden');
        }
        
        modal.classList.remove('hidden');
        
        form.onsubmit = function(e) {
            e.preventDefault();
            var reason = document.getElementById('updateReason').value;
            updateBookingStatus(bookingId, status, reason);
            closeStatusUpdateModal();
        };
    }

    // Handle Search
    function handleSearch(event) {
        state.currentFilters.search = event.target.value;
        loadBookings();
    }

    // Handle Status Filter
    function handleStatusFilter(event) {
        state.currentFilters.status = event.target.value;
        loadBookings();
    }

    // Handle Rental Type Filter
    function handleRentalTypeFilter(event) {
        state.currentFilters.rentalType = event.target.value;
        applyFilters();
        renderBookings();
    }

    // Handle Refresh
    function handleRefresh() {
        loadBookings();
    }

    // Handle Export
    function handleExport() {
        var csv = 'Booking Code,Customer,Email,Vehicle,License Plate,Pickup Date,Return Date,Status,Amount\n';
        
        state.filteredBookings.forEach(function(booking) {
            csv += [
                booking.bookingCode,
                booking.userName,
                booking.userEmail,
                booking.vehicleModel,
                booking.licensePlate,
                formatDateTime(booking.pickupDateTime),
                formatDateTime(booking.returnDateTime),
                booking.status,
                booking.totalAmount
            ].join(',') + '\n';
        });
        
        var blob = new Blob([csv], { type: 'text/csv' });
        var url = window.URL.createObjectURL(blob);
        var a = document.createElement('a');
        a.href = url;
        a.download = 'bookings_' + new Date().toISOString().split('T')[0] + '.csv';
        a.click();
        window.URL.revokeObjectURL(url);
    }

    // Update Statistics
    function updateStatistics() {
        var total = state.bookings.length;
        var pending = state.bookings.filter(function(b) { return b.status === 'Pending'; }).length;
        var approved = state.bookings.filter(function(b) { return b.status === 'Approved'; }).length;
        var ongoing = state.bookings.filter(function(b) { return b.status === 'Ongoing'; }).length;
        
        if (elements.totalBookings) elements.totalBookings.textContent = total;
        if (elements.pendingBookings) elements.pendingBookings.textContent = pending;
        if (elements.approvedBookings) elements.approvedBookings.textContent = approved;
        if (elements.ongoingBookings) elements.ongoingBookings.textContent = ongoing;
    }

    // Utility Functions
    function getStatusClass(status) {
        var classes = {
            'Pending': 'status-pending',
            'Approved': 'status-approved',
            'Rejected': 'status-rejected',
            'Ongoing': 'status-ongoing',
            'Completed': 'status-completed',
            'Cancelled': 'status-cancelled'
        };
        return classes[status] || 'status-pending';
    }

    function getRentalTypeClass(type) {
        var classes = {
            'hourly': 'rental-hourly',
            'daily': 'rental-daily',
            'monthly': 'rental-monthly'
        };
        return classes[type] || 'rental-daily';
    }

    function formatDateTime(dateString) {
        if (!dateString) return '-';
        var date = new Date(dateString);
        return date.toLocaleDateString('vi-VN') + ' ' + date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    }

    function formatCurrency(amount) {
        if (!amount) return '0 ₫';
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    }

    function escapeHtml(text) {
        if (!text) return '';
        var map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.toString().replace(/[&<>"']/g, function(m) { return map[m]; });
    }

    function showLoading() {
        if (elements.bookingsTableBody) {
            elements.bookingsTableBody.innerHTML = '<tr><td colspan="7" class="px-6 py-12 text-center"><i class="fas fa-spinner fa-spin text-3xl text-gray-400 mb-3"></i><p class="text-gray-500">Loading bookings...</p></td></tr>';
        }
    }

    function showEmptyState() {
        if (elements.bookingsTableBody) {
            elements.bookingsTableBody.innerHTML = '<tr><td colspan="7" class="px-6 py-12 text-center"><i class="fas fa-calendar-times text-6xl text-gray-300 mb-4"></i><p class="text-lg font-medium text-gray-900 mb-2">No bookings found</p><p class="text-gray-500">Try adjusting your search or filter criteria</p></td></tr>';
        }
    }

    function hideEmptyState() {
        // Empty state is handled in table body
    }

    function showSuccess(message) {
        alert(message);
    }

    function showError(message) {
        alert(message);
    }

    // Global Functions for Modal
    window.closeBookingDetailModal = function() {
        var modal = document.getElementById('bookingDetailModal');
        if (modal) modal.classList.add('hidden');
    };

    window.closeStatusUpdateModal = function() {
        var modal = document.getElementById('statusUpdateModal');
        if (modal) modal.classList.add('hidden');
    };

    // Initialize on DOM Ready
    console.log('admin-bookings.js loaded!');
    if (document.readyState === 'loading') {
        console.log('Waiting for DOM...');
        document.addEventListener('DOMContentLoaded', init);
    } else {
        console.log('DOM already ready, initializing...');
        init();
    }
})();
