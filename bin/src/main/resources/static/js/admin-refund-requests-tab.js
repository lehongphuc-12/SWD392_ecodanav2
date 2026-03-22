/**
 * Mã JavaScript này dành cho tệp admin-refund-requests-fragment.html (server-side rendered).
 * Nó cung cấp các hàm trợ giúp cho các sự kiện onclick của modal.
 *
 * Các hàm fetch dữ liệu cũ (như initializeRefundRequestsTab, loadStatistics) đã bị loại bỏ
 * vì dữ liệu hiện được nạp bởi Thymeleaf.
 */
(function() {

    /**
     * Hiển thị modal chi tiết yêu cầu hoàn tiền.
     * Hàm này được gọi bởi 'onclick="showRefundDetailModal(this)"' từ HTML.
     * @param {HTMLElement} buttonElement - Nút đã được nhấp vào, chứa các thuộc tính data-*.
     */
    window.showRefundDetailModal = function(buttonElement) {
        const data = buttonElement.dataset;
        const modal = document.getElementById('refundDetailModal');
        if (!modal) {
            console.error('Modal "refundDetailModal" not found!');
            return;
        }

        // Debug: Log all data attributes
        console.log('=== REFUND DETAIL MODAL DATA ===');
        console.log('Bank Name:', data.bankName);
        console.log('Account Number:', data.accountNumber);
        console.log('Account Holder:', data.accountHolder);
        console.log('QR Code Image:', data.qrCodeImage);
        console.log('Customer ID:', data.customerId);
        console.log('All data:', data);

        // 1. Điền thông tin cơ bản vào modal
        document.getElementById('modalRefundId').textContent = 'ID: ' + (data.refundId || '-');
        document.getElementById('modalRefundAmount').textContent = data.refundAmount ? parseFloat(data.refundAmount).toLocaleString('vi-VN') + ' ₫' : '0 ₫';
        document.getElementById('modalBookingCode').textContent = data.bookingCode || '-';
        document.getElementById('modalCreatedDate').textContent = data.createdDate || '-';
        document.getElementById('modalCustomerName').textContent = data.customerName || '-';
        document.getElementById('modalCustomerEmail').textContent = data.customerEmail || '-';
        document.getElementById('modalCustomerPhone').textContent = data.customerPhone || '-';
        document.getElementById('modalCancelReason').textContent = data.cancelReason || '-';

        // 2. Cập nhật Status Badge
        const statusBadge = document.getElementById('statusBadge');
        statusBadge.textContent = data.status || 'Unknown';
        // Reset các lớp CSS cũ
        statusBadge.className = 'px-4 py-2 rounded-full text-sm font-semibold inline-block';

        if (data.status === 'Pending') {
            statusBadge.classList.add('bg-yellow-100', 'text-yellow-800');
            statusBadge.innerHTML = '<i class="fas fa-clock mr-1"></i> Chờ duyệt';
        } else if (data.status === 'Refunded') {
            statusBadge.classList.add('bg-green-100', 'text-green-800');
            statusBadge.innerHTML = '<i class="fas fa-check-circle mr-1"></i> Đã hoàn tiền';
        } else if (data.status === 'Rejected') {
            statusBadge.classList.add('bg-red-100', 'text-red-800');
            statusBadge.innerHTML = '<i class="fas fa-times mr-1"></i> Từ chối';
        }

        // 3. Lưu refund ID để sử dụng sau
        window.currentRefundRequestId = data.refundId;
        window.currentCustomerId = data.customerId;

        // 4. Ẩn/hiện và điền Admin Notes
        const adminNotesSection = document.getElementById('adminNotesSection');
        // Kiểm tra data.adminNotes có tồn tại và không phải là chuỗi 'undefined'
        if (data.adminNotes && data.adminNotes !== 'undefined') {
            document.getElementById('modalAdminNotes').textContent = data.adminNotes;
            adminNotesSection.classList.remove('hidden');
        } else {
            adminNotesSection.classList.add('hidden');
        }

        // 5. Tạo các nút hành động (Approve/Reject/Mark Completed)
        const actionButtonsDiv = document.getElementById('actionButtons');
        actionButtonsDiv.innerHTML = ''; // Xóa các nút cũ

        if (data.status === 'Pending') {
            // Lưu refund ID vào global variable
            window.currentRefundRequestId = data.refundId;
            
            // Nút Upload Ảnh (Chuyển tiền)
            const uploadButton = document.createElement('button');
            uploadButton.innerHTML = '<i class="fas fa-cloud-upload-alt mr-2"></i>Upload Ảnh Chuyển Khoản';
            uploadButton.className = 'px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700';
            uploadButton.type = 'button';
            uploadButton.onclick = function(e) {
                e.preventDefault();
                window.markRefundCompleted(window.currentRefundRequestId);
            };
            actionButtonsDiv.appendChild(uploadButton);

            // Nút Từ chối
            const rejectButton = document.createElement('button');
            rejectButton.innerHTML = '<i class="fas fa-times mr-2"></i>Từ chối';
            rejectButton.className = 'px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700';
            rejectButton.type = 'button';
            rejectButton.onclick = function(e) {
                e.preventDefault();
                // Hiển thị form nhập lý do từ chối
                document.getElementById('rejectionReasonSection').classList.remove('hidden');
                // Ẩn nút từ chối và hiển thị nút xác nhận
                rejectButton.classList.add('hidden');
                uploadButton.classList.add('hidden');
                
                // Tạo nút xác nhận từ chối
                const confirmRejectButton = document.createElement('button');
                confirmRejectButton.innerHTML = '<i class="fas fa-check mr-2"></i>Xác nhận từ chối';
                confirmRejectButton.className = 'px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700';
                confirmRejectButton.type = 'button';
                confirmRejectButton.onclick = function(e) {
                    e.preventDefault();
                    const reason = document.getElementById('rejectionReason').value.trim();
                    if (!reason) {
                        alert('Vui lòng nhập lý do từ chối');
                        return;
                    }
                    window.rejectRefundRequest(window.currentRefundRequestId, reason);
                };
                actionButtonsDiv.appendChild(confirmRejectButton);
                
                // Tạo nút hủy
                const cancelRejectButton = document.createElement('button');
                cancelRejectButton.innerHTML = '<i class="fas fa-undo mr-2"></i>Hủy';
                cancelRejectButton.className = 'px-4 py-2 bg-gray-400 text-white rounded-lg hover:bg-gray-500';
                cancelRejectButton.type = 'button';
                cancelRejectButton.onclick = function(e) {
                    e.preventDefault();
                    document.getElementById('rejectionReasonSection').classList.add('hidden');
                    document.getElementById('rejectionReason').value = '';
                    rejectButton.classList.remove('hidden');
                    uploadButton.classList.remove('hidden');
                    confirmRejectButton.remove();
                    cancelRejectButton.remove();
                };
                actionButtonsDiv.appendChild(cancelRejectButton);
            };
            actionButtonsDiv.appendChild(rejectButton);
        } else if (data.status === 'Refunded') {
            // Nút Xem Ảnh (đã hoàn tiền)
            const viewImageButton = document.createElement('button');
            viewImageButton.innerHTML = '<i class="fas fa-image mr-2"></i>Xem Ảnh Chứng Minh';
            viewImageButton.className = 'px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700';
            viewImageButton.type = 'button';
            viewImageButton.onclick = function(e) {
                e.preventDefault();
                window.showTransferProofImageModal(data.transferProofImagePath);
            };
            actionButtonsDiv.appendChild(viewImageButton);
        }

        // 6. Hiển thị modal
        modal.classList.remove('hidden');
    };

    /**
     * Đóng modal chi tiết.
     * Hàm này được gọi bởi 'onclick="closeRefundDetailModal()"'
     */
    window.closeRefundDetailModal = function() {
        const modal = document.getElementById('refundDetailModal');
        if (modal) {
            modal.classList.add('hidden');
        }
    };

    /**
     * Gửi yêu cầu duyệt hoàn tiền (API call).
     * Khi duyệt, status thành Approved, rồi admin sẽ upload ảnh để chuyển thành Transferred
     */
    window.approveRefundRequest = function(refundRequestId) {
        var notes = prompt('Nhập ghi chú (tùy chọn):', '');
        if (notes === null) return; // Người dùng hủy

        var url = '/admin/api/refund-requests/' + refundRequestId + '/approve';
        if (notes) {
            url += '?adminNotes=' + encodeURIComponent(notes);
        }

        fetch(url, {
            method: 'POST',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(function(response) {
                if (!response.ok) {
                    return response.json().then(function(err) {
                        throw new Error(err.message || 'Failed to approve refund request');
                    });
                }
                return response.json();
            })
            .then(function(result) {
                if (result.status === 'success') {
                    // Cập nhật status badge trong modal
                    var statusBadge = document.getElementById('statusBadge');
                    statusBadge.className = 'px-4 py-2 rounded-full text-sm font-semibold inline-block bg-green-100 text-green-800';
                    statusBadge.innerHTML = '<i class="fas fa-check-circle mr-1"></i> Đã hoàn tiền';
                    
                    // Cập nhật admin notes nếu có
                    if (notes) {
                        var adminNotesSection = document.getElementById('adminNotesSection');
                        document.getElementById('modalAdminNotes').textContent = notes;
                        adminNotesSection.classList.remove('hidden');
                    }
                    
                    // Ẩn các nút hành động (Approve/Reject) và hiển thị nút "Đã Hoàn Thành"
                    var actionButtonsDiv = document.getElementById('actionButtons');
                    actionButtonsDiv.innerHTML = '';
                    
                    // Hiển thị thông báo thành công
                    var successMessage = document.createElement('div');
                    successMessage.className = 'bg-green-50 border-l-4 border-green-500 p-4 rounded mb-4';
                    successMessage.innerHTML = '<p class="text-green-900"><i class="fas fa-check-circle mr-2 text-green-600"></i><strong>✅ Yêu cầu hoàn tiền đã được duyệt và đánh dấu đã hoàn tiền!</strong></p>';
                    
                    // Chèn thông báo vào đầu nội dung modal
                    var modalContent = document.querySelector('#refundDetailModal .p-6');
                    modalContent.insertBefore(successMessage, modalContent.firstChild);
                    
                    // Tự động ẩn thông báo sau 3 giây
                    setTimeout(function() {
                        successMessage.style.transition = 'opacity 0.3s ease-out';
                        successMessage.style.opacity = '0';
                        setTimeout(function() {
                            successMessage.remove();
                        }, 300);
                    }, 3000);

                    // Log để debug
                    console.log('Approve success, actionButtons updated with completed button');
                    console.log('Current refundRequestId:', refundRequestId);
                } else {
                    throw new Error(result.message || 'Approval failed');
                }
            })
            .catch(function(error) {
                console.error('Error:', error);
                alert('❌ Có lỗi xảy ra khi duyệt: ' + error.message);
            });
    };

    /**
     * Gửi yêu cầu từ chối hoàn tiền (API call).
     */
    window.rejectRefundRequest = function(refundRequestId, reason) {
        // Nếu reason không được truyền, dùng prompt (fallback)
        if (!reason) {
            reason = prompt('Nhập lý do từ chối (bắt buộc):', '');
            if (reason === null || reason.trim() === '') {
                alert('Lý do từ chối là bắt buộc');
                return;
            }
        }

        var url = '/admin/api/refund-requests/' + refundRequestId + '/reject?adminNotes=' + encodeURIComponent(reason);

        fetch(url, {
            method: 'POST',
            headers: {
                'Accept': 'application/json'
                // Thêm các headers khác nếu cần (ví dụ: CSRF token)
            }
        })
            .then(function(response) {
                if (!response.ok) {
                    return response.json().then(function(err) {
                        throw new Error(err.message || 'Failed to reject refund request');
                    });
                }
                return response.json();
            })
            .then(function(result) {
                if (result.status === 'success') {
                    // Cập nhật status badge trong modal
                    var statusBadge = document.getElementById('statusBadge');
                    statusBadge.className = 'px-4 py-2 rounded-full text-sm font-semibold inline-block bg-red-100 text-red-800';
                    statusBadge.innerHTML = '<i class="fas fa-times mr-1"></i> Từ chối';
                    
                    // Cập nhật admin notes
                    var adminNotesSection = document.getElementById('adminNotesSection');
                    document.getElementById('modalAdminNotes').textContent = reason;
                    adminNotesSection.classList.remove('hidden');
                    
                    // Ẩn form nhập lý do từ chối
                    document.getElementById('rejectionReasonSection').classList.add('hidden');
                    
                    // Ẩn các nút hành động (Approve/Reject)
                    var actionButtonsDiv = document.getElementById('actionButtons');
                    actionButtonsDiv.innerHTML = '';
                    
                    // Hiển thị thông báo từ chối
                    var rejectMessage = document.createElement('div');
                    rejectMessage.className = 'bg-red-50 border-l-4 border-red-500 p-4 rounded mb-4';
                    rejectMessage.innerHTML = '<p class="text-red-900"><i class="fas fa-times-circle mr-2 text-red-600"></i><strong>✅ Yêu cầu hoàn tiền đã bị từ chối!</strong></p>';
                    
                    // Chèn thông báo vào đầu nội dung modal
                    var modalContent = document.querySelector('#refundDetailModal .p-6');
                    modalContent.insertBefore(rejectMessage, modalContent.firstChild);
                    
                    // Tự động ẩn thông báo sau 3 giây
                    setTimeout(function() {
                        rejectMessage.style.transition = 'opacity 0.3s ease-out';
                        rejectMessage.style.opacity = '0';
                        setTimeout(function() {
                            rejectMessage.remove();
                        }, 300);
                    }, 3000);
                    
                    // Reload danh sách refund requests sau 2 giây
                    setTimeout(function() {
                        console.log('Reloading refund requests list...');
                        location.reload(); // Reload trang để cập nhật danh sách
                    }, 2000);
                } else {
                    throw new Error(result.message || 'Rejection failed');
                }
            })
            .catch(function(error) {
                console.error('Error:', error);
                alert('❌ Có lỗi xảy ra khi từ chối: ' + error.message);
            });
    };

    /**
     * Hiển thị modal để xem tài khoản ngân hàng mặc định
     */
    window.showBankAccountModal = function() {
        const bankAccountModal = document.getElementById('bankAccountModal');
        if (!bankAccountModal) {
            console.error('Bank account modal not found!');
            return;
        }

        // Load default bank account của customer
        if (window.currentCustomerId) {
            console.log('Loading default bank account for customer:', window.currentCustomerId);
            loadDefaultBankAccountForModal(window.currentCustomerId);
        } else {
            console.error('Customer ID not available');
            alert('Không tìm thấy thông tin khách hàng');
            return;
        }

        // Hiển thị modal
        bankAccountModal.classList.remove('hidden');
    };

    /**
     * Đóng modal thông tin tài khoản ngân hàng
     */
    window.closeBankAccountModal = function() {
        const bankAccountModal = document.getElementById('bankAccountModal');
        if (bankAccountModal) {
            bankAccountModal.classList.add('hidden');
        }
    };

    /**
     * Mở lightbox để xem ảnh QR code to
     */
    window.openQRCodeLightbox = function(imageSrc) {
        const lightbox = document.getElementById('qrCodeLightbox');
        const lightboxImage = document.getElementById('qrCodeLightboxImage');
        if (lightbox && lightboxImage) {
            lightboxImage.src = imageSrc;
            lightbox.classList.remove('hidden');
        }
    };

    /**
     * Xem ảnh chứng minh chuyển khoản
     */
    window.showTransferProofImage = function(imageSrc) {
        if (!imageSrc) {
            alert('Không có ảnh chứng minh chuyển khoản');
            return;
        }
        openQRCodeLightbox(imageSrc);
    };

    /**
     * Hiển thị ảnh chứng minh trong modal lightbox
     */
    window.showTransferProofImageModal = function(imageSrc) {
        if (!imageSrc) {
            alert('Không có ảnh chứng minh chuyển khoản');
            return;
        }
        openQRCodeLightbox(imageSrc);
    };

    /**
     * Đóng lightbox QR code
     */
    window.closeQRCodeLightbox = function() {
        const lightbox = document.getElementById('qrCodeLightbox');
        if (lightbox) {
            lightbox.classList.add('hidden');
        }
    };

    // Close lightbox when clicking outside the image
    document.addEventListener('DOMContentLoaded', function() {
        const lightbox = document.getElementById('qrCodeLightbox');
        if (lightbox) {
            lightbox.addEventListener('click', function(e) {
                if (e.target === this) {
                    window.closeQRCodeLightbox();
                }
            });
        }
    });

    /**
     * Hiển thị modal để upload ảnh chuyển khoản
     */
    window.showTransferProofModal = function(refundRequestId) {
        const modal = document.getElementById('transferProofModal');
        if (!modal) {
            console.error('Transfer proof modal not found!');
            return;
        }

        // Reset form
        document.getElementById('transferProofForm').reset();
        window.currentTransferRefundId = refundRequestId;

        // Hiển thị modal
        modal.classList.remove('hidden');
    };

    /**
     * Đóng modal upload ảnh chuyển khoản
     */
    window.closeTransferProofModal = function() {
        const modal = document.getElementById('transferProofModal');
        if (modal) {
            modal.classList.add('hidden');
        }
    };

    /**
     * Cập nhật trạng thái refund thành Completed với ảnh và lời nhắn
     */
    window.markRefundCompleted = function(refundRequestId) {
        // Mở modal để upload ảnh
        showTransferProofModal(refundRequestId);
    };

    /**
     * Submit form upload ảnh chuyển khoản
     */
    window.submitTransferProof = function() {
        const refundRequestId = window.currentTransferRefundId;
        const transferProofFile = document.getElementById('transferProofFile').files[0];

        if (!transferProofFile) {
            alert('Vui lòng chọn ảnh chứng minh chuyển khoản');
            return;
        }

        uploadTransferProofImage(transferProofFile, refundRequestId);
    };

    /**
     * Upload ảnh chuyển khoản qua API server
     */
    window.uploadTransferProofImage = function(file, refundRequestId) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('refundRequestId', refundRequestId);
        formData.append('transferMessage', ''); // Không cần lời nhắn, dùng AdminNotes

        fetch('/admin/api/refund-requests/upload-transfer-proof', {
            method: 'POST',
            body: formData
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.message || 'Upload failed');
                    });
                }
                return response.json();
            })
            .then(result => {
                if (result.status === 'success') {
                    // Cập nhật trạng thái thành Completed
                    updateRefundToCompleted(refundRequestId, result.imageUrl);
                } else {
                    throw new Error(result.message || 'Upload failed');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('❌ Lỗi khi upload ảnh: ' + error.message);
            });
    };

    /**
     * Cập nhật refund request thành Approved (Đã duyệt) sau khi upload ảnh
     */
    window.updateRefundToCompleted = function(refundRequestId, imageUrl) {
        const url = '/admin/api/refund-requests/' + refundRequestId + '/mark-transferred' +
            '?transferProofImagePath=' + encodeURIComponent(imageUrl);

        fetch(url, {
            method: 'POST',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => {
                        throw new Error(err.message || 'Failed to mark as transferred');
                    });
                }
                return response.json();
            })
            .then(result => {
                if (result.status === 'success') {
                    // Cập nhật status badge thành Refunded (Đã hoàn tiền)
                    const statusBadge = document.getElementById('statusBadge');
                    statusBadge.className = 'px-4 py-2 rounded-full text-sm font-semibold inline-block bg-green-100 text-green-800';
                    statusBadge.innerHTML = '<i class="fas fa-check-circle mr-1"></i> Đã hoàn tiền';

                    // Ẩn các nút hành động
                    const actionButtonsDiv = document.getElementById('actionButtons');
                    actionButtonsDiv.innerHTML = '';

                    // Hiển thị thông báo thành công
                    const successMessage = document.createElement('div');
                    successMessage.className = 'bg-green-50 border-l-4 border-green-500 p-4 rounded mb-4';
                    successMessage.innerHTML = '<p class="text-green-900"><i class="fas fa-check-circle mr-2 text-green-600"></i><strong>✅ Hoàn tiền đã được xử lý! Khách hàng sẽ nhận được thông báo với số tiền và ảnh chứng minh.</strong></p>';

                    const modalContent = document.querySelector('#refundDetailModal .p-6');
                    modalContent.insertBefore(successMessage, modalContent.firstChild);

                    // Tự động ẩn thông báo sau 3 giây
                    setTimeout(() => {
                        successMessage.style.transition = 'opacity 0.3s ease-out';
                        successMessage.style.opacity = '0';
                        setTimeout(() => {
                            successMessage.remove();
                        }, 300);
                    }, 3000);

                    // Đóng modal upload
                    closeTransferProofModal();

                    // Reload trang sau 2 giây
                    setTimeout(() => {
                        location.reload();
                    }, 2000);
                } else {
                    throw new Error(result.message || 'Update failed');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('❌ Lỗi khi cập nhật: ' + error.message);
            });
    };

    /**
     * Preview ảnh chuyển khoản trước khi upload
     */
    window.previewTransferImage = function(input) {
        if (input.files && input.files[0]) {
            const reader = new FileReader();
            reader.onload = function(e) {
                const preview = document.getElementById('transferImagePreview');
                const previewImg = document.getElementById('transferImagePreviewImg');
                previewImg.src = e.target.result;
                preview.classList.remove('hidden');
            };
            reader.readAsDataURL(input.files[0]);
        }
    };

    /**
     * Load default bank account của customer cho modal
     */
    window.loadDefaultBankAccountForModal = function(customerId) {
        if (!customerId) {
            console.error('Customer ID not provided');
            return;
        }

        console.log('Loading default bank account for modal, customer:', customerId);
        
        fetch('/admin/api/refund-requests/customer-bank-accounts/' + customerId)
            .then(response => {
                console.log('Response status:', response.status);
                if (!response.ok) {
                    throw new Error('HTTP error, status=' + response.status);
                }
                return response.json();
            })
            .then(data => {
                console.log('Bank accounts response:', data);
                const container = document.getElementById('bankAccountsList');
                
                if (!container) {
                    console.error('Bank accounts list container not found');
                    return;
                }
                
                container.innerHTML = '';
                
                // Get accounts from response
                const bankAccounts = data.accounts || [];
                
                // Find default account
                let defaultAccount = null;
                if (bankAccounts && bankAccounts.length > 0) {
                    defaultAccount = bankAccounts.find(acc => acc.isDefault === true || acc.default === true);
                    // If no default, use first account
                    if (!defaultAccount) {
                        defaultAccount = bankAccounts[0];
                    }
                }
                
                if (defaultAccount) {
                    const accountDiv = document.createElement('div');
                    accountDiv.className = 'bg-blue-50 p-6 rounded';
                    
                    let qrHtml = '';
                    if (defaultAccount.qrCodeImagePath && defaultAccount.qrCodeImagePath !== 'null') {
                        qrHtml = `<div class="mt-4 text-center"><img src="${defaultAccount.qrCodeImagePath}" alt="QR" class="w-48 h-48 border border-gray-300 rounded inline-block cursor-pointer hover:opacity-80 transition" onclick="openQRCodeLightbox('${defaultAccount.qrCodeImagePath}')"></div>`;
                    }
                    
                    accountDiv.innerHTML = `
                        <div class="grid grid-cols-2 gap-4 mb-4">
                            <div>
                                <p class="text-sm text-gray-600">Ngân hàng</p>
                                <p class="text-lg font-semibold text-gray-900">${defaultAccount.bankName}</p>
                            </div>
                            <div>
                                <p class="text-sm text-gray-600">Số tài khoản</p>
                                <p class="text-lg font-semibold text-gray-900">${defaultAccount.accountNumber}</p>
                            </div>
                            <div class="col-span-2">
                                <p class="text-sm text-gray-600">Chủ tài khoản</p>
                                <p class="text-lg font-semibold text-gray-900">${defaultAccount.accountHolderName}</p>
                            </div>
                        </div>
                        ${qrHtml}
                    `;
                    container.appendChild(accountDiv);
                } else {
                    container.innerHTML = '<p class="text-center text-gray-500 py-8"><i class="fas fa-inbox text-3xl mb-2 block"></i>Khách hàng chưa có tài khoản ngân hàng nào</p>';
                }
            })
            .catch(error => {
                console.error('Error loading bank account:', error);
                const container = document.getElementById('bankAccountsList');
                if (container) {
                    container.innerHTML = '<p class="text-center text-red-500 py-4">Lỗi khi tải tài khoản: ' + error.message + '</p>';
                }
            });
    };


})(); // IIFE (Immediately Invoked Function Expression)