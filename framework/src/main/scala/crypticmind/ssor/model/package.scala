package crypticmind.ssor

import crypticmind.ssor.repo.{DepartmentRepo, TeamRepo, UserRepo}
import sangria.execution.deferred.{Deferred, DeferredResolver, Fetcher, HasId}
import sangria.schema._
import sangria.macros.derive._

import scala.concurrent.Future

package object model {

  sealed trait Ref[+T] { def id: String }

  object Ref {
    implicit def hasId[T]: HasId[Ref[T], String] = HasId(_.id)
  }

  case class Id[+T](id: String) extends Ref[T]

  sealed trait Entity[+T] { def value: T }

  case class Transient[+T](value: T) extends Entity[T]

  case class Persistent[+T](id: String, value: T) extends Entity[T] with Ref[T]

  object Persistent {
    implicit def hasId[T]: HasId[Persistent[T], Ref[T]] = HasId(identity)
  }

  case class User(name: String, team: Ref[Team])

  case class Team(name: String)

  case class Department(name: String)

  class API(userRepo: UserRepo, teamRepo: TeamRepo, departmentRepo: DepartmentRepo) {

    implicit val teamType: ObjectType[Unit, Team] = deriveObjectType[Unit, Team](ObjectTypeDescription("A team"))

    case class DeferTeam(team: Ref[Team]) extends Deferred[Persistent[Team]]

    implicit def teamRefAction(rt: Ref[Team]): LeafAction[Unit, Ref[Team]] = DeferredValue(DeferTeam(rt))

    implicit val refTeam: ObjectType[Unit, Ref[Team]] = ref

    implicit val userType: ObjectType[Unit, User] = deriveObjectType[Unit, User](ObjectTypeDescription("A system user"))

    val teams = Fetcher((_: Unit, ids: Seq[Ref[Team]]) => Future.successful(ids.map(ref => teamRepo.getById(ref.id).getOrElse(throw new Exception(s"Unresolved reference to Team with ID ${ref.id}")))))

    val resolver: DeferredResolver[Unit] = DeferredResolver.fetchers(teams)

    val userTeamResolve: Context[Unit, User] => Action[Unit, Ref[Team]] = c => teams.defer(c.value.team)

    val id: Argument[String] = Argument("id", StringType)

    val queryType =
      ObjectType(
        "query",
        fields[Unit, Unit](
          Field("user", OptionType(persistentEntity(userType)),
          description = Some("Returns a user with a specific id"),
          arguments = id :: Nil,
          resolve = c => userRepo.getById(c.arg(id))),
          Field("users", ListType(persistentEntity(userType)),
          description = Some("Returns all users"),
          resolve = _ => userRepo.getAll)))

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
