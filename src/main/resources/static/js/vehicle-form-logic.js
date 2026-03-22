// src/main/resources/static/js/vehicle-form-logic.js

// --- Logic Xử lý Ảnh Preview ---
function handleImagePreview(inputId, previewContainerId, isMultiple = false, maxFiles = 10) {
    const input = document.getElementById(inputId);
    const previewContainer = document.getElementById(previewContainerId);

    // Tìm các element một cách an toàn hơn
    const errorElementId = inputId.replace('Images', 'ImagesError').replace('mainImage', 'mainImageError');
    const errorElement = document.getElementById(errorElementId);
    const newMainImagePreviewDisplayId = inputId.replace('mainImage', 'newMainImagePreviewDisplay');
    const newMainImagePreviewDisplay = document.getElementById(newMainImagePreviewDisplayId);

    if (!input || !previewContainer) {
        // console.warn(`Image preview setup failed: Missing input '${inputId}' or preview container '${previewContainerId}'`);
        return;
    }

    input.addEventListener('change', function(event) {
        // Xóa các ảnh preview "mới" đã tạo
        const existingPreviews = previewContainer.querySelectorAll('img.preview-image');
        existingPreviews.forEach(p => p.remove());

        // Reset preview cho ảnh chính (edit)
        if (newMainImagePreviewDisplay) newMainImagePreviewDisplay.classList.add('hidden');
        if(errorElement) errorElement.classList.add('hidden');

        const files = event.target.files;
        let fileArray = Array.from(files);

        if (isMultiple && fileArray.length > maxFiles) {
            if(errorElement) {
                errorElement.textContent = `Chỉ được chọn tối đa ${maxFiles} ảnh phụ.`;
                errorElement.classList.remove('hidden');
            }
            alert(`Bạn đã chọn ${files.length} ảnh. Chỉ được phép chọn tối đa ${maxFiles}. Vui lòng chọn lại.`);
            input.value = '';
            return;
        }

        if (fileArray.length > 0) {
            fileArray.forEach(file => {
                if (file.type.startsWith('image/')) {
                    const reader = new FileReader();
                    reader.onload = function(e) {
                        const img = document.createElement('img');
                        img.src = e.target.result;
                        img.alt = "Xem trước";
                        img.classList.add('preview-image'); // Class để dễ dàng xóa sau này

                        if (inputId === 'edit-mainImage' && newMainImagePreviewDisplay) {
                            // Ảnh chính (edit modal) - hiển thị ở preview riêng
                            newMainImagePreviewDisplay.src = e.target.result;
                            newMainImagePreviewDisplay.classList.remove('hidden');
                        } else if (!isMultiple) {
                            // Ảnh chính (add modal / add page)
                            // Tìm img GỐC (có thể là ảnh cũ hoặc placeholder)
                            const mainPreviewImg = previewContainer.querySelector('img:not(.preview-image)');
                            if(mainPreviewImg){
                                mainPreviewImg.src = e.target.result;
                                mainPreviewImg.classList.remove('hidden');
                            }
                        }
                        else if (isMultiple) {
                            // Ảnh phụ
                            img.classList.add('max-h-24', 'h-24', 'w-auto', 'object-contain', 'rounded', 'border', 'p-1', 'inline-block');
                            previewContainer.appendChild(img);
                        }
                    }
                    reader.readAsDataURL(file);
                }
            });
        } else if (!isMultiple && inputId === 'add-mainImage') {
            // Ẩn preview ảnh chính (add) nếu không chọn file
            const imgPreview = previewContainer.querySelector('img:not(.preview-image)');
            if(imgPreview) imgPreview.classList.add('hidden');
        } else if (!isMultiple && inputId === 'edit-mainImage') {
            // Ẩn preview ảnh chính (edit) nếu không chọn file
            if (newMainImagePreviewDisplay) newMainImagePreviewDisplay.classList.add('hidden');
        }
    });
}


// --- Logic Xử lý Tính năng (Features) ---
const vehicleFeatures = {
    ElectricCar: [
        "GPS Navigation", "Bluetooth Audio", "Air Conditioning", "Power Windows",
        "Central Locking", "USB Charging Ports", "Backup Camera", "Parking Sensors",
        "Cruise Control", "Keyless Entry", "Sunroof", "Leather Seats",
        "Heated Seats", "Automatic Transmission", "ABS Brakes", "Airbags",
        "LED Headlights", "Fog Lights", "Rain Sensing Wipers", "Auto Climate Control",
    ],
    ElectricMotorcycle: [
        "GPS Navigation", "Bluetooth Audio", "USB Charging Port", "LED Headlights",
        "Digital Display", "Anti-lock Braking System", "Keyless Start",
        "Storage Compartment", "Phone Mount", "Helmet Lock", "Side Stand",
        "Center Stand", "Windshield", "Rear View Mirrors", "Turn Signals",
        "Horn", "Speedometer", "Battery Indicator", "Range Indicator", "Eco Mode",
    ],
};

function renderFeatures(container, hiddenInput, vehicleType) {
    const features = vehicleFeatures[vehicleType] || [];
    if (!container) return; // Thoát nếu không tìm thấy container

    container.innerHTML = ""; // Clear
    let existingFeatures = [];
    if (hiddenInput && hiddenInput.value) {
        try {
            existingFeatures = JSON.parse(hiddenInput.value);
            if (!Array.isArray(existingFeatures)) existingFeatures = [];
        } catch (e) {
            existingFeatures = [];
        }
    }

    features.forEach(feature => {
        const featureId = `feature-${feature.replace(/\s+/g, "-").toLowerCase()}`;
        const isChecked = existingFeatures.includes(feature);

        const checkboxDiv = document.createElement("div");
        checkboxDiv.className = "flex items-center space-x-2";
        checkboxDiv.innerHTML = `
            <input type="checkbox"
                   id="${container.id}-${featureId}"
                   value="${feature}"
                   class="feature-checkbox rounded border-gray-300 text-primary focus:ring-primary"
                   onchange="updateSelectedFeatures('${container.id}', '${hiddenInput.id}')"
                   ${isChecked ? 'checked' : ''}>
            <label for="${container.id}-${featureId}"
                   class="text-sm text-gray-700 cursor-pointer">${feature}</label>
        `;
        container.appendChild(checkboxDiv);
    });
    updateSelectedFeatures(container.id, hiddenInput.id);
}

// Hàm này được gọi bởi cả hai trang (Add Page và Add Modal)
function updateFeatures() {
    let addModal = document.getElementById('add-car-modal');
    let vehicleTypeSelect, container, hiddenInput;

    if (addModal && !addModal.classList.contains('hidden')) {
        // Đang ở trong modal "Add New Car"
        vehicleTypeSelect = addModal.querySelector("#vehicle-type-select");
        container = addModal.querySelector("#features-container");
        hiddenInput = addModal.querySelector("#selected-features");
    } else {
        // Đang ở trang "vehicle-add.html" (trang riêng)
        vehicleTypeSelect = document.querySelector("#vehicle-type-select");
        container = document.querySelector("#features-container");
        hiddenInput = document.querySelector("#selected-features");
    }

    if(!vehicleTypeSelect || !container || !hiddenInput) {
        // console.warn("Could not find feature elements (might be on edit modal).");
        return;
    }

    const vehicleType = vehicleTypeSelect.value;

    // Chỉ reset hiddenInput nếu nó là form 'add' (không có giá trị ban đầu)
    if (!hiddenInput.value) {
        hiddenInput.value = "[]";
    }

    renderFeatures(container, hiddenInput, vehicleType);
}

// Hàm này được gọi bởi Edit Modal
function updateEditFeatures() {
    const vehicleTypeSelect = document.getElementById("edit-type");
    const container = document.getElementById("edit-features-container");
    const hiddenInput = document.getElementById("edit-selected-features");
    if(!vehicleTypeSelect || !container || !hiddenInput) return;

    const vehicleType = vehicleTypeSelect.value;
    renderFeatures(container, hiddenInput, vehicleType);
}

// Hàm cập nhật input ẩn
function updateSelectedFeatures(containerId, hiddenInputId) {
    const container = document.getElementById(containerId);
    const hiddenInput = document.getElementById(hiddenInputId);
    if(!container || !hiddenInput) return;

    const checkboxes = container.querySelectorAll(".feature-checkbox:checked");
    const selectedFeatures = Array.from(checkboxes).map(cb => cb.value);
    hiddenInput.value = JSON.stringify(selectedFeatures);
}

// --- Khởi tạo khi DOM load ---
// Các hàm này sẽ được gọi TỪ BÊN TRONG `cars-management.html`
// hoặc `vehicle-add.html` BÊN TRONG `DOMContentLoaded`
// Chúng ta không tự động gọi `handleImagePreview` ở đây nữa
// để tránh lỗi "document.getElementById(...) is null".