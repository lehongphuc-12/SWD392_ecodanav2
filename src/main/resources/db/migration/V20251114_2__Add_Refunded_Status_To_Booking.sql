-- Migration to add Refunded status to Booking
-- This allows Booking to have a dedicated "Refunded" status instead of just "Cancelled"

ALTER TABLE `Booking` 
MODIFY COLUMN `Status` ENUM(
    'Pending', 
    'Approved', 
    'Rejected', 
    'Ongoing', 
    'Completed', 
    'Cancelled', 
    'AwaitingDeposit', 
    'Confirmed', 
    'RefundPending', 
    'Refunded',
    'LatePickup'
) NOT NULL DEFAULT 'Pending';
