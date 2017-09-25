Arend Tutorial
===============

Here is the data type for booleans in Arend:

~~~~.arend
\open ::Data::Unit
\open ::Data::Empty

\data Bool | true | false

\function
True (b : Bool) : \Prop
    | true => Unit
    | false => Empty

\function
not (b : Bool) : Bool
    | true => false
    | false => true

\function
if {A : \Type} (b : Bool) (then else : A) : A => \elim b
    | true => then
    | false => else
~~~~
