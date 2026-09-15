package website.pelsmi11.bodasbackendcore.web.config;

/**
 * Typed, externalized Cognito configuration (no secrets): issuer URI, expected
 * access-token audience (app client id) and region for the GetUser client.
 * Bound from {@code app.cognito.*} properties (values come from environment
 * variables COGNITO_ISSUER_URI, COGNITO_APP_CLIENT_ID, AWS_REGION).
 */
public class CognitoProperties {

    private String issuerUri;
    private String audience;
    private String region;

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
