// src/main/resources/static/js/bank-account-management.js

/**
 * SỬA LỖI: Gán hàm này vào `window` (global scope)
 * để `onclick` trong HTML có thể tìm thấy và gọi nó.
 */
window.editBankAccount = function(accountId) {
    const account = document.querySelector(`[data-account-id="${accountId}"]`);
    if (!account) {
        console.error("Không tìm thấy tài khoản với ID:", accountId);
        return;
    }

    // Lấy dữ liệu từ các thẻ data-field
    const bankName = account.querySelector('[data-field="bankName"]')?.textContent?.trim() || '';
    const bankCode = account.querySelector('[data-field="bankCode"]')?.textContent?.trim() || '';
    const accountNumber = account.querySelector('[data-field="accountNumber"]')?.textContent?.trim() || '';
    const accountHolderName = account.querySelector('[data-field="accountHolderName"]')?.textContent?.trim() || '';

    // Lấy form
    const bankAccountForm = document.getElementById('bank-account-form');
    if (!bankAccountForm) {
        console.error("Không tìm thấy #bank-account-form");
        return;
    }

    // Điền vào form
    document.getElementById("bankName").value = bankName;
    document.getElementById("bankCode").value = bankCode;
    document.getElementById("accountNumber").value = accountNumber;
    document.getElementById("accountHolderName").value = accountHolderName;

    // Thêm (hoặc cập nhật) trường ID ẩn
    let accountIdField = document.getElementById("bankAccountId");
    if (!accountIdField) {
        accountIdField = document.createElement("input");
        accountIdField.type = "hidden";
        accountIdField.id = "bankAccountId";
        accountIdField.name = "bankAccountId";
        bankAccountForm.appendChild(accountIdField);
    }
    accountIdField.value = accountId;

    // Đổi nút "Thêm" thành "Cập nhật"
    document.querySelector("#bank-account-form button[type='submit']").innerHTML = '<i class="fa-solid fa-save"></i> Cập nhật tài khoản';

    // Hiển thị form
    const bankAccountFormContainer = document.getElementById('bank-account-form-container');
    const addBankBtn = document.getElementById('add-bank-btn');
    if (bankAccountFormContainer) bankAccountFormContainer.style.display = 'block';
    if (addBankBtn) addBankBtn.style.display = 'none';
    bankAccountFormContainer.scrollIntoView({ behavior: 'smooth' });

    // Ẩn QR preview (vì chúng ta không sửa QR)
    const qrPreview = document.getElementById('qr-preview');
    const qrPlaceholder = document.querySelector('#qr-code-box .upload-placeholder');
    if (qrPreview) qrPreview.style.display = 'none';
    if (qrPlaceholder) qrPlaceholder.style.display = 'flex';
}


/**
 * Khởi tạo logic cho phần quản lý tài khoản ngân hàng.
 *
 * @param {object} options - Tùy chọn cấu hình.
 * @param {string} options.ajaxListUrl - (Bắt buộc) URL để tải danh sách (VD: '/customer/bank-accounts/list-ajax').
 * @param {string} options.deleteUrlPrefix - (Bắt buộc) Tiền tố URL để xoá (VD: '/customer/bank-accounts/delete').
 * @param {string} options.setDefaultUrlPrefix - (Bắt buộc) Tiền tố URL để đặt làm mặc định.
 * @param {string} [options.unsetDefaultUrlPrefix] - (Tùy chọn) Tiền tố URL để gỡ trạng thái mặc định.
 * @param {string} options.csrfToken - (Bắt buộc) CSRF token.
 * @param {string} options.csrfParameterName - (Bắt buộc) Tên của CSRF parameter.
 */
function initializeBankAccountManagement(options) {

    // --- Lấy các DOM elements ---
    const addBankBtn = document.getElementById('add-bank-btn');
    const cancelBankBtn = document.getElementById('cancel-bank-btn');
    const bankAccountFormContainer = document.getElementById('bank-account-form-container');
    const bankAccountForm = document.getElementById('bank-account-form');
    const qrCodeInput = document.getElementById('qrCodeFile');
    const qrPreview = document.getElementById('qr-preview');
    const qrBox = document.getElementById('qr-code-box');
    const qrPlaceholder = qrBox ? qrBox.querySelector('.upload-placeholder') : null;
    const accountsContainer = document.getElementById('accounts-container');

    // Kiểm tra các element quan trọng
    if (!accountsContainer || !bankAccountForm) {
        console.error("Bank Account Management: Thiếu các element HTML quan trọng (accounts-container hoặc bank-account-form).");
        return;
    }

    // --- Logic Nút "Thêm/Hủy" ---
    if (addBankBtn) {
        addBankBtn.addEventListener('click', () => {
            bankAccountForm.reset();
            if (bankAccountFormContainer) bankAccountFormContainer.style.display = 'block';
            addBankBtn.style.display = 'none';

            // Reset form về trạng thái "Thêm mới"
            document.querySelector("#bank-account-form button[type='submit']").innerHTML = '<i class="fa-solid fa-save"></i> Thêm tài khoản';
            let accountIdField = document.getElementById("bankAccountId");
            if (accountIdField) accountIdField.value = "";

            if (qrPreview) {
                qrPreview.src = '';
                qrPreview.style.display = 'none';
            }
            if (qrPlaceholder) {
                qrPlaceholder.style.display = 'flex';
            }
        });
    }

    if (cancelBankBtn) {
        cancelBankBtn.addEventListener('click', () => {
            bankAccountForm.reset();
            if (bankAccountFormContainer) bankAccountFormContainer.style.display = 'none';
            if (addBankBtn) addBankBtn.style.display = 'inline-block';

            // Reset form về trạng thái "Thêm mới"
            document.querySelector("#bank-account-form button[type='submit']").innerHTML = '<i class="fa-solid fa-save"></i> Thêm tài khoản';
            let accountIdField = document.getElementById("bankAccountId");
            if (accountIdField) accountIdField.value = "";

            if (qrPreview) {
                qrPreview.src = '';
                qrPreview.style.display = 'none';
            }
            if (qrPlaceholder) {
                qrPlaceholder.style.display = 'flex';
            }
        });
    }

    // --- Logic Preview ảnh QR ---
    if (qrCodeInput) {
        qrCodeInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                // (Thêm validation file size/type nếu cần)
                const reader = new FileReader();
                reader.onload = function(event) {
                    if (qrPreview) {
                        qrPreview.src = event.target.result;
                        qrPreview.style.display = 'block';
                    }
                    if (qrPlaceholder) {
                        qrPlaceholder.style.display = 'none';
                    }
                };
                reader.readAsDataURL(file);
            }
        });
    }


    /**
     * Tải danh sách tài khoản ngân hàng qua AJAX
     */
    function loadBankAccounts() {
        if (!accountsContainer) return;

        accountsContainer.innerHTML = '<p class="no-doc-message">Đang tải danh sách tài khoản...</p>';

        fetch(options.ajaxListUrl)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                if (data.success && data.accounts && data.accounts.length > 0) {
                    accountsContainer.innerHTML = '';
                    data.accounts.forEach(acc => {
                        const isDefault = acc.isDefault || acc.default;
                        const accountCard = document.createElement('div');
                        accountCard.className = isDefault ? "document-info verified" : "document-info pending";
                        accountCard.dataset.accountId = acc.bankAccountId;

                        let actionsHTML = `
                            <button type="button" class="btn btn-primary btn-sm"
                                    style="background: #2196f3; border-color: #2196f3;"
                                    onclick="window.editBankAccount('${acc.bankAccountId}')">
                                <i class="fa-solid fa-edit"></i> Chỉnh sửa
                            </button>
                        `;

                        // Thêm nút "Đặt/Gỡ mặc định"
                        if (isDefault) {
                            // Nếu là mặc định và có URL để gỡ, thêm nút "Gỡ mặc định"
                            if (options.unsetDefaultUrlPrefix) {
                                actionsHTML += `
                                    <form action="${options.unsetDefaultUrlPrefix}/${acc.bankAccountId}" method="POST" style="display: inline-block; margin-left: 8px;">
                                        <input type="hidden" name="${options.csrfParameterName}" value="${options.csrfToken}" />
                                        <button type="submit" class="btn btn-warning btn-sm"
                                                style="background: #f59e0b; color: white; border-color: #f59e0b;"
                                                onclick="return confirm('Bạn có chắc muốn gỡ trạng thái mặc định khỏi tài khoản này?')">
                                            <i class="fa-solid fa-times-circle"></i> Gỡ mặc định
                                        </button>
                                    </form>
                                `;
                            }
                        } else {
                            // Nếu không phải mặc định và có URL để đặt, thêm nút "Đặt làm mặc định"
                            if (options.setDefaultUrlPrefix) {
                                actionsHTML += `
                                    <form action="${options.setDefaultUrlPrefix}/${acc.bankAccountId}" method="POST" style="display: inline-block; margin-left: 8px;">
                                        <input type="hidden" name="${options.csrfParameterName}" value="${options.csrfToken}" />
                                        <button type="submit" class="btn btn-secondary btn-sm"
                                                style="background: #6b7280; color: white; border-color: #6b7280;"
                                                onclick="return confirm('Bạn có chắc muốn đặt tài khoản này làm mặc định?')">
                                            <i class="fa-solid fa-check-circle"></i> Đặt mặc định
                                        </button>
                                    </form>
                                `;
                            }
                        }

                        // Thêm nút "Xóa"
                        if (options.deleteUrlPrefix) {
                            actionsHTML += `
                                <form action="${options.deleteUrlPrefix}/${acc.bankAccountId}" method="POST" style="display: inline-block; margin-left: 8px;">
                                    <input type="hidden" name="${options.csrfParameterName}" value="${options.csrfToken}" />
                                    <button type="submit" class="btn btn-danger btn-sm"
                                            style="background: #ef4444; color: white; border-color: #ef4444;"
                                            onclick="return confirm('Bạn có chắc muốn xóa tài khoản này? Hành động này không thể hoàn tác.')">
                                        <i class="fa-solid fa-trash"></i> Xóa
                                    </button>
                                </form>
                            `;
                        }

                        accountCard.innerHTML = `
                            <div class="doc-status-badge">
                                ${isDefault ? '<i class="fa-solid fa-check-circle"></i> Mặc định' : '<i class="fa-solid fa-clock"></i> Phụ'}
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                                <div class="doc-details" style="margin-bottom: 0;">
                                    <p><strong><span data-field="bankName">${acc.bankName}</span></strong></p>
                                    <p><strong>Số TK:</strong> <span data-field="accountNumber">${acc.accountNumber}</span></p>
                                    <p><strong>Chủ TK:</strong> <span data-field="accountHolderName">${acc.accountHolderName}</span></p>
                                    <span data-field="bankCode" style="display:none;">${acc.bankCode || ''}</span>
                                </div>
                                ${acc.qrCodeImagePath ? `
                                    <div class="doc-images" style="margin: 0;">
                                        <img src="${acc.qrCodeImagePath}" alt="QR Code"
                                             style="width: 100px; height: 100px; border-radius: 8px; border: 1px solid #ddd;"
                                             onclick="window.open('${acc.qrCodeImagePath}', '_blank')">
                                    </div>
                                ` : ''}
                            </div>
                            <div class="doc-actions">
                                ${actionsHTML}
                            </div>
                        `;
                        accountsContainer.appendChild(accountCard);
                    });
                } else {
                    accountsContainer.innerHTML = '<p class="no-doc-message">Chưa có tài khoản ngân hàng nào được thêm.</p>';
                }
            })
            .catch(error => {
                console.error("Error loading bank accounts:", error);
                accountsContainer.innerHTML = '<p class="no-doc-message" style="color: red;">Lỗi khi tải danh sách tài khoản.</p>';
            });
    }

    // --- Tự động tải danh sách khi JS được gọi ---
    loadBankAccounts();
}
