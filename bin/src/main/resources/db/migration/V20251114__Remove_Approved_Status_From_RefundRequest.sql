-- Migration to remove Approved, Transferred, Completed statuses from RefundRequest
-- Only keep: Pending, Rejected, Refunded

-- First, update any existing Approved/Transferred/Completed records to Refunded
UPDATE RefundRequest 
SET Status = 'Refunded' 
WHERE Status IN ('Approved', 'Transferred', 'Completed');

-- Then modify the ENUM to only include: Pending, Rejected, Refunded
ALTER TABLE RefundRequest 
MODIFY COLUMN Status ENUM('Pending', 'Rejected', 'Refunded') NOT NULL DEFAULT 'Pending';


ALTER TABLE Booking
    MODIFY COLUMN Status ENUM(
        'Pending',
        'Approved',
        'AwaitingDeposit',
        'Confirmed',
        'Rejected',
        'Ongoing',
        'Completed',
        'Cancelled',
        'NoShow',
        'RefundPending',
        'Refunded',
        'LatePickup'
        ) NOT NULL DEFAULT 'Pending';
