package crypticmind.ssor.repo

import java.util.concurrent.atomic.AtomicInteger

import crypticmind.ssor.model._

class UserRepo {

  def getAll(limit: Int, after: Option[String]): Page[Persistent[User]] = {
    val offset = after.map(PageItem.fromPosition).map(_ + 1).getOrElse(0)
    val items =
      users.slice(offset, offset + limit)
        .zip(offset until (offset + limit))
        .map { case (p, i) => PageItem(p, PageItem.toPosition(i)) }
    Page(
      total = users.size,
      items = items,
      last = PageItem.toPosition(if (users.isEmpty) -1 else users.size - 1),
      hasMore = if (users.isEmpty) false else offset + limit <= users.size - 1)
  }

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

  def delete(id: String): Boolean = synchronized {
    val found = getById(id).isDefined
    users = users.filterNot(_.id == id)
    found
  }

  private val idGen = new AtomicInteger(1)
  private var users: Seq[Persistent[User]] = 1 to 4 map { i => 
    Persistent(
      idGen.getAndIncrement().toString, 
      User(s"user-$i", Id(i.toString)))
  }

}
