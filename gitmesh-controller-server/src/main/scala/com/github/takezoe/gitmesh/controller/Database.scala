package com.github.takezoe.gitmesh.controller

import java.sql.Connection
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import syntax._

object Database {

  private var dataSource: HikariDataSource = null

  private def createDataSource(config: DatabaseConfig): HikariDataSource = {
    new HikariDataSource(new HikariConfig().unsafeTap { hikariConfig =>
      hikariConfig.setDriverClassName(config.driver)
      hikariConfig.setJdbcUrl(config.url)
      hikariConfig.setUsername(config.user)
      hikariConfig.setPassword(config.password)
      hikariConfig.setAutoCommit(false)
      config.connectionTimeout.foreach(hikariConfig.setConnectionTimeout)
      config.idleTimeout.foreach(hikariConfig.setIdleTimeout)
      config.maxLifetime.foreach(hikariConfig.setMaxLifetime)
      config.minimumIdle.foreach(hikariConfig.setMinimumIdle)
      config.maximumPoolSize.foreach(hikariConfig.setMaximumPoolSize)
    })
  }

  def initializeDataSource(config: DatabaseConfig): Unit = {
    dataSource = createDataSource(config)
  }

  def closeDataSource(): Unit = {
    dataSource.close()
  }

  def withTransaction[T](conn: Connection)(f: => T): T = {
    conn.setAutoCommit(false)
    try {
      val result = f
      conn.commit()
      result
    } catch {
      case e: Exception =>
        conn.rollback()
        throw e
    }
  }

  def withConnection[T](f: (Connection) => T): T = {
    using(dataSource.getConnection){ conn =>
      try {
        f(conn)
      } finally ignoreException {
        conn.close()
      }
    }
  }

}