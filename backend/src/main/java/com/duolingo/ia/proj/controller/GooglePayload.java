// Classe para carregar o que foi pego no google

package com.duolingo.ia.proj.controller;

public class GooglePayload {

    private String googleId;

    private String email;

    private String name;

    private String picture;

    private String hostedDomain;


    public GooglePayload() {
    }


    public GooglePayload(
            String googleId,
            String email,
            String name,
            String picture,
            String hostedDomain) {

        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.hostedDomain = hostedDomain;
    }


    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }


    public String getHostedDomain() {
        return hostedDomain;
    }

    public void setHostedDomain(String hostedDomain) {
        this.hostedDomain = hostedDomain;
    }
}