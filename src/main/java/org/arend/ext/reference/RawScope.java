package org.arend.ext.reference;

import javax.annotation.Nullable;

public interface RawScope {
  @Nullable RawRef resolveName(String name);
  @Nullable RawScope getSubscope(String... path);
}
