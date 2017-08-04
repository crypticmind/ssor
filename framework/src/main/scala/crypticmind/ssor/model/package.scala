package crypticmind.ssor

import crypticmind.ssor.repo.{DepartmentRepo, TeamRepo, UserRepo}
import sangria.execution.deferred.{Deferred, DeferredResolver, Fetcher}
import sangria.schema._
import sangria.macros.derive._

import scala.concurrent.Future

package object model {

  case class Ref[+T](id: String)

  sealed trait Reference[+T] {
    def id: String
  }

  case class JustId[+T](id: String) extends Reference[T]

  sealed trait Entity[+T] { def value: T }

  case class Transient[+T](value: T) extends Entity[T] {
    def withId(id: String): Persistent[T] = Persistent(id, value)
  }

  case class Persistent[+T](id: String, value: T) extends Entity[T] with Reference[T]

  case class User(name: String, team: Reference[Team])

  case class Team(name: String)

  case class Department(name: String)

  class API(userRepo: UserRepo, teamRepo: TeamRepo, departmentRepo: DepartmentRepo) {

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

    implicit def ref[Ctx, T](retrieve: String => Persistent[T])(implicit ot: ObjectType[Ctx, Persistent[T]]): ObjectType[Ctx, Ref[T]] =
      ObjectType(
        ot.name,
        ot.description.getOrElse(s"${ot.name} reference"),
        ot.fields.toList.map(adaptField[Ctx, Persistent[T], Ref[T]](x => retrieve(x.id))))

    implicit def reference[Ctx, T](retrieve: String => Persistent[T])(implicit ot: ObjectType[Ctx, Persistent[T]]): UnionType[Ctx] = {

      val r: ObjectType[Ctx, Reference[T]] =
        ObjectType(
          "Reference",
          Some("Reference"),
          fields[Ctx, Reference[T]](Field("id", StringType, resolve = _.value.id)))
      
      UnionType(
        ot.name,
        ot.description.map(_ + " reference"),
        List(ot, r))
    }

    implicit def transientEntity[Ctx, T](implicit ot: ObjectType[Ctx, T]): ObjectType[Ctx, Transient[T]] =
      ObjectType(
        s"Transient${ot.name}",
        s"${ot.description} (transient)",
        ot.fields.toList.map(adaptField[Ctx, T, Transient[T]](_.value)))

    implicit def persistentEntity[Ctx, T](implicit ot: ObjectType[Ctx, T]): ObjectType[Ctx, Persistent[T]] =
      ObjectType(
        s"Persistent${ot.name}",
        s"${ot.description} (persistent)",
        Field("id", StringType, resolve = (c: Context[Ctx, Persistent[T]]) => c.value.id) ::
          ot.fields.toList.map(adaptField[Ctx, T, Persistent[T]](_.value)))

    implicit val departmentType: ObjectType[Unit, Department] = deriveObjectType[Unit, Department](ObjectTypeDescription("A department"))

    implicit val departmentRef: ObjectType[Unit, Ref[Department]] = ref[Unit, Department]((id: String) => departmentRepo.getById(id).get)

    implicit val teamType: ObjectType[Unit, Team] = deriveObjectType[Unit, Team](ObjectTypeDescription("A team"))

    //implicit val teamRef: ObjectType[Unit, Ref[Team]] = ref[Unit, Team]((id: String) => teamRepo.getById(id).get)
    implicit val teamReference: ObjectType[Unit, Reference[Team]] = reference[Unit, Team]((id: String) => teamRepo.getById(id).get)

    implicit val userType: ObjectType[Unit, User] = deriveObjectType[Unit, User](ObjectTypeDescription("A system user"))

    val id: Argument[String] = Argument("id", StringType)

//    val users: Fetcher[Unit, Persistent[User], Persistent[User], String] =
//      Fetcher((_, ids: Seq[String]) => Future.successful(ids.map(userRepo.getById).map(_.get)))
//
//    case class DeferTeams(ids: Seq[String]) extends Deferred[List[Persistent[Team]]]
//
//    val teams: Fetcher[Unit, Persistent[Team], Persistent[Team], String] =
//      Fetcher((_, ids: Seq[String]) => Future.successful(ids.map(teamRepo.getById).map(_.get)))
//
//    val departments: Fetcher[Unit, Persistent[Department], Persistent[Department], String] =
//      Fetcher((_, ids: Seq[String]) => Future.successful(ids.map(departmentRepo.getById).map(_.get)))
//
//    val resolver: DeferredResolver[Unit] = DeferredResolver.fetchers(users, teams, departments)

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
          resolve = c => userRepo.getAll)))

    val schema = Schema(queryType)
  }

}
