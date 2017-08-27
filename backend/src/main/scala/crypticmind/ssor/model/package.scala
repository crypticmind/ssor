package crypticmind.ssor

import java.util.Base64

import scala.util.Try

package object model {

  // Entity lifecycle and reference

  sealed trait Ref[+T] { def id: String }

  case class Id[+T](id: String) extends Ref[T]

  sealed trait Entity[+T] { def value: T }

  case class Transient[+T](value: T) extends Entity[T]

  case class Persistent[+T](id: String, value: T) extends Entity[T] with Ref[T]

  // Pagination

  case class Page[+T](total: Int, items: Seq[PageItem[T]], last: String, hasMore: Boolean)

  case class PageItem[+T](item: T, position: String)

  object PageItem {

    def toPosition(i: Int): String =
      Base64.getEncoder.encodeToString(i.toString.getBytes)

    def fromPosition(position: String): Int =
      Try(new String(Base64.getDecoder.decode(position)).toInt).getOrElse(0)

  }

  // Domain entities

  case class User(name: String, team: Ref[Team])

  case class Team(name: String, department: Option[Ref[Department]])

  case class Department(name: String)

}
