<h1 id="classes">Classes<a class="headerlink" href="#classes" title="Permanent link">&para;</a></h1>

A _class_ is a record that has several useful properties.
A class is defined in the same way as a record, but it begins with the keyword `\class` instead of `\record`.
Classes can extend other classes and records and vice versa.
The first explicit parameter of a class is called its _classifying field_.
Classes are implicitly coerced to the type of the classifying field.
Let us consider an example:

```arend
\class Semigroup (E : \Set)
  | \infixl 7 * : E -> E -> E
  | *-assoc (x y z : E) : (x * y) * z = x * (y * z)

\func square {S : Semigroup} (x : S) => x * x
```

We can write `x : S` instead of `x : S.E` since `S` is implicitly coerced to an element of type `\Set`, that is `S.E`.

Every parameter of a definition of type `C { ... }` is marked as a _local instance_ of class `C`.
When we use a field of `C` or a definition that has an implicit parameter of type `C`, if the classifying field of a local instance coincides with the expected classifying field, then this instance will be inferred as the argument of the field or the definition.
For example, function `square` in the example above has one local instance `S` of class `Semigroup`.
We use its field `*` in the body of the function and the expected type of this call is `S.E -> S.E -> {?}` since the type of `x` is `S.E`.
This implies that the expected classifying field is `S.E` which is the classifying field of the local instance `S`, so this instance is inferred as the implicit argument of `*`.

We can also define global instances.
To do this, we need to use the keyword `\instance':

```arend
\instance NatSemigroup : Semigroup Nat
  | * => Nat.*
  | *-assoc => {?} -- the proof is omitted
```

A global instance is just a function and can be used as an ordinary function.
The only difference is that it is always defined by [copattern matching](/language-reference/definitions/functions/#copattern-matching) and must define an instance of a class.
Also, the classifying field must be a data or a record applied to some arguments and if some parameters of an instance have a class as a type, its classifying field must be an argument of the classifying field of the result type.

If there is no local instance with the expected classifying field, then this instance will be searched among global instances.
There is no backtracking, so the first appropriate instance will be chosen.
A global instance is appropriate if the expected classifying field is a data or a record applied to some arguments and the classifying field of the instance is the same data or record applied to possibly different arguments.
