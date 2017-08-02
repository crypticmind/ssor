package crypticmind.ssor

import crypticmind.ssor.macros._
import crypticmind.ssor.repo.UserRepo
import sangria.schema._
import sangria.macros.derive._

package object model {

  sealed trait Entity[T] { def value: T }

  case class Transient[T](value: T) extends Entity[T] {
    def withId(id: String): Persistent[T] = Persistent(id, value)
  }

  case class Persistent[T](id: String, value: T) extends Entity[T]

  case class User(name: String)

  object graphql {

    val idType: InterfaceType[Unit, Id] =
      InterfaceType(
        "Id",
        "An entity than can be identified",
        fields[Unit, Id](
          Field("id", StringType, resolve = _.value.id)
        )
      )

    val transientUserType: ObjectType[Unit, User] =
      deriveObjectType[Unit, User](
        ObjectTypeDescription("A transient system user")
      ).copy(name = "TransientUser")

    def fieldsOfPlusId[Ctx, T](ot: ObjectType[Ctx, T]): Seq[Field[Ctx, T with Id]] =
      ot.fields.map { field =>
        field.copy(resolve = (c: Context[Ctx, T with Id]) => field.resolve.asInstanceOf[Context[Ctx, T with Id] => Action[Ctx, String]].apply(c))
      }

    val persistentUserType: ObjectType[Unit, User with Id] =
      ObjectType(
        "User",
        "A system user",
        fields(fieldsOfPlusId(transientUserType): _*)
      )

    val _persistentUserType: ObjectType[Unit, Persistent[User]] =
      ObjectType(
        "User",
        "A system user",
        fields[Unit, Persistent[User]](
          Field("id", StringType, resolve = _.value.id),
          Field("name", StringType, resolve = _.value.value.name)
        )
      )

    val id: Argument[String] = Argument("id", StringType)

    val queryType =
      ObjectType(
        "query",
        fields[UserRepo, Unit](
          Field("user", OptionType(_persistentUserType),
          description = Some("Returns a user with a specific id"),
          arguments = id :: Nil,
          resolve = c => c.ctx.getById(c.arg(id))),
          Field("users", ListType(_persistentUserType),
          description = Some("Returns all users"),
          resolve = c => c.ctx.getAll)
        )
      )

    val schema = Schema(queryType)
  }

}
