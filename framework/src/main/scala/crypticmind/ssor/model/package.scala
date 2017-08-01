package crypticmind.ssor

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

    trait Id {
      def id: String
    }

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
      )

    val persistentUserType: ObjectType[Unit, User with Id] =
      deriveObjectType[Unit, User](
        ObjectTypeDescription("A system user")
      )

    val transientUser: ObjectType[Unit, Transient[User]] = transientType(userType)
    val persistentUser: ObjectType[Unit, Persistent[User]] = persistentType(userType)


    val id: Argument[String] = Argument("id", StringType)

    val queryType =
      ObjectType(
        "query",
        fields[UserRepo, Unit](
          Field("user", OptionType(persistentUser),
          description = Some("Returns a user with a specific id"),
          arguments = id :: Nil,
          resolve = c => c.ctx.getById(c.arg(id))),
          Field("users", ListType(persistentUser),
          description = Some("Returns all users"),
          resolve = c => c.ctx.getAll)
        )
      )

    val schema = Schema(queryType)
  }

}
