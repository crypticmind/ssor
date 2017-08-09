package crypticmind.ssor.repo

import java.util.concurrent.atomic.AtomicInteger

import crypticmind.ssor.model._

class UserRepo {

  def getAll: Seq[Persistent[User]] = users

  def getById(id: String): Option[Persistent[User]] = {
    println(s"${getClass.getSimpleName}.getById($id)")
    users.find(_.id == id)
  }

  def save(user: Entity[User]): Persistent[User] = synchronized {
    user match {
      case Transient(user) =>
        val puser = Persistent(idGen.getAndIncrement().toString, user)
        users :+= puser
        puser
      case puser@Persistent(id, _) =>
        users = users.filterNot(_.id == id)
        users :+= puser
        puser
    }
  }

  def delete(id: String): Unit = synchronized {
    users = users.filterNot(_.id == id)
  }

  private val idGen = new AtomicInteger(1)
  private var users: Seq[Persistent[User]] = 1 to 4 map { i => 
    Persistent(
      idGen.getAndIncrement().toString, 
      User(s"user-$i", Id("1")))
  }

}
