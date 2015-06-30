# ecore2thrift
This is a utility for generating a [Thrift](http://thrift.apache.org/) program from 
an [Ecore](http://www.eclipse.org/modeling/emf/) model.

It is written in EGL from the [Epsilon](http://www.eclipse.org/epsilon/) project.

## Known problems
* Thrift cannot have any method names that are Thrift reserved words, even if they
  are not reserved words in the language you are using Thrift to generate.
  
  This includes, for example, "list" and "delete".
