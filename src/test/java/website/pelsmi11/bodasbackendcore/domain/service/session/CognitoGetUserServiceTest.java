package website.pelsmi11.bodasbackendcore.domain.service.session;

import java.util.List;

import org.junit.jupiter.api.Test;
import website.pelsmi11.bodasbackendcore.web.config.CognitoProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InternalErrorException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.PasswordResetRequiredException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.TooManyRequestsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.core.exception.SdkClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Contractual GetUser mocks (never claimed to reproduce the real service):
 * email present → filled; email absent → null; documented errors per policy.
 */
class CognitoGetUserServiceTest {

    private final CognitoIdentityProviderClient client = mock(CognitoIdentityProviderClient.class);

    private CognitoGetUserService service() {
        return new CognitoGetUserService(client);
    }

    private GetUserResponse response(String email) {
        GetUserResponse.Builder builder = GetUserResponse.builder();
        if (email != null) {
            builder.userAttributes(List.of(AttributeType.builder().name("email").value(email).build()));
        }
        return builder.build();
    }

    @Test
    void getUser_emailPresent_returnsEmail() {
        when(client.getUser(any(GetUserRequest.class))).thenReturn(response("user@example.com"));
        assertThat(service().fetchEmail("tok")).isEqualTo("user@example.com");
    }

    @Test
    void getUser_emailAbsent_returnsNull() {
        when(client.getUser(any(GetUserRequest.class))).thenReturn(response(null));
        assertThat(service().fetchEmail("tok")).isNull();
    }

    @Test
    void getUser_notAuthorized_returns401Neutral() {
        when(client.getUser(any(GetUserRequest.class))).thenThrow((NotAuthorizedException) NotAuthorizedException.builder()
                .message("neutral").build());
        assertThatThrownBy(() -> service().fetchEmail("t"))
                .isInstanceOf(website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException.class)
                .extracting(e -> ((website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getUser_userNotFound_returns401() {
        when(client.getUser(any(GetUserRequest.class))).thenThrow((UserNotFoundException) UserNotFoundException.builder()
                .message("user").build());
        assertThatThrownBy(() -> service().fetchEmail("t"))
                .extracting(e -> ((website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getUser_passwordResetRequired_returns403Neutral() {
        when(client.getUser(any(GetUserRequest.class))).thenThrow(
                (PasswordResetRequiredException) PasswordResetRequiredException.builder().message("reset").build());
        assertThatThrownBy(() -> service().fetchEmail("t"))
                .extracting(e -> ((website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
    }

    @Test
    void getUser_tooManyRequests_returns503Retryable() {
        when(client.getUser(any(GetUserRequest.class))).thenThrow(
                (TooManyRequestsException) TooManyRequestsException.builder().message("throttled").build());
        assertThatThrownBy(() -> service().fetchEmail("t"))
                .extracting(e -> ((website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void getUser_internalError_returns503() {
        when(client.getUser(any(GetUserRequest.class))).thenThrow(
                (InternalErrorException) InternalErrorException.builder().message("boom").build());
        assertThatThrownBy(() -> service().fetchEmail("t"))
                .extracting(e -> ((website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void getUser_networkSdkException_returns503() {
        when(client.getUser(any(GetUserRequest.class)))
                .thenThrow(SdkClientException.create("network down", new RuntimeException("io")));
        assertThatThrownBy(() -> service().fetchEmail("t"))
                .extracting(e -> ((website.pelsmi11.bodasbackendcore.domain.exception.CustomErrorException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }
}
