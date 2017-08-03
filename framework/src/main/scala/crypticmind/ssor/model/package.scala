package crypticmind.ssor

import crypticmind.ssor.repo.{DepartmentRepo, TeamRepo, UserRepo}
import sangria.schema._
import sangria.macros.derive._

package object model {

  sealed trait Entity[+T] { def value: T }

  case class Transient[+T](value: T) extends Entity[T] {
    def withId(id: String): Persistent[T] = Persistent(id, value)
  }

  case class Persistent[+T](id: String, value: T) extends Entity[T]

  case class Ref[+T](id: String)

  case class User(name: String, team: Ref[Team])

  case class Team(name: String, description: String, department: Option[Ref[Department]])

  case class Department(name: String)

  class API(userRepo: UserRepo, teamRepo: TeamRepo, departmentRepo: DepartmentRepo) {

    private class Ctx(val userRepo: UserRepo, val teamRepo: TeamRepo)

    implicit def ref[Ctx, T](retrieve: String => Persistent[T])(implicit ot: ObjectType[Ctx, Persistent[T]]): ObjectType[Ctx, Ref[T]] =
      ObjectType(
        ot.name,
        ot.description.getOrElse(s"${ot.name} reference"),
        unwrapRef(retrieve, ot)
      )

    def unwrapRef[Ctx, T](retrieve: String => Persistent[T], ot: ObjectType[Ctx, Persistent[T]]): List[Field[Ctx, Ref[T]]] = {
      ot.fields.toList.map { field =>
        field.copy(resolve = (c: Context[Ctx, Ref[T]]) => field.resolve.asInstanceOf[Context[Ctx, Persistent[T]] => Action[Ctx, _]].apply(Context[Ctx, Persistent[T]](
          value = retrieve(c.value.id),
          ctx = c.ctx,
          args = c.args,
          schema = c.schema.asInstanceOf[Schema[Ctx, Persistent[T]]],
          field = c.field.asInstanceOf[Field[Ctx, Persistent[T]]],
          parentType = c.parentType,
          marshaller = c.marshaller,
          sourceMapper = c.sourceMapper,
          deprecationTracker = c.deprecationTracker,
          astFields = c.astFields,
          path = c.path,
          deferredResolverState = c.deferredResolverState
        )))
      }
    }

    def unwrapTransient[Ctx, T](ot: ObjectType[Ctx, T]): List[Field[Ctx, Transient[T]]] =
      ot.fields.toList.map { field =>
        field.copy(resolve = (c: Context[Ctx, Transient[T]]) => field.resolve.asInstanceOf[Context[Ctx, T] => Action[Ctx, _]].apply(Context[Ctx, T](
          value = c.value.value,
          ctx = c.ctx,
          args = c.args,
          schema = c.schema.asInstanceOf[Schema[Ctx, T]],
          field = c.field.asInstanceOf[Field[Ctx, T]],
          parentType = c.parentType,
          marshaller = c.marshaller,
          sourceMapper = c.sourceMapper,
          deprecationTracker = c.deprecationTracker,
          astFields = c.astFields,
          path = c.path,
          deferredResolverState = c.deferredResolverState
        )))
      }

    implicit def unwrapPersistent[Ctx, T](ot: ObjectType[Ctx, T]): List[Field[Ctx, Persistent[T]]] =
      ot.fields.toList.map { field =>
        field.copy(resolve = (c: Context[Ctx, Persistent[T]]) => field.resolve.asInstanceOf[Context[Ctx, T] => Action[Ctx, String]].apply(Context[Ctx, T](
          value = c.value.value,
          ctx = c.ctx,
          args = c.args,
          schema = c.schema.asInstanceOf[Schema[Ctx, T]],
          field = c.field.asInstanceOf[Field[Ctx, T]],
          parentType = c.parentType,
          marshaller = c.marshaller,
          sourceMapper = c.sourceMapper,
          deprecationTracker = c.deprecationTracker,
          astFields = c.astFields,
          path = c.path,
          deferredResolverState = c.deferredResolverState
        )))
      } :+ Field("id", StringType, resolve = (c: Context[Ctx, Persistent[T]]) => c.value.id)

    def objectTypesForEntity[Ctx, T](ot: ObjectType[Ctx, T]): (ObjectType[Ctx, Transient[T]], ObjectType[Ctx, Persistent[T]]) = (
      ObjectType(s"Transient${ot.name}", s"${ot.description} (transient)", unwrapTransient(ot)),
      ObjectType(s"Persistent${ot.name}", s"${ot.description} (persistent)", unwrapPersistent(ot))
    )

    implicit val departmentType: ObjectType[Unit, Department] = deriveObjectType[Unit, Department](ObjectTypeDescription("A department"))

    implicit val (transientDepartmentType, persistentDepartmentType) = objectTypesForEntity(departmentType)

    implicit val departmentRef: ObjectType[Unit, Ref[Department]] = ref[Unit, Department]((id: String) => departmentRepo.getById(id).get)

    implicit val teamType: ObjectType[Unit, Team] = deriveObjectType[Unit, Team](ObjectTypeDescription("A team"))

    implicit val (transientTeamType, persistentTeamType) = objectTypesForEntity(teamType)

    implicit val teamRef: ObjectType[Unit, Ref[Team]] = ref[Unit, Team]((id: String) => teamRepo.getById(id).get)
    
    implicit val userType: ObjectType[Unit, User] = deriveObjectType[Unit, User](ObjectTypeDescription("A system user"))
    
    implicit val (transientUserType, persistentUserType) = objectTypesForEntity(userType)

    val id: Argument[String] = Argument("id", StringType)

    val queryType =
      ObjectType(
        "query",
        fields[Unit, Unit](
          Field("user", OptionType(persistentUserType),
          description = Some("Returns a user with a specific id"),
          arguments = id :: Nil,
          resolve = c => userRepo.getById(c.arg(id))),
          Field("users", ListType(persistentUserType),
          description = Some("Returns all users"),
          resolve = c => userRepo.getAll)
        )
      )

    val schema = Schema(queryType)
  }

}
