package crypticmind.ssor


import crypticmind.ssor.macros.CopyWithIdHelper.impl

package object macros {

  import language.experimental.macros
  
  implicit def materializeCopyWithKeyHelper[E](entity: E): CopyWithIdHelper[E] = macro impl[E]

}
