package com.ecodana.evodanavn1.controller.customer;

import com.ecodana.evodanavn1.model.BankAccount;
import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.Vehicle;
import com.ecodana.evodanavn1.service.BankAccountService;
import com.ecodana.evodanavn1.service.BookingService;
import com.ecodana.evodanavn1.service.FavoriteService;
import com.ecodana.evodanavn1.service.UserFeedbackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerDashboardController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private UserFeedbackService userFeedbackService;

    @Autowired
    private BankAccountService bankAccountService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Get user's bookings
            List<Booking> allBookings = bookingService.getBookingsByUserId(user.getId());
            List<Booking> activeBookings = allBookings.stream()
                    .filter(booking -> booking.getStatus() == Booking.BookingStatus.Confirmed || 
                                     booking.getStatus() == Booking.BookingStatus.Ongoing)
                    .toList();

            // Get user's favorite vehicles (placeholder - method may not exist)
            List<Vehicle> favoriteVehicles = new ArrayList<>();
            // TODO: Implement when FavoriteService.getFavoriteVehiclesByUserId() is available

            // Get user's reviews (placeholder - method may not exist)  
            List<Object> reviewsGiven = new ArrayList<>();
            // TODO: Implement when UserFeedbackService.getFeedbacksByUserId() is available

            // Get user's bank accounts
            List<BankAccount> bankAccounts = bankAccountService.getBankAccountsByUserId(user.getId());

            // Add data to model
            model.addAttribute("user", user);
            model.addAttribute("currentUser", user);
            model.addAttribute("bookings", allBookings);
            model.addAttribute("totalBookings", allBookings);
            model.addAttribute("activeBookings", activeBookings);
            model.addAttribute("favoriteVehicles", favoriteVehicles);
            model.addAttribute("reviewsGiven", reviewsGiven);
            model.addAttribute("bankAccounts", bankAccounts);

            return "customer/customer-dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu dashboard");
            return "customer/customer-dashboard";
        }
    }
}
