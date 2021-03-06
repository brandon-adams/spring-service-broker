package com.pivotal.cf.broker.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.jcraft.jsch.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//import java.util.HashMap;
//import java.util.Map;

//import com.pivotal.cf.broker.model.ServiceInstance;
@Service
public class OracleDBManager {

	private Connection dbConn;
	private String dbDriver;// = "jdbc:oracle:thin";
	private String dbSysUser;// = "SYS";
	private String dbSysPass;// = "password";
	private String dbHost;// = "192.168.4.25";
	private String dbPort;// = "1521";

	private JSch shell;
	private Session session;

	// private Channel channel;
	@Autowired
	public OracleDBManager(String dbDriver, String dbSysUser, String dbSysPass,
			String dbHost, String dbPort) {

		this.dbDriver = dbDriver;
		this.dbSysUser = dbSysUser;
		this.dbSysPass = dbSysPass;
		this.dbHost = dbHost;
		this.dbPort = dbPort;
		
		dbConn = null;
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			Properties props = new Properties();
			props.put("user", dbSysUser);
			props.put("password", dbSysPass);
			props.put("internal_logon", "SYSDBA");
			dbConn = DriverManager.getConnection(dbDriver + ":@" + dbHost + ":"
					+ dbPort + ":", props);

			shell = new JSch();

			File dir = new File("util");

			File[] list = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					return filename.endsWith(".pem");
				}
			});

			shell.addIdentity(list[0].getAbsolutePath());
			shell.setKnownHosts("~/.ssh/known_hosts");
			session = shell.getSession("root", dbHost, 22);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}

		System.out.println("Conneciton open");
	}

	public String createDB(String dbName) {

		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			// String sql = "create user " + dbName + " identified by " + pass;
			boolean dbfs = createOracleFS(dbName);
			// System.out.println(dbfs);
			if (!dbfs) {
				// System.out.println("HELP");
				throw new Exception(
						"File system creation failed. Look into this man.");
			}
			String sql = createDBStmt(dbName);
			// System.out.println("Executing create sql statement");
			// stmnt.addBatch(sql);
			// stmnt.addBatch("STARTUP NOMOUNT");
			System.out.println("Executing create sql statement");
			stmnt.addBatch("CREATE DATABASE " + dbName + " "
					+ "USER SYS IDENTIFIED BY " + dbSysPass + " "
					+ "USER SYSTEM IDENTIFIED BY " + dbSysPass + " "
					+ "UNDO TABLESPACE undotbs "
					+ "DEFAULT TEMPORARY TABLESPACE tempts1");
			System.out.println("Executing catalog.sql");
			stmnt.addBatch("@$ORACLE_HOME/rdbms/admin/catalog.sql");
			System.out.println("Executing catproc.sql");
			stmnt.addBatch("@$ORACLE_HOME/rdbms/admin/catproc.sql");
			System.out.println("Executing pubbld.sql");
			stmnt.addBatch("@$ORACLE_HOME/sqlplus/admin/pupbld.sql");
			for (int i = 0; i < stmnt.executeBatch().length; i++) {
				System.out.println(i);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			// return null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			// return null;
		}
		System.out.println("Create success");
		return dbName;
	}

	public Map<String, String> createUser(String userName, String userPass,
			String dbName) {

		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			// String sql = "create user " + dbName + " identified by " + pass;
			String sql = createUserStmt(userName, userPass, dbName);
			stmnt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("Create success");
		Map<String, String> dbUri = new HashMap<String, String>();
		dbUri.put("driver", dbDriver);
		dbUri.put("host", dbHost);
		dbUri.put("port", dbPort);
		return dbUri;
	}

	public String showDB(String dbName) {
		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			String sql = "SELECT * FROM " + dbName;
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

	public String deleteDB(String dbName) {
		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			String sql = "DROP DATABASE " + dbName + "";
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

	public String deleteUser(String userName) {
		Statement stmnt = null;
		try {
			stmnt = dbConn.createStatement();
			String sql = "DROP USER " + userName + " CASCADE";
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

	private boolean createDBInitFile(String dbName) {
		boolean exit = true;
		try {
			session.connect();
			Channel channel = session.openChannel("exec");
			// ((ChannelExec) channel).setCommand("export TEST_ENV=" + dbName +
			// "; echo $TEST_ENV");
			((ChannelExec) channel).setCommand("sh db_init_file.sh " + dbName);
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);
			InputStream in = channel.getInputStream();
			channel.connect();

			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					System.out.print(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					System.out.println("exit-status: "
							+ channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
					System.err.println(ee.getClass().getName() + ": "
							+ ee.getMessage());
				}
			}
			if (channel.getExitStatus() != 0) {
				exit = false;
			}
			channel.disconnect();
			session.disconnect();
			// Runtime.getRuntime().exec("scp -i util/heat_key.pem -r util/scripts root@"
			// + dbHost + ":~/");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			exit = false;
			// System.exit(0);
		}
		return exit;
	}

	private boolean createOracleFS(String dbName) {
		boolean exit = true;
		try {
			session.connect();
			Channel channel = session.openChannel("exec");
			// ((ChannelExec) channel).setCommand("export TEST_ENV=" + dbName +
			// "; echo $TEST_ENV");
			((ChannelExec) channel).setCommand("sh new_oracle_fs.sh " + dbName);
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);
			InputStream in = channel.getInputStream();
			channel.connect();

			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					System.out.print(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					System.out.println("exit-status: "
							+ channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
					System.err.println(ee.getClass().getName() + ": "
							+ ee.getMessage());
				}
			}
			if (channel.getExitStatus() != 0)
				exit = false;
			channel.disconnect();
			((ChannelExec) channel).setCommand("sh db_init_file.sh " + dbName);
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);
			in = channel.getInputStream();
			channel.connect();

			tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					System.out.print(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					System.out.println("exit-status: "
							+ channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
					System.err.println(ee.getClass().getName() + ": "
							+ ee.getMessage());
				}
			}
			if (channel.getExitStatus() != 0) {
				exit = false;
			}
			channel.disconnect();
			session.disconnect();
			// Runtime.getRuntime().exec("scp -i util/heat_key.pem -r util/scripts root@"
			// + dbHost + ":~/");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			exit = false;
			// System.exit(0);
		} finally {

		}
		return exit;
	}

	private String createUserStmt(String userName, String userPass,
			String dbName) {
		StringBuilder createUser = new StringBuilder();
		createUser.append("CREATE USER " + userName + " ")
				.append("IDENTIFIED BY " + userPass + " ")
				.append("DEFAULT TABLESPACE " + dbName + " ")
				.append("TEMPORARY TABLESPACE temp ")
				.append("PROFILE default ")
				.append("QUOTA UNLIMITED ON " + dbName + " ");
		// .append("GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE TO "
		// + userName + "; ");
		// System.out.println(createUser.toString());
		return createUser.toString();
	}

	private String createDBStmt(String dbName) {

		StringBuilder createDB = new StringBuilder();
		createDB.append("CREATE DATABASE " + dbName + " ")
				.append("USER SYS IDENTIFIED BY " + dbSysPass + " ")
				.append("USER SYSTEM IDENTIFIED BY " + dbSysPass + " ")
				.append("LOGFILE GROUP 1 ('/u01/logs/" + dbName
						+ "/redo01a.log','/u02/logs/" + dbName
						+ "/redo01b.log') SIZE 100M BLOCKSIZE 512, ")
				.append("GROUP 2 ('/u01/logs/" + dbName
						+ "/redo02a.log','/u02/logs/" + dbName
						+ "/redo02b.log') SIZE 100M BLOCKSIZE 512, ")
				.append("GROUP 3 ('/u01/logs/" + dbName
						+ "/redo03a.log','/u02/logs/" + dbName
						+ "/redo03b.log') SIZE 100M BLOCKSIZE 512 ")
				.append("MAXLOGHISTORY 1 ")
				.append("MAXLOGFILES 16 ")
				.append("MAXLOGMEMBERS 3 ")
				.append("MAXDATAFILES 1024 ")
				.append("CHARACTER SET AL32UTF8 ")
				.append("NATIONAL CHARACTER SET AL16UTF16 ")
				.append("EXTENT MANAGEMENT LOCAL ")
				.append("DATAFILE '/u01/app/oracle/oradata/" + dbName
						+ "/system01.dbf' ")
				.append("SIZE 700M REUSE AUTOEXTEND ON NEXT 10240K MAXSIZE UNLIMITED ")
				.append("SYSAUX DATAFILE '/u01/app/oracle/oradata/" + dbName
						+ "/sysaux01.dbf' ")
				.append("SIZE 550M REUSE AUTOEXTEND ON NEXT 10240K MAXSIZE UNLIMITED ")
				.append("DEFAULT TABLESPACE users ")
				.append("DATAFILE '/u01/app/oracle/oradata/" + dbName
						+ "/users01.dbf' ")
				.append("SIZE 500M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED ")
				.append("DEFAULT TEMPORARY TABLESPACE tempts1 ")
				.append("TEMPFILE '/u01/app/oracle/oradata/" + dbName
						+ "/temp01.dbf' ")
				.append("SIZE 20M REUSE AUTOEXTEND ON NEXT 640K MAXSIZE UNLIMITED ")
				.append("UNDO TABLESPACE undotbs1 ")
				.append("DATAFILE '/u01/app/oracle/oradata/" + dbName
						+ "/undotbs01.dbf' ")
				.append("SIZE 200M REUSE AUTOEXTEND ON NEXT 5120K MAXSIZE UNLIMITED ")
				.append("USER_DATA TABLESPACE usertbs ")
				.append("DATAFILE '/u01/app/oracle/oradata/" + dbName
						+ "/usertbs01.dbf' ")
				.append("SIZE 200M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED");

		return createDB.toString();
	}

	/*public static void main(String[] args) {
		OracleDBManager test = new OracleDBManager();
		// test.createOracleFS("test");
		try {
			boolean temp = test.createDB("test").isEmpty();
			String result = (temp ? "ERROR" : "SUCCESS");
			System.out.println(result);
			// test.deleteDB("test");
			// System.out.println(test.createDBInitFile("test"));
			// test.createUser("test", "test", "system");
			// test.deleteUser("test");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}*/

}
