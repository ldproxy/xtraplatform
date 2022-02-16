package de.ii.xtraplatform.base.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;

@AutoMultiBind
public interface AppLifeCycle {

  default int getPriority() {
    return 1000;
  }

  default void onStart() {}

  default void onStop() {}
}
