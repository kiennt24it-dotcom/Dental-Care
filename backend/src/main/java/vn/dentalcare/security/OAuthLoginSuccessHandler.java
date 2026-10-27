package vn.dentalcare.security;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import vn.dentalcare.dto.AuthDtos.AuthResponse;
import vn.dentalcare.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService auth;
    private final String frontendUrl;
    public OAuthLoginSuccessHandler(AuthService auth,@Value("${app.frontend-url}") String frontendUrl) { this.auth=auth; this.frontendUrl=frontendUrl; }
    @Override public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication)
            throws IOException,ServletException {
        OAuth2User principal=(OAuth2User)authentication.getPrincipal();
        String email=principal.getAttribute("email");
        Boolean verified=principal.getAttribute("email_verified");
        if(email==null || !Boolean.TRUE.equals(verified)) {
            response.sendRedirect(frontendUrl+"/login?error=google_email_unverified");
            return;
        }
        AuthResponse tokens=auth.oauthLogin(principal.getAttribute("name"),email,principal.getAttribute("picture"));
        String fragment="accessToken="+enc(tokens.accessToken())+"&refreshToken="+enc(tokens.refreshToken());
        response.sendRedirect(frontendUrl+"/oauth/callback#"+fragment);
    }
    private String enc(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8); }
}
