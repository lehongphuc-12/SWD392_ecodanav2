package com.ecodana.evodanavn1.security;

import com.ecodana.evodanavn1.model.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

import com.ecodana.evodanavn1.service.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler successHandler;
    private final CustomAuthenticationSuccessHandler customSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(OAuth2LoginSuccessHandler successHandler,
                          CustomAuthenticationSuccessHandler customSuccessHandler,
                          ClientRegistrationRepository clientRegistrationRepository,
                          UserService userService,
                          PasswordEncoder passwordEncoder,
                          CustomOAuth2UserService customOAuth2UserService) {
        this.successHandler = successHandler;
        this.customSuccessHandler = customSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userService.findByUsername(username);
            if (user == null) {
                user = userService.findByEmail(username);
            }
            if (user == null) {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found: " + username);
            }
            
            // Check if user is banned
            if (user.getStatus() == User.UserStatus.Banned) {
                throw new org.springframework.security.authentication.DisabledException("Tài khoản của bạn đã bị cấm. Vui lòng liên hệ quản trị viên.");
            }
            
            // Check if user is inactive
            if (user.getStatus() == User.UserStatus.Inactive) {
                throw new org.springframework.security.authentication.DisabledException("Tài khoản của bạn chưa được kích hoạt.");
            }
            
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRoleName().toUpperCase())
                    .build();
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // CHO PHÉP TRUY CẬP CÔNG KHAI VÀO API CHATBOT
                        .requestMatchers("/", "/register", "/verify-otp", "/login", "/login-success", "/logout", "/vehicles/**", "/css/**", "/js/**", "/images/**", "/oauth2/**", "/forgot-password", "/reset-password", "/api/discounts/validate", "/api/chatbot/**", "/register-car-info").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/owner/cars/add", "/owner/cars").authenticated()
                        .requestMatchers("/owner/**").hasAnyRole("ADMIN", "STAFF", "OWNER")
                        .requestMatchers("/staff/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/booking/**").authenticated()
                        .requestMatchers("/documents/**").authenticated()
                        .requestMatchers("/feedback/**").authenticated()
                        .anyRequest().authenticated()
                ) 
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/oauth2/**", "/api/**", "/documents/**", "/feedback/**", "/admin/**", "/owner/**", "/favorites/**") // Đã bao gồm /api/chatbot/**
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOAuth2UserService)
                        )
                        .successHandler(successHandler)
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(authorizationRequestResolver(clientRegistrationRepository))
                        )
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry())
                );
        return http.build();
    }

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        authorizationRequestResolver.setAuthorizationRequestCustomizer(
                customizer -> customizer.additionalParameters(params -> {
                    params.put("access_type", "offline");
                    params.put("prompt", "consent select_account");
                })
        );
        return authorizationRequestResolver;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}