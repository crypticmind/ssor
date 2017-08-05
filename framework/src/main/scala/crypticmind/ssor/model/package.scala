package crypticmind.ssor

import crypticmind.ssor.repo.{DepartmentRepo, TeamRepo, UserRepo}
import sangria.schema._
import sangria.macros.derive._

package object model {

  sealed trait Ref[+T] {
    def id: String
  }

  case class Id[+T](id: String) extends Ref[T]

  sealed trait Entity[+T] { def value: T }

  case class Transient[+T](value: T) extends Entity[T] {
    def withId(id: String): Persistent[T] = Persistent(id, value)
  }

  case class Persistent[+T](id: String, value: T) extends Entity[T] with Ref[T]

  case class User(name: String, team: Ref[Team])

  case class Team(name: String)

  case class Department(name: String)

  class API(userRepo: UserRepo, teamRepo: TeamRepo, departmentRepo: DepartmentRepo) {

    implicit val teamType: ObjectType[Unit, Team] = deriveObjectType[Unit, Team](ObjectTypeDescription("A team"))

    implicit val teamRefType: ObjectType[Unit, Ref[Team]] = ref

    implicit val userType: ObjectType[Unit, User] = deriveObjectType[Unit, User](ObjectTypeDescription("A system user"))

    val id: Argument[String] = Argument("id", StringType)

    val queryType =
      ObjectType(
        "query",
        fields[Unit, Unit](
          Field("user", OptionType(persistentEntity(userType)),
          description = Some("Returns a user with a specific id"),
          arguments = id :: Nil,
          resolve = c => userRepo.getById(c.arg(id)).map { case Persistent(eid, user) => Persistent(eid, user.copy(team = teamRepo.getById(user.team.id).get)) }),
          Field("users", ListType(persistentEntity(userType)),
          description = Some("Returns all users"),
          resolve = c => userRepo.getAll.map { case Persistent(eid, user) => Persistent(eid, user.copy(team = teamRepo.getById(user.team.id).get)) })))

    val schema = Schema(queryType)

    private def transientEntity[Ctx, T](implicit ot: ObjectType[Ctx, T]): ObjectType[Ctx, Transient[T]] =
      ObjectType(
        s"Transient${ot.name}",
        s"${ot.description} (transient)",
        ot.fields.toList.map(adaptField[Ctx, T, Transient[T]](_.value)))

    private def persistentEntity[Ctx, T](implicit ot: ObjectType[Ctx, T]): ObjectType[Ctx, Persistent[T]] =
      ObjectType(
        s"Persistent${ot.name}",
        Field("id", StringType, resolve = (c: Context[Ctx, Persistent[T]]) => c.value.id) ::
          ot.fields.toList.map(adaptField[Ctx, T, Persistent[T]](_.value)))

    private def ref[Ctx, T](implicit ot: ObjectType[Ctx, T]): ObjectType[Ctx, Ref[T]] =
      ObjectType(
        s"Reference${ot.name}",
        Field("id", StringType, resolve = (c: Context[Ctx, Ref[T]]) => c.value.id) ::
          ot.fields.toList.map(adaptField[Ctx, T, Ref[T]] {
            case Id(eid) => throw new Exception(s"Unresolved reference to ${ot.name} with ID $eid")
            case Persistent(_, value) => value
          }))

    private def adaptField[Ctx, FT, CT](f: CT => FT)(field: Field[Ctx, _]): Field[Ctx, CT] =
      field.copy(resolve = (c: Context[Ctx, CT]) => field.resolve.asInstanceOf[Context[Ctx, FT] => Action[Ctx, _]].apply(Context[Ctx, FT](
        value = f(c.value),
        ctx = c.ctx,
        args = c.args,
        schema = c.schema.asInstanceOf[Schema[Ctx, FT]],
        field = c.field.asInstanceOf[Field[Ctx, FT]],
        parentType = c.parentType,
        marshaller = c.marshaller,
        sourceMapper = c.sourceMapper,
        deprecationTracker = c.deprecationTracker,
        astFields = c.astFields,
        path = c.path,
        deferredResolverState = c.deferredResolverState)))

  }

}
