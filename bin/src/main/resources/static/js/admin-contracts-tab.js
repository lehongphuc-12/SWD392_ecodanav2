(function() {
    'use strict';
    console.log('Contracts tab script loaded');
    
    var contractsData = [];
    var filteredData = [];
    
    function loadContracts() {
        console.log('Loading contracts...');
        fetch('/admin/api/contracts')
            .then(function(response) {
                console.log('Response status:', response.status);
                if (!response.ok) throw new Error('Failed to load contracts');
                return response.json();
            })
            .then(function(data) {
                console.log('Received contracts:', data);
                contractsData = data || [];
                filteredData = contractsData;
                renderContracts();
                updateStatistics();
            })
            .catch(function(error) {
                console.error('Error loading contracts:', error);
                showError();
            });
    }
    
    function loadStatistics() {
        fetch('/admin/api/contracts/statistics')
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
        var totalEl = document.getElementById('contractsTotalCount');
        var draftEl = document.getElementById('contractsDraftCount');
        var signedEl = document.getElementById('contractsSignedCount');
        var completedEl = document.getElementById('contractsCompletedCount');
        var cancelledEl = document.getElementById('contractsCancelledCount');
        
        if (totalEl) totalEl.textContent = stats.total || 0;
        if (draftEl) draftEl.textContent = stats.draft || 0;
        if (signedEl) signedEl.textContent = stats.signed || 0;
        if (completedEl) completedEl.textContent = stats.completed || 0;
        if (cancelledEl) cancelledEl.textContent = stats.cancelled || 0;
    }
    
    function renderContracts() {
        var tbody = document.getElementById('contractsTableBody');
        if (!tbody) {
            console.error('Contracts table body not found!');
            return;
        }
        
        if (filteredData.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="px-6 py-12 text-center text-gray-500">No contracts found</td></tr>';
            return;
        }
        
        var html = '';
        filteredData.forEach(function(contract) {
            html += createContractRow(contract);
        });
        tbody.innerHTML = html;
    }
    
    function createContractRow(contract) {
        var statusClass = getStatusClass(contract.status);
        var html = '<tr class="hover:bg-gray-50">';
        
        html += '<td class="px-6 py-4 whitespace-nowrap"><div class="text-sm font-medium text-gray-900">' + escapeHtml(contract.contractCode || '-') + '</div></td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<div class="text-sm font-medium text-gray-900">' + escapeHtml(contract.userName || '-') + '</div>';
        html += '<div class="text-xs text-gray-500">' + escapeHtml(contract.userEmail || '-') + '</div>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<div class="text-sm text-gray-900">' + escapeHtml(contract.vehicleModel || '-') + '</div>';
        html += '<div class="text-xs text-gray-500">' + escapeHtml(contract.licensePlate || '-') + '</div>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap">';
        html += '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full ' + statusClass + '">' + escapeHtml(contract.status) + '</span>';
        html += '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + formatDateTime(contract.createdDate) + '</td>';
        html += '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + (contract.signedDate ? formatDateTime(contract.signedDate) : 'Not signed') + '</td>';
        
        html += '<td class="px-6 py-4 whitespace-nowrap text-sm font-medium">';
        html += '<button class="text-indigo-600 hover:text-indigo-900 mr-3" onclick="viewContractDetail(\'' + contract.contractId + '\')"><i class="fas fa-eye"></i></button>';
        
        if (contract.status === 'Draft') {
            html += '<button class="text-green-600 hover:text-green-900 mr-3" onclick="signContract(\'' + contract.contractId + '\')"><i class="fas fa-signature"></i></button>';
        }
        if (contract.status === 'Signed') {
            html += '<button class="text-blue-600 hover:text-blue-900 mr-3" onclick="completeContract(\'' + contract.contractId + '\')"><i class="fas fa-check"></i></button>';
        }
        if (contract.status !== 'Completed' && contract.status !== 'Cancelled') {
            html += '<button class="text-red-600 hover:text-red-900" onclick="cancelContract(\'' + contract.contractId + '\')"><i class="fas fa-times"></i></button>';
        }
        html += '</td>';
        
        html += '</tr>';
        return html;
    }
    
    function updateStatistics() {
        var total = contractsData.length;
        var draft = contractsData.filter(function(c) { return c.status === 'Draft'; }).length;
        var signed = contractsData.filter(function(c) { return c.status === 'Signed'; }).length;
        var completed = contractsData.filter(function(c) { return c.status === 'Completed'; }).length;
        var cancelled = contractsData.filter(function(c) { return c.status === 'Cancelled'; }).length;
        
        updateStatisticsCards({
            total: total,
            draft: draft,
            signed: signed,
            completed: completed,
            cancelled: cancelled
        });
    }
    
    function getStatusClass(status) {
        var classes = {
            'Draft': 'bg-yellow-100 text-yellow-800',
            'Signed': 'bg-green-100 text-green-800',
            'Completed': 'bg-blue-100 text-blue-800',
            'Cancelled': 'bg-red-100 text-red-800'
        };
        return classes[status] || 'bg-gray-100 text-gray-800';
    }
    
    function formatDateTime(dateString) {
        if (!dateString) return '-';
        var date = new Date(dateString);
        return date.toLocaleDateString('vi-VN') + ' ' + date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    }
    
    function escapeHtml(text) {
        if (!text) return '';
        var map = {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'};
        return text.toString().replace(/[&<>"']/g, function(m) { return map[m]; });
    }
    
    function showError() {
        var tbody = document.getElementById('contractsTableBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="7" class="px-6 py-12 text-center text-red-500">Error loading contracts. Please try again.</td></tr>';
        }
    }
    
    // Event listeners
    var statusFilter = document.getElementById('contractsStatusFilter');
    var searchInput = document.getElementById('contractsSearchInput');
    
    if (statusFilter) {
        statusFilter.addEventListener('change', function() {
            var status = this.value;
            filteredData = status ? contractsData.filter(function(c) { return c.status === status; }) : contractsData;
            renderContracts();
        });
    }
    
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            var search = this.value.toLowerCase();
            filteredData = contractsData.filter(function(c) {
                return (c.contractCode && c.contractCode.toLowerCase().includes(search)) ||
                       (c.userName && c.userName.toLowerCase().includes(search)) ||
                       (c.userEmail && c.userEmail.toLowerCase().includes(search)) ||
                       (c.vehicleModel && c.vehicleModel.toLowerCase().includes(search));
            });
            renderContracts();
        });
    }
    
    // Load contracts when tab is shown
    window.addEventListener('tabChanged', function(e) {
        if (e.detail === 'contracts') {
            console.log('Contracts tab activated, loading...');
            loadContracts();
            loadStatistics();
        }
    });
    
    // Global functions for buttons
    window.viewContractDetail = function(contractId) {
        fetch('/admin/api/contracts/' + contractId)
            .then(function(response) {
                if (!response.ok) throw new Error('Failed to load contract');
                return response.json();
            })
            .then(function(contract) {
                showContractDetailModal(contract);
            })
            .catch(function(error) {
                console.error('Error:', error);
                alert('Failed to load contract details');
            });
    };
    
    window.signContract = function(contractId) {
        if (!confirm('Are you sure you want to sign this contract?')) return;
        
        fetch('/admin/contracts/' + contractId + '/sign', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({signatureData: 'admin-signature', signatureMethod: 'digital'})
        })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            alert(result.message);
            if (result.status === 'success') loadContracts();
        })
        .catch(function(error) {
            alert('Error: ' + error.message);
        });
    };
    
    window.completeContract = function(contractId) {
        if (!confirm('Are you sure you want to complete this contract?')) return;
        
        fetch('/admin/contracts/' + contractId + '/complete', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            alert(result.message);
            if (result.status === 'success') loadContracts();
        })
        .catch(function(error) {
            alert('Error: ' + error.message);
        });
    };
    
    window.cancelContract = function(contractId) {
        var reason = prompt('Please enter cancellation reason:');
        if (!reason) return;
        
        fetch('/admin/contracts/' + contractId + '/cancel', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({reason: reason})
        })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            alert(result.message);
            if (result.status === 'success') loadContracts();
        })
        .catch(function(error) {
            alert('Error: ' + error.message);
        });
    };
    
    function showContractDetailModal(contract) {
        var modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4';
        modal.id = 'contractDetailModal';
        modal.onclick = function(e) {
            if (e.target === modal) closeContractDetailModal();
        };
        
        var content = '<div class="relative bg-white rounded-xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto" onclick="event.stopPropagation()">';
        
        // Header with gradient
        content += '<div class="bg-gradient-to-r from-blue-600 to-purple-600 p-6 rounded-t-xl">';
        content += '<div class="flex justify-between items-center">';
        content += '<div>';
        content += '<h3 class="text-2xl font-bold text-white">Contract Details</h3>';
        content += '<p class="text-blue-100 text-sm mt-1">' + escapeHtml(contract.contractCode) + '</p>';
        content += '</div>';
        content += '<button onclick="closeContractDetailModal()" class="text-white hover:text-gray-200 transition-colors">';
        content += '<i class="fas fa-times text-2xl"></i></button>';
        content += '</div></div>';
        
        // Status badge
        content += '<div class="px-6 py-4 border-b border-gray-200 bg-gray-50">';
        content += '<span class="px-4 py-2 rounded-full text-sm font-semibold ' + getStatusClass(contract.status) + '">';
        content += '<i class="fas fa-circle text-xs mr-2"></i>' + contract.status + '</span>';
        content += '</div>';
        
        // Content
        content += '<div class="p-6">';
        
        // Customer Information
        content += '<div class="mb-6">';
        content += '<h4 class="text-lg font-semibold text-gray-900 mb-4 flex items-center">';
        content += '<i class="fas fa-user text-blue-600 mr-2"></i>Customer Information</h4>';
        content += '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">';
        content += createDetailCard('Customer Name', contract.userName || '-', 'fa-user');
        content += createDetailCard('Email', contract.userEmail || '-', 'fa-envelope');
        content += createDetailCard('Phone', contract.userPhone || '-', 'fa-phone');
        content += '</div></div>';
        
        // Vehicle Information
        content += '<div class="mb-6">';
        content += '<h4 class="text-lg font-semibold text-gray-900 mb-4 flex items-center">';
        content += '<i class="fas fa-car text-purple-600 mr-2"></i>Vehicle Information</h4>';
        content += '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">';
        content += createDetailCard('Vehicle Model', contract.vehicleModel || '-', 'fa-car-side');
        content += createDetailCard('License Plate', contract.licensePlate || '-', 'fa-id-card');
        content += createDetailCard('Booking Code', contract.bookingCode || '-', 'fa-ticket-alt');
        content += createDetailCard('Total Amount', contract.totalAmount ? '<span class="text-green-600 font-bold">' + contract.totalAmount.toLocaleString('vi-VN') + ' VND</span>' : '-', 'fa-money-bill-wave');
        content += '</div></div>';
        
        // Contract Information
        content += '<div class="mb-6">';
        content += '<h4 class="text-lg font-semibold text-gray-900 mb-4 flex items-center">';
        content += '<i class="fas fa-file-contract text-green-600 mr-2"></i>Contract Information</h4>';
        content += '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">';
        content += createDetailCard('Created Date', formatDateTime(contract.createdDate), 'fa-calendar-plus');
        content += createDetailCard('Signed Date', contract.signedDate ? formatDateTime(contract.signedDate) : '<span class="text-gray-400">Not signed</span>', 'fa-pen-fancy');
        content += createDetailCard('Completed Date', contract.completedDate ? formatDateTime(contract.completedDate) : '<span class="text-gray-400">Not completed</span>', 'fa-check-circle');
        content += createDetailCard('Terms Accepted', contract.termsAccepted ? '<span class="text-green-600"><i class="fas fa-check-circle mr-1"></i>Yes</span>' : '<span class="text-red-600"><i class="fas fa-times-circle mr-1"></i>No</span>', 'fa-file-signature');
        content += '</div></div>';
        
        // Notes & Cancellation
        if (contract.notes || contract.cancellationReason) {
            content += '<div class="mb-6">';
            if (contract.notes) {
                content += '<div class="bg-blue-50 border-l-4 border-blue-500 p-4 rounded mb-4">';
                content += '<h5 class="font-semibold text-blue-900 mb-2"><i class="fas fa-sticky-note mr-2"></i>Notes</h5>';
                content += '<p class="text-blue-800">' + escapeHtml(contract.notes) + '</p></div>';
            }
            if (contract.cancellationReason) {
                content += '<div class="bg-red-50 border-l-4 border-red-500 p-4 rounded">';
                content += '<h5 class="font-semibold text-red-900 mb-2"><i class="fas fa-exclamation-triangle mr-2"></i>Cancellation Reason</h5>';
                content += '<p class="text-red-800">' + escapeHtml(contract.cancellationReason) + '</p></div>';
            }
            content += '</div>';
        }
        
        content += '</div>';
        
        // Footer with actions
        content += '<div class="px-6 py-4 bg-gray-50 rounded-b-xl flex justify-between items-center">';
        content += '<div class="text-sm text-gray-500"><i class="fas fa-info-circle mr-1"></i>Contract ID: ' + escapeHtml(contract.contractId) + '</div>';
        content += '<div class="flex space-x-3">';
        
        // Action buttons based on status
        if (contract.status === 'Draft') {
            content += '<button onclick="signContract(\'' + contract.contractId + '\'); closeContractDetailModal();" class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors">';
            content += '<i class="fas fa-signature mr-2"></i>Sign Contract</button>';
        }
        if (contract.status === 'Signed') {
            content += '<button onclick="completeContract(\'' + contract.contractId + '\'); closeContractDetailModal();" class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">';
            content += '<i class="fas fa-check mr-2"></i>Complete</button>';
        }
        if (contract.status !== 'Completed' && contract.status !== 'Cancelled') {
            content += '<button onclick="editContract(\'' + contract.contractId + '\'); closeContractDetailModal();" class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors">';
            content += '<i class="fas fa-edit mr-2"></i>Edit</button>';
            content += '<button onclick="cancelContract(\'' + contract.contractId + '\'); closeContractDetailModal();" class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors">';
            content += '<i class="fas fa-times mr-2"></i>Cancel</button>';
        }
        
        content += '<button onclick="closeContractDetailModal()" class="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-colors">';
        content += '<i class="fas fa-times mr-2"></i>Close</button>';
        content += '</div></div>';
        
        content += '</div>';
        
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
    
    window.editContract = function(contractId) {
        // Load contract data first
        fetch('/admin/api/contracts/' + contractId)
            .then(function(response) {
                if (!response.ok) throw new Error('Failed to load contract');
                return response.json();
            })
            .then(function(contract) {
                showEditContractModal(contract);
            })
            .catch(function(error) {
                console.error('Error:', error);
                alert('Failed to load contract for editing');
            });
    };
    
    function showEditContractModal(contract) {
        var modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center p-4';
        modal.id = 'editContractModal';
        modal.onclick = function(e) {
            if (e.target === modal) closeEditContractModal();
        };
        
        var content = '<div class="relative bg-white rounded-xl shadow-2xl w-full max-w-2xl" onclick="event.stopPropagation()">';
        
        // Header
        content += '<div class="bg-gradient-to-r from-indigo-600 to-purple-600 p-6 rounded-t-xl">';
        content += '<div class="flex justify-between items-center">';
        content += '<div>';
        content += '<h3 class="text-2xl font-bold text-white">Edit Contract</h3>';
        content += '<p class="text-indigo-100 text-sm mt-1">' + escapeHtml(contract.contractCode) + '</p>';
        content += '</div>';
        content += '<button onclick="closeEditContractModal()" class="text-white hover:text-gray-200 transition-colors">';
        content += '<i class="fas fa-times text-2xl"></i></button>';
        content += '</div></div>';
        
        // Form
        content += '<form id="editContractForm" class="p-6">';
        
        // Contract Info (Read-only)
        content += '<div class="mb-6 bg-gray-50 p-4 rounded-lg">';
        content += '<h4 class="font-semibold text-gray-900 mb-3">Contract Information</h4>';
        content += '<div class="grid grid-cols-2 gap-4 text-sm">';
        content += '<div><span class="text-gray-600">Customer:</span> <span class="font-medium">' + escapeHtml(contract.userName || '-') + '</span></div>';
        content += '<div><span class="text-gray-600">Vehicle:</span> <span class="font-medium">' + escapeHtml(contract.vehicleModel || '-') + '</span></div>';
        content += '<div><span class="text-gray-600">Status:</span> <span class="px-2 py-1 rounded-full text-xs font-semibold ' + getStatusClass(contract.status) + '">' + contract.status + '</span></div>';
        content += '<div><span class="text-gray-600">Amount:</span> <span class="font-medium text-green-600">' + (contract.totalAmount ? contract.totalAmount.toLocaleString('vi-VN') + ' VND' : '-') + '</span></div>';
        content += '</div></div>';
        
        // Editable Fields
        content += '<div class="space-y-4">';
        
        // Status
        content += '<div>';
        content += '<label for="editStatus" class="block text-sm font-medium text-gray-700 mb-2">';
        content += '<i class="fas fa-info-circle mr-2 text-indigo-600"></i>Status</label>';
        content += '<select id="editStatus" name="status" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500">';
        content += '<option value="Draft" ' + (contract.status === 'Draft' ? 'selected' : '') + '>Draft</option>';
        content += '<option value="Signed" ' + (contract.status === 'Signed' ? 'selected' : '') + '>Signed</option>';
        content += '<option value="Completed" ' + (contract.status === 'Completed' ? 'selected' : '') + '>Completed</option>';
        content += '<option value="Cancelled" ' + (contract.status === 'Cancelled' ? 'selected' : '') + '>Cancelled</option>';
        content += '</select>';
        content += '</div>';
        
        // Notes
        content += '<div>';
        content += '<label for="editNotes" class="block text-sm font-medium text-gray-700 mb-2">';
        content += '<i class="fas fa-sticky-note mr-2 text-blue-600"></i>Notes</label>';
        content += '<textarea id="editNotes" name="notes" rows="4" class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500" placeholder="Add notes about this contract...">' + escapeHtml(contract.notes || '') + '</textarea>';
        content += '</div>';
        
        // Cancellation Reason (show only if status is Cancelled)
        content += '<div id="cancellationReasonDiv" style="' + (contract.status === 'Cancelled' ? '' : 'display: none;') + '">';
        content += '<label for="editCancellationReason" class="block text-sm font-medium text-gray-700 mb-2">';
        content += '<i class="fas fa-exclamation-triangle mr-2 text-red-600"></i>Cancellation Reason</label>';
        content += '<textarea id="editCancellationReason" name="cancellationReason" rows="3" class="w-full px-3 py-2 border border-red-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-red-500" placeholder="Enter reason for cancellation...">' + escapeHtml(contract.cancellationReason || '') + '</textarea>';
        content += '</div>';
        
        // Terms Accepted
        content += '<div class="flex items-center">';
        content += '<input type="checkbox" id="editTermsAccepted" name="termsAccepted" ' + (contract.termsAccepted ? 'checked' : '') + ' class="w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500">';
        content += '<label for="editTermsAccepted" class="ml-2 text-sm text-gray-700">';
        content += '<i class="fas fa-file-signature mr-1 text-green-600"></i>Terms Accepted</label>';
        content += '</div>';
        
        content += '</div>';
        content += '</form>';
        
        // Add JavaScript to show/hide cancellation reason based on status
        content += '<script>';
        content += 'document.getElementById("editStatus").addEventListener("change", function() {';
        content += '  var cancellationDiv = document.getElementById("cancellationReasonDiv");';
        content += '  if (this.value === "Cancelled") {';
        content += '    cancellationDiv.style.display = "block";';
        content += '  } else {';
        content += '    cancellationDiv.style.display = "none";';
        content += '  }';
        content += '});';
        content += '</script>';
        
        // Footer
        content += '<div class="px-6 py-4 bg-gray-50 rounded-b-xl flex justify-end space-x-3">';
        content += '<button onclick="closeEditContractModal()" class="px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-colors">';
        content += '<i class="fas fa-times mr-2"></i>Cancel</button>';
        content += '<button onclick="saveContractChanges(\'' + contract.contractId + '\')" class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors">';
        content += '<i class="fas fa-save mr-2"></i>Save Changes</button>';
        content += '</div>';
        
        content += '</div>';
        
        modal.innerHTML = content;
        document.body.appendChild(modal);
    }
    
    window.saveContractChanges = function(contractId) {
        var status = document.getElementById('editStatus').value;
        var notes = document.getElementById('editNotes').value;
        var termsAccepted = document.getElementById('editTermsAccepted').checked;
        var cancellationReason = document.getElementById('editCancellationReason').value;
        
        var data = {
            status: status,
            notes: notes,
            termsAccepted: termsAccepted,
            cancellationReason: cancellationReason
        };
        
        console.log('Saving contract:', contractId, data);
        
        // Get CSRF token
        var csrfToken = document.querySelector('meta[name="_csrf"]');
        var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
        
        var headers = {'Content-Type': 'application/json'};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader.content] = csrfToken.content;
        }
        
        fetch('/admin/contracts/' + contractId + '/update', {
            method: 'PUT',
            headers: headers,
            body: JSON.stringify(data)
        })
        .then(function(response) {
            console.log('Response status:', response.status);
            if (!response.ok) {
                return response.json().then(function(err) {
                    throw new Error(err.message || 'Failed to update contract');
                });
            }
            return response.json();
        })
        .then(function(result) {
            console.log('Result:', result);
            if (result.status === 'success') {
                alert('✅ Contract updated successfully!');
                closeEditContractModal();
                loadContracts(); // Reload the table
            } else {
                throw new Error(result.message || 'Update failed');
            }
        })
        .catch(function(error) {
            console.error('Error updating contract:', error);
            alert('❌ Failed to update contract: ' + error.message);
        });
    };
    
    window.closeEditContractModal = function() {
        var modal = document.getElementById('editContractModal');
        if (modal) modal.remove();
    };
    
    window.closeContractDetailModal = function() {
        var modal = document.getElementById('contractDetailModal');
        if (modal) modal.remove();
    };
    
    function createDetailItem(label, value) {
        return '<div class="p-3 bg-gray-50 rounded"><div class="text-xs font-semibold text-gray-500 uppercase mb-1">' +
               escapeHtml(label) + '</div><div class="text-sm text-gray-900 font-medium">' + value + '</div></div>';
    }
    
    // Initial load if contracts tab is active
    if (document.getElementById('contracts') && document.getElementById('contracts').classList.contains('active')) {
        loadContracts();
        loadStatistics();
    }
})();
