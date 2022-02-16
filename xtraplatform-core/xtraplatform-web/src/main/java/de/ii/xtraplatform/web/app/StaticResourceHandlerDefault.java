package de.ii.xtraplatform.web.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.web.domain.StaticResourceHandler;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//TODO: who else implements this? either make optional or multi
// remove
@Singleton
@AutoBind
public class StaticResourceHandlerDefault implements StaticResourceHandler {

  @Inject
  public StaticResourceHandlerDefault() {
  }

  @Override
  public boolean handle(String path, HttpServletRequest request,
      HttpServletResponse response) {
    return false;
  }
}
