package com.nexora.social.service;

import com.nexora.social.config.SocialIntegrationProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TokenEncryptionService {

  private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String FORMAT_PREFIX = "enc:v1:";
  private final SocialIntegrationProperties properties;
  private final SecureRandom secureRandom = new SecureRandom();

  public TokenEncryptionService(SocialIntegrationProperties properties) {
    this.properties = properties;
  }

  public String encrypt(String plaintext) {
    if (!StringUtils.hasText(plaintext)) {
      return null;
    }

    try {
      byte[] iv = new byte[12];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, currentKey(), new GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return FORMAT_PREFIX
          + properties.getEncryption().getActiveKeyId()
          + ":"
          + Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
          + ":"
          + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to encrypt social credential material", exception);
    }
  }

  public String decrypt(String ciphertext) {
    if (!StringUtils.hasText(ciphertext)) {
      return null;
    }
    if (ciphertext.startsWith("enc:") && !ciphertext.startsWith(FORMAT_PREFIX)) {
      return ciphertext.substring("enc:".length());
    }
    if (!ciphertext.startsWith(FORMAT_PREFIX)) {
      return ciphertext;
    }

    try {
      String[] parts = ciphertext.split(":");
      if (parts.length != 5) {
        throw new IllegalArgumentException("Unsupported encrypted token format");
      }
      byte[] iv = Base64.getUrlDecoder().decode(parts[3]);
      byte[] encrypted = Base64.getUrlDecoder().decode(parts[4]);
      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, currentKey(), new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to decrypt social credential material", exception);
    }
  }

  public String activeKeyId() {
    return properties.getEncryption().getActiveKeyId();
  }

  private SecretKeySpec currentKey() {
    String secret = properties.getEncryption().getActiveSecret();
    if (!StringUtils.hasText(secret) || secret.length() < 32) {
      throw new IllegalStateException("Social encryption secret must be at least 32 characters long");
    }
    return new SecretKeySpec(sha256(secret), "AES");
  }

  private byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
