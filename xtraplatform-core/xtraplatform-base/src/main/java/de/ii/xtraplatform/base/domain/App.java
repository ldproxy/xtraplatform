package de.ii.xtraplatform.base.domain;

import dagger.BindsInstance;
import dagger.Lazy;
import java.util.Set;

public interface App {

  AppContext appContext();

  Lazy<Set<AppLifeCycle>> lifecycle();

  interface Builder {
    @BindsInstance
    Builder appContext(AppContext appContext);
    App build();
  }
}
