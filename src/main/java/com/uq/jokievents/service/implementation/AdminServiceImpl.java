package com.uq.jokievents.service.implementation;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import com.uq.jokievents.model.Admin;
import com.uq.jokievents.repository.AdminRepository;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.*;

import com.uq.jokievents.dtos.AuthAdminDTO;
import com.uq.jokievents.dtos.RecoverPassAdminDTO;
import com.uq.jokievents.dtos.UpdateAdminDTO;
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
    private EmailService emailService;

    @Override
    public ResponseEntity<?> updateAdmin(String id, @Valid @RequestBody UpdateAdminDTO dto) {
        try {
            Optional<Admin> existingAdmin = adminRepository.findById(id);
            if (existingAdmin.isPresent()) {
                Admin admin = existingAdmin.get();
                
                // Por si acaso 
                if (dto.getUsername() != null) {
                    return new ResponseEntity<>("Username cannot be updated", HttpStatus.BAD_REQUEST);
                }

                admin.setEmail(dto.getEmail());

                Admin updatedAdmin = adminRepository.save(admin);
                return new ResponseEntity<>(updatedAdmin, HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Admin not found", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to update admin", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> deleteAdminById(String id) {
        try {
            Optional<Admin> existingAdmin = adminRepository.findById(id);
            if (existingAdmin.isPresent()) {
                adminRepository.deleteById(id);
                return new ResponseEntity<>("Admin deleted successfully", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Admin not found", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to delete admin", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    

    @Override
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody AuthAdminDTO dto) {
        try {
            String username = dto.getUsername();
            String password = dto.getPassword();
            Optional<Admin> admin = adminRepository.findByUsernameAndPassword(username, password);
            if (admin.isPresent()) {
                return new ResponseEntity<>("Login successful", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Login failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public ResponseEntity<?> sendRecoverPasswordCode(String email) {
        try {
            // Validate if the admin exists by email
            Optional<Admin> adminOptional = adminRepository.findByEmail(email);
            if (!adminOptional.isPresent()) {
                return new ResponseEntity<>("Admin not found", HttpStatus.NOT_FOUND);
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
    
            return new ResponseEntity<>("Recovery code sent successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to send recovery code", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public ResponseEntity<?> recoverPassword(RecoverPassAdminDTO dto) {

        try {
            // Validate if the user exists
            String email = dto.getEmail();
            String verificationCode =  dto.getVerificationCode();
            String newPassword = dto.getNewPassword(); // Soon to be encrypted xdxdxdxdxd
            Optional<Admin> adminOptional = adminRepository.findByEmail(email);
            if (!adminOptional.isPresent()) {
                return new ResponseEntity<>("Admin not found", HttpStatus.NOT_FOUND);
            }
    
            Admin admin = adminOptional.get();
            // Check if the verification code is expired
            if (admin.getVerificationCodeExpiration().isBefore(LocalDateTime.now())) {
                return new ResponseEntity<>("Verification code has expired", HttpStatus.UNAUTHORIZED);
            }
            // Verify if the code matches (assuming the admin entity has a verification code field) (Jose will make sure of it)
            if (!admin.getVerificationCode().equals(verificationCode)) {
                return new ResponseEntity<>("Invalid verification code", HttpStatus.UNAUTHORIZED);
            }
    
            // Update the password
            admin.setPassword(newPassword);  // Will hash the password soon!
            admin.setVerificationCode("");
            adminRepository.save(admin);
    
            return new ResponseEntity<>("Password updated successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Password recovery failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    } 
}