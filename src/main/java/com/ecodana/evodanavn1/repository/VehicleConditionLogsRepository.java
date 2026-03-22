package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.VehicleConditionLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleConditionLogsRepository extends JpaRepository<VehicleConditionLogs, String> {
    // Bạn có thể thêm các phương thức truy vấn tùy chỉnh ở đây nếu cần
}