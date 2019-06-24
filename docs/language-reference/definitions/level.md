<h1 id="levels">Level<a class="headerlink" href="#level" title="Permanent link">&para;</a></h1>

This module is devoted to a number of tools useful for working with homotopy levels of 
[universes](/language-reference/expressions/universes).

## Use level

The homotopy level of a definition is inferred automatically, but sometimes it is possible to prove that it has a smaller level.
For example, we can define the following data types:
```arend
\data Empty
\data Dec (P : \Prop) | yes P | no (P -> Empty)
```

The type of `Empty` is inferred to `\Prop`, which is the right universe for the type.
However, this is not as easy for the type `Dec P`: the type of `Dec P` is inferred to `\Set0`,
whereas it can be proven that `Dec P` is (-1)-type.
`Dec` can be placed in `\Prop` by writing the proof, that any two elements of this type are equal,
in the `\where` block of `Dec`. The proof must be written in the corresponding function, starting with keywords
`\use \level` instead of `\func`:
```arend
\data Empty
\data Dec (P : \Prop) | yes P | no (P -> Empty)
  \where
    \use \level isProp {P : \Prop} (d1 d2 : Dec P) : d1 = d2
      | yes x1, yes x2 => path (\lam i => yes (Path.inProp x1 x2 @ i))
      | yes x1, no e2 => \case e2 x1 \with {}
      | no e1, yes x2 => \case e1 x2 \with {}
      | no e1, no e2 => path (\lam i => no (\lam x => (\case e1 x \return e1 x = e2 x \with {}) @ i))
```

Functions `\use \level` can be specified for `\data`, `\class`, and `\func` definitions.
They must have a particular type.
First parameters of such a function must be parameters of the data type (or the function) or (some) fields of the class.
The rest of parameters together with the result type must prove that the data type (or the function, or the class) has some homotopy level.
That is, it must prove `ofHLevel (D p_1 ... p_k) n` for some constant `n`, where `D` is the data type (or the function, or the class), `p_1`, ... `p_k` are its parameters (or fields), and `ofHLevel` is defined as follows:
```arend
\func \infix 2 ofHLevel_-1+ (A : \Type) (n : Nat) : \Type \elim n
  | 0 => \Pi (a a' : A) -> a = a'
  | suc n => \Pi (a a' : A) -> (a = a') ofHLevel_-1+ n
```

## Level of a type

Sometimes we need to know that some type has a certain homotopy level.
For example, the result type of a [lemma](/language-reference/definitions/functions/#lemmas) 
or a [property](/language-reference/definitions/records/#properties) must be a proposition.
If the type does not belong to the corresponding universe, but it can be proved that it has the correct homotopy level,
the keyword `\level` can be used to convince the typechecker to accept the definition.
This keywords can be specified in the result type of a function, a lemma, a field, or a case expression.
Its first argument is the type and the second is the proof that it belongs to some homotopy level.

For example, if `A` is a type such that `p : \Pi (x y : A) -> x = y`, then a lemma that proves `A` can be defined as follows:
```arend
\lemma lem : \level A p => ...
```

Similarly, a property of type `A` can be defined as follows:
```arend
\record R {
  \property p : \level A p
}
```

While defining a function or a case expression over a truncated type with values in `A`, some clauses can be omitted if
`A` belongs to an appropriate universe.
If it is not, but there is a proof that it has the required homotopy level, then the keyword `\level` can be used to
convince the typechecker that some clauses can be omitted.
For example, if `Trunc` is a propositional truncation with constructor `inT : A -> Trunc A`, `A` and `B` are types,
`g : A -> B` is function, and `p : \Pi (x y : B) -> x = y`, then the function, extending `g` to `Trunc A` can be
defined simply as follows:
```arend
\func f (t : Trunc A) : \level B p
  | inT a => g a
```

Similarly, the keyword `\level` can be used in case expressions:
```arend
\func f' (t : Trunc A) => \case t \return \level B p \with {
  | inT a => g a
}
```
