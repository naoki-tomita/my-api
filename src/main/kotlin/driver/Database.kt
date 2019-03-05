package driver

import domain.*
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException

class Database {

    val users = UsersTable()
    val sessions = SessionTable()

    init {
        Database.connect("jdbc:sqlite:./data.db", "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = java.sql.Connection.TRANSACTION_SERIALIZABLE
        transaction {
            SchemaUtils.create(Sessions)
            SchemaUtils.create(Users)
        }
    }
}

class UsersTable {
    fun create(loginName: String, password: String): User {
        return transaction {
            try {
                val id = Users.insertAndGetId {
                    it[Users.loginName] = loginName
                    it[Users.password] = password
                }
                User(UserId(id.value), LoginName(loginName), Password(password))
            } catch (e: Exception) {
                val original = e.cause
                when (original) {
                    is SQLiteException -> {
                        if (original.resultCode == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE) {
                            throw UserAlreadyExistException(LoginName(loginName))
                        }
                        throw e
                    }
                    else -> throw e;
                }
            }
        }
    }

    fun list(): List<User> {
        return transaction { Companion.queryToUsers(Users.selectAll()) }
    }

    fun findBy(loginName: String, password: String): User {
        val users = transaction { Companion.queryToUsers(Users.select { Users.loginName eq loginName }) }
        if (users.count() == 0) {
            throw UserNotFoundException(LoginName(loginName))
        }
        if (users.first().password.value != password) {
            throw PasswordDidNotMatchException()
        }
        return users.first()
    }

    companion object {
        private fun queryToUsers(query: Query): List<User> {
            return query.map { User.from(it[Users.id].value, it[Users.loginName], it[Users.password]) }
        }
    }
}

class SessionTable {
    fun create(sessionCode: String, userId: Int): Session {
        return transaction {
            val id = Sessions.insertAndGetId {
                it[Sessions.sessionCode] = sessionCode
                it[Sessions.userId] = userId
            }
            Session(SessionId(id.value), SessionCode.from(sessionCode), UserId(userId))
        }
    }

    fun revoke(sessionCode: String) {
        transaction {
            Sessions.deleteWhere {
                Sessions.sessionCode eq sessionCode
            }
        }
    }
}

object Users: IntIdTable() {
    val loginName: Column<String> = Users.varchar("login_name", 50).uniqueIndex()
    val password: Column<String> = Users.varchar("password", 50)
}

object Sessions: IntIdTable() {
    val sessionCode: Column<String> = Sessions.varchar("session_code", 50).uniqueIndex()
    val userId: Column<Int> = Sessions.integer("user_id")
}
