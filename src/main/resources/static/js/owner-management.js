// --- Dữ liệu sẽ được chèn từ HTML ---
// const allBookingsData = [];
// const csrfToken = null;
// const csrfHeaderName = null;
// const currentPage = 'bookings';

// --- Hàm render một booking item ---
function renderBookingItem(booking) {
    const status = booking.status || 'Unknown';
    const statusLower = status.toLowerCase();
    const statusMap = {
        'pending': { text: 'Chờ duyệt', class: 'status-pending', icon: 'fa-clock text-yellow-500' },
        'approved': { text: 'Đã duyệt', class: 'status-approved', icon: 'fa-check-circle text-green-500' },
        'awaitingdeposit': { text: 'Chờ cọc', class: 'status-approved', icon: 'fa-hand-holding-usd text-green-500' },
        'confirmed': { text: 'Đã cọc', class: 'status-ongoing', icon: 'fa-calendar-check text-blue-500' },
        'ongoing': { text: 'Đang thuê', class: 'status-ongoing', icon: 'fa-car-side text-blue-500' },
        'completed': { text: 'Hoàn thành', class: 'status-completed', icon: 'fa-flag-checkered text-gray-500' },
        'rejected': { text: 'Từ chối', class: 'status-rejected', icon: 'fa-ban text-red-500' },
        'cancelled': { text: 'Đã hủy', class: 'status-cancelled', icon: 'fa-times-circle text-red-500' },
        'noshow': { text: 'Không Đến', class: 'status-cancelled', icon: 'fa-question-circle text-gray-500' }
    };
    const statusInfo = statusMap[statusLower] || statusMap['unknown'];

    const customerName = (booking.user?.firstName || '') + ' ' + (booking.user?.lastName || booking.user?.username || 'N/A');
    const vehicleInfo = (booking.vehicle?.vehicleModel || 'N/A') + ' (' + (booking.vehicle?.licensePlate || 'N/A') + ')';
    const pickup = booking.pickupDateTime ? new Date(booking.pickupDateTime).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'N/A';
    const returnDate = booking.returnDateTime ? new Date(booking.returnDateTime).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'N/A';
    const created = booking.createdDate ? new Date(booking.createdDate).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : 'N/A';
    const amount = booking.totalAmount ? booking.totalAmount.toLocaleString('vi-VN') + ' VND' : 'N/A';
    const pickupLocation = booking.pickupLocation || 'Chưa xác định';

    // --- Actions (Nút bấm) ---
    let actionsHtml = `
        <button onclick="viewBooking('${booking.bookingId}')"
                class="text-blue-600 hover:text-blue-800 transition-colors" title="Xem chi tiết">
            <i class="fas fa-eye text-lg"></i>
        </button>
    `;
    if (statusLower === 'pending') {
        actionsHtml += `
            <button onclick="approveBooking('${booking.bookingId}')"
                    class="text-green-600 hover:text-green-800 transition-colors ml-3" title="Duyệt booking">
                <i class="fas fa-check text-lg"></i>
            </button>
            <button onclick="rejectBooking('${booking.bookingId}')"
                    class="text-red-600 hover:text-red-800 transition-colors ml-3" title="Từ chối booking">
                <i class="fas fa-times text-lg"></i>
            </button>
        `;
    }

    if (statusLower === 'confirmed') {
        actionsHtml += `
            <button onclick="handoverBooking('${booking.bookingId}')"
                    class="text-green-600 hover:text-green-800 transition-colors ml-3" title="Giao xe">
                <i class="fas fa-key text-lg"></i>
            </button>
        `;
    }

    if (statusLower === 'ongoing') {
        actionsHtml += `
            <button onclick="openCompleteTripModal('${booking.bookingId}')"
                    class="text-blue-600 hover:text-blue-800 transition-colors ml-3" title="Hoàn thành chuyến đi">
                <i class="fas fa-flag-checkered text-lg"></i>
            </button>
        `;
    }

    // --- HTML của thẻ booking ---
    return `
        <div class="booking-item grid grid-cols-1 md:grid-cols-12 gap-4 items-center"
             data-status="${status}"
             data-code="${booking.bookingCode}"
             data-customer="${customerName}"
             data-created="${booking.createdDate}"
             data-amount="${booking.totalAmount}">

            <div class="md:col-span-4">
                <p class="font-semibold text-primary text-sm">${booking.bookingCode}</p>
                <p class="text-sm text-gray-800 font-medium truncate">${customerName}</p>
                <p class="text-xs text-gray-500 truncate">${vehicleInfo}</p>
            </div>

            <div class="md:col-span-3 text-sm">
                <p class="text-gray-700"><strong class="font-medium text-gray-500">Ở:</strong> ${pickupLocation}</p>
                <p class="text-gray-700"><strong class="font-medium text-gray-500">Từ:</strong> ${pickup}</p>
                <p class="text-gray-700"><strong class="font-medium text-gray-500">Đến:</strong> ${returnDate}</p>
            </div>

            <div class="md:col-span-2">
                <span class="status-badge ${statusInfo.class}">
                    <i class="fas ${statusInfo.icon} mr-2"></i> ${statusInfo.text}
                </span>
            </div>

            <div class="md:col-span-2 md:text-right">
                <p class="text-sm font-bold text-gray-800">${amount}</p>
                <p class="text-xs text-gray-500">Ngày tạo: ${created}</p>
            </div>

            <div class="md:col-span-1 md:text-right">
                ${actionsHtml}
            </div>
        </div>
    `;
}

// --- Hàm thay đổi tab trạng thái ---
function changeStatusTab(tabElement) {
    document.querySelectorAll('#status-tabs .status-tab').forEach(tab => {
        tab.classList.remove('active');
    });
    tabElement.classList.add('active');
    filterAndSortBookings();
}


// --- Hàm sắp xếp và lọc (đọc từ tab) ---
function filterAndSortBookings() {
    const statusFilterElement = document.querySelector('#status-tabs .status-tab.active');
    if (!statusFilterElement) return;

    const statusFilter = statusFilterElement.dataset.status;
    const searchInput = document.getElementById("search-input").value.toLowerCase();
    const sortSelect = document.getElementById("sort-select").value;

    let filteredBookings = allBookingsData.filter(booking => {
        const status = booking.status || 'Unknown';
        const customerName = (booking.user?.firstName || '') + ' ' + (booking.user?.lastName || booking.user?.username || 'N/A');
        const code = booking.bookingCode || '';
        const statusMatch = !statusFilter || statusFilter.split(',').includes(status);
        const searchMatch = !searchInput ||
            code.toLowerCase().includes(searchInput) ||
            customerName.toLowerCase().includes(searchInput);
        return statusMatch && searchMatch;
    });

    filteredBookings.sort((a, b) => {
        switch (sortSelect) {
            case "newest": return new Date(b.createdDate) - new Date(a.createdDate);
            case "oldest": return new Date(a.createdDate) - new Date(b.createdDate);
            case "amount-high": return (b.totalAmount || 0) - (a.totalAmount || 0);
            case "amount-low": return (a.totalAmount || 0) - (b.totalAmount || 0);
            default: return 0;
        }
    });

    renderFilteredBookings(filteredBookings);
}

// --- Hàm render danh sách ---
function renderFilteredBookings(filteredBookings) {
    const container = document.getElementById('booking-list-container');
    const noBookingsMessage = document.getElementById('no-bookings-message');

    while (container.firstChild && container.firstChild !== noBookingsMessage) {
        container.removeChild(container.firstChild);
    }

    if (filteredBookings.length === 0) {
        noBookingsMessage?.classList.remove('hidden');
    } else {
        noBookingsMessage?.classList.add('hidden');
        let allItemsHtml = '';
        filteredBookings.forEach(booking => {
            allItemsHtml += renderBookingItem(booking);
        });
        container.insertAdjacentHTML('afterbegin', allItemsHtml);
    }
}


// --- Các hàm Modal ---

let currentBookingId = null;
let currentCompletingBookingId = null;
let paymentPollInterval = null;

function getCsrfHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (typeof csrfToken !== 'undefined' && csrfToken && typeof csrfHeaderName !== 'undefined' && csrfHeaderName) {
        headers[csrfHeaderName] = csrfToken;
    }
    return headers;
}

function getCsrfHeaderOnly() {
    const headers = {};
    if (typeof csrfToken !== 'undefined' && csrfToken && typeof csrfHeaderName !== 'undefined' && csrfHeaderName) {
        headers[csrfHeaderName] = csrfToken;
    }
    return headers;
}


function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if(modal) modal.classList.add('hidden');

    if (modalId === 'reject-modal') {
        const rejectReason = document.getElementById('reject-reason');
        if(rejectReason) rejectReason.value = '';
    }

    if (modalId === 'complete-trip-modal') {
        // We no longer reset the ID here as it caused bugs.
        // The ID is now read from the hidden input when needed.
        document.getElementById('complete-trip-form').reset();
        document.getElementById('return-images-error').classList.add('hidden');
    } else if (modalId === 'transfer-payment-modal') {
        stopPaymentPolling();
    }
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if(modal) modal.classList.remove('hidden');
}


function showNotification(isSuccess, message) {
    const successEl = document.getElementById('success-notification');
    const errorEl = document.getElementById('error-notification');

    if(successEl) {
        successEl.style.transform = 'translateX(100%)';
        const span = successEl.querySelector('span');
        if(span) span.textContent = '';
    }
    if(errorEl) {
        errorEl.style.transform = 'translateX(100%)';
        const span = errorEl.querySelector('span');
        if(span) span.textContent = '';
    }

    if (isSuccess && successEl) {
        const span = successEl.querySelector('span');
        if(span) span.textContent = message;
        successEl.style.transform = 'translateX(0)';
        setTimeout(() => { successEl.style.transform = 'translateX(100%)'; }, 4000);
    } else if (!isSuccess && errorEl) {
        const span = errorEl.querySelector('span');
        if(span) span.textContent = message;
        errorEl.style.transform = 'translateX(0)';
        setTimeout(() => { errorEl.style.transform = 'translateX(100%)'; }, 4000);
    }
}

function closeNotification(button) {
    const notificationDiv = button.closest('div[id$="-notification"]');
    if (notificationDiv) {
        notificationDiv.style.transform = 'translateX(100%)';
    }
}

// Xem chi tiết
function viewBooking(bookingId) {
    const modal = document.getElementById('booking-detail-modal');
    const content = document.getElementById('booking-detail-content');
    if (!modal || !content) return;

    modal.classList.remove('hidden');
    content.innerHTML = `<div class="text-center py-8"><i class="fas fa-spinner fa-spin text-4xl text-primary"></i><p class="mt-2 text-gray-600">Loading...</p></div>`;

    fetch(`/owner/management/bookings/${bookingId}`)
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            content.innerHTML = `
                <div class="space-y-4">
                    <div class="p-4 border rounded-lg bg-gray-50">
                        <h4 class="text-lg font-semibold text-gray-700 mb-3 border-b pb-2 flex items-center"><i class="fas fa-receipt mr-3 text-primary"></i>Thông tin Booking</h4>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                            <p><strong>Mã:</strong> <span>${data.bookingCode || 'N/A'}</span></p>
                            <p><strong>Trạng thái:</strong> <span class="status-badge status-${data.status ? data.status.toLowerCase() : ''}">${data.status || 'N/A'}</span></p>
                            <p><strong>Tổng tiền:</strong> <span class="font-bold text-green-600">${data.totalAmount ? data.totalAmount.toLocaleString('vi-VN') + ' VND' : 'N/A'}</span></p>
                            <p><strong>Loại thuê:</strong> <span>${data.rentalType || 'N/A'}</span></p>
                            <p><strong>Nhận xe:</strong> <span>${data.pickupDateTime ? new Date(data.pickupDateTime).toLocaleString('vi-VN') : 'N/A'}</span></p>
                            <p><strong>Trả xe:</strong> <span>${data.returnDateTime ? new Date(data.returnDateTime).toLocaleString('vi-VN') : 'N/A'}</span></p>
                            <p class="md:col-span-2"><strong>Địa điểm giao xe:</strong> <span class="font-medium text-blue-600">${data.pickupLocation || 'Chưa có thông tin'}</span></p>
                        </div>
                    </div>
                    <div class="p-4 border rounded-lg bg-gray-50">
                        <h4 class="text-lg font-semibold text-gray-700 mb-3 border-b pb-2 flex items-center"><i class="fas fa-user-circle mr-3 text-primary"></i>Thông tin khách hàng</h4>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                            <p><strong>Tên:</strong> <span>${data.customerName || 'N/A'}</span></p>
                            <p><strong>Email:</strong> <span>${data.customerEmail || 'N/A'}</span></p>
                            <p><strong>SĐT:</strong> <span>${data.customerPhone || 'N/A'}</span></p>
                            <p><strong>Ngày sinh:</strong> <span>${data.customerDOB ? new Date(data.customerDOB).toLocaleDateString('vi-VN') : 'N/A'}</span></p>
                        </div>
                    </div>
                    <div class="p-4 border rounded-lg bg-gray-50">
                        <h4 class="text-lg font-semibold text-gray-700 mb-3 border-b pb-2 flex items-center"><i class="fas fa-car-side mr-3 text-primary"></i>Thông tin xe</h4>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                            <p><strong>Mẫu xe:</strong> <span>${data.vehicleModel || 'N/A'}</span></p>
                            <p><strong>Biển số:</strong> <span>${data.licensePlate || 'N/A'}</span></p>
                            <p><strong>Phân loại:</strong> <span>${data.vehicleCategory || 'N/A'}</span></p>
                            <p><strong>Truyền động:</strong> <span>${data.transmission || 'N/A'}</span></p>
                        </div>
                    </div>
                </div>
            `;
        })
        .catch(error => {
            console.error('Error fetching booking details:', error);
            content.innerHTML = `<p class="text-center text-red-500">Không thể tải chi tiết. Vui lòng thử lại.</p>`;
        });
}

// Duyệt
function approveBooking(bookingId) {
    currentBookingId = bookingId;
    openModal('approve-modal');
}

function confirmApprove() {
    if (!currentBookingId) return;
    const btn = document.getElementById('confirmApproveBtn');
    if(btn) btn.disabled = true;

    fetch(`/owner/management/bookings/${currentBookingId}/approve`, {
        method: 'POST',
        headers: getCsrfHeaders()
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification(true, 'Đã duyệt booking thành công!');
                closeModal('approve-modal');
                location.reload();
            } else {
                throw new Error(data.message || 'Không thể duyệt booking');
            }
        })
        .catch(error => {
            showNotification(false, `Lỗi: ${error.message}`);
            if(btn) btn.disabled = false;
        });
}

// Từ chối
function rejectBooking(bookingId) {
    currentBookingId = bookingId;
    openModal('reject-modal');
}

function confirmReject() {
    if (!currentBookingId) return;
    const reasonEl = document.getElementById('reject-reason');
    const reason = reasonEl ? reasonEl.value : '';

    if (!reason || reason.trim() === '') {
        alert('Vui lòng nhập lý do từ chối');
        return;
    }

    const btn = document.getElementById('confirmRejectBtn');
    if(btn) btn.disabled = true;

    fetch(`/owner/management/bookings/${currentBookingId}/reject`, {
        method: 'POST',
        headers: getCsrfHeaders(),
        body: JSON.stringify({ reason: reason })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification(true, 'Đã từ chối booking thành công!');
                closeModal('reject-modal');
                location.reload();
            } else {
                throw new Error(data.message || 'Không thể từ chối booking');
            }
        })
        .catch(error => {
            showNotification(false, `Lỗi: ${error.message}`);
            if(btn) btn.disabled = false;
        });
}

// Giao xe
function handoverBooking(bookingId) {
    currentBookingId = bookingId;
    const modal = document.getElementById('handover-modal');
    const form = document.getElementById('handover-form');

    if(modal && form) {
        form.action = `/owner/management/bookings/${bookingId}/handover`;
        form.reset();
        openModal('handover-modal');
    }
}

// --- Logic Hoàn thành chuyến đi ---

function validateImageUpload(fileInput, maxFiles) {
    const errorEl = document.getElementById(fileInput.id + '-error');
    if (fileInput.files.length > maxFiles) {
        errorEl.classList.remove('hidden');
        fileInput.value = "";
        return false;
    } else {
        errorEl.classList.add('hidden');
        return true;
    }
}

// Hàm kiểm tra form ghi chú và ảnh
function validateCompletionForm() {
    const images = document.getElementById('return-images').files;
    if (images.length > 5) {
        showNotification(false, 'Chỉ được phép tải lên tối đa 5 ảnh.');
        return false;
    }
    return true;
}

// 1. Mở modal chính và điều chỉnh UI
async function openCompleteTripModal(bookingId) {
    // Reset form and UI elements first
    document.getElementById('complete-trip-form').reset();
    document.getElementById('return-images-error').classList.add('hidden');
    document.getElementById('complete-booking-id').value = bookingId;

    const remainingAmountSection = document.getElementById('remaining-amount-section');
    const remainingAmountDisplay = document.getElementById('remaining-amount-display');
    const paymentButtons = document.getElementById('payment-buttons');
    const confirmationButtons = document.getElementById('confirmation-buttons');

    // Hide all conditional UI elements initially
    remainingAmountSection.classList.add('hidden');
    paymentButtons.classList.add('hidden');
    confirmationButtons.classList.add('hidden');

    try {
        const response = await fetch(`/booking/${bookingId}/remaining-amount`);
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Could not fetch remaining amount.');
        }

        const remainingAmount = parseFloat(data.remainingAmount) || 0;
        document.getElementById('complete-remaining-amount').value = remainingAmount;

        // Logic to show/hide buttons based on remaining amount
        if (remainingAmount > 0) {
            const formattedAmount = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(remainingAmount);
            remainingAmountDisplay.textContent = formattedAmount;
            remainingAmountSection.classList.remove('hidden');
            paymentButtons.classList.remove('hidden');
        } else {
            confirmationButtons.classList.remove('hidden');
        }

        openModal('complete-trip-modal');

    } catch (error) {
        showNotification(false, `Lỗi: ${error.message}`);
    }
}


// 2. Xử lý khi nhấn nút "Tiền mặt"
function openCashPaymentModal() {
    // Kiểm tra ảnh và ghi chú trước
    if (!validateCompletionForm()) return;

    const remainingAmount = document.getElementById('complete-remaining-amount').value;
    const formattedAmount = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(remainingAmount);
    document.getElementById('cash-amount-display').textContent = formattedAmount;

    // Chuyển sang modal thanh toán tiền mặt
    closeModal('complete-trip-modal');
    openModal('cash-payment-modal');
}

// 3. Xử lý khi nhấn nút "Chuyển khoản"
async function openTransferPaymentModal() {
    // Kiểm tra ảnh và ghi chú trước
    if (!validateCompletionForm()) return;

    const bookingId = document.getElementById('complete-booking-id').value;
    const remainingAmount = parseFloat(document.getElementById('complete-remaining-amount').value) || 0;

    // Quy trình thanh toán QR cho số tiền còn lại
    const formattedAmount = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(remainingAmount);
    document.getElementById('transfer-amount-display').textContent = formattedAmount;

    closeModal('complete-trip-modal');
    openModal('transfer-payment-modal');

    const qrContainer = document.getElementById('payos-qr-code-container');
    qrContainer.innerHTML = '<i class="fas fa-spinner fa-spin text-4xl text-gray-400"></i>';

    try {
        const response = await fetch('/api/payos/create-completion-payment', {
            method: 'POST',
            headers: getCsrfHeaders(),
            body: JSON.stringify({
                bookingId: bookingId,
                amount: remainingAmount
            })
        });

        const data = await response.json();

        if (!response.ok || !data.success) {
            throw new Error(data.message || 'Lỗi không xác định từ Backend');
        }

        qrContainer.innerHTML = ''; // Clear the spinner
        new QRCode(qrContainer, {
            text: data.qrCode,
            width: 256,
            height: 256,
            colorDark: "#000000",
            colorLight: "#ffffff",
            correctLevel: QRCode.CorrectLevel.H
        });

        startPaymentPolling(data.orderCode);

    } catch (error) {
        showNotification(false, "Lỗi tạo mã QR: " + error.message);
        qrContainer.innerHTML = `<p class="text-red-500">Lỗi tạo mã QR: ${error.message}</p>`;
    }
}

// 4. Bắt đầu kiểm tra trạng thái thanh toán (polling)
function startPaymentPolling(orderCode) {
    stopPaymentPolling(); // Dừng polling cũ nếu có

    paymentPollInterval = setInterval(async () => {
        try {
            const response = await fetch(`/api/payos/check-payment-status/${orderCode}`);
            const data = await response.json();

            if (data.status === 'PAID') {
                stopPaymentPolling();
                closeModal('transfer-payment-modal');
                openModal('payment-success-modal'); // Chuyển đến modal thành công
            } else if (data.status === 'CANCELLED' || data.status === 'EXPIRED') {
                stopPaymentPolling();
                document.getElementById('payos-qr-code-container').innerHTML = '<p class="text-red-500">Mã thanh toán đã hết hạn/bị hủy. Vui lòng thử lại.</p>';
            }
        } catch (error) {
            console.error('Lỗi khi kiểm tra thanh toán:', error);
        }
    }, 3000);
}

// 5. Dừng Polling
function stopPaymentPolling() {
    if (paymentPollInterval) {
        clearInterval(paymentPollInterval);
        paymentPollInterval = null;
    }
}

// 6. Xử lý hoàn tất (gọi API cuối cùng)
async function processTripCompletion(finalStatus) {
    // Đọc bookingId từ trường ẩn trong modal chính
    const bookingId = document.getElementById('complete-booking-id').value;
    if (!bookingId) {
        showNotification(false, 'Lỗi: Không tìm thấy mã booking.');
        return;
    }

    // Lấy dữ liệu từ form Ghi chú/Ảnh (chỉ có ý nghĩa nếu là TH trả 20%)
    const notes = document.getElementById('return-notes').value;
    const images = document.getElementById('return-images').files;

    const formData = new FormData();
    formData.append('bookingId', bookingId);
    formData.append('returnNotes', notes);
    formData.append('newStatus', finalStatus); // 'Completed' hoặc 'Maintenance'

    for (let i = 0; i < images.length; i++) {
        formData.append('returnImages', images[i]);
    }

    try {
        const response = await fetch('/owner/api/bookings/complete-trip', {
            method: 'POST',
            headers: getCsrfHeaderOnly(),
            body: formData
        });

        if (!response.ok) throw new Error(`Lỗi server: ${response.status}`);

        const result = await response.json();

        if (result.success) {
            showNotification(true, 'Đã hoàn tất chuyến đi thành công!');
            // Đóng tất cả các modal có thể đang mở
            closeModal('cash-payment-modal');
            closeModal('payment-success-modal');
            closeModal('complete-trip-modal');
            closeModal('complete-confirmation-modal'); // Đóng cả modal xác nhận
            location.reload(); // Tải lại trang để cập nhật danh sách
        } else {
            throw new Error(result.message || 'Lỗi không xác định');
        }

    } catch (error) {
        showNotification(false, 'Lỗi: ' + error.message);
    }
}


// --- Khởi chạy ---
document.addEventListener('DOMContentLoaded', function() {
    if (typeof currentPage !== 'undefined' && currentPage && typeof highlightOwnerSidebarLink === 'function') {
        highlightOwnerSidebarLink(currentPage);
    }

    if (typeof loadNotificationCount === 'function') {
        loadNotificationCount();
    }

    const noBookingsMessage = document.getElementById('no-bookings-message');
    if(noBookingsMessage) {
        noBookingsMessage.classList.add('hidden');
    }

    filterAndSortBookings();
});