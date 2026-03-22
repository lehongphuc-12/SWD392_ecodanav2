package com.ecodana.evodanavn1.controller.customer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.ecodana.evodanavn1.model.Discount;
import com.ecodana.evodanavn1.service.DiscountService;
import com.ecodana.evodanavn1.dto.BookingRequest;
import com.ecodana.evodanavn1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.Discount;
import com.ecodana.evodanavn1.model.Payment;
import com.ecodana.evodanavn1.model.RefundRequest;
import com.ecodana.evodanavn1.model.Vehicle; // Đảm bảo import Vehicle
import com.ecodana.evodanavn1.repository.PaymentRepository;
import com.ecodana.evodanavn1.service.BookingService;
import com.ecodana.evodanavn1.service.DiscountService;
import com.ecodana.evodanavn1.service.NotificationService;
import com.ecodana.evodanavn1.service.RefundRequestService;
import com.ecodana.evodanavn1.service.UserFeedbackService;
import com.ecodana.evodanavn1.service.UserService;
import com.ecodana.evodanavn1.service.VehicleService;
import com.ecodana.evodanavn1.service.PayOSService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private DiscountService discountService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VehicleService vehicleService;



    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PayOSService payOSService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRequestService refundRequestService;

    @Autowired
    private UserFeedbackService userFeedbackService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.ecodana.evodanavn1.service.EmailService emailService;

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.2");

    @GetMapping("/{id}/remaining-amount")
    @ResponseBody
    public ResponseEntity<?> getRemainingAmount(@PathVariable String id) {
        Booking booking = bookingService.findById(id)
                .orElse(null);
        if (booking == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Booking not found"));
        }
        return ResponseEntity.ok(Map.of("remainingAmount", booking.getRemainingAmount()));
    }

    /**
     * Show checkout page
     */
    @PostMapping("/checkout")
    public String showCheckout(@ModelAttribute BookingRequest bookingRequest, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đặt xe!");
            return "redirect:/login";
        }

        try {
            // Get vehicle
            Vehicle vehicle = vehicleService.getVehicleById(bookingRequest.getVehicleId()).orElse(null);
            if (vehicle == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy xe!");
                return "redirect:/vehicles";
            }

            // Parse dates and times for display
            LocalDate pickupDate = LocalDate.parse(bookingRequest.getPickupDate());
            LocalDate returnDate = LocalDate.parse(bookingRequest.getReturnDate());
            LocalTime pickupTime = LocalTime.parse(bookingRequest.getPickupTime());
            LocalTime returnTime = LocalTime.parse(bookingRequest.getReturnTime());

            // Calculate rental price based on exact hours
            BigDecimal dailyPrice = vehicle.getDailyPriceFromJson();
            BigDecimal hourlyPrice = vehicle.getHourlyPriceFromJson();

            // Calculate total hours
            LocalDateTime pickupDateTimeObj = LocalDateTime.of(pickupDate, pickupTime);
            LocalDateTime returnDateTimeObj = LocalDateTime.of(returnDate, returnTime);

            // Format for display
            String pickupDateTime = pickupDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " " + pickupTime;
            String returnDateTime = returnDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " " + returnTime;
            long totalHours = java.time.Duration.between(pickupDateTimeObj, returnDateTimeObj).toHours();
            double totalHoursDouble = java.time.Duration.between(pickupDateTimeObj, returnDateTimeObj).toMinutes() / 60.0;

            // Calculate price: (full days × daily price) + (remaining hours × hourly price)
            long fullDays = totalHours / 24;
            double remainingHours = totalHoursDouble - (fullDays * 24);

            BigDecimal rentalPrice = dailyPrice.multiply(new BigDecimal(fullDays))
                    .add(hourlyPrice.multiply(new BigDecimal(remainingHours)));

            // Get discount info and calculate discount amount
            String discountCode = null;
            BigDecimal discountAmount = BigDecimal.ZERO;

            // Debug: Log discount info from request
            System.out.println("=== Discount Debug ===");
            System.out.println("Discount ID from request: " + bookingRequest.getDiscountId());
            System.out.println("Discount Amount from request: " + bookingRequest.getDiscountAmount());

            // Sửa: Lấy discount code từ bookingRequest.getDiscountId() (vì nó đang lưu code)
            if (bookingRequest.getDiscountId() != null && !bookingRequest.getDiscountId().isEmpty()) {
                System.out.println("Searching for discount with code: " + bookingRequest.getDiscountId());
                Discount discount = discountService.findByVoucherCode(bookingRequest.getDiscountId()).orElse(null); // Tìm bằng Code
                if (discount != null) {
                    System.out.println("Discount found: " + discount.getDiscountName());
                    if (discountService.isDiscountValid(discount)) {
                        discountCode = discount.getVoucherCode();
                        BigDecimal subtotal = rentalPrice;
                        discountAmount = discountService.calculateDiscountAmount(discount, subtotal);
                        System.out.println("Discount is valid. Amount: " + discountAmount);
                    } else {
                        System.out.println("Discount is NOT valid (expired, inactive, or usage limit reached)");
                    }
                } else {
                    System.out.println("Discount NOT found in database!");
                }
            } else {
                System.out.println("No discount ID provided in request");
            }

            // Calculate total amount - always recalculate to ensure accuracy
            BigDecimal totalAmount = rentalPrice.subtract(discountAmount);

            // Ensure total is not negative
            if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                totalAmount = BigDecimal.ZERO;
            }

            // Debug logging
            System.out.println("=== Checkout Calculation ===");
            System.out.println("Rental Days: " + bookingRequest.getRentalDays());
            System.out.println("Daily Price: " + dailyPrice);
            System.out.println("Rental Price: " + rentalPrice);
            System.out.println("Discount Code: " + discountCode);
            System.out.println("Discount Amount: " + discountAmount);
            System.out.println("Total Amount: " + totalAmount);

            // Add attributes to model
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleId", vehicle.getVehicleId());
            model.addAttribute("pickupDateTime", pickupDateTime);
            model.addAttribute("returnDateTime", returnDateTime);
            model.addAttribute("pickupDate", bookingRequest.getPickupDate());
            model.addAttribute("pickupTime", bookingRequest.getPickupTime());
            model.addAttribute("returnDate", bookingRequest.getReturnDate());
            model.addAttribute("returnTime", bookingRequest.getReturnTime());
            model.addAttribute("pickupLocation", bookingRequest.getPickupLocation());
            model.addAttribute("rentalDays", bookingRequest.getRentalDays());
            model.addAttribute("rentalPrice", rentalPrice);
            model.addAttribute("dailyPrice", dailyPrice);
            model.addAttribute("hourlyPrice", hourlyPrice);
            model.addAttribute("fullDays", fullDays);
            model.addAttribute("remainingHours", Math.ceil(remainingHours));
            model.addAttribute("discountId", discountCode); // Vẫn gửi ID (code)
            model.addAttribute("discountCode", discountCode); // Tên mã
            model.addAttribute("discountAmount", discountAmount);
            model.addAttribute("totalAmount", totalAmount); // Sử dụng total đã tính
            model.addAttribute("currentUser", user);

            // Lấy danh sách các mã giảm giá có sẵn và đang hoạt động
            List<Discount> availableDiscounts = discountService.getAvailableDiscountsForCustomer();
            model.addAttribute("availableDiscounts", availableDiscounts);

            return "customer/booking-checkout";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/vehicles/" + bookingRequest.getVehicleId();
        }
    }

    /**
     * Create new booking
     */
    @PostMapping("/create")
    @Transactional
    public String createBooking(@ModelAttribute BookingRequest bookingRequest, HttpSession session, RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đặt xe!");
            return "redirect:/login";
        }

        try {
            // Prevent duplicate booking creation - check if a pending booking already exists for this vehicle and time
            LocalDate pickup = LocalDate.parse(bookingRequest.getPickupDate());
            LocalDate returnD = LocalDate.parse(bookingRequest.getReturnDate());
            LocalTime pickupT = LocalTime.parse(bookingRequest.getPickupTime());
            LocalTime returnT = LocalTime.parse(bookingRequest.getReturnTime());

            LocalDateTime pickupDateTime = LocalDateTime.of(pickup, pickupT);
            LocalDateTime returnDateTime = LocalDateTime.of(returnD, returnT);

            // Check for existing pending/confirmed booking with same vehicle and time
            List<Booking> existingBookings = bookingService.getBookingsByUserId(user.getId());
            for (Booking existing : existingBookings) {
                if (existing.getVehicle().getVehicleId().equals(bookingRequest.getVehicleId()) &&
                        existing.getPickupDateTime().equals(pickupDateTime) &&
                        existing.getReturnDateTime().equals(returnDateTime) &&
                        (existing.getStatus() == Booking.BookingStatus.Pending ||
                                existing.getStatus() == Booking.BookingStatus.Confirmed ||
                                existing.getStatus() == Booking.BookingStatus.AwaitingDeposit)) {
                    System.out.println("=== DUPLICATE BOOKING DETECTED ===");
                    System.out.println("Existing booking: " + existing.getBookingCode());
                    redirectAttributes.addFlashAttribute("warning", "Bạn đã có đơn đặt xe này rồi! Mã đơn: " + existing.getBookingCode());
                    return "redirect:/booking/confirmation/" + existing.getBookingId();
                }
            }


            // Validate dates
            if (pickupDateTime.isBefore(LocalDateTime.now().minusMinutes(5))) { // Cho phép trễ 5 phút
                redirectAttributes.addFlashAttribute("error", "Ngày nhận xe không thể là quá khứ!");
                return "redirect:/vehicles/" + bookingRequest.getVehicleId();
            }

            if (returnDateTime.isBefore(pickupDateTime)) {
                redirectAttributes.addFlashAttribute("error", "Ngày trả xe phải sau ngày nhận xe!");
                return "redirect:/vehicles/" + bookingRequest.getVehicleId();
            }

            // Get vehicle
            Vehicle vehicle = vehicleService.getVehicleById(bookingRequest.getVehicleId()).orElse(null);
            if (vehicle == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy xe!");
                return "redirect:/vehicles";
            }

            // Check if vehicle is available
            if (!"Available".equals(vehicle.getStatus().toString())) {
                redirectAttributes.addFlashAttribute("error", "Xe không khả dụng để đặt!");
                return "redirect:/vehicles/" + bookingRequest.getVehicleId();
            }

            vehicle.setStatus(Vehicle.VehicleStatus.Rented);
            vehicleService.updateVehicle(vehicle);


            // Handle discount if provided
            Discount discount = null;
            if (bookingRequest.getDiscountId() != null && !bookingRequest.getDiscountId().isEmpty()) {
                // bookingRequest.getDiscountId() đang lưu voucherCode
                discount = discountService.findByVoucherCode(bookingRequest.getDiscountId())
                        .orElse(null);

                // Validate discount again on server side
                if (discount != null && discountService.isDiscountValid(discount)) {
                    // Increment usage count
                    discount.setUsedCount(discount.getUsedCount() + 1);
                    discountService.updateDiscount(discount);
                } else {
                    discount = null; // Vô hiệu hóa discount nếu không hợp lệ
                }
            }

            // Create booking
            Booking booking = new Booking();
            booking.setBookingId(UUID.randomUUID().toString());
            booking.setUser(user);
            booking.setVehicle(vehicle);
            booking.setPickupDateTime(pickupDateTime);
            booking.setReturnDateTime(returnDateTime);

            // Lấy tiền thuê xe gốc (trước discount) từ request
            BigDecimal vehicleRentalFee = bookingRequest.getVehicleRentalFee();
            if (vehicleRentalFee == null || vehicleRentalFee.compareTo(BigDecimal.ZERO) <= 0) {
                // Tính toán lại nếu frontend không gửi
                BigDecimal dailyPrice = vehicle.getDailyPriceFromJson();
                BigDecimal hourlyPrice = vehicle.getHourlyPriceFromJson();
                long totalHours = java.time.Duration.between(pickupDateTime, returnDateTime).toHours();
                double totalHoursDouble = java.time.Duration.between(pickupDateTime, returnDateTime).toMinutes() / 60.0;
                long fullDays = totalHours / 24;
                double remainingHours = totalHoursDouble - (fullDays * 24);
                vehicleRentalFee = dailyPrice.multiply(new BigDecimal(fullDays))
                        .add(hourlyPrice.multiply(new BigDecimal(remainingHours)));
            }
            // Tính toán các loại phí
            // Phí nền tảng (12% của tiền thuê gốc), làm tròn 2 chữ số
            BigDecimal platformFee = vehicleRentalFee.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
            // Tiền chủ xe nhận = Tiền thuê gốc - Phí nền tảng
            BigDecimal ownerPayout = vehicleRentalFee.subtract(platformFee);

            // Tổng tiền khách trả (đã bao gồm discount)
            BigDecimal totalAmount = bookingRequest.getTotalAmount();

            booking.setPaymentOption(bookingRequest.getPaymentMethod());

            // Set các giá trị phí vào booking
            booking.setVehicleRentalFee(vehicleRentalFee);
            booking.setPlatformFee(platformFee);
            booking.setOwnerPayout(ownerPayout);
            booking.setTotalAmount(totalAmount);
            booking.setStatus(Booking.BookingStatus.Pending); // Trạng thái chờ Owner duyệt
            booking.setBookingCode("BK" + System.currentTimeMillis());
            booking.setRentalType(Booking.RentalType.daily);
            booking.setCreatedDate(LocalDateTime.now());
            booking.setPickupLocation(bookingRequest.getPickupLocation());
            booking.setTermsAgreed(true);
            booking.setTermsAgreedAt(LocalDateTime.now());
            booking.setExpectedPaymentMethod(bookingRequest.getPaymentMethod() != null ? bookingRequest.getPaymentMethod() : "Cash");
            booking.setDiscount(discount);

            bookingService.addBooking(booking);

            // Create notification for all admins AND Owner
            try {
                String customerName = (user.getFirstName() != null) ? (user.getFirstName() + " " + user.getLastName()) : user.getUsername();
                String notificationMessage = String.format(
                        "Đơn đặt xe mới #%s - Khách hàng: %s - Xe: %s - Tổng: %,d ₫",
                        booking.getBookingCode(),
                        customerName,
                        vehicle.getVehicleModel(),
                        bookingRequest.getTotalAmount().longValue()
                );

                // Gửi cho Owner của xe
                if(vehicle.getOwnerId() != null) {
                    notificationService.createNotification(vehicle.getOwnerId(), notificationMessage, booking.getBookingId(), "BOOKING_REQUEST");

                    // Gửi email cho Owner
                    try {
                        User owner = userService.findById(vehicle.getOwnerId());
                        if (owner != null && owner.getEmail() != null) {
                            String ownerName = (owner.getFirstName() != null) ? (owner.getFirstName() + " " + owner.getLastName()) : owner.getUsername();
                            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                            String pickupDateStr = booking.getPickupDateTime().format(formatter);
                            String returnDateStr = booking.getReturnDateTime().format(formatter);

                            emailService.sendBookingNotificationToOwner(
                                    owner.getEmail(),
                                    ownerName,
                                    booking.getBookingCode(),
                                    vehicle.getVehicleModel(),
                                    customerName,
                                    pickupDateStr,
                                    returnDateStr
                            );
                        }
                    } catch (Exception emailError) {
                        System.out.println("Warning: Failed to send email to owner: " + emailError.getMessage());
                    }
                } else {
                    // Fallback: Gửi cho admin nếu không tìm thấy owner
                    notificationService.createNotificationForAllAdmins(notificationMessage, booking.getBookingId(), "BOOKING_REQUEST");
                }

            } catch (Exception notifError) {
                System.out.println("Warning: Failed to create notification: " + notifError.getMessage());
            }

            redirectAttributes.addFlashAttribute("success", "Đặt xe thành công! Vui lòng chờ chủ xe duyệt.");
            System.out.println("=== Booking created successfully ===");
            System.out.println("Booking ID: " + booking.getBookingId());
            System.out.println("Booking Code: " + booking.getBookingCode());
            System.out.println("Redirecting to confirmation page");

            // Redirect đến trang confirmation - chờ owner duyệt
            return "redirect:/booking/confirmation/" + booking.getBookingId();

        } catch (Exception e) {
            System.out.println("=== ERROR creating booking ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/vehicles/" + bookingRequest.getVehicleId();
        }
    }

    /**
     * Show payment page
     */
    @GetMapping("/payment/{bookingId}")
    public String showPaymentPage(@PathVariable String bookingId, HttpSession session, Model model, HttpServletRequest request) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        Booking booking = bookingService.findById(bookingId).orElse(null);
        if (booking == null) {
            return "redirect:/booking/my-bookings";
        }

        // Verify booking ownership
        String bookingUserId = booking.getUser() != null ? booking.getUser().getId() : null;
        if (bookingUserId == null || !bookingUserId.equals(user.getId())) {
            return "redirect:/booking/my-bookings";
        }

        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            return "redirect:/booking/my-bookings";
        }

        // Tính toán số tiền cọc 20% và số tiền còn lại 80%
        BigDecimal totalAmount = booking.getTotalAmount();
        BigDecimal depositAmount = booking.getDepositAmountRequired();

        // Tính toán lại nếu chưa có
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) == 0) {
            depositAmount = totalAmount.multiply(new BigDecimal("0.2")); // 20%

            booking.setDepositAmountRequired(depositAmount);
            bookingService.updateBooking(booking);
        }
        BigDecimal remainingAmount = totalAmount.subtract(depositAmount);


        model.addAttribute("booking", booking);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("depositAmount", depositAmount);
        model.addAttribute("remainingAmount", remainingAmount);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("currentUser", user);

        return "customer/booking-payment";
    }

    /**
     * Process payment - Create PayOS payment link
     */
    @PostMapping("/payment/process/{bookingId}")
    public String processPayment(
            @PathVariable String bookingId,
            @RequestParam("paymentType") String paymentType,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        System.out.println("=== processPayment called ===");
        System.out.println("bookingId: " + bookingId);
        System.out.println("paymentType: " + paymentType);
        System.out.println("payOSService: " + payOSService);

        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            System.out.println("User not logged in");
            return "redirect:/login";
        }

        Booking booking = bookingService.findById(bookingId).orElse(null);
        if (booking == null) {
            System.out.println("Booking not found");
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn đặt xe!");
            return "redirect:/booking/my-bookings";
        }

        try {
            System.out.println("Creating payment link...");
            long amount;
            String orderInfo;

            if ("deposit".equals(paymentType)) {
                // Thanh toán 20% cọc - không nhân 1000 vì đã là VND
                amount = booking.getDepositAmountRequired().longValue(); // Already in VND
                orderInfo = "Coc 20% " + booking.getBookingCode(); // Max 25 chars
                System.out.println("=== DEPOSIT PAYMENT ===");
                System.out.println("Deposit Required (VND): " + booking.getDepositAmountRequired());
                System.out.println("Amount to PayOS (VND): " + amount);
            } else {
                // Thanh toán 100% - không nhân 1000 vì đã là VND
                amount = booking.getTotalAmount().longValue(); // Already in VND
                orderInfo = "Toan bo " + booking.getBookingCode(); // Max 25 chars
                System.out.println("=== FULL PAYMENT ===");
                System.out.println("Total Amount (VND): " + booking.getTotalAmount());
                System.out.println("Amount to PayOS (VND): " + amount);
            }

            // Tạo link thanh toán PayOS
            String paymentUrl = payOSService.createPaymentLink(amount, orderInfo, bookingId, paymentType, request);

            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tạo thanh toán: " + e.getMessage());
            return "redirect:/booking/payment/" + bookingId;
        }
    }

    /**
     * PayOS payment return handler
     */
    @GetMapping("/payment/payos-return")
    @Transactional
    public String payosReturn(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String cancel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) String bookingId,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            System.out.println("=== PayOS Return Handler ===");
            System.out.println("BookingId: " + bookingId);
            System.out.println("Status: " + status);
            System.out.println("Code: " + code);
            System.out.println("OrderCode: " + orderCode);

            // Check if payment was successful
            if ("PAID".equals(status) || "00".equals(code)) {
                model.addAttribute("success", true);
                model.addAttribute("message", "Thanh toán thành công! Đơn đặt xe của bạn đã được xác nhận.");

                BigDecimal paidAmount = BigDecimal.ZERO;
                if (orderCode != null) {
                    Optional<Payment> optionalPayment = paymentRepository.findByOrderCode(orderCode);
                    if (optionalPayment.isPresent()) {
                        Payment payment = optionalPayment.get();
                        if (payment.getPaymentStatus() != Payment.PaymentStatus.Completed) {
                            payment.setPaymentStatus(Payment.PaymentStatus.Completed);
                            paymentRepository.save(payment);
                            System.out.println("Payment " + payment.getPaymentId() + " status updated to Completed.");
                        }
                        paidAmount = payment.getAmount(); // Get the actual paid amount
                    }
                }

                if (bookingId != null) {
                    Booking booking = bookingService.findById(bookingId).orElse(null);
                    if (booking != null) {
                        // FALLBACK: Cập nhật trạng thái booking nếu webhook chưa xử lý
                        if (booking.getStatus() != Booking.BookingStatus.Confirmed) {
                            System.out.println("=== FALLBACK: Updating booking status ===");
                            System.out.println("Current booking status: " + booking.getStatus());

                            booking.setStatus(Booking.BookingStatus.Confirmed);
                            booking.setPaymentConfirmedAt(LocalDateTime.now());

                            // Update depositAmountRequired based on actual paid amount
                            if (paidAmount.compareTo(booking.getTotalAmount()) >= 0) { // Paid full amount or more
                                booking.setDepositAmountRequired(booking.getTotalAmount());
                            } else { // Paid deposit amount
                                booking.setDepositAmountRequired(paidAmount);
                            }
                            bookingService.updateBooking(booking);
                            System.out.println("Booking status updated to Confirmed");
                        }

                        model.addAttribute("bookingId", bookingId);
                        model.addAttribute("bookingCode", booking.getBookingCode());
                        model.addAttribute("paidAmount", paidAmount); // Use the actual paid amount
                        model.addAttribute("totalAmount", booking.getTotalAmount()); // Add total amount for context
                        // Calculate remaining amount for display
                        BigDecimal remainingAmountForDisplay = booking.getTotalAmount().subtract(paidAmount);
                        model.addAttribute("remainingAmount", remainingAmountForDisplay);
                        model.addAttribute("orderCode", orderCode);
                        model.addAttribute("paymentDate", LocalDateTime.now());
                    }
                }

                return "customer/payos-return";

            } else if ("CANCELLED".equals(status) || cancel != null) {
                model.addAttribute("warning", true);
                model.addAttribute("message", "Bạn đã hủy thanh toán. Vui lòng thử lại khi sẵn sàng.");
                model.addAttribute("bookingId", bookingId);
                return "customer/payos-return";

            } else {
                model.addAttribute("error", true);
                model.addAttribute("message", "Thanh toán không thành công. Vui lòng thử lại.");
                model.addAttribute("bookingId", bookingId);
                return "customer/payos-return";
            }
        } catch (Exception e) {
            model.addAttribute("error", true);
            model.addAttribute("message", "Có lỗi xảy ra: " + e.getMessage());
            return "customer/payos-return";
        }
    }

    /**
     * Payment cancellation handler
     */
    @GetMapping("/payment/cancel")
    public String paymentCancel(
            @RequestParam(required = false) String bookingId,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("currentUser");
        // Nếu không có user trong session, thử lấy từ bookingId
        if (user == null && bookingId != null) {
            try {
                Booking booking = bookingService.findById(bookingId).orElse(null);
                if (booking != null) {
                    user = booking.getUser();
                }
            } catch (Exception e) {
                System.out.println("Could not retrieve user from bookingId on payment cancel: " + e.getMessage());
            }
        }

        model.addAttribute("warning", true);
        model.addAttribute("message", "Bạn đã hủy thanh toán. Vui lòng thử lại khi sẵn sàng.");
        model.addAttribute("bookingId", bookingId);
        model.addAttribute("currentUser", user); // Thêm currentUser vào model

        return "customer/payos-return";
    }

    /**
     * Show booking confirmation page
     */
    @GetMapping("/confirmation/{bookingId}")
    public String bookingConfirmation(@PathVariable String bookingId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        Booking booking = bookingService.findById(bookingId).orElse(null);
        if (booking == null) {
            return "redirect:/booking/my-bookings";
        }

        // Get user ID from booking entity relationship
        String bookingUserId = booking.getUser() != null ? booking.getUser().getId() : null;
        if (bookingUserId == null || !bookingUserId.equals(user.getId())) {
            return "redirect:/booking/my-bookings";
        }

        // Get vehicle from booking entity relationship
        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            return "redirect:/booking/my-bookings";
        }

        // Get owner information
        if (vehicle.getOwnerId() != null) {
            User owner = userService.findById(vehicle.getOwnerId());
            model.addAttribute("vehicleOwner", owner);
        }

        // Get payment information to show paid amount and remaining amount
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);

        // DEBUG: Log all payments for this booking
        System.out.println("=== BOOKING CONFIRMATION DEBUG ===");
        System.out.println("BookingId: " + bookingId);
        System.out.println("Total Amount: " + booking.getTotalAmount());
        System.out.println("Deposit Required: " + booking.getDepositAmountRequired());
        System.out.println("Total Payments Found: " + payments.size());

        for (Payment p : payments) {
            System.out.println("Payment: ID=" + p.getPaymentId() +
                    ", Type=" + p.getPaymentType() +
                    ", Status=" + p.getPaymentStatus() +
                    ", Amount=" + p.getAmount() +
                    ", OrderCode=" + p.getOrderCode());
        }
        BigDecimal paidAmount = payments.stream()
                .filter(p -> p.getPaymentStatus() == Payment.PaymentStatus.Completed)
                .filter(p -> p.getPaymentType() == Payment.PaymentType.Deposit ||
                        p.getPaymentType() == Payment.PaymentType.FinalPayment)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("Calculated Paid Amount: " + paidAmount);

        BigDecimal remainingAmount = booking.getTotalAmount().subtract(paidAmount);
        System.out.println("Remaining Amount: " + remainingAmount);
        System.out.println("=== END DEBUG ===");

        model.addAttribute("booking", booking);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("currentUser", user);
        model.addAttribute("paidAmount", paidAmount);
        model.addAttribute("remainingAmount", remainingAmount);

        return "customer/booking-confirmation";
    }

    @GetMapping("/my-bookings")
    public String myBookings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<Booking> bookings = bookingService.getBookingsByUser(user);

        // Add feedback information to each booking
        for (Booking booking : bookings) {
            boolean hasFeedback = userFeedbackService.hasFeedbackForBooking(booking);
            booking.setHasFeedback(hasFeedback);

            // Determine review eligibility: Completed and at least 5 days since pickup
            boolean eligible = false;
            try {
                if (booking.getStatus() == Booking.BookingStatus.Completed) {
                    java.time.LocalDate eligibleDate = booking.getPickupDateTime().toLocalDate().plusDays(5);
                    eligible = !java.time.LocalDate.now().isBefore(eligibleDate);
                }
            } catch (Exception ignored) {}
            booking.setCanReview(eligible && !hasFeedback);

            // Fallback: Check if payment was successful but webhook was delayed
            // If booking is Confirmed but has Pending payments, mark them as Completed
            if (booking.getStatus() == Booking.BookingStatus.Confirmed) {
                try {
                    java.util.List<com.ecodana.evodanavn1.model.Payment> payments =
                            bookingService.getPaymentsByBookingId(booking.getBookingId());
                    boolean hasPendingPayments = payments.stream()
                            .anyMatch(p -> p.getPaymentStatus() == com.ecodana.evodanavn1.model.Payment.PaymentStatus.Pending);

                    if (hasPendingPayments) {
                        // Mark all pending payments as Completed since booking is Confirmed
                        for (com.ecodana.evodanavn1.model.Payment payment : payments) {
                            if (payment.getPaymentStatus() == com.ecodana.evodanavn1.model.Payment.PaymentStatus.Pending) {
                                payment.setPaymentStatus(com.ecodana.evodanavn1.model.Payment.PaymentStatus.Completed);
                                bookingService.updatePayment(payment);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("currentUser", user);

        return "customer/my-bookings";
    }

    /**
     * Show cancel booking form
     */
    @GetMapping("/cancel/{bookingId}")
    public String showCancelForm(@PathVariable String bookingId,
                                 HttpSession session,
                                 Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.findById(bookingId).orElse(null);
            if (booking == null) {
                return "redirect:/booking/my-bookings";
            }

            // Check ownership
            if (!booking.getUser().getId().equals(user.getId())) {
                return "redirect:/booking/my-bookings";
            }

            // Only allow cancellation for Confirmed bookings (paid)
            if (booking.getStatus() != Booking.BookingStatus.Confirmed) {
                return "redirect:/booking/my-bookings";
            }

            model.addAttribute("booking", booking);
            model.addAttribute("currentUser", user);

            return "customer/cancel-booking-form";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/booking/my-bookings";
        }
    }

    /**
     * Process cancel booking request
     */
    @PostMapping("/cancel/{bookingId}")
    public String processCancelBooking(@PathVariable String bookingId,
                                       @RequestParam String cancelReason,
                                       @RequestParam(required = false) String bankAccountId,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.findById(bookingId).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy booking!");
                return "redirect:/booking/my-bookings";
            }

            // Check ownership
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Không có quyền hủy booking này!");
                return "redirect:/booking/my-bookings";
            }

            // Only allow cancellation for Confirmed bookings (paid)
            if (booking.getStatus() != Booking.BookingStatus.Confirmed) {
                redirectAttributes.addFlashAttribute("error", "Chỉ có thể hủy booking đã thanh toán!");
                return "redirect:/booking/my-bookings";
            }

            // Create refund request
            System.out.println("=== CREATE REFUND REQUEST ===");
            System.out.println("Booking ID: " + bookingId);
            System.out.println("Booking Status: " + booking.getStatus());
            System.out.println("Cancel Reason: " + cancelReason);
            System.out.println("Bank Account ID: " + bankAccountId);

            RefundRequest refundRequest = refundRequestService.createRefundRequest(booking, user, cancelReason, bankAccountId);

            System.out.println("RefundRequest created: " + refundRequest.getRefundRequestId());
            System.out.println("Booking Status after: " + booking.getStatus());

            redirectAttributes.addFlashAttribute("success",
                    "Yêu cầu hủy xe đã được gửi đến admin. Mã yêu cầu: " + refundRequest.getRefundRequestId().substring(0, 8) +
                            ". Bạn sẽ nhận được thông báo khi admin xử lý.");

            return "redirect:/booking/my-bookings";

        } catch (Exception e) {
            System.out.println("ERROR in processCancelBooking: " + e.getMessage());
            e.printStackTrace(System.out);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/booking/my-bookings";
        }
    }

    /**
     * Cancel car (for confirmed bookings - đã thanh toán)
     */
    @PostMapping("/cancel-car/{bookingId}")
    public String cancelCar(
            @PathVariable String bookingId,
            @RequestParam(required = false) String cancelReason,
            @RequestParam(required = false) String bankAccountId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        System.out.println("=== Cancel Car Request ===");
        System.out.println("Booking ID: " + bookingId);
        System.out.println("Bank Account ID: " + bankAccountId);

        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            System.out.println("ERROR: User not logged in");
            return "redirect:/login";
        }
        System.out.println("User ID: " + user.getId());

        Booking booking = bookingService.findById(bookingId).orElse(null);
        if (booking == null) {
            System.out.println("ERROR: Booking not found");
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy booking!");
            return "redirect:/booking/my-bookings";
        }
        System.out.println("Booking found. Status: " + booking.getStatus());

        // Check booking ownership
        String bookingUserId = booking.getUser() != null ? booking.getUser().getId() : null;
        System.out.println("Booking User ID: " + bookingUserId);

        if (bookingUserId == null || !bookingUserId.equals(user.getId())) {
            System.out.println("ERROR: User ID mismatch");
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy booking!");
            return "redirect:/booking/my-bookings";
        }

        // Cho phép hủy khi đã Confirmed (đã thanh toán) hoặc AwaitingDeposit (chưa thanh toán)
        if (booking.getStatus() != Booking.BookingStatus.Confirmed &&
                booking.getStatus() != Booking.BookingStatus.AwaitingDeposit) {
            System.out.println("ERROR: Cannot cancel booking. Current status: " + booking.getStatus());
            redirectAttributes.addFlashAttribute("error", "Không thể hủy booking ở trạng thái hiện tại: " + booking.getStatus());
            return "redirect:/booking/my-bookings";
        }

        try {
            System.out.println("=== ATTEMPTING CANCELLATION ===");
            System.out.println("Booking Status: " + booking.getStatus());
            System.out.println("User ID: " + user.getId());

            String finalReason = cancelReason != null ? cancelReason : "Khách hàng yêu cầu hủy";
            Map<String, Object> refundResult = bookingService.processCancellationAndRefund(bookingId, finalReason, user, bankAccountId);

            if (refundResult.get("success") == Boolean.TRUE) {
                redirectAttributes.addFlashAttribute("success", refundResult.get("message").toString());
            } else {
                redirectAttributes.addFlashAttribute("error", refundResult.get("message").toString());
            }
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống khi hủy xe: " + e.getMessage());
        }
        return "redirect:/booking/my-bookings";
    }

    /**
     * Manual check payment status and update booking
     */
    @PostMapping("/payment/check-status/{bookingId}")
    public String checkPaymentStatus(@PathVariable String bookingId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.findById(bookingId).orElse(null);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn đặt xe!");
                return "redirect:/booking/my-bookings";
            }

            // Check if booking belongs to user
            if (!booking.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập đơn đặt xe này!");
                return "redirect:/booking/my-bookings";
            }

            System.out.println("=== Manual Payment Status Check ===");
            System.out.println("BookingId: " + bookingId);
            System.out.println("Current Status: " + booking.getStatus());

            // Force update to Confirmed if payment was successful
            if (booking.getStatus() == Booking.BookingStatus.AwaitingDeposit) {
                booking.setStatus(Booking.BookingStatus.Confirmed);
                booking.setPaymentConfirmedAt(LocalDateTime.now());
                bookingService.updateBooking(booking);

                redirectAttributes.addFlashAttribute("success", "Trạng thái đơn đặt xe đã được cập nhật thành công!");
                System.out.println("Booking status manually updated to Confirmed");
            } else {
                redirectAttributes.addFlashAttribute("info", "Trạng thái đơn đặt xe hiện tại: " + booking.getStatus());
            }

            return "redirect:/booking/detail/" + bookingId;

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/booking/my-bookings";
        }
    }

    /**
     * Confirm return success - Change RefundPending to Completed
     */
    @PostMapping("/confirm-return-success/{bookingId}")
    public ResponseEntity<?> confirmReturnSuccess(@PathVariable String bookingId,
                                                  HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            Booking booking = bookingService.findById(bookingId).orElse(null);
            if (booking == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Booking not found");
            }

            // Check if booking belongs to user
            if (!booking.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            // Check if booking is in RefundPending status
            if (booking.getStatus() != Booking.BookingStatus.RefundPending) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking is not in RefundPending status");
            }

            // Update booking status to Completed
            booking.setStatus(Booking.BookingStatus.Completed);
            bookingService.updateBooking(booking);

            // Send notification to owner
            Vehicle vehicle = booking.getVehicle();
            if (vehicle != null && vehicle.getOwnerId() != null) {
                notificationService.createNotification(
                        vehicle.getOwnerId(),
                        "Chuyến đi " + booking.getBookingCode() + " đã hoàn thành thành công",
                        booking.getBookingId(),
                        "RENTAL_COMPLETED"
                );
            }

            return ResponseEntity.ok().body("Return confirmed successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * Confirm return failed - Keep RefundPending status and record reason
     */
    @PostMapping("/confirm-return-failed/{bookingId}")
    public ResponseEntity<?> confirmReturnFailed(@PathVariable String bookingId,
                                                 @RequestBody Map<String, String> body,
                                                 HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            Booking booking = bookingService.findById(bookingId).orElse(null);
            if (booking == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Booking not found");
            }

            // Check if booking belongs to user
            if (!booking.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            // Check if booking is in RefundPending status
            if (booking.getStatus() != Booking.BookingStatus.RefundPending) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Booking is not in RefundPending status");
            }

            // Record the reason in cancelReason field
            String reason = body.get("reason");
            if (reason != null && !reason.isEmpty()) {
                booking.setCancelReason("Hoàn chuyến không thành công: " + reason);
            } else {
                booking.setCancelReason("Hoàn chuyến không thành công");
            }
            bookingService.updateBooking(booking);

            // Send notification to owner
            Vehicle vehicle = booking.getVehicle();
            if (vehicle != null && vehicle.getOwnerId() != null) {
                notificationService.createNotification(
                        vehicle.getOwnerId(),
                        "Chuyến đi " + booking.getBookingCode() + " hoàn chuyến không thành công",
                        booking.getBookingId(),
                        "RENTAL_FAILED"
                );
            }

            return ResponseEntity.ok().body("Return failure recorded");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    /**
     * Debug endpoint to check booking and payment status
     */
    @GetMapping("/debug/{bookingId}")
    public String debugBooking(@PathVariable String bookingId,
                               HttpSession session,
                               Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Booking booking = bookingService.findById(bookingId).orElse(null);
            if (booking == null) {
                model.addAttribute("error", "Booking not found");
                return "debug/booking-debug";
            }

            // Get all payments for this booking
            List<Payment> payments = paymentRepository.findByBookingId(bookingId);

            model.addAttribute("booking", booking);
            model.addAttribute("payments", payments);
            model.addAttribute("currentUser", user);

            System.out.println("=== BOOKING DEBUG ===");
            System.out.println("Booking ID: " + bookingId);
            System.out.println("Booking Status: " + booking.getStatus());
            System.out.println("Payment Confirmed At: " + booking.getPaymentConfirmedAt());
            System.out.println("Total Payments: " + payments.size());

            for (Payment payment : payments) {
                System.out.println("Payment: " + payment.getPaymentId() +
                        ", Amount: " + payment.getAmount() +
                        ", Status: " + payment.getPaymentStatus() +
                        ", Method: " + payment.getPaymentMethod());
            }

            return "debug/booking-debug";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error: " + e.getMessage());
            return "debug/booking-debug";
        }
    }

    /**
     * API endpoint to get refund request information for a booking
     * Customer can view transfer proof image
     */
    @GetMapping("/api/refund-info/{bookingId}")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<?> getRefundInfo(@PathVariable String bookingId, HttpSession session) {
        try {
            // Check if user is logged in
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Vui lòng đăng nhập"));
            }

            // Get booking
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "Không tìm thấy booking"));
            }

            // Check if booking belongs to current user
            if (!booking.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "message", "Bạn không có quyền xem thông tin này"));
            }

            // Get refund request
            List<RefundRequest> refundRequests = refundRequestService.getRefundRequestsByBookingId(bookingId);
            if (refundRequests.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "Không tìm thấy yêu cầu hoàn tiền"));
            }

            RefundRequest refundRequest = refundRequests.get(0); // Get first refund request

            // Return refund info
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "refundRequestId", refundRequest.getRefundRequestId(),
                    "refundAmount", refundRequest.getRefundAmount(),
                    "refundStatus", refundRequest.getStatus().toString(),
                    "transferProofImagePath", refundRequest.getTransferProofImagePath() != null ?
                            refundRequest.getTransferProofImagePath() : "",
                    "adminNotes", refundRequest.getAdminNotes() != null ? refundRequest.getAdminNotes() : "",
                    "processedDate", refundRequest.getProcessedDate() != null ?
                            refundRequest.getProcessedDate().toString() : ""
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Lỗi: " + e.getMessage()));
        }
    }
}