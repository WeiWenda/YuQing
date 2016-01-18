package org.apache.java.jdbc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class MySqlData implements Serializable{
    public MySqlData(String hostname,String dbname,String dbuser,String dbpwd){
        outputDataSqlPath="jdbc:mysql://"+hostname+":3306/"+dbname+"?useUnicode=true&characterEncoding=utf-8";
        outputDataSqlUsername=dbuser;
        outputDataSqlPassword=dbpwd;
    }
    private String outputDataSqlPath=null;
    private String outputDataSqlUsername =null;
    private String outputDataSqlPassword = null;
    public Statement stmt;
    public Connection conn;

    public boolean connectOutputDatabaseTxt() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            //		System.out.println("数据库驱动加载成功");
        } catch (Exception e) {
            System.out.println("数据库驱动加载失败" + e.getMessage());
            return false;
        }
        try {
            conn = DriverManager.getConnection(this.outputDataSqlPath, this.outputDataSqlUsername, this.outputDataSqlPassword);
            stmt = conn.createStatement();
            //		System.out.println("数据库连接成功");
        } catch (Exception e) {
            System.out.println("数据库连接失败");
            return false;
        }
        return true;
    }

    public boolean closeConnStmt() {
        try {
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }

            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.err.println("连接关闭失败");
            return false;
        }
        return true;

    }
}

