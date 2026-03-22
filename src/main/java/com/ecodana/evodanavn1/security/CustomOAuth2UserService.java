package com.ecodana.evodanavn1.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.ecodana.evodanavn1.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.ecodana.evodanavn1.service.UserService;

@Service
public class CustomOAuth2UserService extends OidcUserService {

    @Autowired
    private UserService userService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Lấy OidcUser mặc định từ Google
        OidcUser oidcUser = super.loadUser(userRequest);

        // Lấy thông tin provider
        String loginProvider = userRequest.getClientRegistration().getRegistrationId(); // vd: "google"
        String providerKey = oidcUser.getSubject(); // Đây là ID duy nhất từ Google
        String email = oidcUser.getAttribute("email");

        System.out.println("CustomOAuth2UserService - Provider: " + loginProvider + ", Key: " + providerKey + ", Email: " + email);

        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Không có email từ nhà cung cấp OAuth2");
        }

        // --- LOGIC MỚI: TÌM BẰNG PROVIDER KEY TRƯỚC ---
        Optional<User> userOpt = userService.findUserByLogin(loginProvider, providerKey);
        User user;

        if (userOpt.isPresent()) {
            // 1. TÌM THẤY: Người dùng này đã đăng nhập bằng Google trước đây.
            user = userOpt.get();
            // Cập nhật thông tin (nếu cần, ví dụ: avatar)
            // (Bỏ qua để đơn giản)
            System.out.println("CustomOAuth2UserService - Đã tìm thấy người dùng bằng ProviderKey: " + user.getEmail());

        } else {
            // 2. KHÔNG TÌM THẤY BẰNG PROVIDER KEY: Đây là lần đầu tiên họ đăng nhập bằng Google
            // Kiểm tra xem email đã tồn tại trong hệ thống chưa (đăng ký bằng password)
            Optional<User> userByEmailOpt = Optional.ofNullable(userService.findByEmailWithRole(email));

            if (userByEmailOpt.isPresent()) {
                // 2a. Email đã tồn tại (Đã đăng ký bằng password) -> Chỉ cần LIÊN KẾT
                user = userByEmailOpt.get();
                System.out.println("CustomOAuth2UserService - Đã tìm thấy người dùng bằng Email, sẽ liên kết tài khoản: " + user.getEmail());
                // Việc liên kết (tạo UserLogins) sẽ được thực hiện trong SuccessHandler
                // vì nó cần ghi vào CSDL (tốt nhất là sau khi xác thực thành công)
            } else {
                // 2b. Email không tồn tại -> Sẽ TẠO MỚI user trong SuccessHandler
                user = null; // Đánh dấu là user mới, cần được tạo
                System.out.println("CustomOAuth2UserService - Không tìm thấy người dùng, sẽ tạo mới.");
            }
        }

        // Tạo authorities dựa trên user (nếu tìm thấy)
        Collection<? extends GrantedAuthority> authorities = (user != null) ? createAuthorities(user) : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

        // Trả về CustomOidcUser với user (có thể là null nếu là user mới)
        return new CustomOidcUser(oidcUser.getClaims(), oidcUser.getIdToken(), oidcUser.getUserInfo(), authorities, user);
    }

    private Collection<? extends GrantedAuthority> createAuthorities(User user) {
        if (user.getRole() != null && user.getRole().getRoleName() != null) {
            String roleName = user.getRole().getRoleName().toUpperCase();
            System.out.println("CustomOAuth2UserService - Đang tạo quyền cho vai trò: " + roleName);
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
        }
        System.out.println("CustomOAuth2UserService - Không tìm thấy vai trò, dùng quyền mặc định ROLE_CUSTOMER");
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
    
    // Custom OidcUser implementation that includes our User object
    public static class CustomOidcUser implements OidcUser {
        private final Map<String, Object> claims;
        private final OidcIdToken idToken;
        private final OidcUserInfo userInfo;
        private final Collection<? extends GrantedAuthority> authorities;
        private final User user;
        
        public CustomOidcUser(Map<String, Object> claims, 
                             OidcIdToken idToken,
                             OidcUserInfo userInfo,
                             Collection<? extends GrantedAuthority> authorities, 
                             User user) {
            this.claims = claims;
            this.idToken = idToken;
            this.userInfo = userInfo;
            this.authorities = authorities;
            this.user = user;
        }
        
        @Override
        public Map<String, Object> getClaims() {
            return claims;
        }
        
        @Override
        public OidcIdToken getIdToken() {
            return idToken;
        }
        
        @Override
        public OidcUserInfo getUserInfo() {
            return userInfo;
        }
        
        @Override
        public Map<String, Object> getAttributes() {
            return claims;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getName() {
            if (user != null && user.getUsername() != null) {
                return user.getUsername();
            }
            return (String) this.claims.get("email");
        }
        
        public User getUser() {
            return user;
        }
    }
}
