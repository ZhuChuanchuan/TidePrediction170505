package com.predict.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.lang.Process;

public class RegressionLineService {
	private String StartDate;  //startDate, endDate, stationName,indexFileName,keepPath,conn
	private String EndDate;
	private String IndexFileName;
	private String KeepPath;
	private Connection Conn;
	private String Station;
	
	Integer[] RKLLs=null;

	public RegressionLine RLService(String startDate,String endDate,String station,String indexFileName,String keepPath,Connection conn,Integer[] rklls){
		
		StartDate=startDate;
		EndDate=endDate;
		IndexFileName=indexFileName;
		KeepPath=keepPath;
		Conn=conn;
		Station=station;
		
		RKLLs=rklls;
		
		//组织文件 Index TQ
		RegressionLine line = new RegressionLine();
		
		FileWriter preInit_fw = null;
		FileReader preResult_fr = null;
		BufferedReader preResult_br = null;
		
		String preResultPath = KeepPath + "\\tideResult";
		File preResultFile = new File(preResultPath);
		if (preResultFile.exists()) {
			preResultFile.delete();
		}
		String preInitFilePath = KeepPath + "\\PreInit";
		File preInitFile = new File(preInitFilePath);
		if (preInitFile.exists()) {
			preInitFile.delete();
		}
		try {
			preResultFile.createNewFile();
			// 1.预测参数文件PreInit参数：spanHours，IndexPro，TQ
			preInitFile.createNewFile();
			preInit_fw = new FileWriter(preInitFile, true);
			preInit_fw.write("0");
			preInit_fw.write("\r\n");
			preInit_fw.write(IndexFileName);
			preInit_fw.write("\r\n");
			preInit_fw.write("TQ");
			preInit_fw.write("\r\n");
			preInit_fw.flush();
			preInit_fw.close();

			// 2.TQ文件 返回实测数据
			ArrayList<Float> scTideList=CreateTQ();
			
			
			// 调线程运行预测算法，写入Result
			Process p = null;
			Runtime rt = Runtime.getRuntime();
			try {
				String fileName = "\\Prediction.exe";
				File dir = new File(KeepPath);
				p = rt.exec(KeepPath + fileName, null, dir);
				p.waitFor();
				p.getOutputStream().close();
			} catch (Exception e) {
				System.out.println(e);
			}
			
			preResult_fr = new FileReader(preResultPath);
			preResult_br = new BufferedReader(preResult_fr);
			String strLine = null;
			ArrayList<Float> preTideList=new ArrayList<Float>();
			
			while (((strLine = preResult_br.readLine()) != null)) {
				if (strLine != "") {
					String[] strs = strLine.split("\\s+");
					DecimalFormat df = new DecimalFormat("#.00");
					double time = Double.parseDouble(strs[1]);
					time = Double.valueOf(df.format(time)); // 四舍五入保留两位小数点
					Float tide = Float.parseFloat(strs[2]);
					tide = Float.valueOf(df.format(tide));
					preTideList.add(tide);
				}
			}
			// 预测数据preTideList 实测数据 scTideList 进行线性回归
			int count=0;
			if(preTideList.size()==scTideList.size()){
				Iterator<Float> it=scTideList.iterator();
				Iterator<Float> it2=preTideList.iterator();
				while(it.hasNext()&&it2.hasNext()){
					Float scTide=it.next();
					Float preTide=it2.next();
					if(preTide/scTide>=0.75&&preTide/scTide<=1.25){
						line.addDataPoint(new DataPoint(preTide,scTide));
						count++;
					}
				}
				System.out.println("总有线性"+count);
				 
			}else{
				System.out.println("线性回归数据有误");
			}
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
		return line;
	}

	private ArrayList<Float> CreateTQ() {
		//大通流量插值 2017-8-3 21:43:29
		// 2017-4-19 15:22:35 TODO:前两天的实测潮位写入TQ
		// 1.获取前3天实测数据  2.组织写入TQ 总条数 时间 rkll
		
		//取大通流量(全局变量）
		PreparedStatement pre = null;
		ResultSet result = null;
		
		ArrayList<Float> scTide=new ArrayList<Float>();
		String sql2 = "select CLSW,CLSJ from T_SZHD_SWDTSLXX t where SWZID=(select ID from T_SZHD_SWJBXX where SWZMC=?) "
    		+ "and CLSJ<to_date(? ,'yyyy-MM-dd') "
    		+ "and CLSJ>=to_date(?,'yyyy-MM-dd') order by CLSJ";
	    try {
			try {
				pre = Conn.prepareStatement(sql2);
				java.sql.Date sDate = new java.sql.Date(new Date().getTime());
				Calendar cl2 = Calendar.getInstance();						
				cl2.setTime(new Date());
				cl2.add(Calendar.DAY_OF_YEAR,-3);
				java.sql.Date eDate = new java.sql.Date(cl2.getTime().getTime());
				pre.setString(1,Station);
				pre.setString(2,sDate.toString());
				pre.setString(3, eDate.toString());
				result=pre.executeQuery(); //前3天实测水位
				int count=0; //总数
				
				ArrayList<String> tqRows=new ArrayList<String>();
				while (result.next()) {
					if(result.getString(1)==null)
						continue;
					
					Double clsw = Double.parseDouble(result.getString(1));
					String clsj = result.getString(2);
					DecimalFormat df = new DecimalFormat("#.00");
					Float tide = Float.valueOf(df.format(clsw));
					scTide.add(tide);
					
					SimpleDateFormat FORMAT2 = new SimpleDateFormat("yyyy-M-d H:m:s");
					Date tempTime = FORMAT2.parse(clsj);
					
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(tempTime);
					int tempHour=calendar.get(Calendar.HOUR_OF_DAY);
					int tempMinute=calendar.get(Calendar.MINUTE);
					DateFormat df0=new SimpleDateFormat("yyyy/MM/dd");
					String tideDate=df0.format(tempTime);
					//String todayDate=df0.format(new Date());
					int daySpan=CommonMethod.daysBetween("2010/1/1",tideDate);  
					float hourSpan=daySpan*24+tempHour+tempMinute/60f;
					
					//hourSpan=hourSpan; //相位差
					//System.out.println(tempTime+" "+hourSpan+" "+daySpan+" "+tempHour);
					/*
					Float rkll=0f;
					if(CommonMethod.daysBetween(tideDate, todayDate)==1){
						rkll=RKLL[2]; //第前3天
					}else if(CommonMethod.daysBetween(tideDate, todayDate)==2){
						rkll=RKLL[1]; //第前4天
					}else if(CommonMethod.daysBetween(tideDate, todayDate)==3){
						rkll=RKLL[0]; //第前5天
					}
					*/
					
					Double rkll=CommonMethod.getInterpolationByDate(tempTime, RKLLs);
					System.out.println(tempTime+" "+hourSpan+" "+rkll);
					tqRows.add(hourSpan+" "+rkll/10000);
					count++;
				}
				System.out.println(count);
				// count tqRows写入TQ
				String TQFilePath = KeepPath + "\\TQ";
				File TQFile = new File(TQFilePath);
				if (TQFile.exists()) {
					TQFile.delete();
				}
				FileWriter tq_fw = null;
				try {
					TQFile.createNewFile();
					tq_fw = new FileWriter(TQFile, true);
					tq_fw.write(String.valueOf(count));
					tq_fw.write("\n");
					
					Iterator<String> it=tqRows.iterator();
					while(it.hasNext()){
						tq_fw.write(it.next());
						tq_fw.write("\n");
					}
					
					tq_fw.flush();
					tq_fw.close();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return scTide;
	}
}
