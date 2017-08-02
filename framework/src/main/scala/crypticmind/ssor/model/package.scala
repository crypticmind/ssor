package crypticmind.ssor

import crypticmind.ssor.repo.{TeamRepo, UserRepo}
import sangria.schema._
import sangria.macros.derive._

package object model {

  sealed trait Entity[+T] { def value: T }

  case class Transient[+T](value: T) extends Entity[T] {
    def withId(id: String): Persistent[T] = Persistent(id, value)
  }

  case class Persistent[+T](id: String, value: T) extends Entity[T]

  sealed trait Ref[+T] {
    def get: T
    def isEmpty: Boolean
    def isDefined: Boolean = !isEmpty
    @inline final def getOrElse[U >: T](default: => U): U = if (isEmpty) default else this.get
    @inline final def orNull[U >: T](implicit ev: Null <:< U): U = this getOrElse ev(null)
  }

  case class Found[+T](value: T) extends Ref[T] {
    def get: T = value
    val isEmpty = false
  }

  case object NotAvailable extends Ref[Nothing] {
    def get = throw new NoSuchElementException
    val isEmpty = true
  }

  case class User(name: String, team: Ref[Team])

  case class Team(name: String)

  object graphql {

    implicit def ref[Ctx, T](implicit ot: ObjectType[Ctx, T], ev: Null <:< T): ObjectType[Ctx, Ref[T]] =
      ObjectType(
        s"Ref${ot.name}",
        s"${ot.description} (reference)",
        fields[Ctx, Ref[T]](unwrapRef(ot): _*)
      )

    def unwrapRef[Ctx, T](ot: ObjectType[Ctx, T])(implicit ev: Null <:< T): Seq[Field[Ctx, Ref[T]]] =
      ot.fields.map { field =>
        field.copy(resolve = (c: Context[Ctx, Ref[T]]) => field.resolve.asInstanceOf[Context[Ctx, T] => Action[Ctx, _]].apply(Context[Ctx, T](
          value = c.value.orNull,
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

    def unwrapTransient[Ctx, T](ot: ObjectType[Ctx, T]): Seq[Field[Ctx, Transient[T]]] =
      ot.fields.map { field =>
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

    implicit def unwrapPersistent[Ctx, T](ot: ObjectType[Ctx, T]): Seq[Field[Ctx, Persistent[T]]] =
      ot.fields.map { field =>
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
      ObjectType(s"Transient${ot.name}", s"${ot.description} (transient)", fields[Ctx, Transient[T]](unwrapTransient(ot): _*)),
      ObjectType(s"Persistent${ot.name}", s"${ot.description} (persistent)", fields[Ctx, Persistent[T]](unwrapPersistent(ot): _*))
    )

    implicit val teamType: ObjectType[Unit, Team] = deriveObjectType[Unit, Team](ObjectTypeDescription("A team"))

    implicit val refTeam: ObjectType[Unit, Ref[Team]] = ref[Unit, Team]

    implicit val userType: ObjectType[Unit, User] = deriveObjectType[Unit, User](ObjectTypeDescription("A system user"))
    
    val (transientUserType, persistentUserType) = objectTypesForEntity(userType)

    val id: Argument[String] = Argument("id", StringType)

    val queryType =
      ObjectType(
        "query",
        fields[(UserRepo, TeamRepo), Unit](
          Field("user", OptionType(persistentUserType),
          description = Some("Returns a user with a specific id"),
          arguments = id :: Nil,
          resolve = c => c.ctx._1.getById(c.arg(id))),
          Field("users", ListType(persistentUserType),
          description = Some("Returns all users"),
          resolve = c => c.ctx._1.getAll)
        )
      )

    val schema = Schema(queryType)
  }

}
