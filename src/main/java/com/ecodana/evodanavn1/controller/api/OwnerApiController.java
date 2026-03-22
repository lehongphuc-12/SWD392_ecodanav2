package com.ecodana.evodanavn1.controller.api;

import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.Vehicle;
import com.ecodana.evodanavn1.service.BookingService;
import com.ecodana.evodanavn1.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/owner/api")
public class OwnerApiController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BookingService bookingService;

    private static final Logger logger = LoggerFactory.getLogger(OwnerApiController.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/cars/{id}")
    public ResponseEntity<?> getCarForEdit(@PathVariable String id) {
        try {
            return vehicleService.getVehicleById(id).map(vehicle -> {
                Map<String, Object> car = new HashMap<>();
                car.put("vehicleId", vehicle.getVehicleId());
                car.put("vehicleModel", vehicle.getVehicleModel());

                if (vehicle.getVehicleType() != null) {
                    car.put("type", vehicle.getVehicleType().name());
                }

                if (vehicle.getTransmissionType() != null) {
                    car.put("transmission", Map.of("transmissionTypeId", vehicle.getTransmissionType().getTransmissionTypeId()));
                } else {
                    car.put("transmission", null);
                }

                if (vehicle.getCategory() != null) {
                    car.put("category", Map.of("categoryId", vehicle.getCategory().getCategoryId()));
                } else {
                    car.put("category", null);
                }

                car.put("licensePlate", vehicle.getLicensePlate());
                car.put("seats", vehicle.getSeats());
                car.put("odometer", vehicle.getOdometer());

                try {
                    car.put("hourlyRate", vehicle.getHourlyPriceFromJson());
                    car.put("dailyRate", vehicle.getDailyPriceFromJson());
                    car.put("monthlyRate", vehicle.getMonthlyPriceFromJson());
                } catch (Exception e) {
                    car.put("hourlyRate", 0);
                    car.put("dailyRate", 0);
                    car.put("monthlyRate", 0);
                }

                car.put("batteryCapacity", vehicle.getBatteryCapacity());
                car.put("description", vehicle.getDescription());
                car.put("requiresLicense", vehicle.getRequiresLicense());

                if(vehicle.getStatus() != null) {
                    car.put("status", vehicle.getStatus().name());
                }

                car.put("yearManufactured", vehicle.getYearManufactured());
                car.put("mainImageUrl", vehicle.getMainImageUrl());
                car.put("imageUrls", vehicle.getImageUrlsFromJson());
                car.put("features", vehicle.getFeaturesFromJson());

                return ResponseEntity.ok(car);
            }).orElse(ResponseEntity.status(404).body(Map.of("message", "Vehicle not found")));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Failed to get vehicle: " + e.getMessage()));
        }
    }

    @PostMapping("/bookings/complete-trip")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completeTrip(
            @RequestParam("bookingId") String bookingId,
            @RequestParam(value = "returnNotes", required = false) String returnNotes,
            @RequestParam("newStatus") String newStatus,
            @RequestParam(value = "returnImages", required = false) List<MultipartFile> returnImages) {

        try {
            Booking booking = bookingService.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy booking với ID: " + bookingId));

            if (returnNotes != null && !returnNotes.isEmpty()) {
                booking.setReturnNotes(returnNotes);
            }

            if (returnImages != null && !returnImages.isEmpty()) {
                List<String> imageUrls = new ArrayList<>();
                // Your image upload logic here
                // for (MultipartFile image : returnImages) {
                //     String url = cloudinaryService.uploadFile(image);
                //     imageUrls.add(url);
                // }
                booking.setReturnImageUrlsList(imageUrls); // Changed to setReturnImageUrlsList
                logger.info("Đã lưu {} ảnh trả xe cho booking {}", imageUrls.size(), bookingId);
            }

            // Determine the final booking status
            if ("Maintenance".equalsIgnoreCase(newStatus)) {
                booking.setStatus(Booking.BookingStatus.Completed); // Booking is still complete
            } else {
                booking.setStatus(Booking.BookingStatus.Completed);
            }

            // Update vehicle status based on the input
            Vehicle vehicle = booking.getVehicle();
            if (vehicle != null) {
                if ("Maintenance".equalsIgnoreCase(newStatus)) {
                    vehicle.setStatus(Vehicle.VehicleStatus.Maintenance);
                    logger.info("Đã cập nhật trạng thái xe {} thành Bảo trì.", vehicle.getVehicleId());
                } else {
                    vehicle.setStatus(Vehicle.VehicleStatus.Available); // Set back to available
                    logger.info("Đã cập nhật trạng thái xe {} thành Có sẵn.", vehicle.getVehicleId());
                }
                vehicleService.updateVehicle(vehicle);
            } else {
                logger.warn("Không tìm thấy xe cho booking {} để cập nhật trạng thái.", bookingId);
            }

            bookingService.updateBooking(booking);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã hoàn tất chuyến đi thành công");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Lỗi khi hoàn tất chuyến đi (ID: " + bookingId + "): ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}