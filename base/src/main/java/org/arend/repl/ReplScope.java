package org.arend.repl;

import org.arend.naming.reference.Referable;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.ImportedScope;
import org.arend.naming.scope.MergeScope;
import org.arend.naming.scope.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ReplScope implements Scope {
  private @Nullable Scope myCurrentLineScope;
  private final @NotNull List<Scope> myPreviousScopes;
  private final MergeScope myPreviousMergeScope;

  /**
   * @param previousScopes recommended implementation: {@link java.util.LinkedList}
   */
  public ReplScope(@Nullable Scope currentLineScope, @NotNull List<Scope> previousScopes) {
    myCurrentLineScope = currentLineScope;
    myPreviousScopes = previousScopes;
    myPreviousMergeScope = new MergeScope(previousScopes);
  }

  /**
   * @return the scope added to the scope list
   */
  public @Nullable Scope freezeAndFlushCurrentLine() {
    if (myCurrentLineScope != null) {
      var scope = CachingScope.make(myCurrentLineScope);
      addScope(scope);
      myCurrentLineScope = null;
      return scope;
    } else return null;
  }

  public void addScope(@NotNull Scope scope) {
    myPreviousScopes.add(0, scope);
  }

  public void addPreludeScope(@NotNull Scope preludeScope) {
    myPreviousScopes.add(preludeScope);
  }

  public void currentLine(@Nullable Scope nextLineScope) {
    myCurrentLineScope = nextLineScope;
  }

  @Override
  public @Nullable Referable find(Predicate<Referable> pred) {
    return Optional
      .ofNullable(myCurrentLineScope)
      .map(scope -> scope.find(pred))
      .orElseGet(() -> myPreviousMergeScope.find(pred));
  }

  @Override
  public @Nullable Scope resolveNamespace(String name, boolean onlyInternal) {
    return Optional
      .ofNullable(myCurrentLineScope)
      .map(scope -> scope.resolveNamespace(name, onlyInternal))
      .orElseGet(() -> myPreviousMergeScope.resolveNamespace(name, onlyInternal));
  }

  @Override
  public @Nullable Referable resolveName(String name) {
    return Optional
      .ofNullable(myCurrentLineScope)
      .map(scope -> scope.resolveName(name))
      .orElseGet(() -> myPreviousMergeScope.resolveName(name));
  }

  @Override
  public @NotNull Scope getGlobalSubscopeWithoutOpens() {
    var previousScopes = new ArrayList<Scope>(myPreviousScopes.size());
    var currentLineSubscope = myCurrentLineScope != null ? myCurrentLineScope.getGlobalSubscopeWithoutOpens() : null;
    for (Scope previousScope : myPreviousScopes)
      previousScopes.add(previousScope.getGlobalSubscopeWithoutOpens());
    return new ReplScope(currentLineSubscope, previousScopes);
  }

  @Override
  public @NotNull List<Referable> getElements() {
    var list = new ArrayList<Referable>();
    if (myCurrentLineScope != null)
      list.addAll(myCurrentLineScope.getElements());
    for (Scope previousScope : myPreviousScopes)
      list.addAll(previousScope.getElements());
    return list;
  }

  @Override
  public @Nullable ImportedScope getImportedSubscope() {
    return Optional
      .ofNullable(myCurrentLineScope)
      .map(Scope::getImportedSubscope)
      .orElseGet(myPreviousMergeScope::getImportedSubscope);
  }
}
