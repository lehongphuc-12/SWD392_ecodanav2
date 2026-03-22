/**
 * File: owner-dashboard.js
 * Chức năng: Quản lý toàn bộ trang dashboard của Owner, bao gồm:
 * 1. Chuyển đổi các tab chính (Dashboard, Cars, Bookings, Payments...).
 * 2. Khởi tạo và xử lý logic cho từng tab cụ thể (vd: lọc, sắp xếp cho tab Payments).
 */

document.addEventListener('DOMContentLoaded', function() {
    // =================================================================
    // SECTION 1: LOGIC LỌC VÀ SẮP XẾP CHO TAB "PAYMENTS"
    // =================================================================
    function initializePaymentManagement() {
        const paymentTabContent = document.getElementById('payments-content');
        if (!paymentTabContent || paymentTabContent.dataset.initialized === 'true') {
            return; // Không làm gì nếu không ở đúng tab hoặc đã khởi tạo
        }
        paymentTabContent.dataset.initialized = 'true'; // Đánh dấu đã khởi tạo

        console.log("Initializing Payment Management Logic...");

        let currentStatus = ''; // Biến cục bộ để lưu trạng thái đang được chọn
        const searchInput = paymentTabContent.querySelector('#search-input');
        const sortSelect = paymentTabContent.querySelector('#sort-select');
        const paymentListBody = paymentTabContent.querySelector('#payment-list-body');
        const statusTabs = paymentTabContent.querySelectorAll('#status-tabs .status-tab');

        function filterAndSortPayments() {
            if (!paymentListBody) return;

            const searchKeyword = searchInput.value.toLowerCase().trim();
            const sortValue = sortSelect.value;
            const allRows = Array.from(paymentListBody.getElementsByTagName('tr'));
            let visibleRows = 0;

            allRows.forEach(row => {
                if (row.getElementsByTagName('td').length <= 1) {
                    return; // Bỏ qua dòng "Không có dữ liệu"
                }

                const displayStatus = row.getAttribute('data-display-status');
                const searchInfo = row.getAttribute('data-search-info') || '';

                const statusMatch = currentStatus === '' || currentStatus.split(',').includes(displayStatus);
                const searchMatch = searchKeyword === '' || searchInfo.includes(searchKeyword);

                if (statusMatch && searchMatch) {
                    row.style.display = ''; // Hiện dòng
                    visibleRows++;
                } else {
                    row.style.display = 'none'; // Ẩn dòng
                }
            });

            const visibleRowElements = allRows.filter(row => row.style.display !== 'none' && row.getElementsByTagName('td').length > 1);

            visibleRowElements.sort((a, b) => {
                const dateA = new Date(a.getAttribute('data-created-date'));
                const dateB = new Date(b.getAttribute('data-created-date'));
                const amountA = parseFloat(a.getAttribute('data-amount') || 0);
                const amountB = parseFloat(b.getAttribute('data-amount') || 0);

                switch (sortValue) {
                    case 'oldest': return dateA - dateB;
                    case 'amount-high': return amountB - amountA;
                    case 'amount-low': return amountA - amountB;
                    case 'newest': default: return dateB - dateA;
                }
            });

            visibleRowElements.forEach(row => paymentListBody.appendChild(row));

            const noResultsMessage = paymentTabContent.querySelector('#no-payments-message');
            if (noResultsMessage) {
                noResultsMessage.style.display = visibleRows === 0 ? '' : 'none';
            }
        }

        statusTabs.forEach(tab => {
            tab.addEventListener('click', function() {
                statusTabs.forEach(t => t.classList.remove('active'));
                this.classList.add('active');
                currentStatus = this.getAttribute('data-status');
                filterAndSortPayments();
            });
        });

        searchInput.addEventListener('keyup', filterAndSortPayments);
        sortSelect.addEventListener('change', filterAndSortPayments);

        // Thêm dòng "Không có kết quả" nếu chưa có
        if (paymentListBody && !paymentTabContent.querySelector('#no-payments-message')) {
            const tr = document.createElement('tr');
            tr.id = 'no-payments-message';
            tr.style.display = 'none';
            tr.innerHTML = `<td colspan="7" class="text-center py-12"><div class="text-center text-gray-500"><i class="fas fa-search text-6xl text-gray-300 mb-4"></i><p class="text-lg">Không tìm thấy kết quả phù hợp.</p></div></td>`;
            paymentListBody.appendChild(tr);
        }

        // Kích hoạt tab "Tất cả" và lọc lần đầu
        const allTab = paymentTabContent.querySelector('.status-tab[data-status=""]');
        if (allTab) {
            allTab.click();
        } else {
            filterAndSortPayments();
        }
    }

    // =================================================================
    // SECTION 2: LOGIC CHUYỂN TAB CHÍNH VÀ CÁC CHỨC NĂNG CHUNG
    // =================================================================

    // Hàm chuyển đổi tab chính (Dashboard, Cars, Bookings, Payments...)
    function switchMainTab(tabName) {
        // Ẩn tất cả các nội dung tab
        document.querySelectorAll('.main-tab-content').forEach(content => {
            content.style.display = 'none';
        });

        // Hiện nội dung tab được chọn
        const activeContent = document.getElementById(tabName + '-content');
        if (activeContent) {
            activeContent.style.display = 'block';
        }

        // Cập nhật URL mà không tải lại trang
        const url = new URL(window.location);
        url.searchParams.set('tab', tabName);
        window.history.pushState({ path: url.href }, '', url.href);

        // Highlight sidebar link
        highlightOwnerSidebarLink(tabName);

        // Khởi tạo logic JS cho tab tương ứng
        if (tabName === 'payments' && typeof initializePaymentManagement === 'function') {
            initializePaymentManagement();
        }
        // Thêm các hàm khởi tạo cho các tab khác ở đây nếu cần
        // if (tabName === 'bookings' && typeof initializeBookingManagement === 'function') {
        //     initializeBookingManagement();
        // }
    }

    // Hàm highlight sidebar link
    window.highlightOwnerSidebarLink = function(activePage) {
        document.querySelectorAll('#owner-sidebar-nav a').forEach(link => {
            const linkPage = link.getAttribute('data-page');
            link.classList.toggle('active', linkPage === activePage);
        });
    };

    // Gắn sự kiện click cho các link sidebar
    document.querySelectorAll('#owner-sidebar-nav a').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const page = this.getAttribute('data-page');
            if (page) {
                switchMainTab(page);
            }
        });
    });

    // Xử lý khi người dùng tải lại trang hoặc truy cập trực tiếp qua URL
    const initialTab = new URLSearchParams(window.location.search).get('tab') || 'dashboard';
    switchMainTab(initialTab);

    // Logic cho dropdown menu và các chức năng chung khác
    const userMenuButton = document.getElementById('userMenuButton');
    const userMenuDropdown = document.getElementById('userMenuDropdown');
    const notificationBell = document.getElementById('notificationBell');
    const notificationDropdown = document.getElementById('notificationDropdown');

    if (userMenuButton) {
        userMenuButton.addEventListener('click', () => userMenuDropdown.classList.toggle('hidden'));
    }

    if (notificationBell) {
        notificationBell.addEventListener('click', () => notificationDropdown.classList.toggle('hidden'));
    }

    // Đóng dropdown khi click ra ngoài
    document.addEventListener('click', function(event) {
        if (userMenuButton && userMenuDropdown && !userMenuButton.contains(event.target) && !userMenuDropdown.contains(event.target)) {
            userMenuDropdown.classList.add('hidden');
        }
        if (notificationBell && notificationDropdown && !notificationBell.contains(event.target) && !notificationDropdown.contains(event.target)) {
            notificationDropdown.classList.add('hidden');
        }
    });

    // Logic đóng thông báo flash
    window.closeNotification = function(element) {
        element.closest('.fixed').remove();
    };

    document.querySelectorAll('.flash-notification').forEach(notification => {
        const delay = notification.id.includes('error') ? 7000 : 5000;
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => notification.remove(), 300);
        }, delay);
    });
});