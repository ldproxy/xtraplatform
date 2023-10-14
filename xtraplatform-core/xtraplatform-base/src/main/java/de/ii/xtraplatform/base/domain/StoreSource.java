/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @langEn # Store (new)
 *     <p>::: info This is the documentation of the new store that was introduced in `v3.5`. For the
 *     old store that will be removed in `v4.0` see [Store](40-store.md). :::
 *     <p>The store represents all files that make up a deployment of ldproxy besides the
 *     application itself. That includes all configuration files but also other resources like file
 *     databases or caches. In the most simple case all of these files will exist in the local data
 *     directory, but that for example would not be a scalable approach for cloud environments.
 *     <p>The flexibility to meet such different demands is provided by store sources, which in
 *     theory allow to integrate any imaginable local or remote file source. The store can be
 *     composed of any number of sources, how that works is described in the following paragraphs.
 *     <p>## Options
 *     <p>These are the configuration options for key `store` in the global configuration.
 *     <p>{@docTable:properties}
 *     <p>## Store Sources
 *     <p>### Options
 *     <p>{@docTable:sourceProperties}
 *     <p>### Source Types
 *     <p>Store sources may have different source types, which allows to integrate any local or
 *     remote files.
 *     <p>#### `FS`
 *     <p>Access files from the local file system. `src` must be a path to a directory or a ZIP
 *     file. The path can either be absolute or relative to the data directory. `FS` directory
 *     sources are writable by default.
 *     <p>#### `HTTP`
 *     <p>Access files from a web server. `src` must be a valid URL pointing to a ZIP file. Can be
 *     neither writable nor watchable.
 *     <p>#### `GITHUB`
 *     <p>Access files from a GitHub repository. Convenience wrapper for `HTTP`. `src` must be a
 *     relative path composed of the organization, the repository and an optional branch (default is
 *     `main`), e.g. `ldproxy/demo:main`.
 *     <p>#### `GITLAB`
 *     <p>Access files from a GitLab repository. Convenience wrapper for `HTTP`. `src` must be a
 *     relative path composed of an optional host (default is `gitlab.com`), the organization, the
 *     repository and an optional branch (default is `main`), e.g. `my-gitlab/ldproxy/demo:main`.
 *     <p>#### `GITEA`
 *     <p>Access files from a Gitea repository. Convenience wrapper for `HTTP`. `src` must be a
 *     relative path composed of a host, the organization, the repository and an optional branch
 *     (default is `main`), e.g. `my-gitea/ldproxy/demo:main`.
 *     <p>### Content Types
 *     <p>Store sources may have different content types, which allows a fine-granular composition
 *     of the store. The `ALL` type would be sufficient since it contains all other types, but then
 *     all sources would have to follow the required directory structure. The other types basically
 *     allow to include files from an arbitrary directory structure, which may just be convenient or
 *     even necessary when the files are also used in another context.
 *     <p>#### `ALL`
 *     <p>Store sources with content type `ALL` are a container for these other content types:
 *     <p><code>
 * - `CFG` in path `cfg.yml`
 * - `ENTITIES` in path `entities/`
 * - `RESOURCES` in path `resources/`
 *     </code>
 *     <p>#### `CFG`
 *     <p>Store sources with content type `CFG` contain a single YAML file with global configuration
 *     settings.
 *     <p>#### `ENTITIES`
 *     <p>Entities make up the user-defined part of the application, for example APIs and Data
 *     Providers. The configuration of these entities is defined in YAML files.
 *     <p>Store sources with content type `ENTITIES` are a container for these other content types:
 *     <p><code>
 * - `INSTANCES` in path `instances/`
 * - `DEFAULTS` in path `defaults/`
 * - `OVERRIDES` in path `overrides/`
 *     </code>
 *     <p>#### `INSTANCES`
 *     <p>Store sources with content type `INSTANCES` contain the main definitions of entities. The
 *     paths are made up of an entity type and an entity id, for example `services/foo.yml`. An
 *     instance has to be unique, so it can only be defined once across all store sources.
 *     <p>#### `DEFAULTS`
 *     <p>Store sources with content type `DEFAULTS` may contain default configurations for the
 *     different entity types that are applied before the instance configuration. The paths are made
 *     up of an entity type and an optional entity subtype, for example `services.yml` would contain
 *     common defaults for all subtypes and `services/ogc_api.yml` would contain defaults for the
 *     subtype `ogc_api`. Some entity types may also allow another level of files with partial
 *     defaults, for example `services/ogc_api/metadata.yml`.
 *     <p>#### `OVERRIDES`
 *     <p>Store sources with content type `OVERRIDES` may contain override configurations for
 *     entities that are applied after the instance configuration. The paths have to match the
 *     instance paths, for example `services/foo.yml`.
 *     <p>#### `RESOURCES`
 *     <p>Store sources with content type `RESOURCES` may contain any other files that are needed by
 *     the application. Some features may also need a writable source to work properly. The paths
 *     are defined by the components that need them, their documentation will state something like
 *     "are resources with path `foo/bar`".
 *     <p>#### `MULTI`
 *     <p>Store sources with content type `MULTI` are a container with a common root that contains a
 *     list of other content types. This type can be used for convenience for example for an
 *     `ALL`-like source with a different directory structure.
 *     <p>### Order
 *     <p>#### `CFG`
 *     <p>Global configuration files are read and merged in the order of the sources on startup.
 *     <p>#### `ENTITIES`
 *     <p>Entity configuration files are read in the order of the sources on startup. First all
 *     `DEFAULTS` are read and merged in the order of the sources. Then the `INSTANCES` are created
 *     in order of the sources. If an instance exists in multiple sources, the first one will win
 *     since an instance can only exist once. Duplicates will be logged as error. Last all
 *     `OVERRIDES` are applied to the instances in order of the sources.
 *     <p>#### `RESOURCES`
 *     <p>Resources are only accessed on-demand. When the application wants to read a resource with
 *     a specific path, the sources are checked in reverse order for the existence of that path. So
 *     if a path exists in more than one source, the one defined later will win. When the
 *     application wants to write a resource with a specific path, the sources are checked in
 *     reverse order for the first that is writable for resources with the given prefix. So if more
 *     than one source could take the given resource, the one defined later will win.
 * @langDe # Store (neu)
 *     <p>::: info Dies ist die Dokumentation des neuen Stores, der in `v3.5` eingeführt wurde. Für
 *     den alten Store, der in `v4.0` entfernt werden wird, siehe [Store](40-store.md). :::
 *     <p>Der Store repräsentiert alle Dateien, die ein Deployment von ldproxy ausmachen, außer der
 *     Anwendung selbst. Dazu gehören alle Konfigurationsdateien, aber auch andere Ressourcen wie
 *     File-Datenbanken oder Caches. Im einfachsten Fall existieren alle diese Dateien im lokalen
 *     Daten-Verzeichnis, aber das wäre zum Beispiel für Cloud-Umgebungen kein skalierbarer Ansatz.
 *     <p>Die Flexibilität, um solchen unterschiedlichen Anforderungen gerecht zu werden, bieten
 *     Store Sources, die theoretisch die Integration aller erdenklichen lokalen oder entfernten
 *     Datei-Quellen ermöglichen. Der Store kann aus einer beliebigen Anzahl von Store Sources
 *     zusammengesetzt werden. Wie das funktioniert, wird in den folgenden Abschnitten beschrieben.
 *     <p>## Optionen
 *     <p>Dies sind die Konfigurationsoptionen für den Key `store` in der globalen Konfiguration.
 *     <p>{@docTable:properties}
 *     <p>## Store Sources
 *     <p>### Optionen
 *     <p>{@docTable:sourceProperties}
 *     <p>### Source Types
 *     <p>Store Sources können verschiedene Source Types haben, was die Integration beliebiger
 *     lokaler oder entfernter Dateien erlaubt.
 *     <p>#### `FS`
 *     <p>Zugriff auf Dateien aus dem lokalen Dateisystem. `src` muss ein Pfad zu einem Verzeichnis
 *     oder einer ZIP-Datei sein. Der Pfad kann entweder absolut oder relativ zum Datenverzeichnis
 *     sein. `FS` Verzeichnisse sind standardmäßig beschreibbar.
 *     <p>#### `HTTP`
 *     <p>Zugriff auf Dateien von einem Webserver. `src` muss eine gültige URL sein, die auf eine
 *     ZIP-Datei verweist. Kann nicht beschreibbar sein.
 *     <p>#### `GITHUB`
 *     <p>Zugriff auf Dateien aus einem GitHub-Repository. Convenience-Wrapper für `HTTP`. `src`
 *     muss ein relativer Pfad sein, der sich aus der Organisation, dem Repository und einem
 *     optionalen Branch (Standard ist `main`) zusammensetzt, z.B. `ldproxy/demo:main`.
 *     <p>#### `GITLAB`
 *     <p>Zugriff auf Dateien aus einem GitLab-Repository. Convenience-Wrapper für `HTTP`. `src`
 *     muss ein relativer Pfad sein, der aus einem optionalen Host (Standard ist `gitlab.com`), der
 *     Organisation, dem Repository und einem optionalen Branch (Standard ist `main`) besteht, z.B.
 *     `my-gitlab/ldproxy/demo:main`.
 *     <p>#### `GITEA`
 *     <p>Zugriff auf Dateien aus einem Gitea-Repository. Convenience-Wrapper für `HTTP`. `src` muss
 *     ein relativer Pfad sein, der aus einem Host, der Organisation, dem Repository und einem
 *     optionalen Branch (Standard ist `main`) besteht, z.B. `my-gitea/ldproxy/demo:main`.
 *     <p>### Content Types
 *     <p>Store Sources können verschiedene Content Types haben, was eine feingranulare
 *     Zusammensetzung des Stores erlaubt. Der `ALL` Typ wäre ausreichend, da er alle anderen Typen
 *     enthält, aber dann müssten alle Sources der geforderten Verzeichnisstruktur folgen. Die
 *     anderen Typen erlauben es, Dateien aus einer beliebigen Verzeichnisstruktur einzubinden, was
 *     vielleicht praktisch oder sogar notwendig sein kann, wenn die Dateien auch in einem anderen
 *     Kontext verwendet werden.
 *     <p>#### `ALL`
 *     <p>Store Sources mit dem Content Type `ALL` sind ein Container für diese anderen Content
 *     Types:
 *     <p><code>
 * - `CFG` im Pfad `cfg.yml`
 * - `ENTITIES` im Pfad `entities/`
 * - `RESOURCES` im Pfad `resources/`
 *     </code>
 *     <p>#### `CFG`
 *     <p>Store Sources mit dem Content Type `CFG` enthalten eine einzelne YAML-Datei mit globalen
 *     Konfigurations-Einstellungen.
 *     <p>#### `ENTITIES`
 *     <p>Entities bilden den benutzerdefinierten Teil der Anwendung, zum Beispiel APIs und
 *     Daten-Provider. Die Konfiguration der Entities erfolgt in YAML-Dateien.
 *     <p>Store Sources mit dem Content Type `ENTITIES` sind ein Container für diese anderen Content
 *     Types:
 *     <p><code>
 * - `INSTANCES` im Pfad `instances/`
 * - `DEFAULTS` im Pfad `defaults/`
 * - `OVERRIDES` im Pfad `overrides/`
 *     </code>
 *     <p>#### `INSTANCES`*
 *     <p>Store Sources mit dem Content Typ `INSTANCES` enthalten die Haupt-Definitionen von
 *     Entities. Die Pfade setzen sich aus einem Entity-Typ und einer Entity-Id zusammen, zum
 *     Beispiel `services/foo.yml`. Eine Instanz muss einzigartig sein, sie kann also über alle
 *     Store Sources hinweg nur einmal definiert sein.
 *     <p>#### `DEFAULTS`
 *     <p>Store Sources mit dem Content Type `DEFAULTS` können Default-Konfigurationen für die
 *     verschiedenen Entity-Typen enthalten, die vor der Instanz-Konfiguration angewendet werden.
 *     Die Pfade bestehen aus einem Entity-Typ und einem optionalen Entity-Sub-Typ, zum Beispiel
 *     würde `services.yml` allgemeine Defaults für alle Sub-Typen enthalten und
 *     `services/ogc_api.yml` würde Defaults für den Sub-Typ `ogc_api`. Einige Entity-Typen können
 *     auch eine weitere Ebene von Dateien mit partiellen Defaults erlauben, zum Beispiel
 *     `services/ogc_api/metadata.yml`.
 *     <p>#### `OVERRIDES`
 *     <p>Store Source mit dem Content Type `OVERRIDES` können Override-Konfigurationen für Entities
 *     enthalten, die nach der Instanz-Konfiguration angewendet werden. Die Pfade müssen mit den
 *     Instanz-Pfaden übereinstimmen, zum Beispiel `services/foo.yml`.
 *     <p>#### `RESOURCES`
 *     <p>Store Sources mit dem Content Type `RESOURCES` können alle anderen Dateien enthalten, die
 *     von der Anwendung benötigt werden. Einige Funktionen benötigen möglicherweise auch eine
 *     beschreibbare Store Source, um richtig zu funktionieren. Die Pfade werden von den Komponenten
 *     definiert, die sie benötigen, ihre Dokumentation wird etwas enthalten wie "sind Ressourcen
 *     mit dem Pfad `foo/bar`".
 *     <p>#### `MULTI`
 *     <p>Store Sources mit dem Content Type `MULTI` sind ein Container mit einer gemeinsamen
 *     Wurzel, die eine Liste von anderen Content Types enthält. Dieser Typ kann der Einfachheit
 *     halber verwendet werden, zum Beispiel für eine `ALL`-ähnliche Store Source mit einer anderen
 *     Verzeichnisstruktur.
 *     <p>### Sortierung
 *     <p>#### `CFG`
 *     <p>Globale Konfigurationsdateien werden beim Start in der Reihenfolge der Store Sources
 *     gelesen und zusammengeführt.
 *     <p>#### `ENTITIES`
 *     <p>Entity-Konfigurationsdateien werden beim Start in der Reihenfolge der Store Sources
 *     gelesen. Zuerst werden alle `DEFAULTS` gelesen und in der Reihenfolge der Store Sources
 *     zusammengeführt. Dann werden die `INSTANCES` in der Reihenfolge der Store Sources erstellt.
 *     Wenn eine Instanz in mehreren Store Sources existiert, wird nur die erste erstellt da eine
 *     Instanz nur einmal existieren kann. Duplikate werden als Fehler geloggt. Zuletzt werden alle
 *     `OVERRIDES` auf die Instanzen in der Reihenfolge der Store Sources angewendet.
 *     <p>#### `RESOURCES`
 *     <p>Ressourcen werden nur bei Bedarf abgerufen. Wenn die Anwendung eine Ressource mit einem
 *     bestimmten Pfad lesen will, werden die Store Sources in umgekehrter Reihenfolge auf das
 *     Vorhandensein dieses Pfades überprüft. Wenn also ein Pfad in mehr als einer Store Source
 *     vorhanden ist, gewinnt die später definierte Store Source. Wenn die Anwendung eine Ressource
 *     mit einem bestimmten Pfad schreiben will, werden die Store Sources in umgekehrter Reihenfolge
 *     nach der ersten Quelle durchsucht, die für Ressourcen mit dem angegebenen Präfix beschreibbar
 *     ist. Wenn also mehr als eine Quelle für die angegebene Ressource in Frage kommt, gewinnt
 *     diejenige, die später definiert wurde.
 * @langEn ### Examples
 *     <p>#### Entity instances from local ZIP file
 * @langDe ### Beispiele
 *     <p>#### Entity Instanzen aus lokaler ZIP-Datei
 * @langAll <code>
 * ```yml
 * store:
 *   sources:
 *   - type: FS
 *     content: INSTANCES
 *     src: /path/to.zip
 * ```
 *     </code>
 *     <p>*ZIP*
 *     <p><code>
 * ```yml
 * services/
 *   foo.yml
 *   bar.yml
 * ```
 *     </code>
 * @langEn #### GeoPackages from remote ZIP file
 * @langDe #### GeoPackages aus entfernter ZIP-Datei
 * @langAll <code>
 * ```yml
 * store:
 *   sources:
 *   - type: HTTP
 *     content: RESOURCES
 *     prefix: features
 *     src: https://example.org/path/to.zip
 * ```
 *     </code>
 *     <p>*ZIP*
 *     <p><code>
 * ```yml
 * foo.gpkg
 * bar.gpkg
 * ```
 *     </code>
 * @langEn #### Subfolder from a GitHub Repository
 * @langDe #### Unterverzeichnis aus GitHub Repository
 * @langAll <code>
 * ```yml
 * store:
 *   sources:
 *   - type: GITHUB
 *     src: org/repo
 *     archiveRoot: /path/to/store
 * ```
 *     </code>
 * @langEn #### Data directory with old store layout
 *     <p>You could use this if you want to keep using the old layout with `v4`.
 * @langEn #### Datenverzeichnis mit altem Store-Layout
 *     <p>Kann benutzt werden falls das alte Layout weiterhin mit `v4` verwendet werden soll.
 * @langAll <code>
 * ```yml
 * store:
 *   sources:
 *   - type: FS
 *     content: MULTI
 *     src: /old/data
 *     parts:
 *     - content: CFG
 *       src: cfg.yml
 *     - content: ENTITIES
 *       src: store
 *     - content: RESOURCES
 *       src: api-resources
 *     - content: RESOURCES
 *       src: api-resources/resources
 *       prefix: api-resources
 *     - content: RESOURCES
 *       src: store/resources
 *       mode: RO
 *     - content: RESOURCES
 *       src: cache/tiles
 *       prefix: tiles
 *     - content: RESOURCES
 *       src: cache/tiles3d
 *       prefix: tiles3d
 *     - content: RESOURCES
 *       src: proj
 *       prefix: proj
 *     - content: RESOURCES
 *       src: templates/html
 *       prefix: html/templates
 * ```
 *     </code>
 * @langDe # Store (neu)
 * @ref:properties {@link de.ii.xtraplatform.base.domain.ImmutableStoreConfiguration}
 * @ref:sourceProperties {@link de.ii.xtraplatform.base.domain.ImmutableStoreSourceDummy}
 */
@DocFile(
    path = "application",
    name = "41-store-new.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:properties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "sourceProperties",
          rows = {@DocStep(type = Step.JSON_PROPERTIES)},
          columnSet = ColumnSet.JSON_PROPERTIES),
    })
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = StoreSource.TYPE_PROP,
    // defaultImpl = StoreSourcePartial.class,
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = StoreSourceFs.class, name = Type.FS_KEY),
  @JsonSubTypes.Type(value = StoreSourceHttp.class, name = Type.HTTP_KEY),
  @JsonSubTypes.Type(value = StoreSourceGithub.class, name = StoreSourceGithub.KEY),
  @JsonSubTypes.Type(value = StoreSourceGitea.class, name = StoreSourceGitea.KEY),
  @JsonSubTypes.Type(value = StoreSourceGitlab.class, name = StoreSourceGitlab.KEY),
  @JsonSubTypes.Type(value = StoreSourceDefault.class, name = StoreSourceDefault.KEY),
  @JsonSubTypes.Type(value = StoreSourceEmpty.class, name = Type.EMPTY_KEY),
  @JsonSubTypes.Type(value = StoreSourceFsV3.class, name = StoreSourceFsV3.KEY),
  @JsonSubTypes.Type(value = StoreSourceFsV3Auto.class, name = StoreSourceFsV3Auto.KEY),
  @JsonSubTypes.Type(value = StoreSourceHttpV3.class, name = StoreSourceHttpV3.KEY),
  @JsonSubTypes.Type(value = StoreSourceGithubV3.class, name = StoreSourceGithubV3.KEY),
  @JsonSubTypes.Type(value = StoreSourceS3.class, name = "S3"),
})
public interface StoreSource {

  String TYPE_PROP = "type";
  String MODE_PROP = "mode";
  String ZIP_SUFFIX = ".zip";

  enum Type {
    EMPTY(Type.EMPTY_KEY),
    FS(Type.FS_KEY),
    HTTP(Type.HTTP_KEY);

    public static final String EMPTY_KEY = "EMPTY";
    public static final String FS_KEY = "FS";
    public static final String HTTP_KEY = "HTTP";

    private final String key;

    Type(String key) {
      this.key = key;
    }

    public String key() {
      return key;
    }
  }

  enum Content {
    ALL,
    NONE,
    CFG,
    ENTITIES,
    DEFAULTS,
    INSTANCES_OLD,
    INSTANCES,
    OVERRIDES,
    VALUES,
    RESOURCES,
    MULTI;

    public String getPrefix() {
      return Objects.equals(this, ALL)
          ? ""
          : Objects.equals(this, INSTANCES_OLD) ? "entities" : this.name().toLowerCase(Locale.ROOT);
    }

    public String getLabel() {
      return this.name().charAt(0) + this.name().substring(1).toLowerCase(Locale.ROOT);
    }

    public boolean isEvent() {
      return Objects.equals(this, DEFAULTS)
          || Objects.equals(this, INSTANCES_OLD)
          || Objects.equals(this, INSTANCES)
          || Objects.equals(this, OVERRIDES);
    }

    public static boolean isEvent(String prefix) {
      return Objects.equals(prefix, DEFAULTS.getPrefix())
          || Objects.equals(prefix, INSTANCES_OLD.getPrefix())
          || Objects.equals(prefix, INSTANCES.getPrefix())
          || Objects.equals(prefix, OVERRIDES.getPrefix());
    }
  }

  enum Mode {
    RO,
    RW,
  }

  /**
   * @langEn The [Source Type](#source-types).
   * @langDe Der [Source Type](#source-types).
   * @since v3.5
   */
  @JsonProperty(StoreSource.TYPE_PROP)
  String getType();

  /**
   * @langEn The [Content Type](#content-types).
   * @langDe Der [Content Type](#content-types).
   * @default ALL
   * @since v3.5
   */
  @JsonProperty("content")
  @Value.Default
  default Content getContent() {
    return Content.ALL;
  }

  /**
   * @langEn Set to `RW` to make the source writable.
   * @langDe Kann auf `RW` gesetzt werden, um die Source schreibbar zu machen.
   * @default RO
   * @since v3.5
   */
  @JsonProperty(StoreSource.MODE_PROP)
  @Value.Default
  default Mode getDesiredMode() {
    return Mode.RO;
  }

  @JsonIgnore
  @Value.Derived
  default Mode getMode() {
    return isArchive() ? Mode.RO : getDesiredMode();
  }

  /**
   * @langEn The source path, see [Source Types](#source-types) for details.
   * @langDe Der Source-Pfad, siehe [Source Types](#source-types) für Details.
   * @since v3.5
   */
  @JsonProperty("src")
  String getSrc();

  /**
   * @langEn Prefix for file paths from a source, may be needed to align directory structures.
   * @langDe Prefix für Datei-Pfade einer Source, kann benötigt werden, um Verzeichnisstrukturen
   *     anzugleichen.
   * @default null
   * @since v3.5
   */
  @JsonProperty("prefix")
  Optional<String> getPrefix();

  /**
   * @langEn Can be set to use a subdirectory from a ZIP file.
   * @langDe Kann gesetzt werden, um ein Unterverzeichnis aus einer ZIP-Datei zu verwenden.
   * @default /
   * @since v3.5
   */
  @JsonProperty("archiveRoot")
  @Value.Default
  default String getArchiveRoot() {
    return "/";
  }

  @DocIgnore
  @Value.Default
  default boolean getArchiveCache() {
    return true;
  }

  @DocIgnore
  boolean isWatchable();

  /**
   * @langEn List of partial sources for content type [MULTI](#multi).
   * @langDe Liste von partiellen Sources für Content Type [MULTI](#multi).
   * @default []
   * @since v3.5
   */
  @JsonProperty("parts")
  List<StoreSourcePartial> getParts();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isWritable() {
    return getMode() == Mode.RW;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isSingleContent() {
    return getContent() != Content.ALL
        && getContent() != Content.MULTI
        && getContent() != Content.ENTITIES;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isArchive() {
    return getSrc().toLowerCase(Locale.ROOT).endsWith(ZIP_SUFFIX);
  }

  @JsonIgnore
  @Value.Default
  @Value.Auxiliary
  default String getLabel() {
    return String.format("%s[%s]", getType(), Path.of(getSrc()));
  }

  @JsonIgnore
  @Value.Lazy
  @Value.Auxiliary
  default String getLabelSpaces() {
    return getLabel().replace('[', ' ').replace("]", "");
  }

  @JsonDeserialize(builder = ImmutableStoreSourcePartial.Builder.class)
  default List<StoreSource> explode() {
    return List.of(this);
  }

  default Path getPath(Content content) {
    Path path = Path.of(getSrc());

    if (isSingleContent()) {
      return path;
    }

    if (content.isEvent() && getContent() == Content.ALL) {
      return path.resolve(Content.ENTITIES.getPrefix()).resolve(content.getPrefix());
    }

    return path.resolve(content.getPrefix());
  }
}
