package org.arend.ext.reference;

public interface RawScope {
  RawRef resolveName(String name);
  RawScope getSubscope(String... path);
}
