package Controllers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import Models.Objects.User;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "Auth", urlPatterns = {"/auth"})
public class Auth extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Root device
    private final String RootUrl;

    // Consistent OIDC attributes
    private final String OidcWellknown;
    private final String OidcClientId;
    private final String OidcClientSecret;
    private final String OidcScope;

    // OIDC attributes based on well-known ports
    private String OidcIssuer;
    private String OidcTokenEndpoint;
    private String OidcAuthEndpoint;
    private String OidcJwksUri;

    private boolean isOidcEnabled;

    public Auth() {
        this.RootUrl = (System.getenv("ROOT_ENV") != null) ? System.getenv("ROOT_ENV") : "http://localhost:8080";
        
        this.OidcWellknown = System.getenv("OIDC_WELL_KNOWN");
        this.OidcClientId = System.getenv("OIDC_CLIENT_ID");
        this.OidcClientSecret = System.getenv("OIDC_CLIENT_SECRET");
        this.OidcScope = System.getenv("OIDC_SCOPE");
        
        String OidcIssuerProvided = System.getenv("OIDC_ISSUER");

        if (this.OidcClientId != null && this.OidcClientSecret != null && this.OidcWellknown != null && this.OidcScope != null && this.LoadOidcConfiguration()) {
            if (OidcIssuerProvided != null && this.OidcIssuer.equals(OidcIssuerProvided)) {
                this.isOidcEnabled = true;
            }
        }
    }


    /*
     *  Authentication global functional methods
     *      - Those methods are used widely by many flows, and access within the Auth only
     */
    /////////////////////////////////////////////
    private boolean IsSignedIn(HttpServletRequest request) {
        User u = (User) request.getSession().getAttribute("loggedUser");
        return u != null;
    }

    private boolean LoadOidcConfiguration() {
        try {
            URL url = new URI(this.OidcWellknown).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Failed to fetch OIDC configuration. HTTP status: " + status);
            }

            try (JsonReader reader = Json.createReader(conn.getInputStream())) {
                JsonObject config = reader.readObject();

                this.OidcIssuer = config.getString("issuer");
                this.OidcTokenEndpoint = config.getString("token_endpoint");
                this.OidcJwksUri = config.getString("jwks_uri");
                this.OidcAuthEndpoint = config.getString("authorization_endpoint");

                Logger.getLogger(Auth.class.getName()).log(Level.INFO, "OIDC configuration loaded successfully.");
                return true;
            }
        }
        catch (IOException | URISyntaxException e) {
            // Hard-coded exception handler
            Logger.getLogger(Auth.class.getName()).log(Level.SEVERE, "Error loading OIDC configuration: " + e.getMessage(), e);
            return false;
        }
    }

    /*
     *  Authentication flow methods
     *      - Those methods are the flow of authentication (e.g. sign-in, sign-up, and so on)
     */
    /////////////////////////////////////////////

    // Start OIDC login
    private void HandleOidcLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!this.isOidcEnabled) {
            response.sendError(500, "OIDC is not enabled");
            return;
        }

        String state = UUID.randomUUID().toString();
        HttpSession session = request.getSession();

        session.setAttribute("oidc_state", state);

        String redirectUri = this.RootUrl + "/auth?action=oidc_callback";

        String authUrl = String.format(
                "%s?"
                + "client_id=%s&"
                + "redirect_uri=%s&"
                + "scope=%s&"
                + "response_type=code&"
                + "state=%s&"
                + "prompt=login",
                this.OidcAuthEndpoint,
                URLEncoder.encode(this.OidcClientId, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(this.OidcScope, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(state, StandardCharsets.UTF_8.toString())
        );

        response.sendRedirect(authUrl);
    }

    private void HandleOidcCallback(HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException {
        if (!this.isOidcEnabled) {
            response.sendError(500, "OIDC is not enabled");
            return;
        }

        String state = request.getParameter("state");
        String code = request.getParameter("code");

        HttpSession session = request.getSession();

        if (session == null || state == null || !state.equals(session.getAttribute("oidc_state"))) {
            response.sendError(500, "Invalid OIDC state.");
            return;
        }

        if (code == null) {
            response.sendError(500, "Authorization code is missing.");
            return;
        }

        String redirectUri = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath()) + "/auth?action=oidc_callback";

        // Exchange code for tokens
        URL tokenUrl = new URI(this.OidcTokenEndpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) tokenUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // Set timeouts
        conn.setConnectTimeout(10000); // 10 seconds
        conn.setReadTimeout(10000);    // 10 seconds

        String body = String.format(
                "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
                URLEncoder.encode(code, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(this.OidcClientId, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(this.OidcClientSecret, StandardCharsets.UTF_8.toString())
        );

        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

        int status = conn.getResponseCode();
        if (status != 200) {
            response.sendError(500, "Token endpoint returned " + status);
            return;
        }

        try (JsonReader jr = Json.createReader(conn.getInputStream())) {
            JsonObject jo = jr.readObject();
            String idToken = jo.getString("id_token", null);

            if (idToken == null) {
                response.sendError(500, "No id_token in token response.");
                return;
            }

            try {
                URL jwksUrl = new URI(this.OidcJwksUri).toURL();

                ResourceRetriever resourceRetriever = new DefaultResourceRetriever(5000, 5000);

                JWKSource<SecurityContext> keySource = JWKSourceBuilder.create(jwksUrl, resourceRetriever).build();

                ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

                jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));

                JWTClaimsSet claims = jwtProcessor.process(idToken, null);

                if (!claims.getAudience().contains(this.OidcClientId)) {
                    response.sendError(500, "Token audience mismatch.");
                    return;
                }

                Date exp = claims.getExpirationTime();
                if (exp == null || exp.toInstant().isBefore(Instant.now())) {
                    response.sendError(500, "Token expired.");
                    return;
                }

                String preferred_username = claims.getStringClaim("preferred_username");
                String email = claims.getStringClaim("email");
                String name = claims.getStringClaim("name");
                String country = claims.getStringClaim("country");
                String iss = claims.getStringClaim("iss");

                // Sign in locally
                session.setAttribute("loggedUser", new User(preferred_username, email, name, country));
                session.setAttribute("issuer_oidc", iss);
                response.sendRedirect(request.getContextPath() + "/");
            }
            catch (JOSEException | BadJOSEException | IOException | ParseException ex) {
                response.sendError(500, "Failed with reason: " + ex.getMessage());
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        switch (request.getParameter("action")) {
            case null -> {
                if (this.IsSignedIn(request)) {
                    response.sendRedirect(request.getContextPath() + "/");
                } else {
                    response.sendRedirect(request.getContextPath() + "/auth?action=signin");
                }
            }
            case "signin" -> {
                if (this.IsSignedIn(request)) {
                    response.sendRedirect(request.getContextPath() + "/");
                } else {
                    request.getRequestDispatcher("/WEB-INF/JSPViews/AuthView/SignIn.jsp").forward(request, response);
                }
            }
            case "oidc_signin" -> {
                if (this.IsSignedIn(request)) {
                    response.sendRedirect(request.getContextPath() + "/");
                } else {
                    if (this.isOidcEnabled) {
                        this.HandleOidcLogin(request, response);
                    } else {
                        response.sendError(500, "OIDC is not enabled");

                    }
                }
            }
            case "oidc_callback" -> {
                try {
                    if (this.isOidcEnabled) {
                        this.HandleOidcCallback(request, response);
                    } else {
                        response.sendError(500, "OIDC is not enabled");
                    }
                }
                catch (URISyntaxException ex) {
                    Logger.getLogger(Auth.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            case "logout" -> {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect(request.getContextPath() + "/");
            }
            default -> {
                if (this.IsSignedIn(request)) {
                    response.sendRedirect(request.getContextPath() + "/");
                } else {
                    response.sendRedirect(request.getContextPath() + "/auth?action=signin");
                }
            }
        }
    }
}
