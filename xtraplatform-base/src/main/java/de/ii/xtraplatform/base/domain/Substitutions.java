/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import de.ii.xtraplatform.docs.DocFile;
import org.apache.commons.text.StringSubstitutor;

/**
 * @langEn # Substitutions
 *     <p>Configuration files (cfg.yml, entities, values) may contain placeholders that are replaced
 *     at runtime by variables from the environment or other sources.
 *     <p>## Syntax
 *     <p>*Substitution*
 *     <p><code>
 * ```yml
 * connectionInfo:
 *   host: ${db.host}
 * ```
 *     </code>
 *     <p>*Substitution with default value*
 *     <p><code>
 * ```yml
 * connectionInfo:
 *   host: ${db.host:-localhost}
 * ```
 *     </code>
 *     <p>### Substitutions in values
 *     <p>Substitution values can contain placeholders themselves, these will also be replaced:
 *     <p><code>
 * ```yml
 * connectionInfo:
 *   host: ${db.host:-localhost:${db.port:-5432}}
 * ```
 *     </code>
 *     <p>## Sources
 *     <p>### Environment variables
 *     <p>Environment variables are the primary source for substitutions. The placeholder name is
 *     converted to uppercase and dots are replaced by underscores. So the placeholder `db.host` in
 *     the examples above would be replaced by the value of the environment variable `DB_HOST`.
 *     <p>### cfg.yml
 *     <p>A secondary source for substitutions are `cfg.yml` files. These substitutions are only
 *     applied to entities and values. The values are defined under the `substitutions` key as a
 *     nested object structure. So a value for the placeholder `db.host` would be defined like this:
 *     <p><code>
 * ```yml
 * substitutions:
 *   db:
 *     host: other:5433
 * ```
 *     </code>
 * @langDe # Substitutionen
 *     <p>Konfigurationsdateien (cfg.yml, entities, values) können Platzhalter enthalten, die zur
 *     Laufzeit durch Variablen aus der Umgebung oder anderen Quellen ersetzt werden.
 *     <p>## Syntax
 *     <p>*Substitution*
 *     <p><code>
 * ```yml
 * connectionInfo:
 *   host: ${db.host}
 * ```
 *     </code>
 *     <p>*Substitution with default value*
 *     <p><code>
 * ```yml
 * connectionInfo:
 *   host: ${db.host:-localhost}
 * ```
 *     </code>
 *     <p>### Substitutionen in Werten
 *     <p>Substitutionswerte können selbst Platzhalter enthalten, diese werden ebenfalls ersetzt:
 *     <p><code>
 * ```yml
 * connectionInfo:
 *   host: ${db.host:-localhost:${db.port:-5432}}
 * ```
 *     </code>
 *     <p>## Quellen
 *     <p>### Umgebungsvariablen
 *     <p>Umgebungsvariablen sind die primäre Quelle für Substitutionen. Der Platzhaltername wird in
 *     Großbuchstaben umgewandelt und Punkte werden durch Unterstriche ersetzt. Der Platzhalter
 *     `db.host` in den obigen Beispielen würde also durch den Wert der Umgebungsvariable `DB_HOST`
 *     ersetzt.
 *     <p>### cfg.yml
 *     <p>Eine sekundäre Quelle für Substitutionen sind `cfg.yml`-Dateien. Diese Substitutionen
 *     werden nur auf Entities und Values angewendet. Die Werte sind unter dem Schlüssel
 *     `substitutions` als verschachtelte Objektstruktur definiert. Ein Wert für den Platzhalter
 *     `db.host` würde also so definiert:
 *     <p><code>
 * ```yml
 * substitutions:
 *   db:
 *     host: other:5433
 * ```
 *     </code>
 */
@DocFile(path = "application/20-configuration", name = "95-substitutions.md")
public interface Substitutions {

  StringSubstitutor getSubstitutor(boolean strict, boolean substitutionInVariables);
}
