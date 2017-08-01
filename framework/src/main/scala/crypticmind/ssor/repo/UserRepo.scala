package crypticmind.ssor.repo

import java.util.UUID

import crypticmind.ssor.model._

class UserRepo {

  private var users: Seq[Persistent[User]] =
    1 to 4 map { i => Persistent(UUID.randomUUID().toString, User(s"user$i")) }

  def getAll: Seq[Persistent[User]] = users

  def getById(id: String): Option[Persistent[User]] =
    users.find(_.id == id)

  def save(user: Entity[User]): Persistent[User] = user match {
    case tuser @ Transient(_) =>
      val puser = tuser.withId(UUID.randomUUID().toString)
      users :+= puser
      puser
    case puser @ Persistent(id, _) =>
      users = users.filterNot(_.id == id)
      users :+= puser
      puser
  }

  def delete(id: String): Unit =
    users = users.filterNot(_.id == id)

}
