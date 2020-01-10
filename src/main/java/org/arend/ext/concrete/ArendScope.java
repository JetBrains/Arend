package org.arend.ext.concrete;

import javax.annotation.Nullable;

public interface ArendScope {
  @Nullable ArendRef resolveName(String name);
  @Nullable ArendScope getSubscope(String... path);
}
