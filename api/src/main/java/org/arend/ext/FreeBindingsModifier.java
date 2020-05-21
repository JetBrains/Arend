package org.arend.ext;

import org.arend.ext.core.context.CoreBinding;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FreeBindingsModifier {
  public static class Command {
    public enum Kind { REMOVE, RETAIN, ADD, REPLACE, REPLACE_REMOVE, CLEAR }

    public final Kind kind;
    public final Object bindings;

    private Command(Kind kind, Object bindings) {
      this.kind = kind;
      this.bindings = bindings;
    }
  }

  public final List<Command> commands = new ArrayList<>();

  /**
   * Adds a binding to the context.
   */
  public @NotNull FreeBindingsModifier add(CoreBinding binding) {
    return add(Collections.singletonList(binding));
  }

  /**
   * Adds given bindings to the context.
   */
  public @NotNull FreeBindingsModifier add(Collection<? extends CoreBinding> bindings) {
    commands.add(new Command(Command.Kind.ADD, bindings));
    return this;
  }

  /**
   * Removes a binding from the context.
   */
  public @NotNull FreeBindingsModifier remove(CoreBinding binding) {
    return remove(Collections.singleton(binding));
  }

  /**
   * Removes given bindings from the context.
   */
  public @NotNull FreeBindingsModifier remove(Set<? extends CoreBinding> bindings) {
    commands.add(new Command(Command.Kind.REMOVE, bindings));
    return this;
  }

  /**
   * Removes all bindings except for the given one.
   */
  public @NotNull FreeBindingsModifier retain(CoreBinding binding) {
    return retain(Collections.singleton(binding));
  }

  /**
   * Removes all bindings except for the given ones.
   */
  public @NotNull FreeBindingsModifier retain(Set<? extends CoreBinding> bindings) {
    commands.add(new Command(Command.Kind.RETAIN, bindings));
    return this;
  }

  /**
   * Removes all bindings.
   */
  public @NotNull FreeBindingsModifier clear() {
    commands.add(new Command(Command.Kind.CLEAR, null));
    return this;
  }

  /**
   * Replaces a binding with a new one.
   */
  public @NotNull FreeBindingsModifier replace(CoreBinding oldBinding, CoreBinding newBinding) {
    return replace(Collections.singletonMap(oldBinding, newBinding));
  }

  /**
   * Replaces given bindings with new ones.
   */
  public @NotNull FreeBindingsModifier replace(Map<? extends CoreBinding, ? extends CoreBinding> replacement) {
    commands.add(new Command(Command.Kind.REPLACE, replacement));
    return this;
  }

  /**
   * Replaces given bindings with new ones and removes bindings for which {@code replacement.get()} returns {@code null}.
   */
  public @NotNull FreeBindingsModifier replaceAndRemove(Map<? extends CoreBinding, ? extends CoreBinding> replacement) {
    commands.add(new Command(Command.Kind.REPLACE_REMOVE, replacement));
    return this;
  }
}
