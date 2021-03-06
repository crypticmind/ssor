package crypticmind.ssor.repo

import java.util.concurrent.atomic.AtomicInteger

import crypticmind.ssor.model._

class DepartmentRepo {

  def getAll: Seq[Persistent[Department]] = departments

  def getById(id: String): Option[Persistent[Department]] = {
    println(s"${getClass.getSimpleName}.getById($id)")
    departments.find(_.id == id)
  }

  def save(user: Entity[Department]): Persistent[Department] = synchronized {
    user match {
      case Transient(department) =>
        val pdepartment = Persistent(idGen.getAndIncrement().toString, department)
        departments :+= pdepartment
        pdepartment
      case pdepartment @ Persistent(id, _) =>
        departments = departments.filterNot(_.id == id)
        departments :+= pdepartment
        pdepartment
    }
  }

  def delete(id: String): Boolean = synchronized {
    val found = getById(id).isDefined
    departments = departments.filterNot(_.id == id)
    found
  }

  private val idGen = new AtomicInteger(1)
  private var departments: Seq[Persistent[Department]] = 1 to 4 map { i =>
    Persistent(idGen.getAndIncrement().toString, Department(s"department-$i"))
  }

}
