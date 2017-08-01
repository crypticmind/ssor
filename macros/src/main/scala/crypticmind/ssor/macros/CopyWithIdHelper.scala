package crypticmind.ssor.macros

import crypticmind.ssor.model.Id


import reflect.macros.whitebox.Context
import scala.language.implicitConversions

trait CopyWithIdHelper[E] {
  val entity: E
  def withId(nid: String): E with Id
}

object CopyWithIdHelper {

  def impl[E: c.WeakTypeTag](c: Context)(entity: c.Expr[E]): c.Expr[CopyWithIdHelper[E]] = {
    import c.universe._

    val enttpe = weakTypeOf[E]

    val attrs = enttpe.decl(TermName("copy")).asMethod.paramLists.head.map { sym =>
      val attrName = sym.name.toTermName
      q"$attrName = entity.$attrName"
    }

    c.Expr[CopyWithIdHelper[E]] {
      q"""
        new CopyWithIdHelper[$enttpe] {
          val entity = $entity
          def withId(nid: String) =
            new $enttpe(..$attrs) with crypticmind.ssor.model.Id {
              val id = nid
            }
        }
    """
    }
  }

}
