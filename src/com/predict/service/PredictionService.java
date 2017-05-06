package com.predict.service;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import net.sf.json.JSONObject;


public class PredictionService {
	private Connection conn=null;
	// String
	// keepFilePath=this.getServlet().getServletContext().getRealPath("files");
//	String keepPath = System.getProperty("user.dir").substring(0,
//			System.getProperty("user.dir").lastIndexOf("\\"))
//			+ "\\webapps\\DemoPredictTide\\files";
	String keepPath="d:\\PredictTide\\toll";
	String startDate = "";
	String endDate = "";
	String indexFileName = "";// 训练参数
	String TQFileName = "TQ"; // TQ流量文件
	//当前日期
	Date dt=new Date();
    
	
	public PredictionService(Connection con){
		conn=con;
	}
	public boolean DoPredict(int predictInterval) { //predictInterval 预测多少天
		 String stations[] = { "NT", "JY", "TSG", "XLJ","NJ"};
		
		 //修改 2016-11-10 预测前5天水位
		 Calendar c=Calendar.getInstance();
		 c.setTime(dt);
		 c.add(Calendar.DAY_OF_YEAR, -5);
		 SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd");
		 startDate=sdf.format(c.getTime());
		 
		 Calendar cl = Calendar.getInstance();						
		 cl.setTime(dt);
		 cl.add(Calendar.DAY_OF_YEAR, 0);
		 endDate=sdf.format(cl.getTime());
		 System.out.println(startDate);
		 System.out.println(endDate);
		for (int i = 0; i < stations.length; i++) {
			String indexFilePath = keepPath + "\\"+stations[i]+"Index";
			File indexFile = new File(indexFilePath);
			if (indexFile.exists()) { //调和常数存在
				if (!PredictTide(stations[i]))
				{
					System.out.println(++i+"pridictTide()返回为false");
					return false;
				}
				System.out.println("--------"+stations[i]+"预测完成-------------");
			}else{
				System.out.println("没有"+stations[i]+"调和常数");
			}
			
		}
		
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("------------所有站点预测完成-------------");
		return true;
	}
	//执行单个站点的站点预测
	private boolean PredictTide (String station)  {
		indexFileName = station + "Index";		 
		  
		String stationName = "";
		if(station.equals("NT")){
			stationName="南通";
		} else if (station.equals("JY")) {
			stationName = "江阴";
		} else if (station.equals("TSG")) {
			stationName = "天生港";
		} else if (station.equals("XLJ")) {
			stationName = "徐六泾";
		}else if (station.equals("NJ")) {
			stationName = "南京";
		}
		//TODO:求一元线性回归
		RegressionLineService regreesionLineService=new RegressionLineService();
		RegressionLine  regressionLine=regreesionLineService.RLService(startDate, endDate, stationName,indexFileName,keepPath,conn);
		float k=regressionLine.getA1();
		float b=regressionLine.getA0();
		System.out.println("y="+k+"x+"+b);
		
		if (getFlow2File(startDate, endDate, stationName))
		{
			FileWriter preInit_fw = null;
			FileReader preResult_fr = null;
			BufferedReader preResult_br = null;
			try {

				String preResultPath = keepPath + "\\tideResult";
				File preResultFile = new File(preResultPath);
				if (preResultFile.exists()) {
					preResultFile.delete();
				}
				preResultFile.createNewFile();

				// 预测参数文件PreInit参数：spanHours，IndexPro，TQ
				int daysSpan = CommonMethod.daysBetween(startDate, endDate);
				int spanHours = daysSpan * 60;
				String preInitFilePath = keepPath + "\\PreInit";
				File preInitFile = new File(preInitFilePath);
				if (preInitFile.exists()) {
					preInitFile.delete();
				}
				preInitFile.createNewFile();
				preInit_fw = new FileWriter(preInitFile, true);
				preInit_fw.write(String.valueOf(spanHours));
				preInit_fw.write("\r\n");
				preInit_fw.write(indexFileName);
				preInit_fw.write("\r\n");
				preInit_fw.write(TQFileName);
				preInit_fw.write("\r\n");
				preInit_fw.flush();
				preInit_fw.close();

				// 调线程运行预测算法，写入Result
				Process p = null;
				Runtime rt = Runtime.getRuntime();
				try {
					String fileName = "\\Prediction.exe";
					File dir = new File(keepPath);
					p = rt.exec(keepPath + fileName, null, dir);
					p.waitFor();
					p.getOutputStream().close();
				} catch (Exception e) {
					System.out.println(e);
				}

				// TODO: 返回文件数据
				
		        
			    preResult_fr = new FileReader(preResultPath);
			    preResult_br = new BufferedReader(preResult_fr);
			    String strLine="";
				while ((strLine = preResult_br.readLine()) != null) {
					if (strLine != "") {
						String[] strs = strLine.split("\\s+");
						DecimalFormat df = new DecimalFormat("#.00");
						double time = Double.parseDouble(strs[1]) - spanHours;
						time = Double.valueOf(df.format(time)); // 四舍五入保留两位小数点
						double tide = Double.parseDouble(strs[2]);
						tide = Double.valueOf(df.format(tide));
						Date dateTime = GetDate(time);

						//进行潮位校正 y=kx+b
						String sTide=Double.toString(tide);
						if(!Float.isNaN(k)&&!Float.isNaN(b)){
							DecimalFormat dformat = new DecimalFormat("######0.00"); 
							tide=k*tide+b;
							sTide=dformat.format(tide);
						}
						// 插入预测表，参数：站点，时间，潮位
						Calendar cl = Calendar.getInstance();						
						cl.setTime(dateTime);
						if (station.equals("NJ")) {									
							cl.add(Calendar.DAY_OF_YEAR, 1);
						} else if (station.equals("JY")) {							
    						cl.add(Calendar.DAY_OF_YEAR, 2);
						} else if (station.equals("TSG")) {
							cl.add(Calendar.DAY_OF_YEAR, 2);
							cl.add(Calendar.HOUR, 1);
							cl.add(Calendar.MINUTE, 30);
						} else if (station.equals("XLJ")) {
							cl.add(Calendar.DAY_OF_YEAR, 2);
							cl.add(Calendar.HOUR, 2);							
						}else if (station.equals("NT")) {							
    						cl.add(Calendar.DAY_OF_YEAR, 2);
						}
						dateTime=cl.getTime();
						java.sql.Date stationTime = new java.sql.Date(dateTime.getTime());						
						DateFormat df2 = DateFormat.getDateTimeInstance();
						
						DateFormat df0=new SimpleDateFormat("yyyy-MM-dd");
						Date today=new Date();
						String tideDate=df0.format(stationTime);
						String todayDate=df0.format(today);
						if(tideDate.compareTo(todayDate)>=0){
							PreparedStatement prep = null;
							PreparedStatement prep2 = null;
							String delSql="delete from tb_PredictionData where preTime=to_date(?,'yyyy-mm-dd hh24:mi:ss') and StationName=?";
							prep=conn.prepareStatement(delSql);
							prep.setString(1, df2.format(dateTime).toString());
							prep.setString(2,stationName);
							prep.executeUpdate();
							String sql = "insert into tb_PredictionData (StationName,preTime,preTide) values(?,to_date(?,'yyyy-mm-dd hh24:mi:ss'),?)";
							prep2 = conn.prepareStatement(sql);
							prep2.setString(1, stationName);												
							prep2.setString(2, df2.format(dateTime).toString());
							prep2.setString(3, sTide);
							prep2.executeUpdate();
							prep.close();
							prep2.close();
						}
					}
				}

				preResult_br.close();
				preResult_fr.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		System.out.println(stationName+"预测完成");
		return true;
	}
	private Date GetDate(double h) {
		double m = h - (int) h;
		m *= 60;

		Calendar cal = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			cal = Calendar.getInstance();
			cal.setTime(sdf.parse(startDate));
			cal.add(Calendar.MINUTE, (int) m);
			cal.add(Calendar.HOUR, (int) h);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cal.getTime();
	}
	// 从数据库读入要预测的流量写入文件
	private boolean getFlow2File(String startDate, String endDate,String station) {
		TreeMap tideTreeMap = new TreeMap();
		
		PreparedStatement pre = null;
		ResultSet result = null;
		try {
			String sql = "select to_char(SJ,'yyyy/MM/dd HH:mm:ss'),RKLL from t_szhd_rgswllxx t where MC='大通' and SJ>=to_date(?,'yyyy-MM-dd hh:mi:ss') and SJ<to_date(?,'yyyy-MM-dd hh:mi:ss') order by SJ";
			pre = conn.prepareStatement(sql);
			pre.setString(1,startDate);
			pre.setString(2,endDate);
			result = pre.executeQuery();
			//System.out.print("111");
			while (result.next()) {
				if(result.getString(2)==null)
				{
					System.out.println("没有入库大通流量");
					return false;
				}
				tideTreeMap.put(result.getString(1), result.getString(2));
			}

			// 将数据写入TQ文件
			WriteFlow2TQ(tideTreeMap);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				result.close();
				pre.close();				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}
	//写入流量到TQ文件
	private void WriteFlow2TQ(TreeMap tideTreeMap) {
		// 1.创建文件，2.处理数据，3.写入
		String tqFilepath = keepPath + "\\" + TQFileName;
		File tqfile = new File(tqFilepath);
		FileWriter fw = null;

		int daysSpan = 0;
		try {
			daysSpan = CommonMethod.daysBetween(startDate, endDate);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int interval = 30;// interval为时间间隔
		int dayPointNum = (60 * 24) / interval;
		int totalPointNum = dayPointNum * daysSpan;

		double time = 0;
		LinkedList<Double> qList = new LinkedList<Double>();
		Iterator ite = tideTreeMap.keySet().iterator();
		while (ite.hasNext()) {
			String date = (String) ite.next();
			Double q = Double.parseDouble(tideTreeMap.get(date).toString()) / 10000;
			qList.add(q);
		}
		try {
			if (tqfile.exists())
				tqfile.delete();
			tqfile.createNewFile();
			fw = new FileWriter(tqfile, true);
			fw.write(totalPointNum + "\r\n");
			int num = qList.size();
			if (daysSpan == qList.size()) {
				for (int i = 0; i < daysSpan; i++) {
					for (int j = 0; j <= dayPointNum; j++) {
						time = time + (double) interval / 60;
						String timeQStr = "";
						if (i < num - 1) {
							// String timeQStr=time+" "+qList.get(i);
							timeQStr = time
									+ " "
									+ (qList.get(i) + (qList.get(i + 1) - qList
											.get(i))
											* (time - i * 24) / 24);
						} else {
							timeQStr = time + " " + qList.get(i);
						}
						fw.write(timeQStr);
						fw.write("\r\n");
						// System.out.println(timeQStr);
					}
				}
			} else {
				System.out.println("WriteFlow2TQ()时间间隔出错");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	

	//查询预测流量
	public String QueryPredictTide(String station, String startDate,
			String endDate) throws ParseException {
		String stationName = "";
		if (station.equals("NJ")) {
			stationName = "南京";
		} else if (station.equals("JY")) {
			stationName = "江阴";
		} else if (station.equals("TSG")) {
			stationName = "天生港";
		} else if (station.equals("XLJ")) {
			stationName = "徐六泾";
		}else if(station.equals("NT")){
			stationName="南通";
		}
		//截止日期加一天,用于sql语句控制
//		SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//		Date queryEndDate=sdf.parse(endDate);
//		Calendar cl=Calendar.getInstance();
//		cl.setTime(queryEndDate);
//		cl.add(Calendar.DAY_OF_YEAR, 1);
//		String finalDate=cl.getTime().toString();
		
		JSONObject jsonObj = null;
		
		PreparedStatement pre = null;
		ResultSet result = null;
		try {
//			Class.forName("oracle.jdbc.driver.OracleDriver");
//			String url =orclUrl;
//			String user = orclUser;
//			String password = orclPass;
//			conn = DriverManager.getConnection(url, user, password);
			 //"select to_char(tTime,'yyyy/mm/dd'),FLOW from TB_TIDEPRE t where station=? and ttime>to_date(?,'yyyymmdd') and ttime<=to_date(?,'yyyymmdd') order by tTime";
			String sql = "select to_char(PreTime,'yyyy/mm/dd HH:mm:ss'),to_char(t.PreTide,'fm99999999999999999990.00') from TB_PREDICTIONDATA t where STATIONNAME=? and PRETIME>to_date(?,'yyyy/mm/dd') and PRETIME<to_date(?,'yyyy/mm/dd') order by PRETIME";
			pre = conn.prepareStatement(sql);
			pre.setString(1, stationName);
			pre.setString(2, startDate);
			pre.setString(3, endDate);
			result = pre.executeQuery();
			
			Map<String,Object> resMap=new HashMap<String,Object>();
			ArrayList categories = new ArrayList();
			ArrayList values = new ArrayList();			
            while (result.next()) {
            	 categories.add(result.getString(1));
				 values.add(result.getString(2));
				 //resMap.put(result.getString(1), result.getString(2));
				 //System.out.println(result.getString(1)+" "+ result.getString(2));
            }
            resMap.put("time", categories);
			resMap.put("tide", values);
            jsonObj=JSONObject.fromObject(resMap);
            
		}  catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				result.close();
				//pre.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return jsonObj.toString();
	}
	private static void printSums(RegressionLine line) {  
        System.out.println("\n数据点个数 n = " + line.getDataPointCount());  
        System.out.println("\nSum x  = " + line.getSumX());  
        System.out.println("Sum y  = " + line.getSumY());  
        System.out.println("Sum xx = " + line.getSumXX());  
        System.out.println("Sum xy = " + line.getSumXY());  
        System.out.println("Sum yy = " + line.getSumYY());  
  
    }  
  
    /** 
     * Print the regression line function. 
     *  
     * @param line 
     *            the regression line 
     */  
    private static void printLine(RegressionLine line) {  
        System.out.println("\n回归线公式:  y = " + line.getA1() + "x + "  
                + line.getA0());  
        System.out.println("误差：     R^2 = " + line.getR());  
    }  
}
