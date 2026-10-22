package com.duolinfo.ia.proj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
@Inheritance(strategy = jakarta.persistence.InheritanceType.JOINED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "google_email_verified")
    private Boolean googleEmailVerified;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "profile_picture", columnDefinition = "TEXT")
    private String profilePicture;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_admin", nullable = false, columnDefinition = "boolean default false")
    private boolean admin;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "ui_language", length = 10)
    private String uiLanguage;

    @Column(name = "theme_mode", length = 10)
    private String themeMode;

    @Column(name = "daily_goal_minutes")
    private Integer dailyGoalMinutes;

    @Column(name = "notif_daily_reminders")
    private Boolean notifDailyReminders;

    @Column(name = "notif_achievements")
    private Boolean notifAchievements;

    @Column(name = "notif_ranking")
    private Boolean notifRanking;

    @Column(name = "notif_class_updates")
    private Boolean notifClassUpdates;

    public boolean isSuperAdmin() {
        return email != null && "admin@duolingo.com".equalsIgnoreCase(email.trim());
    }

    public static String normalizeGender(String rawGender) {
        if (rawGender != null && "FEMININO".equalsIgnoreCase(rawGender.trim())) {
            return "FEMININO";
        }
        return "MASCULINO";
    }

    public String getNormalizedGender() {
        return normalizeGender(gender);
    }

    public String getNormalizedUiLanguage() {
        if (uiLanguage != null) {
            String lang = uiLanguage.trim().toLowerCase();
            if ("en".equals(lang) || "es".equals(lang) || "pt".equals(lang)) {
                return lang;
            }
        }
        return "pt";
    }

    public String getNormalizedThemeMode() {
        if (themeMode != null && "dark".equalsIgnoreCase(themeMode.trim())) {
            return "dark";
        }
        return "light";
    }

    public int getEffectiveDailyGoalMinutes() {
        if (dailyGoalMinutes != null && dailyGoalMinutes > 0) {
            return dailyGoalMinutes;
        }
        return 10;
    }

    public boolean isEffectiveNotifDailyReminders() {
        return notifDailyReminders == null || notifDailyReminders;
    }

    public boolean isEffectiveNotifAchievements() {
        return notifAchievements == null || notifAchievements;
    }

    public boolean isEffectiveNotifRanking() {
        return Boolean.TRUE.equals(notifRanking);
    }

    public boolean isEffectiveNotifClassUpdates() {
        return notifClassUpdates == null || notifClassUpdates;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
