package crypticmind.ssor

import crypticmind.ssor.repo.{DepartmentRepo, TeamRepo, UserRepo}
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.schema._

import scala.concurrent.Future

package object model {

  sealed trait Ref[+T] { def id: String }

  object Ref {
    implicit def asId[T](ref: Ref[T]): Id[T] = ref match {
      case id: Id[T] => id
      case Persistent(id, _) => Id(id)
    }
  }

  case class Id[+T](id: String) extends Ref[T]

  sealed trait Entity[+T] { def value: T }

  case class Transient[+T](value: T) extends Entity[T]

  case class Persistent[+T](id: String, value: T) extends Entity[T] with Ref[T]

  object Persistent {
    implicit def hasId[T]: HasId[Persistent[T], Ref[T]] = HasId(identity)
  }

  case class User(name: String, team: Ref[Team])

  case class Team(name: String, department: Option[Ref[Department]])

  case class Department(name: String)

  class API(userRepo: UserRepo, teamRepo: TeamRepo, departmentRepo: DepartmentRepo) {

    val departments: Fetcher[Unit, Persistent[Department], Persistent[Department], Ref[Department]] =
      Fetcher { (_, refs) =>
        Future.successful {
          refs.map(ref => departmentRepo.getById(ref.id).getOrElse(throw new Exception(s"Unresolved reference to Department with ID ${ref.id}")))
        }
      }

    val departmentType: ObjectType[Unit, Persistent[Department]] = ObjectType(
      "Department",
      fields[Unit, Persistent[Department]](
        Field("id", StringType, resolve = c => c.value.id),
        Field("name", StringType, resolve = c => c.value.value.name)))

    val teams: Fetcher[Unit, Persistent[Team], Persistent[Team], Ref[Team]] =
      Fetcher { (_, refs) =>
        Future.successful {
          refs.map(ref => teamRepo.getById(ref.id).getOrElse(throw new Exception(s"Unresolved reference to Team with ID ${ref.id}")))
        }
      }

    val teamType: ObjectType[Unit, Persistent[Team]] = ObjectType[Unit, Persistent[Team]](
      "Team",
      fields[Unit, Persistent[Team]](
        Field("id", StringType, resolve = c => c.value.id),
        Field("name", StringType, resolve = c => c.value.value.name),
        Field("department", OptionType(departmentType), resolve = c => c.value.value.department match {
          case Some(ref) => departments.deferOpt(ref)
          case None => Value(None)
        })))

    val userType: ObjectType[Unit, Persistent[User]] = ObjectType[Unit, Persistent[User]](
      "User",
      fields[Unit, Persistent[User]](
        Field("id", StringType, resolve = c => c.value.id),
        Field("name", StringType, resolve = c => c.value.value.name),
        Field("team", teamType, resolve = c => teams.defer(c.value.value.team))))

    val resolver: DeferredResolver[Unit] = DeferredResolver.fetchers(teams, departments)

    val id: Argument[String] = Argument("id", StringType)

    val queryType =
      ObjectType(
        "query",
        fields[Unit, Unit](
          Field("user", OptionType(userType),
          description = Some("Returns a user with a specific id"),
          arguments = id :: Nil,
          resolve = c => userRepo.getById(c.arg(id))),
          Field("users", ListType(userType),
          description = Some("Returns all users"),
          resolve = _ => userRepo.getAll)))

    val schema = Schema(queryType)

  }

}
