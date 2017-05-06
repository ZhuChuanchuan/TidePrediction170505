package com.predict.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;

public class Main {

	/**
	 * @param args
	 */
	//修改时间：2016-10-19
	//修改内容：加入潮位校正，根据前5天的数据对以后的潮位进行预测，选择参数为"今后几天"
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Connection con=null;
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			String url = "jdbc:oracle:thin:@127.0.0.1:1521:ORCL";
			String user = "Orcl_Z";
			String password = "orcl";
			con = DriverManager.getConnection(url, user, password);
			PredictionService ps=new PredictionService(con);
//			//System.out.print(0%10);
			if(ps.DoPredict(3))
				System.out.println("chenggong");
			else
				System.out.println("shibai");
			
//			String s="";
//			try {
//				s = ps.QueryPredictTide("NT", "2017/02/13", "2017/02/14");
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			//System.out.println(s);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			try {
				con.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

}
