package org.example.additional

import java.sql.DriverManager

fun main() {
    DriverManager.getConnection("jdbc:sqlite:data.db")
        .use { connection ->
            val statement = connection.createStatement()
            statement.executeUpdate(
                """
                      CREATE TABLE IF NOT EXISTS "words" (
                          "id" integer PRIMARY KEY,
                          "text" varchar,
                          "translate" varchar
                      );
              """.trimIndent()
            )
            statement.executeUpdate("insert into words values(0, 'hello', 'привет')")
        }
}