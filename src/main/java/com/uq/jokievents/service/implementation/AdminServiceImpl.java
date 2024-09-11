package com.uq.jokievents.service.implementation;

import javax.validation.Valid;

import com.uq.jokievents.dtos.*;
import com.uq.jokievents.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import com.uq.jokievents.model.Admin;
import com.uq.jokievents.model.Coupon;
import com.uq.jokievents.repository.AdminRepository;
import com.uq.jokievents.repository.CouponRepository;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.*;

import com.uq.jokievents.service.interfaces.AdminService;
import com.uq.jokievents.utils.EmailService;
import com.uq.jokievents.utils.Generators;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService{

    @Autowired
    private final AdminRepository adminRepository;
    @Autowired
    private final EmailService emailService;
    @Autowired
    private final CouponRepository couponRepository;

    @Override
    public ResponseEntity<?> updateAdmin(String id, @Valid @RequestBody UpdateAdminDTO dto) {
        try {
            Optional<Admin> existingAdmin = adminRepository.findById(id);
            if (existingAdmin.isPresent()) {
                Admin admin = existingAdmin.get();
                
                // Por si acaso 
                if (dto.username() != null) {
                    ApiResponse<String> response = new ApiResponse<>("Error", "Username cannot be updated", null);
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

                admin.setEmail(dto.email());

                Admin updatedAdmin = adminRepository.save(admin);
                ApiResponse<Admin> response = new ApiResponse<>("Success", "Admin update done", updatedAdmin);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                ApiResponse<String> response = new ApiResponse<>("Error", "Admin not found", null);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>("Error", "Failed to update admin", null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> deleteAdminById(String id) {
        try {
            Optional<Admin> existingAdmin = adminRepository.findById(id);
            if (existingAdmin.isPresent()) {
                adminRepository.deleteById(id);
                ApiResponse<String> response = new ApiResponse<>("Success", "Admin exterminated", null);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                ApiResponse<String> response = new ApiResponse<>("Error", "Admin not found", null);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>("Error", "Failed to delete admin", null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    

    @Override
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody AuthAdminDTO dto) {
        try {
            String username = dto.username();
            String password = dto.password();
            Optional<Admin> admin = adminRepository.findByUsernameAndPassword(username, password);
            if (admin.isPresent()) {
                ApiResponse<String> response = new ApiResponse<>("Success", "Login done", null);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                ApiResponse<String> response = new ApiResponse<>("Error", "Invalid username or password", null);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>("Error", "Login failed", null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public ResponseEntity<?> sendRecoverPasswordCode(String email) {
        try {
            // Validate if the admin exists by email
            Optional<Admin> adminOptional = adminRepository.findByEmail(email);
            if (!adminOptional.isPresent()) {
                ApiResponse<String> response = new ApiResponse<>("Error", "Admin not found.", null);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
    
            Admin admin = adminOptional.get();
    
            // Generate a new verification code 
            String verificationCode = Generators.generateRndVerificationCode();
            
            // Set the expiration time to 20 minutes from now
            admin.setVerificationCode(verificationCode);
            admin.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(20));
    
            // Save the updated admin with the verification code and expiration time
            adminRepository.save(admin);
    
            // Send the recovery email
            emailService.sendRecuperationEmail(admin.getEmail(), verificationCode);

            ApiResponse<String> response = new ApiResponse<>("Success", "Recovery code sent", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>("Error", "Failed to send recovery code", null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public ResponseEntity<?> recoverPassword(@Valid RecoverPassAdminDTO dto) {

        try {
            // Validate if the user exists
            String email = dto.email();
            String verificationCode =  dto.verificationCode();
            String newPassword = dto.newPassword(); // Soon to be encrypted
            Optional<Admin> adminOptional = adminRepository.findByEmail(email);
            if (!adminOptional.isPresent()) { // Shouldn't this be the root conditional? Whatever!!!
                ApiResponse<String> response = new ApiResponse<>("Error", "Admin not found", null);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
    
            Admin admin = adminOptional.get();
            // Check if the verification code is expired
            if (admin.getVerificationCodeExpiration().isBefore(LocalDateTime.now())) {
                ApiResponse<String> response = new ApiResponse<>("Error", "Verification code has expired", null);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            // Verify if the code matches (assuming the admin entity has a verification code field) (Jose will make sure of it)
            if (!admin.getVerificationCode().equals(verificationCode)) {
                ApiResponse<String> response = new ApiResponse<>("Error", "Invalid verification code", null);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
    
            // Update the password
            admin.setPassword(newPassword);  // Will hash the password soon!
            admin.setVerificationCode("");
            adminRepository.save(admin);

            ApiResponse<String> response = new ApiResponse<>("Success", "Password recovery done", null);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>("Error", "Password recovery failed", null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    } 

    @Override
    public ResponseEntity<?> createCoupon(@Valid CreateCouponDTO dto) {

        // Check if a coupon with the same name already exists
        Optional<Coupon> existingCoupon = couponRepository.findByName(dto.name());

        if (existingCoupon.isPresent()) {
            ApiResponse<String> response = new ApiResponse<>("Error", "Coupon with the same name already exists", null);
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        Coupon coupon = new Coupon();
        coupon.setName(dto.name());
        coupon.setDiscountPercent(dto.discount());
        coupon.setExpirationDate(dto.expirationDate());
        coupon.setMinPurchaseAmount(dto.minPurchaseAmount());

        Coupon savedCoupon = couponRepository.save(coupon);
        ApiResponse<Coupon> response = new ApiResponse<>("Success", "Coupon creation done", savedCoupon);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<?> updateCoupon(String id, @Valid UpdateCouponDTO dto) {
        try {
            Optional<Coupon> optionalCoupon = couponRepository.findById(id);

            if (optionalCoupon.isPresent()) {
                Coupon coupon = optionalCoupon.get();

                // Update the fields
                coupon.setDiscountPercent(dto.discount());
                coupon.setExpirationDate(dto.expirationDate());
                coupon.setMinPurchaseAmount(dto.minPurchaseAmount());

                // Save the updated coupon
                Coupon updatedCoupon = couponRepository.save(coupon);
                ApiResponse<Coupon> response = new ApiResponse<>("Success", "Coupon updated", updatedCoupon);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                ApiResponse<String> response = new ApiResponse<>("Error", "Coupon not found", null);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>("Error", "Failed to update coupon", null);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
