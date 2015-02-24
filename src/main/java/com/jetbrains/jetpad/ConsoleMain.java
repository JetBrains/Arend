package com.jetbrains.jetpad;

import com.jetbrains.jetpad.parser.BuildVisitor;
import com.jetbrains.jetpad.parser.VcgrammarLexer;
import com.jetbrains.jetpad.parser.VcgrammarParser;
import com.jetbrains.jetpad.term.NotInScopeException;
import com.jetbrains.jetpad.term.definition.Definition;
import com.jetbrains.jetpad.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.term.typechecking.TypeCheckingException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
        for (String var : builder.getUnknownVariables()) {
            System.err.print("Not in scope: " + var);
        }
        for (Definition def : defs) {
            if (def instanceof FunctionDefinition) {
                if (builder.getUnknownVariables().isEmpty()) {
                    def = def.checkTypes();
                    def = new FunctionDefinition(def.getName(), def.getSignature(), ((FunctionDefinition)def).getTerm().normalize());
                }
            }
            def.prettyPrint(System.out, new ArrayList<String>(), 0);
            System.out.println();
            System.out.println();
        }
    }
}
