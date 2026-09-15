package website.pelsmi11.bodasbackendcore.domain.service.session;

import java.util.List;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InternalErrorException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.PasswordResetRequiredException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.TooManyRequestsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException;

/**
 * Reliable email source: Cognito GetUser with the Access Token sent as the
 * {@code GetUserRequest.AccessToken} parameter (NEVER an Authorization header).
 * The call happens BEFORE the reconciliation transaction opens.
 *
 * Contract: {@code email: string | null} — filled when GetUser succeeds and the
 * attribute exists; {@code null} when the attribute is absent.
 *
 * Documented exception policy:
 * - NotAuthorizedException / UserNotFoundException / UserNotConfirmedException → neutral 401
 * - PasswordResetRequiredException → neutral authenticable error (403)
 * - TooManyRequestsException / InternalErrorException / network failures → retryable 503
 * - WAF ForbiddenException → neutral response without details
 */
@Service
public class CognitoGetUserService {

    private final CognitoIdentityProviderClient cognitoClient;

    public CognitoGetUserService(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    /**
     * Returns the confirmed email attribute or {@code null} when absent.
     * Must be called before opening the reconciliation transaction.
     */
    public String fetchEmail(String accessToken) {
        try {
            GetUserResponse response = cognitoClient.getUser(GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build());
            return findEmail(response.userAttributes());
        } catch (NotAuthorizedException
                | UserNotFoundException
                | UserNotConfirmedException ex) {
            throw website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException
                    .handlerCustomError("Authentication failed", org.springframework.http.HttpStatus.UNAUTHORIZED);
        } catch (PasswordResetRequiredException ex) {
            throw website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException
                    .handlerCustomError("Password reset required before continuing", org.springframework.http.HttpStatus.FORBIDDEN);
        } catch (TooManyRequestsException
                | InternalErrorException ex) {
            throw retryable();
        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException ex) {
            // Neutral response for unexpected provider rejections (e.g., WAF blocks);
            // more specific SDK exceptions are caught first.
            throw website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException
                    .handlerCustomError("Authentication failed", org.springframework.http.HttpStatus.UNAUTHORIZED);
        } catch (software.amazon.awssdk.core.exception.SdkException ex) {
            // Network failures are retryable; nothing was written yet.
            throw retryable();
        }
    }

    private static website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException retryable() {
        return website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException
                .handlerCustomError("Temporary issue contacting the identity provider; please retry",
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static String findEmail(List<AttributeType> attributes) {
        return attributes.stream()
                .filter(attribute -> "email".equals(attribute.name()))
                .map(AttributeType::value)
                .findFirst()
                .orElse(null);
    }
}
