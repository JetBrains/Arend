package org.arend.ext.module;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LongReference {
    public static final LongReference EMPTY = new LongReference(Collections.emptyList());

    private final List<ArendRef> myReferences;

    public LongReference(@NotNull ArendRef @NotNull... references) {
        myReferences = Arrays.asList(references);
    }

    public LongReference(@NotNull List<@NotNull ArendRef> references) {
        myReferences = references;
    }

    @Contract("-> new")
    public LongName toLongName() {
        var collector = new ArrayList<String>(myReferences.size());
        for (ArendRef ref : myReferences) {
            collector.add(ref.getRefName());
        }
        return new LongName(collector);
    }

    public ArendRef getFirstRef() {
        return myReferences.get(0);
    }

    public int size() {
        return myReferences.size();
    }

    @Contract("_ -> new")
    public LongReference withTrailing(ArendRef newRef) {
        var newList = new ArrayList<>(myReferences);
        newList.add(newRef);
        return new LongReference(newList);
    }
}
