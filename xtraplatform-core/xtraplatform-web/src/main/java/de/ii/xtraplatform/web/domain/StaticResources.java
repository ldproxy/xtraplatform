package de.ii.xtraplatform.web.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;

@AutoMultiBind
public interface StaticResources {

  String getResourcePath();

  String getUrlPath();

  default boolean isEnabled() {
    return true;
  }
}
