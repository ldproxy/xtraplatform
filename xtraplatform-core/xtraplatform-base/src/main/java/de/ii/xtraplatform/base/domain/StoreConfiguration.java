/*
 * Copyright 2019-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.base.domain.StoreSource.Content;
import de.ii.xtraplatform.base.domain.StoreSource.Mode;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

/**
 * @langEn # Store
 *     <p>The store contains configuration objects.
 *     <p>## Configuration
 *     <p><code>
 * |Option |Type |Default |Description
 * | --- | --- | --- | ---
 * |`mode` |enum |`READ_WRITE` |`READ_WRITE` or `READ_ONLY`. Dictates if the application may apply changes to the store.
 * |`additionalLocations` |array |`[]` | List of paths with [additional directories](#additional-locations).
 * </code>
 *     <p>## Store structure
 *     <p>Each configuration object has a type, an optional sub-type, and an id unique to the type
 *     (see [configuration-object-types](README.md#configuration-object-types)). The path to the
 *     corresponding configuration file then looks like this:
 *     <p><code>
 * ```text
 * store/entities/{type}/{id}.yml
 * ```
 * </code>
 *     <p>Defaults for all configuration objects of a type or sub-type can optionally be set in such
 *     files:
 *     <p><code>
 * ```text
 * store/defaults/{type}.yml
 * store/defaults/{type}.{sub-type}.yml
 * ```
 * </code>
 *     <p>You can also create overrides for configuration objects. These have e.g. the purpose to
 *     make environment specific adjustments without having to change the main configuration files.
 *     The path to the overrides looks like this:
 *     <p><code>
 * ```text
 * store/overrides/{type}/{id}.yml
 * ```
 * </code>
 *     <p>Defaults, configuration objects and overrides are read and merged in this order. This
 *     means that information in the configuration object overwrites information in defaults and
 *     information in overrides overwrites both information in defaults and in the configuration
 *     object.
 *     <p>The merging functions also for nested structures, i.e. one does not have to repeat data in
 *     the different files, but can set e.g. in Overrides only purposefully the data, which one
 *     wants to overwrite.
 *     <p>The merged configuration objects must contain then all obligation data, otherwise it comes
 *     with the start to an error.
 *     <p>## Additional directories
 *     <p>For fixed predefined or standardized configuration objects it may make sense to make
 *     environment specific adjustments in a separate directory. One or more such directories can be
 *     configured with `additionalLocations`. The paths to be specified can be either absolute or
 *     relative to the data directory, e.g.:
 *     <p><code>
 * ```yml
 * store:
 *   additionalLocations:
 *     - env/test
 * ```
 * </code>
 *     <p>Such a directory can then again contain defaults and overrides, e.g.:
 *     <p><code>
 * ```text
 * env/test/defaults/{type}.yml
 * env/test/overrides/{type}/{id}.yml
 * ```
 * </code>
 *     <p>The merge order for all listed paths would then look like this:
 *     <p><code>
 * ```text
 * store/defaults/{type}.yml
 * store/defaults/{type}.{sub-type}.yml
 * env/test/defaults/{type}.yml
 * store/entities/{type}/{id}.yml
 * store/overrides/{type}/{id}.yml
 * env/test/overrides/{type}/{id}.yml
 * ```
 * </code>
 *     <p>## Splitting of defaults and overrides
 *     <p>Defaults and overrides can be split into smaller files, e.g. to increase the clarity. The
 *     splitting follows the object structure in the configuration objects.
 *     <p><code>
 * ```yml
 * key1:
 *   key2:
 *     key3: value1
 * ```
 * </code>
 *     <p>To set a default or override for this value, the files described above could be used:
 *     <p><code>
 * ```text
 * store/defaults/{type}.{sub-type}.yml
 * store/overrides/{type}/{id}.yml
 * ```
 * </code>
 *     <p>However, separate files can be created for the object `key1` or the object `key2`, for
 *     example:
 *     <p><code>
 * ```text
 * store/defaults/{type}/{sub-type}/key1.yml
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```yml
 * key2:
 *   key3: value2
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```text
 * store/overrides/{type}/{id}/key1/key2.yml
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```yml
 * key3: value3
 * ```
 * </code>
 *     <p>So the path of the object can be moved from the YAML to the file system, so to speak.
 *     <p>The order of merging follows the specificity of the path. For all paths listed, it would
 *     look like this:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.{sub-typ}.yml
 * store/defaults/{typ}/{sub-typ}/key1.yml
 * store/entities/{typ}/{id}.yml
 * store/overrides/{typ}/{id}.yml
 * store/overrides/{typ}/{id}/key1/key2.yml
 * ```
 * </code>
 *     <p><a name="array-exceptions"></a>
 *     <p>There are some special cases where splitting is not only allowed based on object paths,
 *     but also e.g. for uniquely referenceable array elements. These special cases are discussed in
 *     the description of [configuration-object-types](README.md#configuration-object-types) in the
 *     ["special-cases"](README.md#special-cases) section.
 * @langDe # Store
 *     <p>Der Store enthält Konfigurationsobjekte.
 *     <p>## Konfiguration
 *     <p><code>
 * |Option |Typ |Default |Beschreibung
 * | --- | --- | --- | ---
 * |`mode` |enum |`READ_WRITE` |`READ_WRITE` oder `READ_ONLY`. Bestimmt ob die Applikation Änderungen am Store vornehmen darf.
 * |`additionalLocations` |array |`[]` | Liste von Pfaden mit [zusätzlichen Verzeichnissnen](#additional-locations).
 * </code>
 *     <p>## Struktur des Store
 *     <p>Jedes Konfigurationsobjekt hat einen Typ, einen optionalen Sub-Typ sowie eine für den Typ
 *     eindeutige Id (siehe [Konfigurationsobjekt-Typen](README.md#configuration-object-types)). Der
 *     Pfad zur entsprechenden Konfigurationsdatei sieht dann so aus:
 *     <p><code>
 * ```text
 * store/entities/{typ}/{id}.yml
 * ```
 * </code>
 *     <p>Defaults für alle Konfigurationsobjekte eines Typs oder Sub-Typs können optional in
 *     solchen Dateien gesetzt werden:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.yml
 * store/defaults/{typ}.{sub-typ}.yml
 * ```
 * </code>
 *     <p>Außerdem kann man noch Overrides für Konfigurationsobjekte anlegen. Diese haben z.B. den
 *     Zweck, umgebungsspezifische Anpassungen vorzunehmen, ohne die Haupt-Konfigurationsdateien
 *     ändern zu müssen. Der Pfad zu den Overrides sieht so aus:
 *     <p><code>
 * ```text
 * store/overrides/{typ}/{id}.yml
 * ```
 * </code>
 *     <p>Defaults, Konfigurationsobjekte und Overrides werden in dieser Reihenfolge eingelesen und
 *     zusammengeführt. Das heißt Angaben im Konfigurationsobjekt überschreiben Angaben in Defaults
 *     und Angaben in Overrides überschreiben sowohl Angaben in Defaults als auch im
 *     Konfigurationsobjekt.
 *     <p>Das Zusammenführen funktioniert auch für verschachtelte Strukturen, d.h. man muss in den
 *     verschiedenen Dateien keine Angaben wiederholen, sondern kann z.B. in Overrides nur gezielt
 *     die Angaben setzen, die man überschreiben will.
 *     <p>Die zusammengeführten Konfigurationsobjekte müssen dann alle Pflichtangaben enthalten,
 *     ansonsten kommt es beim Start zu einem Fehler.
 *     <p>## Zusätzliche Verzeichnisse
 *     <p>Bei fest vordefinierten oder standardisierten Konfigurationsobjekten kann es Sinn machen,
 *     umgebungsspezifische Anpassungen in einem separaten Verzeichnis vorzunehmen. Ein oder mehrere
 *     solche Verzeichnisse können mit `additionalLocations` konfiguriert werden. Die anzugebenden
 *     Pfade können entweder absolut oder relativ zum Daten-Verzeichnis sein, also z.B.:
 *     <p><code>
 * ```yml
 * store:
 *   additionalLocations:
 *     - env/test
 * ```
 * </code>
 *     <p>Ein solches Verzeichnis kann dann wiederum Defaults und Overrides enthalten, also z.B.:
 *     <p><code>
 * ```text
 * env/test/defaults/{typ}.yml
 * env/test/overrides/{typ}/{id}.yml
 * ```
 * </code>
 *     <p>Die Reihenfolge der Zusammenführung für alle aufgeführten Pfade sähe dann so aus:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.yml
 * store/defaults/{typ}.{sub-typ}.yml
 * env/test/defaults/{typ}.yml
 * store/entities/{typ}/{id}.yml
 * store/overrides/{typ}/{id}.yml
 * env/test/overrides/{typ}/{id}.yml
 * ``
 * </code>
 *     <p>## Aufsplitten von Defaults und Overrides
 *     <p>Defaults und Overrides können in kleinere Dateien aufgesplittet werden, z.B. um die
 *     Übersichtlichkeit zu erhöhen. Die Aufsplittung folgt dabei der Objektstruktur in den
 *     Konfigurationsobjekten.
 *     <p><code>
 * ```yml
 * key1:
 *   key2:
 *     key3: value1
 * ```
 * </code>
 *     <p>Um ein Default oder Override für diesen Wert zu setzen, könnten die oben beschriebenen
 *     Dateien verwendet werden:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.{sub-typ}.yml
 * store/overrides/{typ}/{id}.yml
 * ```
 * </code>
 *     <p>Es können aber auch separate Dateien für das Objekt `key1` oder das Objekt `key2` angelegt
 *     werden, z.B.:
 *     <p><code>
 * ```text
 * store/defaults/{typ}/{sub-typ}/key1.yml
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```yml
 * key2:
 *   key3: value2
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```text
 * store/overrides/{typ}/{id}/key1/key2.yml
 * ```
 * </code>
 *     <p>
 *     <p><code>
 * ```yml
 * key3: value3
 * ```
 * </code>
 *     <p>Der Pfad des Objekts kann also sozusagen aus dem YAML ins Dateisystem verlagert werden.
 *     <p>Die Reihenfolge der Zusammenführung folgt der Spezifität des Pfads. Für alle aufgeführten
 *     Pfade sähe dann so aus:
 *     <p><code>
 * ```text
 * store/defaults/{typ}.{sub-typ}.yml
 * store/defaults/{typ}/{sub-typ}/key1.yml
 * store/entities/{typ}/{id}.yml
 * store/overrides/{typ}/{id}.yml
 * store/overrides/{typ}/{id}/key1/key2.yml
 * ```
 * </code>
 *     <p><a name="array-exceptions"></a>
 *     <p>Es gibt einige Sonderfälle, bei denen das Aufsplitten nicht nur anhand der Objektpfade
 *     erlaubt ist, sondern z.B. auch für eindeutig referenzierbare Array-Element. Auf diese
 *     Sonderfälle wird in der Beschreibung der
 *     [Konfigurationsobjekt-Typen](README.md#configuration-object-types) im Abschnitt
 *     ["Besonderheiten"](README.md#special-cases) eingegangen.
 */
@DocFile(
    path = "application",
    name = "40-store.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {@DocStep(type = Step.JSON_PROPERTIES)},
          columnSet = ColumnSet.JSON_PROPERTIES)
    })
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableStoreConfiguration.class)
public interface StoreConfiguration {

  String DEFAULT_LOCATION = "store";

  enum StoreMode {
    READ_WRITE,
    READ_ONLY,
    DISTRIBUTED,
  }

  /**
   * @langEn The store contains configuration objects.
   * @langDe `READ_WRITE` oder `READ_ONLY`. Bestimmt ob die Software Änderungen am Store vornehmen
   *     darf.
   * @default `READ_WRITE`
   */
  @Value.Default
  default StoreMode getMode() {
    return StoreMode.READ_WRITE;
  }

  /**
   * @langEn List of paths with [additional directories](#additional-locations).
   * @langDe Liste von Pfaden mit [zusätzlichen Verzeichnissnen](#additional-locations).
   * @default `[]`
   */
  @Deprecated(since = "3.3")
  List<String> getAdditionalLocations();

  @Value.Default
  default boolean isWatch() {
    return false;
  }

  @Value.Default
  default boolean isFailOnUnknownProperties() {
    return false;
  }

  @JsonMerge
  List<StoreSource> getSources();

  Optional<StoreFilters> getFilter();

  @JsonIgnore
  @Value.Derived
  default boolean isReadOnly() {
    return getMode() == StoreMode.READ_ONLY;
  }

  @JsonIgnore
  @Value.Derived
  default boolean isReadWrite() {
    return getMode() == StoreMode.READ_WRITE;
  }

  @JsonIgnore
  @Value.Derived
  default boolean isFiltered() {
    return getFilter().isPresent();
  }

  @Deprecated(since = "3.3")
  @Value.Check
  default StoreConfiguration backwardsCompatibility() {
    if (!getAdditionalLocations().isEmpty()) {
      return new ImmutableStoreConfiguration.Builder()
          .from(this)
          .additionalLocations(List.of())
          .addAllSources(
              getAdditionalLocations().stream()
                  .map(
                      location ->
                          new ImmutableStoreSourceFs.Builder()
                              .typeString(Type.FS.key())
                              .content(Content.ALL)
                              .desiredMode(Mode.RO)
                              .src(location)
                              .build())
                  .collect(Collectors.toList()))
          .build();
    }

    if (getMode() == StoreMode.READ_ONLY) {
      return new ImmutableStoreConfiguration.Builder()
          .from(this)
          .mode(StoreMode.READ_WRITE)
          .sources(
              getSources().stream()
                  .map(
                      source -> {
                        if (source instanceof StoreSourceDefaultV3) {
                          return new ImmutableStoreSourceDefaultV3.Builder()
                              .desiredMode(Mode.RO)
                              .build();
                        }
                        return source;
                      })
                  .collect(Collectors.toList()))
          .build();
    }

    return this;
  }
}
