package com.jetbrains.jetpad.vclang.term.legacy;

import com.google.common.collect.Streams;
import com.jetbrains.jetpad.vclang.frontend.resolving.HasOpens;
import com.jetbrains.jetpad.vclang.frontend.resolving.OpenCommand;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Legacy Abstract emulation layer. Meant to be used exclusively by those whose brain is not capable
 * of comprehending the ideas behind the new {@link Abstract} interface.
 */
public class LegacyAbstract {
  public interface Statement extends Abstract.SourceNode {
    <P, R> R accept(LegacyAbstractStatementVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface DefineStatement extends Statement {
    @Nonnull Abstract.Definition getDefinition();

    static DefineStatement create(@Nonnull Abstract.Definition definition) {
      return new DefineStatement() {
        @Nonnull
        @Override
        public Abstract.Definition getDefinition() {
          return definition;
        }

        @Override
        public <P, R> R accept(LegacyAbstractStatementVisitor<? super P, ? extends R> visitor, P params) {
          return visitor.visitDefine(this, params);
        }
      };
    }
  }

  public interface NamespaceCommandStatement extends Statement {
    enum Kind { OPEN, EXPORT }

    @Nonnull Kind getKind();
    @Nullable ModulePath getModulePath();
    @Nonnull List<String> getPath();

    @Nullable Abstract.GlobalReferableSourceNode getResolvedClass();

    boolean isHiding();
    @Nullable List<String> getNames();

    static NamespaceCommandStatement fromOpenCommand(@Nonnull OpenCommand openCommand) {
      return new NamespaceCommandStatement() {
        @Nonnull
        @Override
        public Kind getKind() {
          return Kind.OPEN;
        }

        @Nullable
        @Override
        public ModulePath getModulePath() {
          return openCommand.getModulePath();
        }

        @Nonnull
        @Override
        public List<String> getPath() {
          return openCommand.getPath();
        }

        @Nullable
        @Override
        public Abstract.GlobalReferableSourceNode getResolvedClass() {
          return openCommand.getResolvedClass();
        }

        @Override
        public boolean isHiding() {
          return openCommand.isHiding();
        }

        @Nullable
        @Override
        public List<String> getNames() {
          return openCommand.getNames();
        }

        @Override
        public <P, R> R accept(LegacyAbstractStatementVisitor<? super P, ? extends R> visitor, P params) {
          return visitor.visitNamespaceCommand(this, params);
        }
      };
    }
  }


  public static @Nonnull Collection<? extends Statement> getGlobalStatements(Abstract.Definition definition) {
    final Stream<NamespaceCommandStatement> opens;
    if (definition instanceof HasOpens) {
      opens = StreamSupport.stream(HasOpens.GET.apply(definition).spliterator(), false)
          .map(NamespaceCommandStatement::fromOpenCommand);
    } else {
      opens = Stream.empty();
    }
    final Stream<DefineStatement> defines;
    if (definition instanceof Abstract.DefinitionCollection) {
      defines = ((Abstract.DefinitionCollection) definition).getGlobalDefinitions().stream()
          .map(DefineStatement::create);
    } else {
      defines = Stream.empty();
    }
    return Streams.concat(opens, defines).collect(Collectors.toList());
  }
}
