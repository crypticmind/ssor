package crypticmind.ssor.service

import crypticmind.ssor.model._
import crypticmind.ssor.repo._

class Service(userRepo: UserRepo, teamRepo: TeamRepo, departmentRepo: DepartmentRepo) {


  def getUsers(limit: Int, after: Option[String]): Page[Persistent[User]] =
    userRepo.getAll(limit, after)


  def getUser(id: String): Option[Persistent[User]] =
    userRepo.getById(id)

  def getUsers: Seq[Persistent[User]] =
    userRepo.getAll

  def createUser(name: String, teamId: String): Persistent[User] = {
    val team = teamRepo.getById(teamId).getOrElse(throw new Exception(s"No team found with ID $teamId"))
    userRepo.save(Transient(User(name, team)))
  }

  def updateUser(id: String, name: String, teamId: String): Option[Persistent[User]] = {
    val team = teamRepo.getById(teamId).getOrElse(throw new Exception(s"No team found with ID $teamId"))
    userRepo.getById(id).map(p => userRepo.save(p.copy(value = User(name, team))))
  }

  def deleteUser(id: String): Boolean =
    userRepo.delete(id)

  def getTeam(id: String): Option[Persistent[Team]] =
    teamRepo.getById(id)

  def getTeams: Seq[Persistent[Team]] =
    teamRepo.getAll

  def createTeam(name: String, departmentId: Option[String]): Persistent[Team] = {
    val department = departmentId.map(id => departmentRepo.getById(id).getOrElse(throw new Exception(s"No department found with ID $id")))
    teamRepo.save(Transient(Team(name, department)))
  }

  def updateTeam(id: String, name: String, departmentId: Option[String]): Option[Persistent[Team]] = {
    val department = departmentId.map(id => departmentRepo.getById(id).getOrElse(throw new Exception(s"No department found with ID $id")))
    teamRepo.getById(id).map(p => teamRepo.save(p.copy(value = Team(name, department))))
  }

  def deleteTeam(id: String): Boolean =
    teamRepo.delete(id)

  def getDepartment(id: String): Option[Persistent[Department]] =
    departmentRepo.getById(id)

  def getDepartments: Seq[Persistent[Department]] =
    departmentRepo.getAll

  def createDepartment(name: String): Persistent[Department] =
    departmentRepo.save(Transient(Department(name)))

  def updateDepartment(id: String, name: String): Option[Persistent[Department]] =
    departmentRepo.getById(id).map(p => departmentRepo.save(p.copy(value = Department(name))))

  def deleteDepartment(id: String): Boolean =
    departmentRepo.delete(id)

}
