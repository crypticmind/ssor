package crypticmind.ssor

package object model {

  sealed trait Ref[+T] { def id: String }

  object Ref {
    implicit def asId[T](ref: Ref[T]): Id[T] = ref match {
      case id: Id[T] => id
      case other => Id(other.id)
    }
  }

  case class Id[+T](id: String) extends Ref[T]

  sealed trait Entity[+T] { def value: T }

  case class Transient[+T](value: T) extends Entity[T]

  case class Persistent[+T](id: String, value: T) extends Entity[T] with Ref[T]

  case class User(name: String, team: Ref[Team])

  case class Team(name: String, department: Option[Ref[Department]])

  case class Department(name: String)

}
