package org.arend.naming.scope;

public class Scopes {
  private final Scope myExpressionScope;
  private final Scope myPLevelScope;
  private final Scope myHLevelScope;

  public final static Scopes EMPTY = new Scopes(EmptyScope.INSTANCE, EmptyScope.INSTANCE, EmptyScope.INSTANCE);

  public Scopes(Scope expressionScope, Scope pLevelScope, Scope hLevelScope) {
    myExpressionScope = expressionScope;
    myPLevelScope = pLevelScope;
    myHLevelScope = hLevelScope;
  }

  public Scope getExpressionScope() {
    return myExpressionScope;
  }

  public Scope getPLevelScope() {
    return myPLevelScope;
  }

  public Scope getHLevelScope() {
    return myHLevelScope;
  }

  public Scopes caching() {
    return new Scopes(CachingScope.make(myExpressionScope), NameCachingScope.make(myPLevelScope), NameCachingScope.make(myHLevelScope));
  }
}
