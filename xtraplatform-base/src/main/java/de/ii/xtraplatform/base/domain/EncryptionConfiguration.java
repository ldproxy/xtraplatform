/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @langEn # Encryption
 *     <p>Encryption configuration for sensitive data. May be used to encrypt data at rest, e.g.
 *     feature properties or resources. The symmetric key must be 32 (AES-256).
 *     <p>## Options
 *     <p>{@docTable:properties}
 * @langDe # Verschlüsselung
 *     <p>Konfiguration der Verschlüsselung für sensible Daten. Kann verwendet werden, um Daten im
 *     Ruhezustand zu verschlüsseln, z.B. Feature-Properties oder Ressourcen. Der symmetrische
 *     Schlüssel muss 32 Bytes lang sein (AES-256).
 *     <p>## Optionen
 *     <p>{@docTable:properties}
 * @ref:cfgProperties {@link ImmutableEncryptionConfiguration}
 */
@DocFile(
    path = "application/20-configuration",
    name = "130-encryption.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfgProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES)
    })
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableEncryptionConfiguration.class)
public interface EncryptionConfiguration {

  /**
   * @langEn A file with the symmetric key for encryption. The key must be 32 bytes long (AES-256).
   * @langDe Eine Datei mit dem symmetrischen Schlüssel für die Verschlüsselung. Der Schlüssel muss
   *     32 Bytes lang sein (AES-256).
   * @since v4.9
   * @default null
   */
  Optional<String> getKeyFile();

  /**
   * @langEn Instead of a key file, the key might also be provided inline, encoded as Base64. The
   *     decoded key must be 32 bytes long (AES-256). It is recommended to reference an environment
   *     variable, e.g. `${ENCRYPTION_KEY}`, instead of storing the key in the configuration.
   * @langDe Anstatt einer Schlüsseldatei kann der Schlüssel auch inline angegeben werden, kodiert
   *     als Base64. Der dekodierte Schlüssel muss 32 Bytes lang sein (AES-256). Es wird empfohlen,
   *     eine Umgebungsvariable zu referenzieren, z.B. `${ENCRYPTION_KEY}`, statt den Schlüssel in
   *     der Konfiguration zu speichern.
   * @since v4.9
   * @default null
   */
  Optional<String> getKey();
}
