/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.Encryption;
import de.ii.xtraplatform.base.domain.EncryptionConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Symmetric encryption for data. The stored representation is {@code nonce (12 bytes) || ciphertext
 * || tag (16 bytes)} using AES-256-GCM.
 */
@AutoBind
@Singleton
@SuppressWarnings({"PMD.ConstructorCallsOverridableMethod", "PMD.AvoidSynchronizedStatement"})
public class EncryptionImpl implements Encryption, AppLifeCycle {

  public static final int KEY_LENGTH = 32;
  public static final int NONCE_LENGTH = 12;
  public static final int TAG_LENGTH_BITS = 128;
  private static final String ALGORITHM = "AES";
  private static final String CIPHER = "AES/GCM/NoPadding";

  private final EncryptionConfiguration configuration;
  private SecretKeySpec secretKey;
  private Cipher cipher;
  private SecureRandom random;

  @Inject
  EncryptionImpl(AppContext appContext) {
    this.configuration = appContext.getConfiguration().getEncryption();
  }

  // For testing only
  EncryptionImpl(String key) {
    this.configuration = () -> Optional.ofNullable(key);
    onStart(false);
  }

  @Override
  public CompletionStage<Void> onStart(boolean isStartupAsync) {
    if (configuration.getKey().isPresent()) {
      byte[] key = parseKey(configuration.getKey().get());

      this.secretKey = new SecretKeySpec(key, ALGORITHM);
      this.random = new SecureRandom();
      try {
        this.cipher = Cipher.getInstance(CIPHER);
      } catch (GeneralSecurityException e) {
        throw new IllegalStateException("AES-256-GCM is not available in this runtime.", e);
      }
    }
    return AppLifeCycle.super.onStart(isStartupAsync);
  }

  @Override
  public boolean isEnabled() {
    return secretKey != null && cipher != null && random != null;
  }

  @Override
  public byte[] encrypt(String data) {
    byte[] nonce = new byte[NONCE_LENGTH];
    random.nextBytes(nonce);
    try {
      byte[] ciphertext;
      synchronized (this) {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
        ciphertext = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
      }

      byte[] encrypted = new byte[NONCE_LENGTH + ciphertext.length];
      System.arraycopy(nonce, 0, encrypted, 0, NONCE_LENGTH);
      System.arraycopy(ciphertext, 0, encrypted, NONCE_LENGTH, ciphertext.length);

      return encrypted;
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Encryption failed.", e);
    }
  }

  @Override
  public String decrypt(byte[] encrypted, String errorContext) {
    if (encrypted.length <= NONCE_LENGTH + TAG_LENGTH_BITS / 8) {
      throw new IllegalStateException(
          String.format("Decryption failed%s: the stored value is too short.", errorContext));
    }
    try {
      byte[] plaintext;
      synchronized (this) {
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            new GCMParameterSpec(TAG_LENGTH_BITS, encrypted, 0, NONCE_LENGTH));
        plaintext = cipher.doFinal(encrypted, NONCE_LENGTH, encrypted.length - NONCE_LENGTH);
      }

      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          String.format("Decryption failed %s: wrong key or corrupted value.", errorContext), e);
    }
  }

  private static byte[] parseKey(String base64Key) {
    byte[] key;
    try {
      key = Base64.getDecoder().decode(base64Key);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("The encryption key is not valid Base64.", e);
    }
    if (key.length != KEY_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "The encryption key must be %d bytes long (AES-256), found %d bytes.",
              KEY_LENGTH, key.length));
    }
    return key;
  }
}
