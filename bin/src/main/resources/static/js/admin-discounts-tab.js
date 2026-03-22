(function() {
    'use strict';
    console.log('Discounts tab script loaded');
    
    var discountsData = [];
    var filteredData = [];
    
    function loadDiscounts() {
        console.log('Loading discounts...');
        fetch('/admin/api/discounts')
            .then(function(response) {
                console.log('Response status:', response.status);
                if (!response.ok) throw new Error('Failed to load discounts');
                return response.json();
            })
            .then(function(data) {
                console.log('Received discounts:', data);
                discountsData = data || [];
                filteredData = discountsData;
                renderDiscounts();
                updateStatistics();
            })
            .catch(function(error) {
                console.error('Error loading discounts:', error);
                showError();
            });
    }
    
    function loadStatistics() {
        fetch('/admin/api/discounts/statistics')
            .then(function(response) {
                if (!response.ok) throw new Error('Failed to load statistics');
                return response.json();
            })
            .then(function(stats) {
                updateStatisticsCards(stats);
            })
            .catch(function(error) {
                console.error('Error loading statistics:', error);
            });
    }
    
    function updateStatisticsCards(stats) {
        var totalEl = document.getElementById('discountsTotalCount');
        var activeEl = document.getElementById('discountsActiveCount');
        var inactiveEl = document.getElementById('discountsInactiveCount');
        var expiredEl = document.getElementById('discountsExpiredCount');
        
        if (totalEl) totalEl.textContent = stats.total || 0;
        if (activeEl) activeEl.textContent = stats.active || 0;
        if (inactiveEl) inactiveEl.textContent = stats.inactive || 0;
        if (expiredEl) expiredEl.textContent = stats.expired || 0;
    }
    
    function renderDiscounts() {
        var tbody = document.getElementById('discountsTableBody');
        if (!tbody) {
            console.error('Discounts table body not found!');
            return;
        }
        
        if (filteredData.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="px-6 py-12 text-center text-gray-500">No discounts found</td></tr>';
            return;
        }
        
        var html = '';
        filteredData.forEach(function(discount) {
            html += createDiscountRow(discount);
        });
        tbody.innerHTML = html;
    }
    
    function createDiscountRow(discount) {
        var statusInfo = getStatusInfo(discount);
        var html = '<tr class="hover:bg-gray-50">';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<div class="text-sm font-medium text-gray-900">' + escapeHtml(discount.discountName) + '</div>';
        html += '<div class="text-xs text-gray-500">' + escapeHtml(discount.description || '') + '</div>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ' + getTypeClass(discount.discountType) + '">';
        html += escapeHtml(discount.discountType) + '</span>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-purple-600">';
        if (discount.discountType === 'Percentage') {
            html += discount.discountValue + '%';
        } else {
            html += discount.discountValue.toLocaleString('vi-VN') + ' VND';
        }
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<code class="px-2 py-1 bg-gray-100 text-gray-800 rounded text-xs font-mono">' + escapeHtml(discount.voucherCode || '-') + '</code>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">';
        html += formatDate(discount.startDate) + '<br/><span class="text-xs text-gray-500">to</span><br/>' + formatDate(discount.endDate);
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">';
        var usageLimit = discount.usageLimit || '∞';
        html += discount.usedCount + ' / ' + usageLimit;
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ' + statusInfo.class + '">';
        html += '<i class="fas fa-circle text-xs mr-1"></i>' + statusInfo.text + '</span>';
        html += '</td>';
        
        html += '<td class="sticky right-0 bg-white px-6 py-4 whitespace-nowrap text-right text-sm font-medium shadow-lg">';
        html += '<button class="text-indigo-600 hover:text-indigo-900 mr-2" onclick="viewDiscountDetail(\'' + discount.discountId + '\')" title="View"><i class="fas fa-eye"></i></button>';
        html += '<button class="text-blue-600 hover:text-blue-900 mr-2" onclick="editDiscount(\'' + discount.discountId + '\')" title="Edit"><i class="fas fa-edit"></i></button>';
        html += '<button class="text-' + (discount.isActive ? 'orange' : 'green') + '-600 hover:text-' + (discount.isActive ? 'orange' : 'green') + '-900 mr-2" onclick="toggleDiscountStatus(\'' + discount.discountId + '\')" title="Toggle Status"><i class="fas fa-power-off"></i></button>';
        html += '<button class="text-red-600 hover:text-red-900" onclick="deleteDiscount(\'' + discount.discountId + '\')" title="Delete"><i class="fas fa-trash"></i></button>';
        html += '</td>';
        
        html += '</tr>';
        return html;
    }
    
    function updateStatistics() {
        var total = discountsData.length;
        var active = discountsData.filter(function(d) { return d.isActive; }).length;
        var inactive = discountsData.filter(function(d) { return !d.isActive; }).length;
        var today = new Date().toISOString().split('T')[0];
        var expired = discountsData.filter(function(d) { return d.endDate < today; }).length;
        
        updateStatisticsCards({
            total: total,
            active: active,
            inactive: inactive,
            expired: expired
        });
    }
    
    function getStatusInfo(discount) {
        var today = new Date().toISOString().split('T')[0];
        if (discount.endDate < today) {
            return { text: 'Expired', class: 'bg-red-100 text-red-800' };
        } else if (!discount.isActive) {
            return { text: 'Inactive', class: 'bg-gray-100 text-gray-800' };
        } else if (discount.usageLimit && discount.usedCount >= discount.usageLimit) {
            return { text: 'Used Up', class: 'bg-orange-100 text-orange-800' };
        } else {
            return { text: 'Active', class: 'bg-green-100 text-green-800' };
        }
    }
    
    function getTypeClass(type) {
        return type === 'Percentage' ? 'bg-blue-100 text-blue-800' : 'bg-purple-100 text-purple-800';
    }
    
    function formatDate(dateString) {
        if (!dateString) return '-';
        var date = new Date(dateString);
        return date.toLocaleDateString('vi-VN');
    }
    
    function escapeHtml(text) {
        if (!text) return '';
        var map = {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'};
        return text.toString().replace(/[&<>"']/g, function(m) { return map[m]; });
    }
    
    function showError() {
        var tbody = document.getElementById('discountsTableBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="8" class="px-6 py-12 text-center text-red-500">Error loading discounts. Please try again.</td></tr>';
        }
    }
    
    // Event listeners
    var statusFilter = document.getElementById('discountsStatusFilter');
    var typeFilter = document.getElementById('discountsTypeFilter');
    var searchInput = document.getElementById('discountsSearchInput');
    
    if (statusFilter) {
        statusFilter.addEventListener('change', function() {
            applyFilters();
        });
    }
    
    if (typeFilter) {
        typeFilter.addEventListener('change', function() {
            applyFilters();
        });
    }
    
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            applyFilters();
        });
    }
    
    function applyFilters() {
        var status = statusFilter ? statusFilter.value : '';
        var type = typeFilter ? typeFilter.value : '';
        var search = searchInput ? searchInput.value.toLowerCase() : '';
        var today = new Date().toISOString().split('T')[0];
        
        filteredData = discountsData.filter(function(d) {
            var matchStatus = !status || 
                (status === 'active' && d.isActive && d.endDate >= today) ||
                (status === 'inactive' && !d.isActive) ||
                (status === 'expired' && d.endDate < today);
            
            var matchType = !type || d.discountType === type;
            
            var matchSearch = !search || 
                (d.discountName && d.discountName.toLowerCase().includes(search)) ||
                (d.voucherCode && d.voucherCode.toLowerCase().includes(search)) ||
                (d.description && d.description.toLowerCase().includes(search));
            
            return matchStatus && matchType && matchSearch;
        });
        
        renderDiscounts();
    }
    
    // Load discounts when tab is shown
    window.addEventListener('tabChanged', function(e) {
        if (e.detail === 'discounts') {
            console.log('Discounts tab activated, loading...');
            loadDiscounts();
            loadStatistics();
        }
    });
    
    // View Discount Detail
    window.viewDiscountDetail = function(discountId) {
        fetch('/admin/api/discounts/' + discountId)
            .then(function(response) {
                if (!response.ok) throw new Error('Failed to load discount');
                return response.json();
            })
            .then(function(discount) {
                showDiscountDetailModal(discount);
            })
            .catch(function(error) {
                console.error('Error:', error);
                alert('Failed to load discount details');
            });
    };
    
    function showDiscountDetailModal(discount) {
        var modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4';
        modal.id = 'discountDetailModal';
        modal.onclick = function(e) {
            if (e.target === modal) closeDiscountDetailModal();
        };
        
        var statusInfo = getStatusInfo(discount);
        var content = '<div class="relative bg-white rounded-xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto" onclick="event.stopPropagation()">';
        
        // Header
        content += '<div class="bg-gradient-to-r from-purple-600 to-pink-600 p-6 rounded-t-xl">';
        content += '<div class="flex justify-between items-center">';
        content += '<div>';
        content += '<h3 class="text-2xl font-bold text-white">' + escapeHtml(discount.discountName) + '</h3>';
        content += '<p class="text-purple-100 text-sm mt-1">' + escapeHtml(discount.description || '') + '</p>';
        content += '</div>';
        content += '<button onclick="closeDiscountDetailModal()" class="text-white hover:text-gray-200 transition-colors">';
        content += '<i class="fas fa-times text-2xl"></i></button>';
        content += '</div></div>';
        
        // Status badge
        content += '<div class="px-6 py-4 border-b border-gray-200 bg-gray-50">';
        content += '<span class="px-4 py-2 rounded-full text-sm font-semibold ' + statusInfo.class + '">';
        content += '<i class="fas fa-circle text-xs mr-2"></i>' + statusInfo.text + '</span>';
        content += '</div>';
        
        // Content
        content += '<div class="p-6 grid grid-cols-2 gap-4">';
        content += createDetailCard('Discount Type', '<span class="px-2 py-1 rounded-full text-xs font-semibold ' + getTypeClass(discount.discountType) + '">' + discount.discountType + '</span>', 'fa-tag');
        
        var valueText = discount.discountType === 'Percentage' ? discount.discountValue + '%' : discount.discountValue.toLocaleString('vi-VN') + ' VND';
        content += createDetailCard('Discount Value', '<span class="text-purple-600 font-bold text-lg">' + valueText + '</span>', 'fa-percentage');
        content += createDetailCard('Voucher Code', '<code class="px-2 py-1 bg-gray-100 text-gray-800 rounded font-mono">' + escapeHtml(discount.voucherCode || '-') + '</code>', 'fa-ticket-alt');
        content += createDetailCard('Category', discount.discountCategory, 'fa-folder');
        content += createDetailCard('Min Order Amount', discount.minOrderAmount.toLocaleString('vi-VN') + ' VND', 'fa-shopping-cart');
        content += createDetailCard('Max Discount', discount.maxDiscountAmount ? discount.maxDiscountAmount.toLocaleString('vi-VN') + ' VND' : 'No limit', 'fa-coins');
        content += createDetailCard('Start Date', formatDate(discount.startDate), 'fa-calendar-plus');
        content += createDetailCard('End Date', formatDate(discount.endDate), 'fa-calendar-times');
        content += createDetailCard('Usage Limit', discount.usageLimit || 'Unlimited', 'fa-infinity');
        content += createDetailCard('Used Count', discount.usedCount, 'fa-users');
        content += createDetailCard('Created Date', formatDate(discount.createdDate), 'fa-clock');
        content += createDetailCard('Active Status', discount.isActive ? '<span class="text-green-600"><i class="fas fa-check-circle mr-1"></i>Active</span>' : '<span class="text-red-600"><i class="fas fa-times-circle mr-1"></i>Inactive</span>', 'fa-power-off');
        content += '</div>';
        
        // Footer
        content += '<div class="px-6 py-4 bg-gray-50 rounded-b-xl flex justify-end space-x-3">';
        content += '<button onclick="closeDiscountDetailModal(); editDiscount(\'' + discount.discountId + '\');" class="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors">';
        content += '<i class="fas fa-edit mr-2"></i>Edit</button>';
        content += '<button onclick="closeDiscountDetailModal()" class="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-colors">';
        content += '<i class="fas fa-times mr-2"></i>Close</button>';
        content += '</div></div>';
        
        modal.innerHTML = content;
        document.body.appendChild(modal);
    }
    
    function createDetailCard(label, value, icon) {
        return '<div class="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">' +
               '<div class="flex items-start">' +
               '<div class="flex-shrink-0 w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center mr-3">' +
               '<i class="fas ' + icon + ' text-gray-600"></i></div>' +
               '<div class="flex-1 min-w-0">' +
               '<p class="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">' + escapeHtml(label) + '</p>' +
               '<p class="text-sm font-semibold text-gray-900 break-words">' + value + '</p>' +
               '</div></div></div>';
    }
    
    window.closeDiscountDetailModal = function() {
        var modal = document.getElementById('discountDetailModal');
        if (modal) modal.remove();
    };
    
    // Edit Discount
    window.editDiscount = function(discountId) {
        fetch('/admin/api/discounts/' + discountId)
            .then(function(response) {
                if (!response.ok) throw new Error('Failed to load discount');
                return response.json();
            })
            .then(function(discount) {
                showEditDiscountModal(discount);
            })
            .catch(function(error) {
                console.error('Error:', error);
                alert('Failed to load discount for editing');
            });
    };
    
    function showEditDiscountModal(discount) {
        var modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4';
        modal.id = 'editDiscountModal';
        modal.onclick = function(e) {
            if (e.target === modal) closeEditDiscountModal();
        };
        
        var content = '<div class="relative bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto" onclick="event.stopPropagation()">';
        
        // Header
        content += '<div class="bg-gradient-to-r from-purple-600 to-pink-600 p-6 rounded-t-xl">';
        content += '<div class="flex justify-between items-center">';
        content += '<h3 class="text-2xl font-bold text-white">Edit Discount</h3>';
        content += '<button onclick="closeEditDiscountModal()" class="text-white hover:text-gray-200 transition-colors">';
        content += '<i class="fas fa-times text-2xl"></i></button>';
        content += '</div></div>';
        
        // Form
        content += '<form id="editDiscountForm" class="p-6 space-y-4">';
        
        content += '<div class="grid grid-cols-2 gap-4">';
        content += '<div class="col-span-2"><label class="block text-sm font-medium text-gray-700 mb-2">Discount Name</label>';
        content += '<input type="text" id="editDiscountName" value="' + escapeHtml(discount.discountName) + '" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div class="col-span-2"><label class="block text-sm font-medium text-gray-700 mb-2">Description</label>';
        content += '<textarea id="editDescription" rows="2" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500">' + escapeHtml(discount.description || '') + '</textarea></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Type</label>';
        content += '<select id="editDiscountType" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500">';
        content += '<option value="Percentage" ' + (discount.discountType === 'Percentage' ? 'selected' : '') + '>Percentage</option>';
        content += '<option value="FixedAmount" ' + (discount.discountType === 'FixedAmount' ? 'selected' : '') + '>Fixed Amount</option>';
        content += '</select></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Value</label>';
        content += '<input type="number" id="editDiscountValue" value="' + discount.discountValue + '" step="0.01" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Voucher Code</label>';
        content += '<input type="text" id="editVoucherCode" value="' + escapeHtml(discount.voucherCode || '') + '" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Category</label>';
        content += '<input type="text" id="editCategory" value="' + escapeHtml(discount.discountCategory) + '" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Start Date</label>';
        content += '<input type="date" id="editStartDate" value="' + discount.startDate + '" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">End Date</label>';
        content += '<input type="date" id="editEndDate" value="' + discount.endDate + '" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Min Order Amount</label>';
        content += '<input type="number" id="editMinOrder" value="' + discount.minOrderAmount + '" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Max Discount</label>';
        content += '<input type="number" id="editMaxDiscount" value="' + (discount.maxDiscountAmount || '') + '" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Usage Limit</label>';
        content += '<input type="number" id="editUsageLimit" value="' + (discount.usageLimit || '') + '" placeholder="Unlimited" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"></div>';
        
        content += '<div class="flex items-center"><input type="checkbox" id="editIsActive" ' + (discount.isActive ? 'checked' : '') + ' class="w-4 h-4 text-purple-600 border-gray-300 rounded focus:ring-purple-500">';
        content += '<label for="editIsActive" class="ml-2 text-sm text-gray-700">Active</label></div>';
        
        content += '</div></form>';
        
        // Footer
        content += '<div class="px-6 py-4 bg-gray-50 rounded-b-xl flex justify-end space-x-3">';
        content += '<button onclick="closeEditDiscountModal()" class="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-colors"><i class="fas fa-times mr-2"></i>Cancel</button>';
        content += '<button onclick="saveDiscountChanges(\'' + discount.discountId + '\')" class="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"><i class="fas fa-save mr-2"></i>Save Changes</button>';
        content += '</div></div>';
        
        modal.innerHTML = content;
        document.body.appendChild(modal);
    }
    
    window.saveDiscountChanges = function(discountId) {
        var data = {
            discountName: document.getElementById('editDiscountName').value,
            description: document.getElementById('editDescription').value,
            discountType: document.getElementById('editDiscountType').value,
            discountValue: parseFloat(document.getElementById('editDiscountValue').value),
            voucherCode: document.getElementById('editVoucherCode').value,
            discountCategory: document.getElementById('editCategory').value,
            startDate: document.getElementById('editStartDate').value,
            endDate: document.getElementById('editEndDate').value,
            minOrderAmount: parseFloat(document.getElementById('editMinOrder').value),
            maxDiscountAmount: document.getElementById('editMaxDiscount').value ? parseFloat(document.getElementById('editMaxDiscount').value) : null,
            usageLimit: document.getElementById('editUsageLimit').value ? parseInt(document.getElementById('editUsageLimit').value) : null,
            isActive: document.getElementById('editIsActive').checked
        };
        
        fetch('/admin/api/discounts/' + discountId, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        })
        .then(function(response) {
            if (!response.ok) throw new Error('Failed to update discount');
            return response.json();
        })
        .then(function(result) {
            if (result.status === 'success') {
                alert('✅ Discount updated successfully!');
                closeEditDiscountModal();
                loadDiscounts();
            } else {
                throw new Error(result.message || 'Update failed');
            }
        })
        .catch(function(error) {
            console.error('Error:', error);
            alert('❌ Failed to update discount: ' + error.message);
        });
    };
    
    window.closeEditDiscountModal = function() {
        var modal = document.getElementById('editDiscountModal');
        if (modal) modal.remove();
    };
    
    window.deleteDiscount = function(discountId) {
        if (!confirm('Are you sure you want to delete this discount?')) return;
        
        fetch('/admin/api/discounts/' + discountId, {
            method: 'DELETE'
        })
        .then(function(response) {
            if (!response.ok) throw new Error('Failed to delete discount');
            return response.json();
        })
        .then(function(result) {
            if (result.status === 'success') {
                alert('✅ Discount deleted successfully!');
                loadDiscounts();
            } else {
                throw new Error(result.message || 'Delete failed');
            }
        })
        .catch(function(error) {
            console.error('Error:', error);
            alert('❌ Failed to delete discount: ' + error.message);
        });
    };
    
    window.toggleDiscountStatus = function(discountId) {
        fetch('/admin/api/discounts/' + discountId + '/toggle-status', {
            method: 'POST'
        })
        .then(function(response) {
            if (!response.ok) throw new Error('Failed to toggle status');
            return response.json();
        })
        .then(function(result) {
            if (result.status === 'success') {
                alert('✅ Status updated!');
                loadDiscounts();
            } else {
                throw new Error(result.message || 'Operation failed');
            }
        })
        .catch(function(error) {
            console.error('Error:', error);
            alert('❌ Failed: ' + error.message);
        });
    };
    
    window.showAddDiscountModal = function() {
        var modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4';
        modal.id = 'addDiscountModal';
        modal.onclick = function(e) {
            if (e.target === modal) closeAddDiscountModal();
        };
        
        var today = new Date().toISOString().split('T')[0];
        var content = '<div class="relative bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto" onclick="event.stopPropagation()">';
        
        // Header
        content += '<div class="bg-gradient-to-r from-purple-600 to-pink-600 p-6 rounded-t-xl">';
        content += '<div class="flex justify-between items-center">';
        content += '<h3 class="text-2xl font-bold text-white"><i class="fas fa-plus-circle mr-2"></i>Add New Discount</h3>';
        content += '<button onclick="closeAddDiscountModal()" class="text-white hover:text-gray-200 transition-colors">';
        content += '<i class="fas fa-times text-2xl"></i></button>';
        content += '</div></div>';
        
        // Form
        content += '<form id="addDiscountForm" class="p-6 space-y-4">';
        
        content += '<div class="grid grid-cols-2 gap-4">';
        content += '<div class="col-span-2"><label class="block text-sm font-medium text-gray-700 mb-2">Discount Name *</label>';
        content += '<input type="text" id="addDiscountName" placeholder="e.g., Summer Sale 2024" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div class="col-span-2"><label class="block text-sm font-medium text-gray-700 mb-2">Description</label>';
        content += '<textarea id="addDescription" rows="2" placeholder="Describe this discount..." class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"></textarea></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Type *</label>';
        content += '<select id="addDiscountType" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required>';
        content += '<option value="Percentage">Percentage (%)</option>';
        content += '<option value="FixedAmount">Fixed Amount (VND)</option>';
        content += '</select></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Value *</label>';
        content += '<input type="number" id="addDiscountValue" placeholder="e.g., 10 or 50000" step="0.01" min="0" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Voucher Code</label>';
        content += '<input type="text" id="addVoucherCode" placeholder="e.g., SUMMER2024" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Category *</label>';
        content += '<input type="text" id="addCategory" placeholder="e.g., Seasonal" value="General" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Start Date *</label>';
        content += '<input type="date" id="addStartDate" value="' + today + '" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">End Date *</label>';
        content += '<input type="date" id="addEndDate" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Min Order Amount *</label>';
        content += '<input type="number" id="addMinOrder" placeholder="e.g., 100000" value="0" min="0" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500" required></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Max Discount Amount</label>';
        content += '<input type="number" id="addMaxDiscount" placeholder="Leave empty for no limit" min="0" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"></div>';
        
        content += '<div><label class="block text-sm font-medium text-gray-700 mb-2">Usage Limit</label>';
        content += '<input type="number" id="addUsageLimit" placeholder="Leave empty for unlimited" min="1" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"></div>';
        
        content += '<div class="flex items-center"><input type="checkbox" id="addIsActive" checked class="w-4 h-4 text-purple-600 border-gray-300 rounded focus:ring-purple-500">';
        content += '<label for="addIsActive" class="ml-2 text-sm text-gray-700">Active</label></div>';
        
        content += '</div></form>';
        
        // Footer
        content += '<div class="px-6 py-4 bg-gray-50 rounded-b-xl flex justify-end space-x-3">';
        content += '<button onclick="closeAddDiscountModal()" class="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-colors"><i class="fas fa-times mr-2"></i>Cancel</button>';
        content += '<button onclick="createNewDiscount()" class="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"><i class="fas fa-plus mr-2"></i>Create Discount</button>';
        content += '</div></div>';
        
        modal.innerHTML = content;
        document.body.appendChild(modal);
    };
    
    window.createNewDiscount = function() {
        var data = {
            discountName: document.getElementById('addDiscountName').value,
            description: document.getElementById('addDescription').value,
            discountType: document.getElementById('addDiscountType').value,
            discountValue: parseFloat(document.getElementById('addDiscountValue').value),
            voucherCode: document.getElementById('addVoucherCode').value || null,
            discountCategory: document.getElementById('addCategory').value,
            startDate: document.getElementById('addStartDate').value,
            endDate: document.getElementById('addEndDate').value,
            minOrderAmount: parseFloat(document.getElementById('addMinOrder').value),
            maxDiscountAmount: document.getElementById('addMaxDiscount').value ? parseFloat(document.getElementById('addMaxDiscount').value) : null,
            usageLimit: document.getElementById('addUsageLimit').value ? parseInt(document.getElementById('addUsageLimit').value) : null,
            isActive: document.getElementById('addIsActive').checked
        };
        
        // Validation
        if (!data.discountName || !data.discountType || !data.discountValue || !data.startDate || !data.endDate) {
            alert('❌ Please fill in all required fields!');
            return;
        }
        
        if (data.startDate > data.endDate) {
            alert('❌ End date must be after start date!');
            return;
        }
        
        fetch('/admin/api/discounts', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
            },
            body: JSON.stringify(data)
        })
        .then(function(response) {
            if (!response.ok) throw new Error('Failed to create discount');
            return response.json();
        })
        .then(function(result) {
            if (result.status === 'success') {
                alert('✅ Discount created successfully!');
                closeAddDiscountModal();
                loadDiscounts();
                loadStatistics();
            } else {
                throw new Error(result.message || 'Creation failed');
            }
        })
        .catch(function(error) {
            console.error('Error:', error);
            alert('❌ Failed to create discount: ' + error.message);
        });
    };
    
    window.closeAddDiscountModal = function() {
        var modal = document.getElementById('addDiscountModal');
        if (modal) modal.remove();
    };
    
    // Initial load if discounts tab is active
    if (document.getElementById('discounts') && document.getElementById('discounts').classList.contains('active')) {
        loadDiscounts();
        loadStatistics();
    }
})();
