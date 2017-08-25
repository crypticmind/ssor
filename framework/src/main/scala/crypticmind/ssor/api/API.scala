package crypticmind.ssor.api

import crypticmind.ssor.model._
import crypticmind.ssor.repo._
import sangria.execution.deferred._
import sangria.schema._

import scala.concurrent.Future

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

  val idArg: Argument[String] = Argument("id", StringType)
  val nameArg: Argument[String] = Argument("name", StringType)
  val optNameArg: Argument[Option[String]] = Argument("name", OptionInputType(StringType))

  val queryType =
    ObjectType(
      "Query",
      fields[Unit, Unit](
        //          Field(
        //            "user",
        //            OptionType(userType),
        //            description = Some("Returns a user with a specific id"),
        //            arguments = idArg :: Nil,
        //            resolve = c => userRepo.getById(c.arg(idArg))),
        //          Field(
        //            "users",
        //            ListType(userType),
        //            description = Some("Returns all users"),
        //            resolve = _ => userRepo.getAll),
        Field(
          "department",
          OptionType(departmentType),
          description = Some("Returns a department with a specific id"),
          arguments = idArg :: Nil,
          resolve = c => departmentRepo.getById(c.arg(idArg))),
        Field(
          "departments",
          ListType(departmentType),
          description = Some("Returns all departments"),
          resolve = _ => departmentRepo.getAll)))

  val mutationType =
    ObjectType(
      "Mutation",
      fields[Unit, Unit](
        Field(
          "add_department",
          departmentType,
          arguments = nameArg :: Nil,
          resolve = c => departmentRepo.save(Transient(Department(c.arg(nameArg))))),
        Field(
          "update_department",
          OptionType(departmentType),
          arguments = idArg :: optNameArg :: Nil,
          resolve = { c =>
            departmentRepo.getById(c.arg(idArg)).map { pd =>
              departmentRepo.save(pd.copy(value = pd.value.copy(name = c.arg(optNameArg).getOrElse(pd.value.name))))
            }
          }),
        Field(
          "delete_department",
          BooleanType,
          arguments = idArg :: Nil,
          resolve = c => departmentRepo.delete(c.arg(idArg)))))

  val schema = Schema(queryType, Some(mutationType))

}
