package com.ecodana.evodanavn1.repository;

import com.ecodana.evodanavn1.model.UserLogins;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLoginsRepository extends JpaRepository<UserLogins, UserLogins.UserLoginId> {

    /**
     * Tìm kiếm một liên kết đăng nhập bằng nhà cung cấp và mã định danh của nhà cung cấp.
     *
     * @param loginProvider Ví dụ: "google", "facebook"
     * @param providerKey   ID duy nhất từ nhà cung cấp đó
     * @return Optional<UserLogins>
     */
    Optional<UserLogins> findByLoginProviderAndProviderKey(String loginProvider, String providerKey);
    List<UserLogins> findByUser_Id(String userId);
}