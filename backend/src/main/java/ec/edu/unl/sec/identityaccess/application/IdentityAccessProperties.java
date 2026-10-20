package ec.edu.unl.sec.identityaccess.application;

import jakarta.validation.constraints.*;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component @Validated @ConfigurationProperties("sec.identity-access")
public class IdentityAccessProperties {
    @NotNull private Duration sessionIdleTimeout=Duration.ofMinutes(30), failedAttemptWindow=Duration.ofMinutes(15);
    @Min(1) private int failedAttemptLimit=5; @Min(10) @Max(16) private int bcryptStrength=12;
    private boolean cookieSecure; @NotBlank private String subjectHmacSecret;
    public Duration getSessionIdleTimeout(){return sessionIdleTimeout;} public void setSessionIdleTimeout(Duration v){sessionIdleTimeout=v;}
    public Duration getFailedAttemptWindow(){return failedAttemptWindow;} public void setFailedAttemptWindow(Duration v){failedAttemptWindow=v;}
    public int getFailedAttemptLimit(){return failedAttemptLimit;} public void setFailedAttemptLimit(int v){failedAttemptLimit=v;}
    public int getBcryptStrength(){return bcryptStrength;} public void setBcryptStrength(int v){bcryptStrength=v;}
    public boolean isCookieSecure(){return cookieSecure;} public void setCookieSecure(boolean v){cookieSecure=v;}
    public String getSubjectHmacSecret(){return subjectHmacSecret;} public void setSubjectHmacSecret(String v){subjectHmacSecret=v;}
}
