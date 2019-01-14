<h1 id="hits">Higher Inductive Types<a class="headerlink" href="#hits" title="Permanent link">&para;</a></h1>

Higher inductive types generalize ordinary [inductive types](/language-reference/definitions/data).
They allow us to define various (higher) colimits of types and other constructions such as truncations.

# Data types with conditions

If `con` is a constructor of an ordinary data type, then an expression of the form `con a_1 ... a_n` is always a normal form.
A _condition_ on a constructor is a rule that says how such an expression might evaluate.
For example, we can define integers as a data type with two constructors for positive and negative integers.
We can add a condition on the second constructor that says that positive and negative zero are computationally equal:

```arend
\data Int
  | pos Nat
  | neg Nat \with {
    | zero => pos zero
  }
```

To put conditions on a constructor, we need to define it as a function by [pattern matching](/language-reference/definitions/functions/#pattern-matching).
The only difference is that it is not required that all cases are covered.
The general syntax is the same as for ordinary pattern matching.
We need to add either `\with { ... }` or `\elim x_1, ... x_n { ... }` after parameters of the constructor, where `...` is a list of clauses.

A constructor with conditions will evaluate if its arguments match the specification in the same way as a function defined by pattern matching.
This means that a function over a data type with conditions must respect the equality between different constructors.
The typechecker checks that functions over data types with conditions are correctly defined.
For example, a function of type `Int -> X` must map positive and negative zero to the same value.
Thus, we cannot define the following function:

```arend
\func f (x : Int) : Nat
  | pos n => n
  | neg n => suc n
```

# Higher inductive types

A higher inductive type is a data type with a constructor that has conditions of the form `| left => e` and `| right => e'`.
Let us give a few examples:

```arend
-- Circle
\data S1
  | base
  | loop I \with {
    | left => base
    | right => base
  }

-- Suspension
\data Susp (A : \Type)
  | north
  | south
  | merid A (i : I) \elim i {
    | left => north
    | right => south
  }

-- Propositional truncation
\data Trunc (A : \Type)
  | inT A
  | truncT (x y : Trunc A) (i : I) \elim i {
    | left => x
    | right => x
  }

-- Set quotient
\data Quotient (A : \Type) (R : A -> A -> \Type)
  | inQ A
  | equivQ (x y : A) (R x y) (i : I) \elim i {
    | left => inQ x
    | right => inQ y
  }
  | truncQ (a a' : Quotient A R) (p p' : a = a') (i j : I) \elim i, j {
    | i, left  => p @ i
    | i, right => p' @ i
    | left,  _ => a
    | right, _ => a'
  }
```

If `X` is a proposition, then, to define a function of type `Trunc A -> X`, it is enough to specify its value for `inT a`.
The same works for any higher inductive type and any level.
For example, to define a function `Quotient A -> X`, it is enough to specify its value for `inQ a` and `equivQ x y r i` if `X` is a set and only for `inQ a` if it is a proposition.
This also workds for several arguments.
For example, if `X` is a set, then, to define a function `Quotient A -> Quotient A -> X`, it is enough to specify its value for `inQ a, inQ a'`, `inQ a, equivQ x y r i`, and `equivQ x y r i, inQ a`.
