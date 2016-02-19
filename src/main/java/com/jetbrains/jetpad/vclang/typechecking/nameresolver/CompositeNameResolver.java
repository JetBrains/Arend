package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;

import java.util.ArrayList;
import java.util.List;

public class CompositeNameResolver implements NameResolver {
  private final List<NameResolver> myNameResolvers;

  public CompositeNameResolver() {
    myNameResolvers = new ArrayList<>(2);
  }

  public CompositeNameResolver(NameResolver nameResolver1, NameResolver nameResolver2) {
    myNameResolvers = new ArrayList<>(2);
    myNameResolvers.add(nameResolver1);
    myNameResolvers.add(nameResolver2);
  }

  public CompositeNameResolver(List<NameResolver> nameResolvers) {
    myNameResolvers = nameResolvers;
  }

  public void pushNameResolver(NameResolver nameResolver) {
    myNameResolvers.add(nameResolver);
  }

  public void popNameResolver() {
    myNameResolvers.remove(myNameResolvers.size() - 1);
  }

  @Override
  public NamespaceMember locateName(String name) {
    for (int i = myNameResolvers.size() - 1; i >= 0; --i) {
      NamespaceMember result = myNameResolvers.get(i).locateName(name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    for (int i = myNameResolvers.size() - 1; i >= 0; --i) {
      NamespaceMember result = myNameResolvers.get(i).getMember(parent, name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}
