package de.ii.xtraplatform.base.domain;

import dagger.BindsInstance;
import java.util.Set;

public interface App {

  AppContext appContext();

  Set<Lifecycle> lifecycle();

  interface Builder {
    @BindsInstance
    Builder appContext(AppContext appContext);
    App build();
  }
}
