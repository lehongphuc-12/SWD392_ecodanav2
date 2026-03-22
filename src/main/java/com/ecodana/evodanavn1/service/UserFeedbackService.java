package com.ecodana.evodanavn1.service;

import com.ecodana.evodanavn1.model.UserFeedback;
import com.ecodana.evodanavn1.model.Booking;
import com.ecodana.evodanavn1.model.User;
import com.ecodana.evodanavn1.model.Vehicle;
import com.ecodana.evodanavn1.repository.UserFeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserFeedbackService {

    @Autowired
    private UserFeedbackRepository userFeedbackRepository;

    public List<UserFeedback> getAllFeedback() {
        return userFeedbackRepository.findAllByOrderByCreatedDateDesc();
    }

    public List<UserFeedback> getFeedbackByUser(User user) {
        return userFeedbackRepository.findByUserOrderByCreatedDateDesc(user);
    }

    public List<UserFeedback> getFeedbackByVehicle(Vehicle vehicle) {
        return userFeedbackRepository.findByVehicle(vehicle);
    }

    public List<UserFeedback> getFeedbackByBooking(Booking booking) {
        return userFeedbackRepository.findByBooking(booking);
    }

    public List<UserFeedback> getFeedbackForOwner(User owner) {
        return userFeedbackRepository.findByVehicleOwnerId(owner.getId());
    }

    public List<UserFeedback> getFeedbackWithReplies() {
        return userFeedbackRepository.findFeedbackWithReplies();
    }

    public List<UserFeedback> getFeedbackWithoutReplies() {
        return userFeedbackRepository.findFeedbackWithoutReplies();
    }

    public UserFeedback createFeedback(User user, Booking booking, int rating, String content) {
        UserFeedback feedback = new UserFeedback();
        feedback.setFeedbackId(UUID.randomUUID().toString());
        feedback.setUser(user);
        feedback.setBooking(booking);
        feedback.setVehicle(booking.getVehicle());
        feedback.setRating(rating);
        feedback.setContent(content);
        feedback.setReviewed(LocalDate.now());
        feedback.setCreatedDate(LocalDateTime.now());
        
        return userFeedbackRepository.save(feedback);
    }

    public UserFeedback replyToFeedback(String feedbackId, String reply) {
        UserFeedback feedback = userFeedbackRepository.findById(feedbackId).orElse(null);
        if (feedback != null) {
            feedback.setStaffReply(reply);
            feedback.setReplyDate(LocalDateTime.now());
            return userFeedbackRepository.save(feedback);
        }
        return null;
    }

    public boolean deleteFeedback(String feedbackId) {
        if (userFeedbackRepository.existsById(feedbackId)) {
            userFeedbackRepository.deleteById(feedbackId);
            return true;
        }
        return false;
    }

    public UserFeedback getFeedbackById(String feedbackId) {
        return userFeedbackRepository.findById(feedbackId).orElse(null);
    }

    public boolean hasFeedbackForBooking(Booking booking) {
        return !userFeedbackRepository.findByBooking(booking).isEmpty();
    }
}
