**GROUP: 3B**

* Clara Martins
    * **Nr:** 201806528
    * **Grade:** 0 to 20 value
    * **Contribution:** 25 %
* Daniel Monteiro
    * **Nr:** 201806185
    * **Grade:** 0 to 20 value
    * **Contribution:** 20 %
* Gonçalo Pascoal
    * **Nr:** 201806332
    * **Grade:** 0 to 20 value
    * **Contribution:** 30 %
* Rui Pinto
    * **Nr:** 201806441
    * **Grade:** 0 to 20 value
    * **Contribution:** 25 %

**GLOBAL Grade of the project:** <0 to 20>

### SUMMARY

All errors detected during a compilation phase can be seen in error messages. These contain the line and column where the error occurred and a small explanation of the error.

It identifies uses of variables and evaluation of expressions that can be replaced by constants. It also tries to identify program flow on `if` statements, analysing the value of the condition.


### DEALING WITH SYNTACTIC ERRORS
* The grammar used is mostly LL(1), only using a lookahead of 2 to differentiate between a `VarDeclaration` and a `Statement`; it also doesn't allow `Identifier;` to be obtained from `Statement`.
* It includes error recovery from `while` loops, only exiting after the entire code has been analysed.
* It respects the operator precedence.

(Describe how the syntactic error recovery of your tool works. Does it exit after the first error?)

### SEMANTIC ANALYSIS
* No variable is declared more than once
* No function is declared more than once (although it can be declared more than once if the arguments are different, in number and/or type)
* No function call is made with wrong argument types
* The operators receive the correct types
* Both sides of the assignment have the same type

### CODE GENERATION
* Code generation based on OLLIR.

### TASK DISTRIBUTION
Most of the tasks were completed as a group.

#### PROS
* It supports method overload
* It supports optimizations, such as register allocation, constant propagation and constant folding
* When the `if` condition is a constant, it identifies the program flow

### CONS
* There might be an issue with the register allocation of variables that are assigned and never used
* The while optimization was not implemented
