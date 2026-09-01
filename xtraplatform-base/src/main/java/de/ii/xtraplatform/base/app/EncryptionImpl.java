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
import de.ii.xtraplatform.base.domain.Encryption;
import de.ii.xtraplatform.base.domain.EncryptionConfiguration;
import de.ii.xtraplatform.base.domain.LogContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Symmetric encryption for data. The stored representation is {@code nonce (12 bytes) || ciphertext
 * || tag (16 bytes)} using AES-256-GCM.
 *
 * <p>Cipher, key and RNG are resolved in the constructor, so that everything an operation needs is
 * in place as soon as this class is injected. It cannot be done in an {@link
 * de.ii.xtraplatform.base.domain.AppLifeCycle} callback: this class is provided by the encapsulated
 * component of the base layer, and that component's {@code AppLifeCycle} contributions do not reach
 * the set that the launcher starts, so the callback would never run.
 *
 * <p>The constructor never throws, for anything: it runs inside whichever injection first needs
 * encryption, and a failure there would abort that unrelated injection instead of reporting a
 * configuration problem. An unusable key or runtime is logged and leaves encryption disabled, which
 * fails the startup of every provider that declares encrypted properties.
 */
@AutoBind
@Singleton
@SuppressWarnings("PMD.AvoidSynchronizedStatement")
public class EncryptionImpl implements Encryption {

  private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionImpl.class);

  public static final int KEY_LENGTH = 32;
  public static final int NONCE_LENGTH = 12;
  public static final int TAG_LENGTH_BITS = 128;
  private static final String ALGORITHM = "AES";
  private static final String CIPHER = "AES/GCM/NoPadding";

  private final SecretKeySpec secretKey;
  private final Cipher cipher;
  private final SecureRandom random;

  @Inject
  EncryptionImpl(AppContext appContext) {
    this(appContext.getConfiguration().getEncryption(), appContext.getDataDir());
  }

  // For testing only
  EncryptionImpl(String key) {
    this(
        new EncryptionConfiguration() {
          @Override
          public Optional<String> getKeyFile() {
            return Optional.empty();
          }

          @Override
          public Optional<String> getKey() {
            return Optional.ofNullable(key);
          }
        },
        Path.of("").toAbsolutePath());
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private EncryptionImpl(EncryptionConfiguration configuration, Path dataDir) {
    SecretKeySpec parsedKey = null;
    Cipher gcmCipher = null;
    SecureRandom nonceSource = null;

    try {
      Optional<byte[]> keyBytes =
          configuration
              .getKeyFile()
              .filter(keyFile -> !keyFile.isBlank())
              .map(dataDir::resolve)
              .filter(Files::exists)
              .map(EncryptionImpl::parseKeyFile)
              .or(() -> configuration.getKey().map(EncryptionImpl::parseKeyBase64));

      if (keyBytes.isPresent()) {
        Cipher aesGcm = Cipher.getInstance(CIPHER);
        SecureRandom secureRandom = new SecureRandom();
        SecretKeySpec key = new SecretKeySpec(keyBytes.get(), ALGORITHM);

        // published together, so a failure cannot leave a partially set up instance behind
        gcmCipher = aesGcm;
        nonceSource = secureRandom;
        parsedKey = key;
      }
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "Encryption is disabled");
    }

    this.secretKey = parsedKey;
    this.cipher = gcmCipher;
    this.random = nonceSource;
  }

  @Override
  public boolean isEnabled() {
    return secretKey != null;
  }

  @Override
  public byte[] encrypt(byte[] data) {
    if (!isEnabled()) {
      throw new IllegalStateException("Encryption is not enabled.");
    }

    byte[] nonce = new byte[NONCE_LENGTH];
    random.nextBytes(nonce);
    try {
      byte[] ciphertext;
      synchronized (this) {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
        ciphertext = cipher.doFinal(data);
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
  public byte[] decrypt(byte[] encrypted, String errorContext) {
    if (!isEnabled()) {
      throw new IllegalStateException("Encryption is not enabled.");
    }

    if (encrypted.length <= NONCE_LENGTH + TAG_LENGTH_BITS / 8) {
      throw new IllegalStateException(
          String.format("Decryption failed%s: the stored value is too short.", errorContext));
    }
    try {
      byte[] decrypted;
      synchronized (this) {
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            new GCMParameterSpec(TAG_LENGTH_BITS, encrypted, 0, NONCE_LENGTH));
        decrypted = cipher.doFinal(encrypted, NONCE_LENGTH, encrypted.length - NONCE_LENGTH);
      }

      return decrypted;
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          String.format("Decryption failed %s: wrong key or corrupted value.", errorContext), e);
    }
  }

  private static byte[] parseKeyBase64(String base64Key) {
    byte[] key;
    try {
      key = Base64.getDecoder().decode(base64Key);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("the configured encryption key is not valid Base64", e);
    }
    return parseKey(key);
  }

  private static byte[] parseKeyFile(Path keyFile) {
    byte[] key;
    try {
      key = Files.readAllBytes(keyFile);
    } catch (IOException e) {
      throw new IllegalArgumentException("the configured encryption key file could not be read", e);
    }
    return parseKey(key);
  }

  private static byte[] parseKey(byte[] key) {
    if (key.length != KEY_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "the configured encryption key must be %d bytes long (AES-256), found %d bytes",
              KEY_LENGTH, key.length));
    }
    return key;
  }
}
