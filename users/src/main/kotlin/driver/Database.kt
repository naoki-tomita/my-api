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
    fun create(loginName: String, password: String): UserEntity {
        return transaction {
            try {
                val id = Users.insertAndGetId {
                    it[Users.loginName] = loginName
                    it[Users.password] = password
                }
                UserEntity(id.value, loginName, password)
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

    fun list(): List<UserEntity> {
        return transaction { Companion.queryToUsers(Users.selectAll()) }
    }

    fun findBy(loginName: String): UserEntity? =
        transaction { Companion.queryToUsers(Users.select { Users.loginName eq loginName }) }.firstOrNull()

    fun findBy(userId: Int): UserEntity? =
        transaction { Companion.queryToUsers(Users.select { Users.id eq userId }) }.firstOrNull()

    companion object {
        private fun queryToUsers(query: Query): List<UserEntity> {
            return query.map { UserEntity(it[Users.id].value, it[Users.loginName], it[Users.password]) }
        }
    }
}

class SessionTable {
    fun create(sessionCode: String, userId: Int): SessionEntity {
        return transaction {
            val id = Sessions.insertAndGetId {
                it[Sessions.sessionCode] = sessionCode
                it[Sessions.userId] = userId
            }
            SessionEntity(id.value, sessionCode, userId)
        }
    }

    fun revoke(sessionCode: String) {
        transaction {
            Sessions.deleteWhere { Sessions.sessionCode eq sessionCode }
        }
    }

    fun findBy(sessionCode: String): SessionEntity? {
        return transaction {
            Sessions.select { Sessions.sessionCode eq sessionCode }
                .map { SessionEntity(it[Sessions.id].value, it[Sessions.sessionCode], it[Sessions.userId]) }
        }.firstOrNull()
    }
}

data class UserEntity(val id: Int, val loginName: String, val password: String)
object Users : IntIdTable() {
    val loginName: Column<String> = Users.varchar("login_name", 50).uniqueIndex()
    val password: Column<String> = Users.varchar("password", 50)
}


data class SessionEntity(val id: Int, val sessionCode: String, val userId: Int)
object Sessions : IntIdTable() {
    val sessionCode: Column<String> = Sessions.varchar("session_code", 50).uniqueIndex()
    val userId: Column<Int> = Sessions.integer("user_id")
}
