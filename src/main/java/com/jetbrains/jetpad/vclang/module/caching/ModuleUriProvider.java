package com.jetbrains.jetpad.vclang.module.caching;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;

public interface ModuleUriProvider<SourceIdT> {
  @Nonnull URI getUri(SourceIdT sourceId);
  @Nullable SourceIdT getModuleId(URI sourceUrl);
}
