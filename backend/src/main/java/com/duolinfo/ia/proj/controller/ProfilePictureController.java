package com.duolinfo.ia.proj.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.service.ProfilePictureStorageService;
import com.duolinfo.ia.proj.service.ProfilePictureStorageService.StoredPicture;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class ProfilePictureController {

    private final ProfilePictureStorageService profilePictureStorage;

    public ProfilePictureController(ProfilePictureStorageService profilePictureStorage) {
        this.profilePictureStorage = profilePictureStorage;
    }

    @GetMapping("/media/profile-pictures/{fileName:.+}")
    @Operation(summary = "Exibe uma foto de perfil salva no servidor")
    public ResponseEntity<?> getProfilePicture(@PathVariable String fileName) {
        StoredPicture picture = profilePictureStorage.load(fileName);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(picture.contentType()))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
            .header("X-Content-Type-Options", "nosniff")
            .body(picture.resource());
    }
}
