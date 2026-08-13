// lugar para a classe de requisição usada no authControlle.java

package com.duolingo.ia.proj.requesters;

public class GoogleLoginRequest {

    private String credential;

    public GoogleLoginRequest() {
    }

    public GoogleLoginRequest(String credential) {
        this.credential = credential;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}