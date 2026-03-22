// src/main/resources/static/js/sidebar-common.js

document.addEventListener('DOMContentLoaded', function() {
    // Chỉ gọi setupSidebarCommon nếu các element cần thiết tồn tại
    if (document.getElementById('sidebar') && document.getElementById('sidebar-toggle')) {
        setupSidebarCommon();
    } else {
        console.warn("Sidebar elements not found, skipping common setup.");
    }
});

function setupSidebarCommon() {
    const sidebar = document.getElementById('sidebar');
    const sidebarToggle = document.getElementById('sidebar-toggle'); // Nút này cần có trong top bar/nav bar
    const sidebarClose = document.getElementById('sidebar-close'); // Nút này trong header của sidebar
    const sidebarOverlay = document.getElementById('sidebar-overlay');

    if (!sidebar || !sidebarOverlay) {
        console.error("Sidebar structure incomplete: #sidebar or #sidebar-overlay missing.");
        return; // Không thể tiếp tục nếu thiếu cấu trúc cơ bản
    }

    // Toggle sidebar on mobile
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', function(e) {
            e.stopPropagation();
            openSidebar();
        });
    } else {
        console.warn("#sidebar-toggle button not found.");
    }


    // Close sidebar button inside header
    if (sidebarClose) {
        sidebarClose.addEventListener('click', function(e) {
            e.stopPropagation();
            closeSidebar();
        });
    } else {
        console.warn("#sidebar-close button not found inside sidebar header.");
    }


    // Close sidebar when clicking overlay
    sidebarOverlay.addEventListener('click', function() {
        closeSidebar();
    });

    // Close sidebar on escape key
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && sidebar.classList.contains('sidebar-open')) {
            closeSidebar();
        }
    });

    // Handle window resize
    window.addEventListener('resize', function() {
        // Nếu màn hình lớn và sidebar đang mở (trên mobile), thì đóng lại
        if (window.innerWidth >= 1024 && sidebar.classList.contains('sidebar-open')) {
            closeSidebar();
        }
        // Nếu màn hình lớn, đảm bảo sidebar hiện (loại bỏ translate)
        else if (window.innerWidth >= 1024) {
            sidebar.classList.remove('-translate-x-full', 'sidebar-open');
            sidebarOverlay.classList.add('hidden'); // Ẩn overlay trên desktop
            document.body.style.overflow = ''; // Cho phép cuộn
        }
        // Nếu màn hình nhỏ và sidebar đang không ẩn bằng translate (trường hợp load trang desktop rồi thu nhỏ), ẩn nó đi
        else if (window.innerWidth < 1024 && !sidebar.classList.contains('-translate-x-full') && !sidebar.classList.contains('sidebar-open')) {
            sidebar.classList.add('-translate-x-full');
        }
    });
    console.log("Common sidebar setup complete.");
}

function openSidebar() {
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebar-overlay');

    if (sidebar && sidebarOverlay) {
        sidebar.classList.remove('-translate-x-full');
        sidebar.classList.add('sidebar-open'); // Class để nhận biết trạng thái mở (JS)
        sidebarOverlay.classList.remove('hidden');
        document.body.style.overflow = 'hidden';
        console.log('Sidebar opened');
    } else {
        console.error('Sidebar or overlay not found for opening');
    }
}

function closeSidebar() {
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebar-overlay');

    if (sidebar && sidebarOverlay) {
        // Chỉ thêm class translate nếu màn hình nhỏ (dưới lg)
        if (window.innerWidth < 1024) {
            sidebar.classList.add('-translate-x-full');
        }
        sidebar.classList.remove('sidebar-open');
        sidebarOverlay.classList.add('hidden');
        document.body.style.overflow = '';
        console.log('Sidebar closed');
    } else {
        console.error('Sidebar or overlay not found for closing');
    }
}

// Hàm highlight cho Owner sidebar (khi dùng thẻ <a> và load lại trang)
function highlightOwnerSidebarLink(currentPage) {
    const sidebarItems = document.querySelectorAll('#sidebar .sidebar-item');
    sidebarItems.forEach(item => {
        const link = item.closest('a'); // Tìm thẻ a
        if (link) {
            const linkPage = item.getAttribute('data-page'); // Lấy data-page từ thẻ a
            if (linkPage === currentPage) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        }
    });
    console.log(`Highlighting Owner sidebar for page: ${currentPage}`);
}