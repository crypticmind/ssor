package crypticmind.ssor.repo

import java.util.concurrent.atomic.AtomicInteger

import crypticmind.ssor.model._

class TeamRepo {

  def getAll: Seq[Persistent[Team]] = teams

  def getById(id: String): Option[Persistent[Team]] = {
    println(s"${getClass.getSimpleName}.getById($id)")
    teams.find(_.id == id)
  }

  def save(user: Entity[Team]): Persistent[Team] = synchronized {
    user match {
      case Transient(team) =>
        val pteam = Persistent(idGen.getAndIncrement().toString, team)
        teams :+= pteam
        pteam
      case pteam @ Persistent(id, _) =>
        teams = teams.filterNot(_.id == id)
        teams :+= pteam
        pteam
    }
  }

  def delete(id: String): Unit = synchronized {
    teams = teams.filterNot(_.id == id)
  }

  private val idGen = new AtomicInteger(1)
  private var teams: Seq[Persistent[Team]] = 1 to 4 map { i =>
    Persistent(idGen.getAndIncrement().toString, Team(s"team-$i", if (i % 2 == 0) Some(Id(i.toString)) else None))
  }

}
