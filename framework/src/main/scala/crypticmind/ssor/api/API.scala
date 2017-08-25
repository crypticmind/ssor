package crypticmind.ssor.api

import crypticmind.ssor.model._
import crypticmind.ssor.service.Service
import sangria.execution.deferred._
import sangria.schema._

import scala.concurrent.Future

class API(service: Service) {

  implicit def hasId[T]: HasId[Persistent[T], String] = HasId(_.id)
  
  val departments: Fetcher[Unit, Persistent[Department], Persistent[Department], String] =
    Fetcher { (_, ids) =>
      Future.successful {
        ids.map(id => service.getDepartment(id).getOrElse(throw new Exception(s"Unresolved reference to Department with ID $id")))
      }
    }

  val departmentType: ObjectType[Unit, Persistent[Department]] = ObjectType(
    "Department",
    fields[Unit, Persistent[Department]](
      Field("id", StringType, resolve = c => c.value.id),
      Field("name", StringType, resolve = c => c.value.value.name)))

  val teams: Fetcher[Unit, Persistent[Team], Persistent[Team], String] =
    Fetcher { (_, ids) =>
      Future.successful {
        ids.map(id => service.getTeam(id).getOrElse(throw new Exception(s"Unresolved reference to Team with ID $id")))
      }
    }

  val teamType: ObjectType[Unit, Persistent[Team]] = ObjectType[Unit, Persistent[Team]](
    "Team",
    fields[Unit, Persistent[Team]](
      Field("id", StringType, resolve = c => c.value.id),
      Field("name", StringType, resolve = c => c.value.value.name),
      Field("department", OptionType(departmentType), resolve = c => departments.deferOpt(c.value.value.department.map(_.id)))))

  val userType: ObjectType[Unit, Persistent[User]] = ObjectType[Unit, Persistent[User]](
    "User",
    fields[Unit, Persistent[User]](
      Field("id", StringType, resolve = c => c.value.id),
      Field("name", StringType, resolve = c => c.value.value.name),
      Field("team", teamType, resolve = c => teams.defer(c.value.value.team.id))))

  val resolver: DeferredResolver[Unit] = DeferredResolver.fetchers(teams, departments)

  val idArg: Argument[String] = Argument("id", StringType)
  val nameArg: Argument[String] = Argument("name", StringType)
  val teamIdArg: Argument[String] = Argument("teamId", StringType)
  val optDepartmentIdArg: Argument[Option[String]] = Argument("departmentId", OptionInputType(StringType))

  val queryType =
    ObjectType(
      "Query",
      fields[Unit, Unit](
        Field(
          "user",
          OptionType(userType),
          description = Some("Returns a user with a specific id"),
          arguments = idArg :: Nil,
          resolve = c => service.getUser(c.arg(idArg))),
        Field(
          "users",
          ListType(userType),
          description = Some("Returns all users"),
          resolve = _ => service.getUsers),
        Field(
          "team",
          OptionType(teamType),
          description = Some("Returns a team with a specific id"),
          arguments = idArg :: Nil,
          resolve = c => service.getTeam(c.arg(idArg))),
        Field(
          "teams",
          ListType(teamType),
          description = Some("Returns all teams"),
          resolve = _ => service.getTeams),
        Field(
          "department",
          OptionType(departmentType),
          description = Some("Returns a department with a specific id"),
          arguments = idArg :: Nil,
          resolve = c => service.getDepartment(c.arg(idArg))),
        Field(
          "departments",
          ListType(departmentType),
          description = Some("Returns all departments"),
          resolve = _ => service.getDepartments)))

  val mutationType =
    ObjectType(
      "Mutation",
      fields[Unit, Unit](
        Field(
          "create_user",
          userType,
          description = Some("Creates a new user"),
          arguments = nameArg :: teamIdArg :: Nil,
          resolve = c => service.createUser(c.arg(nameArg), c.arg(teamIdArg))),
        Field(
          "update_user",
          OptionType(userType),
          description = Some("Updates an existing user"),
          arguments = idArg :: nameArg :: teamIdArg :: Nil,
          resolve = c => service.updateUser(c.arg(idArg), c.arg(nameArg), c.arg(teamIdArg))),
        Field(
          "delete_user",
          BooleanType,
          description = Some("Deletes an existing user"),
          arguments = idArg :: Nil,
          resolve = c => service.deleteUser(c.arg(idArg))),
        Field(
          "create_team",
          teamType,
          description = Some("Creates a new team"),
          arguments = nameArg :: optDepartmentIdArg :: Nil,
          resolve = c => service.createTeam(c.arg(nameArg), c.arg(optDepartmentIdArg))),
        Field(
          "update_team",
          OptionType(teamType),
          description = Some("Updates an existing team"),
          arguments = idArg :: nameArg :: optDepartmentIdArg :: Nil,
          resolve = c => service.updateTeam(c.arg(idArg), c.arg(nameArg), c.arg(optDepartmentIdArg))),
        Field(
          "delete_team",
          BooleanType,
          description = Some("Deletes an existing team"),
          arguments = idArg :: Nil,
          resolve = c => service.deleteTeam(c.arg(idArg))),
        Field(
          "create_department",
          departmentType,
          description = Some("Creates a new department"),
          arguments = nameArg :: Nil,
          resolve = c => service.createDepartment(c.arg(nameArg))),
        Field(
          "update_department",
          OptionType(departmentType),
          description = Some("Updates an existing department"),
          arguments = idArg :: nameArg :: Nil,
          resolve = c => service.updateDepartment(c.arg(idArg), c.arg(nameArg))),
        Field(
          "delete_department",
          BooleanType,
          description = Some("Deletes an existing department"),
          arguments = idArg :: Nil,
          resolve = c => service.deleteDepartment(c.arg(idArg)))))

  val schema = Schema(queryType, Some(mutationType))

}
