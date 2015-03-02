package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.NotInScopeException;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsoleMain {

  public static void main(String[] args) throws IOException, NotInScopeException, TypeCheckingException {
    InputStream stream = args.length == 0 ? System.in : new FileInputStream(new File(args[0]));
    ANTLRInputStream input = new ANTLRInputStream(stream);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    VcgrammarParser.DefsContext tree = parser.defs();
    BuildVisitor builder = new BuildVisitor();
    List<Definition> defs = builder.visitDefs(tree);
    Map<String, Definition> context = new HashMap<>();
    for (Definition def : defs) {
      if (def instanceof FunctionDefinition) {
        def = def.checkTypes(context);
        def = new FunctionDefinition(def.getName(), def.getSignature(), ((FunctionDefinition)def).getTerm().normalize());
        context.put(def.getName(), def);
      }
      def.prettyPrint(System.out, new ArrayList<String>(), 0);
      System.out.println();
      System.out.println();
    }
  }
}
