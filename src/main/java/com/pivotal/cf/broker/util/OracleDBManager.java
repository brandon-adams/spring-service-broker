package com.pivotal.cf.broker.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.stereotype.Service;
//import java.util.HashMap;
//import java.util.Map;

//import com.pivotal.cf.broker.model.ServiceInstance;
@Service
public class OracleDBManager {

	private Connection dbConn;
	private final String dbSysUser = "SYSTEM";
	private final String dbSysPass = "password";
	private final String dbHost = "192.168.4.25";
	
	public OracleDBManager(){
		
		dbConn = null;
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			dbConn = DriverManager.getConnection("jdbc:oracle:thin:@" + dbHost + ":1521:", dbSysUser, dbSysPass);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		//System.out.println("Conneciton open");
	}
	
	public String createDB(String dbName){
		
		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			//String sql = "create user " + dbName + " identified by " + pass;
			String sql = createDBStmt(dbName);
			stmnt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Create success");
		return dbName;
	}
	
	public String createUser(String userName, String userPass, String dbName){
		
		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			//String sql = "create user " + dbName + " identified by " + pass;
			String sql = createUserStmt(userName, userPass, dbName);
			stmnt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Create success");
		return userName;
	}
	
	public String showDB(String dbName){
		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			String sql = "show database " + dbName + ";";
			stmnt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return dbName;
	}
	
	public String deleteDB(String dbName){
		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			String sql = "DROP USER " + dbName;
			stmnt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Delete success");
		return dbName;
	}
	
	public String deleteUser(String userName){
		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			String sql = "DROP USER " + userName + ";";
			stmnt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Delete success");
		return userName;
	}
	
	private String createUserStmt(String userName, String userPass, String dbName){
		StringBuilder createDB = new StringBuilder();
		createDB.append("CREATE USER " + userName + " ")
			.append("IDENTIFIED BY " + userPass + " ")
			.append("DEFAULT TABLESPACE " + dbName + " ")
			.append("TEMPORARY TABLESPACE temp ")
			.append("PROFILE default; ")
			.append("ALTER USER " + userName + " QUOTA UNLIMITED ON " + dbName + "; ")
			.append("GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE TO " + userName + "; ");
			
		return null;
	}
	
	private String createDBStmt(String dbName){
		StringBuilder createDB = new StringBuilder();
		createDB.append("CREATE DATABASE mynewdb ")
			.append("USER SYS IDENTIFIED BY " + dbSysPass + " ")
			.append("USER SYSTEM IDENTIFIED BY " + dbSysPass + " ")
			.append("LOGFILE GROUP 1 ('/u01/logs/my/redo01a.log','/u02/logs/my/redo01b.log') SIZE 100M BLOCKSIZE 512, ")
			.append("GROUP 2 ('/u01/logs/my/redo02a.log','/u02/logs/my/redo02b.log') SIZE 100M BLOCKSIZE 512, ")
			.append("GROUP 3 ('/u01/logs/my/redo03a.log','/u02/logs/my/redo03b.log') SIZE 100M BLOCKSIZE 512 ")
			.append("MAXLOGHISTORY 1 ")
			.append("MAXLOGFILES 16 ")
			.append("MAXLOGMEMBERS 3 ")
			.append("MAXDATAFILES 1024 ")
			.append("CHARACTER SET AL32UTF8 ")
			.append("NATIONAL CHARACTER SET AL16UTF16 ")
			.append("EXTENT MANAGEMENT LOCAL ")
			.append("DATAFILE '/u01/app/oracle/oradata/mynewdb/system01.dbf' ")
			.append("SIZE 700M REUSE AUTOEXTEND ON NEXT 10240K MAXSIZE UNLIMITED ")
			.append("SYSAUX DATAFILE '/u01/app/oracle/oradata/mynewdb/sysaux01.dbf' ")
			.append("SIZE 550M REUSE AUTOEXTEND ON NEXT 10240K MAXSIZE UNLIMITED ")
			.append("DEFAULT TABLESPACE users ")
			.append("DATAFILE '/u01/app/oracle/oradata/mynewdb/users01.dbf' ")
			.append("SIZE 500M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED ")
			.append("DEFAULT TEMPORARY TABLESPACE tempts1 ")
			.append("TEMPFILE '/u01/app/oracle/oradata/mynewdb/temp01.dbf' ")
			.append("SIZE 20M REUSE AUTOEXTEND ON NEXT 640K MAXSIZE UNLIMITED ")
			.append("UNDO TABLESPACE undotbs1 ")
			.append("DATAFILE '/u01/app/oracle/oradata/mynewdb/undotbs01.dbf' ")
			.append("SIZE 200M REUSE AUTOEXTEND ON NEXT 5120K MAXSIZE UNLIMITED ")
			.append("USER_DATA TABLESPACE usertbs ")
			.append("DATAFILE '/u01/app/oracle/oradata/mynewdb/usertbs01.dbf' ")
			.append("SIZE 200M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;");
		
		return null;
	}
	
}
