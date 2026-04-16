package com.nexora.auth.service;

import com.nexora.auth.config.AuthSecurityProperties;
import com.nexora.auth.repository.AuthRepository;
import com.nexora.auth.repository.AuthRepository.AuthMethodRecord;
import com.nexora.auth.repository.AuthRepository.RefreshSessionRecord;
import com.nexora.auth.repository.AuthRepository.UserIdentityRecord;
import com.nexora.auth.repository.AuthRepository.VerificationChallengeRecord;
import com.nexora.platform.core.auth.UnauthorizedException;
import com.nexora.platform.core.security.TokenHashingUtils;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthWorkflowService {

  private static final String CHALLENGE_TYPE_EMAIL_VERIFICATION = "email_verification";
  private static final String CHALLENGE_TYPE_PASSWORD_RESET = "password_reset";
  private static final Logger log = LoggerFactory.getLogger(AuthWorkflowService.class);

  private final AuthRepository authRepository;
  private final AuthUserAccessClient authUserAccessClient;
  private final AuthSessionRevocationStore authSessionRevocationStore;
  private final JwtAccessTokenService jwtAccessTokenService;
  private final PasswordEncoder passwordEncoder;
  private final AuthSecurityProperties authSecurityProperties;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  public AuthWorkflowService(
      AuthRepository authRepository,
      AuthUserAccessClient authUserAccessClient,
      AuthSessionRevocationStore authSessionRevocationStore,
      JwtAccessTokenService jwtAccessTokenService,
      PasswordEncoder passwordEncoder,
      AuthSecurityProperties authSecurityProperties,
      Clock authClock) {
    this.authRepository = authRepository;
    this.authUserAccessClient = authUserAccessClient;
    this.authSessionRevocationStore = authSessionRevocationStore;
    this.jwtAccessTokenService = jwtAccessTokenService;
    this.passwordEncoder = passwordEncoder;
    this.authSecurityProperties = authSecurityProperties;
    this.clock = authClock;
  }

  @Transactional
  public AuthSessionView signUp(SignUpCommand command, ClientContext clientContext) {
    String email = normalizeEmail(command.email());
    try {
      if (authRepository.findUserByEmail(email).isPresent()) {
        throw new IllegalArgumentException("An account with this email already exists");
      }

      String displayName = normalizeDisplayName(command.displayName());
      String locale = normalizeLocale(command.locale());
      String timezone = normalizeTimezone(command.timezone());
      UUID userId = UUID.randomUUID();

      authRepository.insertUserIdentity(userId, email, displayName, "pending", locale, timezone);
      authRepository.insertPasswordAuthMethod(
          UUID.randomUUID(),
          userId,
          email,
          passwordEncoder.encode(command.password()),
          true);

      AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess =
          authUserAccessClient.bootstrapWorkspace(
              userId,
              email,
              displayName,
              normalizeWorkspaceName(command.workspaceName(), displayName),
              locale,
              timezone);

      UserIdentityRecord user =
          authRepository.findUserById(userId)
              .orElseThrow(() -> new IllegalStateException("Newly created user could not be reloaded"));

      AuthSessionView sessionView =
          issueFreshSession(
              user,
              workspaceAccess,
              clientContext,
              EmailState.unverified(),
              null);

      ChallengeDispatchView verificationChallenge =
          createChallengeForUser(
              userId,
              CHALLENGE_TYPE_EMAIL_VERIFICATION,
              authSecurityProperties.getChallenge().getEmailVerificationTtl());

      return sessionView.withChallenge(verificationChallenge.challengeToken(), null);
    } catch (RuntimeException exception) {
      log.error("Signup workflow failed for {}", email, exception);
      throw exception;
    }
  }

  @Transactional
  public AuthSessionView login(LoginCommand command, ClientContext clientContext) {
    String email = normalizeEmail(command.email());

    AuthMethodRecord passwordMethod =
        authRepository.findPrimaryPasswordMethodByEmail(email)
            .orElseThrow(this::invalidCredentials);

    if (!passwordEncoder.matches(command.password(), passwordMethod.secretHash())) {
      throw invalidCredentials();
    }

    UserIdentityRecord user =
        authRepository.findUserById(passwordMethod.userId())
            .orElseThrow(this::invalidCredentials);

    validateUserStatus(user);

    EmailState emailState = EmailState.of(passwordMethod.verifiedAt() != null);
    AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess =
        selectWorkspaceAccess(
            user,
            command.workspaceId(),
            null,
            authUserAccessClient.listWorkspaceAccess(user.id()));

    return issueFreshSession(user, workspaceAccess, clientContext, emailState, null);
  }

  @Transactional
  public AuthSessionView refresh(RefreshCommand command, ClientContext clientContext) {
    RefreshSessionRecord session = loadActiveRefreshSession(command.refreshToken());
    UserIdentityRecord user =
        authRepository.findUserById(session.userId())
            .orElseThrow(() -> new UnauthorizedException("The session user no longer exists"));

    validateUserStatus(user);

    EmailState emailState =
        EmailState.of(
            authRepository.findPrimaryPasswordMethodByUserId(user.id())
                .map(method -> method.verifiedAt() != null)
                .orElse(false));

    AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess =
        selectWorkspaceAccess(
            user,
            command.workspaceId(),
            session.workspaceContextId(),
            authUserAccessClient.listWorkspaceAccess(user.id()));

    return rotateSession(user, session, workspaceAccess, clientContext, emailState, null);
  }

  @Transactional(readOnly = true)
  public AuthSessionView me(String rawAccessToken) {
    JwtAccessTokenService.ParsedAccessToken parsedAccessToken = requireActiveAccessToken(rawAccessToken);
    UserIdentityRecord user =
        authRepository.findUserById(parsedAccessToken.userId())
            .orElseThrow(() -> new UnauthorizedException("The session user no longer exists"));

    List<AuthUserAccessClient.WorkspaceAccessResponse> workspaceAccessList =
        authUserAccessClient.listWorkspaceAccess(user.id());
    AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess =
        selectWorkspaceAccess(
            user,
            parsedAccessToken.workspaceId(),
            parsedAccessToken.workspaceId(),
            workspaceAccessList);

    return new AuthSessionView(
        toUserView(user, parsedAccessToken.emailVerified()),
        toWorkspaceView(workspaceAccess),
        workspaceAccessList.stream().map(this::toWorkspaceView).toList(),
        null,
        null,
        parsedAccessToken.expiresAt(),
        null,
        null,
        null);
  }

  @Transactional
  public void logout(String rawAccessToken) {
    JwtAccessTokenService.ParsedAccessToken parsedAccessToken = requireActiveAccessToken(rawAccessToken);
    Instant now = Instant.now(clock);

    authRepository.findRefreshSessionById(parsedAccessToken.sessionId()).ifPresent(session -> {
      authRepository.revokeRefreshSession(session.id(), now);
      authSessionRevocationStore.revokeRefreshSession(session.id(), session.expiresAt());
    });

    authSessionRevocationStore.revokeAccessToken(parsedAccessToken.tokenId(), parsedAccessToken.expiresAt());
  }

  @Transactional
  public ChallengeDispatchView requestEmailVerification(String emailAddress) {
    return dispatchChallengeByEmail(
        emailAddress,
        CHALLENGE_TYPE_EMAIL_VERIFICATION,
        authSecurityProperties.getChallenge().getEmailVerificationTtl(),
        true);
  }

  @Transactional
  public ChallengeCompletionView confirmEmailVerification(String rawToken) {
    VerificationChallengeRecord challenge =
        requireActiveChallenge(rawToken, CHALLENGE_TYPE_EMAIL_VERIFICATION);
    Instant now = Instant.now(clock);

    authRepository.consumeVerificationChallenge(challenge.id(), now);
    authRepository.markPrimaryAuthMethodsVerified(challenge.userId(), now);
    authRepository.updateUserStatus(challenge.userId(), "active");

    UserIdentityRecord user =
        authRepository.findUserById(challenge.userId())
            .orElseThrow(() -> new IllegalStateException("Verified user could not be reloaded"));

    return new ChallengeCompletionView(user.email(), CHALLENGE_TYPE_EMAIL_VERIFICATION, now);
  }

  @Transactional
  public ChallengeDispatchView requestPasswordReset(String emailAddress) {
    return dispatchChallengeByEmail(
        emailAddress,
        CHALLENGE_TYPE_PASSWORD_RESET,
        authSecurityProperties.getChallenge().getPasswordResetTtl(),
        false);
  }

  @Transactional
  public ChallengeCompletionView resetPassword(String rawToken, String newPassword) {
    VerificationChallengeRecord challenge =
        requireActiveChallenge(rawToken, CHALLENGE_TYPE_PASSWORD_RESET);
    AuthMethodRecord passwordMethod =
        authRepository.findPrimaryPasswordMethodByUserId(challenge.userId())
            .orElseThrow(
                () -> new IllegalStateException(
                    "Password reset requested for a user without a password method"));

    Instant now = Instant.now(clock);
    authRepository.updatePasswordHash(passwordMethod.id(), passwordEncoder.encode(newPassword));
    authRepository.consumeVerificationChallenge(challenge.id(), now);

    UserIdentityRecord user =
        authRepository.findUserById(challenge.userId())
            .orElseThrow(() -> new IllegalStateException("Reset user could not be reloaded"));

    return new ChallengeCompletionView(user.email(), CHALLENGE_TYPE_PASSWORD_RESET, now);
  }

  @Transactional
  public AuthSessionView acceptInvite(InviteAcceptanceCommand command, ClientContext clientContext) {
    JwtAccessTokenService.ParsedAccessToken parsedAccessToken = requireActiveAccessToken(command.accessToken());
    RefreshSessionRecord refreshSession =
        requireRefreshSessionForAccessToken(command.refreshToken(), parsedAccessToken.sessionId());

    UserIdentityRecord user =
        authRepository.findUserById(parsedAccessToken.userId())
            .orElseThrow(() -> new UnauthorizedException("The session user no longer exists"));

    validateUserStatus(user);

    AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess =
        authUserAccessClient.acceptInvite(command.inviteToken(), user.id(), user.email());

    return rotateSession(
        user,
        refreshSession,
        workspaceAccess,
        clientContext,
        EmailState.of(parsedAccessToken.emailVerified()),
        parsedAccessToken);
  }

  @Transactional
  public AuthSessionView switchWorkspace(SwitchWorkspaceCommand command, ClientContext clientContext) {
    JwtAccessTokenService.ParsedAccessToken parsedAccessToken = requireActiveAccessToken(command.accessToken());
    RefreshSessionRecord refreshSession =
        requireRefreshSessionForAccessToken(command.refreshToken(), parsedAccessToken.sessionId());

    UserIdentityRecord user =
        authRepository.findUserById(parsedAccessToken.userId())
            .orElseThrow(() -> new UnauthorizedException("The session user no longer exists"));

    validateUserStatus(user);

    AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess =
        selectWorkspaceAccess(
            user,
            command.workspaceId(),
            refreshSession.workspaceContextId(),
            authUserAccessClient.listWorkspaceAccess(user.id()));

    return rotateSession(
        user,
        refreshSession,
        workspaceAccess,
        clientContext,
        EmailState.of(parsedAccessToken.emailVerified()),
        parsedAccessToken);
  }

  private ChallengeDispatchView dispatchChallengeByEmail(
      String emailAddress,
      String challengeType,
      java.time.Duration ttl,
      boolean skipIfVerified) {
    String normalizedEmail = normalizeEmail(emailAddress);
    UserIdentityRecord user = authRepository.findUserByEmail(normalizedEmail).orElse(null);
    if (user == null) {
      return new ChallengeDispatchView(challengeType, true, null, null);
    }

    if (skipIfVerified
        && authRepository.findPrimaryPasswordMethodByUserId(user.id())
            .map(method -> method.verifiedAt() != null)
            .orElse(false)) {
      return new ChallengeDispatchView(challengeType, true, null, null);
    }

    return createChallengeForUser(user.id(), challengeType, ttl);
  }

  private ChallengeDispatchView createChallengeForUser(
      UUID userId,
      String challengeType,
      java.time.Duration ttl) {
    String rawToken = generateOpaqueToken(48);
    Instant expiresAt = Instant.now(clock).plus(ttl);
    authRepository.insertVerificationChallenge(
        UUID.randomUUID(),
        userId,
        challengeType,
        TokenHashingUtils.sha256(rawToken),
        expiresAt);

    String token = authSecurityProperties.isExposeDevelopmentTokens() ? rawToken : null;
    return new ChallengeDispatchView(challengeType, true, expiresAt, token);
  }

  private VerificationChallengeRecord requireActiveChallenge(String rawToken, String challengeType) {
    VerificationChallengeRecord challenge =
        authRepository.findVerificationChallengeByTokenHash(TokenHashingUtils.sha256(rawToken))
            .orElseThrow(() -> new IllegalArgumentException("Challenge token is invalid"));

    if (!Objects.equals(challenge.challengeType(), challengeType)) {
      throw new IllegalArgumentException("Challenge token type is invalid");
    }
    if (!"pending".equals(challenge.status())) {
      throw new IllegalArgumentException("Challenge token is no longer pending");
    }
    if (challenge.expiresAt().isBefore(Instant.now(clock))) {
      throw new IllegalArgumentException("Challenge token has expired");
    }
    return challenge;
  }

  private AuthSessionView issueFreshSession(
      UserIdentityRecord user,
      AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess,
      ClientContext clientContext,
      EmailState emailState,
      JwtAccessTokenService.ParsedAccessToken supersededToken) {
    Instant issuedAt = Instant.now(clock);
    Instant refreshExpiresAt = issuedAt.plus(authSecurityProperties.getJwt().getRefreshTokenTtl());
    UUID sessionId = UUID.randomUUID();
    String rawRefreshToken = generateOpaqueToken(64);

    authRepository.insertRefreshSession(
        sessionId,
        user.id(),
        TokenHashingUtils.sha256(rawRefreshToken),
        workspaceAccess.workspaceId(),
        "active",
        issuedAt,
        refreshExpiresAt,
        clientContext.ipAddress(),
        clientContext.userAgent());

    return buildSessionView(
        user,
        workspaceAccess,
        authUserAccessClient.listWorkspaceAccess(user.id()),
        rawRefreshToken,
        refreshExpiresAt,
        sessionId,
        emailState,
        supersededToken);
  }

  private AuthSessionView rotateSession(
      UserIdentityRecord user,
      RefreshSessionRecord refreshSession,
      AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess,
      ClientContext clientContext,
      EmailState emailState,
      JwtAccessTokenService.ParsedAccessToken supersededToken) {
    Instant issuedAt = Instant.now(clock);
    Instant refreshExpiresAt = issuedAt.plus(authSecurityProperties.getJwt().getRefreshTokenTtl());
    String rawRefreshToken = generateOpaqueToken(64);

    authRepository.rotateRefreshSession(
        refreshSession.id(),
        TokenHashingUtils.sha256(rawRefreshToken),
        workspaceAccess.workspaceId(),
        issuedAt,
        refreshExpiresAt,
        clientContext.userAgent());

    return buildSessionView(
        user,
        workspaceAccess,
        authUserAccessClient.listWorkspaceAccess(user.id()),
        rawRefreshToken,
        refreshExpiresAt,
        refreshSession.id(),
        emailState,
        supersededToken);
  }

  private AuthSessionView buildSessionView(
      UserIdentityRecord user,
      AuthUserAccessClient.WorkspaceAccessResponse workspaceAccess,
      List<AuthUserAccessClient.WorkspaceAccessResponse> allWorkspaceAccess,
      String rawRefreshToken,
      Instant refreshExpiresAt,
      UUID sessionId,
      EmailState emailState,
      JwtAccessTokenService.ParsedAccessToken supersededToken) {
    JwtAccessTokenService.IssuedToken accessToken =
        jwtAccessTokenService.issueToken(
            new JwtAccessTokenService.AccessTokenIdentity(
                user.id(),
                user.email(),
                user.displayName(),
                emailState.verified(),
                workspaceAccess.workspaceId(),
                workspaceAccess.workspaceName(),
                sessionId,
                List.copyOf(workspaceAccess.permissions())));

    if (supersededToken != null) {
      authSessionRevocationStore.revokeAccessToken(supersededToken.tokenId(), supersededToken.expiresAt());
    }

    return new AuthSessionView(
        toUserView(user, emailState.verified()),
        toWorkspaceView(workspaceAccess),
        allWorkspaceAccess.stream()
            .sorted(Comparator.comparing(AuthUserAccessClient.WorkspaceAccessResponse::workspaceName))
            .map(this::toWorkspaceView)
            .toList(),
        accessToken.token(),
        rawRefreshToken,
        accessToken.expiresAt(),
        refreshExpiresAt,
        null,
        null);
  }

  private AuthUserAccessClient.WorkspaceAccessResponse selectWorkspaceAccess(
      UserIdentityRecord user,
      UUID requestedWorkspaceId,
      UUID fallbackWorkspaceId,
      List<AuthUserAccessClient.WorkspaceAccessResponse> workspaceAccessList) {
    List<AuthUserAccessClient.WorkspaceAccessResponse> resolvedAccess =
        workspaceAccessList == null ? List.of() : workspaceAccessList;

    if (resolvedAccess.isEmpty()) {
      return authUserAccessClient.bootstrapWorkspace(
          user.id(),
          user.email(),
          user.displayName(),
          normalizeWorkspaceName(null, user.displayName()),
          user.localeCode(),
          user.defaultTimezone());
    }

    UUID targetWorkspaceId = requestedWorkspaceId != null ? requestedWorkspaceId : fallbackWorkspaceId;
    if (targetWorkspaceId == null) {
      return resolvedAccess.get(0);
    }

    return resolvedAccess.stream()
        .filter(access -> access.workspaceId().equals(targetWorkspaceId))
        .findFirst()
        .orElseThrow(
            () -> new UnauthorizedException("The session is not authorized for the requested workspace"));
  }

  private RefreshSessionRecord loadActiveRefreshSession(String rawRefreshToken) {
    RefreshSessionRecord session =
        authRepository.findRefreshSessionByTokenHash(TokenHashingUtils.sha256(rawRefreshToken))
            .orElseThrow(() -> new UnauthorizedException("Refresh session is invalid"));

    if (!"active".equals(session.status())) {
      throw new UnauthorizedException("Refresh session is not active");
    }
    if (session.expiresAt().isBefore(Instant.now(clock))) {
      authRepository.revokeRefreshSession(session.id(), Instant.now(clock));
      throw new UnauthorizedException("Refresh session has expired");
    }
    if (authSessionRevocationStore.isRefreshSessionRevoked(session.id())) {
      throw new UnauthorizedException("Refresh session has been revoked");
    }
    return session;
  }

  private RefreshSessionRecord requireRefreshSessionForAccessToken(String rawRefreshToken, UUID sessionId) {
    RefreshSessionRecord session = loadActiveRefreshSession(rawRefreshToken);
    if (!session.id().equals(sessionId)) {
      throw new UnauthorizedException("Refresh session does not belong to the authenticated session");
    }
    return session;
  }

  private JwtAccessTokenService.ParsedAccessToken requireActiveAccessToken(String rawAccessToken) {
    JwtAccessTokenService.ParsedAccessToken parsedAccessToken;
    try {
      parsedAccessToken = jwtAccessTokenService.parse(rawAccessToken);
    } catch (RuntimeException exception) {
      throw new UnauthorizedException("Access token is invalid");
    }

    if (parsedAccessToken.expiresAt().isBefore(Instant.now(clock))) {
      throw new UnauthorizedException("Access token has expired");
    }
    if (authSessionRevocationStore.isAccessTokenRevoked(parsedAccessToken.tokenId())) {
      throw new UnauthorizedException("Access token has been revoked");
    }
    return parsedAccessToken;
  }

  private void validateUserStatus(UserIdentityRecord user) {
    if ("suspended".equals(user.accountStatus()) || "deleted".equals(user.accountStatus())) {
      throw new UnauthorizedException("The account is not available for authentication");
    }
  }

  private IllegalArgumentException invalidCredentials() {
    return new IllegalArgumentException("The provided credentials are invalid");
  }

  private UserView toUserView(UserIdentityRecord user, boolean emailVerified) {
    return new UserView(
        user.id(),
        user.email(),
        user.displayName(),
        user.accountStatus(),
        user.localeCode(),
        user.defaultTimezone(),
        emailVerified);
  }

  private WorkspaceView toWorkspaceView(AuthUserAccessClient.WorkspaceAccessResponse response) {
    return new WorkspaceView(
        response.membershipId(),
        response.workspaceId(),
        response.workspaceName(),
        response.workspaceSlug(),
        response.workspaceStatus(),
        response.roleCode(),
        response.roleName(),
        response.membershipStatus(),
        response.joinedAt(),
        Set.copyOf(response.permissions()));
  }

  private String normalizeEmail(String value) {
    return Objects.requireNonNullElse(value, "").trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeDisplayName(String value) {
    String normalized = Objects.requireNonNullElse(value, "").trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("Display name is required");
    }
    return normalized;
  }

  private String normalizeWorkspaceName(String requestedWorkspaceName, String displayName) {
    String normalized = Objects.requireNonNullElse(requestedWorkspaceName, "").trim();
    return normalized.isBlank() ? displayName + " Workspace" : normalized;
  }

  private String normalizeLocale(String locale) {
    String normalized = Objects.requireNonNullElse(locale, "").trim();
    return normalized.isBlank() ? "en" : normalized;
  }

  private String normalizeTimezone(String timezone) {
    String normalized = Objects.requireNonNullElse(timezone, "").trim();
    return normalized.isBlank() ? "Asia/Calcutta" : normalized;
  }

  private String generateOpaqueToken(int byteLength) {
    byte[] bytes = new byte[byteLength];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public record ClientContext(String ipAddress, String userAgent) {}

  public record SignUpCommand(
      String email,
      String password,
      String displayName,
      String workspaceName,
      String locale,
      String timezone) {}

  public record LoginCommand(String email, String password, UUID workspaceId) {}

  public record RefreshCommand(String refreshToken, UUID workspaceId) {}

  public record InviteAcceptanceCommand(String accessToken, String refreshToken, String inviteToken) {}

  public record SwitchWorkspaceCommand(String accessToken, String refreshToken, UUID workspaceId) {}

  public record AuthSessionView(
      UserView user,
      WorkspaceView currentWorkspace,
      List<WorkspaceView> workspaces,
      String accessToken,
      String refreshToken,
      Instant accessTokenExpiresAt,
      Instant refreshTokenExpiresAt,
      String emailVerificationToken,
      String passwordResetToken) {
    AuthSessionView withChallenge(String emailVerificationToken, String passwordResetToken) {
      return new AuthSessionView(
          user,
          currentWorkspace,
          workspaces,
          accessToken,
          refreshToken,
          accessTokenExpiresAt,
          refreshTokenExpiresAt,
          emailVerificationToken,
          passwordResetToken);
    }
  }

  public record UserView(
      UUID userId,
      String email,
      String displayName,
      String accountStatus,
      String locale,
      String timezone,
      boolean emailVerified) {}

  public record WorkspaceView(
      UUID membershipId,
      UUID workspaceId,
      String workspaceName,
      String workspaceSlug,
      String workspaceStatus,
      String roleCode,
      String roleName,
      String membershipStatus,
      Instant joinedAt,
      Set<String> permissions) {}

  public record ChallengeDispatchView(
      String challengeType,
      boolean accepted,
      Instant expiresAt,
      String challengeToken) {}

  public record ChallengeCompletionView(
      String email,
      String challengeType,
      Instant completedAt) {}

  private record EmailState(boolean verified) {
    static EmailState of(boolean verified) {
      return new EmailState(verified);
    }

    static EmailState unverified() {
      return new EmailState(false);
    }
  }
}
