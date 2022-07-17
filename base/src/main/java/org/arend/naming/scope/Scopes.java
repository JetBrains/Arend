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

  public Scope getScope(Scope.Kind kind) {
    return kind == Scope.Kind.EXPR ? myExpressionScope : kind == Scope.Kind.PLEVEL ? myPLevelScope : myHLevelScope;
  }

  public Scopes caching() {
    return new Scopes(CachingScope.make(myExpressionScope), CachingScope.make(myPLevelScope), CachingScope.make(myHLevelScope));
  }
}
