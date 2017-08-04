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
	String Station="";
	String StationCn="";
	String indexFileName = "";// 训练参数
	String TQFileName = "TQ"; // TQ流量文件
	//当前日期
	Date dt=new Date();
	
	Integer[] RKLLs=null; //大通前五天流量
    
	
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
			Station=stations[i];
			String indexFilePath = keepPath + "\\"+Station+"Index";
			File indexFile = new File(indexFilePath);
			if (indexFile.exists()) { //调和常数存在
				if (!PredictTide(Station))
				{
					System.out.println(++i+"pridictTide()返回为false");
					return false;
				}
				System.out.println("--------"+Station+"预测完成-------------");
			}else{
				System.out.println("没有"+Station+"调和常数");
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
		if(station.equals("NT")){
			StationCn="南通";
		} else if (station.equals("JY")) {
			StationCn = "江阴";
		} else if (station.equals("TSG")) {
			StationCn = "天生港";
		} else if (station.equals("XLJ")) {
			StationCn = "徐六泾";
		}else if (station.equals("NJ")) {
			StationCn = "南京";
		}
		
		//获取全局大通流量
		RKLLs=getRKLL();
		
		//求一元线性回归
		RegressionLineService regreesionLineService=new RegressionLineService();
		RegressionLine  regressionLine=regreesionLineService.RLService(startDate, endDate, StationCn,indexFileName,keepPath,conn,RKLLs);
		float k=regressionLine.getA1();
		float b=regressionLine.getA0();
		
		System.out.println("y="+k+"x+"+b);
		
		//预测
		if (getFlow2File(startDate, endDate, StationCn))
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
				String preInitFilePath = keepPath + "\\PreInit";
				File preInitFile = new File(preInitFilePath);
				if (preInitFile.exists()) {
					preInitFile.delete();
				}
				preInitFile.createNewFile();
				preInit_fw = new FileWriter(preInitFile, true);
				preInit_fw.write(String.valueOf(0)); //从2010/1/1开始
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
			    
			    int daysSpan = CommonMethod.daysBetween("2010/1/1", endDate);
				int spanHours = daysSpan * 24;
				
				//取出水位站ID
				ResultSet resultId = null;
				PreparedStatement prep0 = null;
				String selectSql="select ID from T_SZHD_SWJBXX where SWZMC=?";
				prep0=conn.prepareStatement(selectSql);
				prep0.setString(1,StationCn);
				resultId=prep0.executeQuery();
				
				String StationId="";
				while(resultId.next()){
					StationId=resultId.getString(1);
				}
				prep0.close();
				
				while ((strLine = preResult_br.readLine()) != null&&StationId!="") {
					if (strLine != "") {
						String[] strs = strLine.split("\\s+");
						DecimalFormat df = new DecimalFormat("#.00");
						double time = Double.parseDouble(strs[1]) - spanHours;
						time = Double.valueOf(df.format(time)); // 当天+小时数
						Date dateTime = GetDate(time);
						
						double tide = Double.parseDouble(strs[2]);
						tide = Double.valueOf(df.format(tide));
						//进行潮位校正 y=kx+b
						String sTide=Double.toString(tide);
						if(!Float.isNaN(k)&&!Float.isNaN(b)){
							DecimalFormat dformat = new DecimalFormat("######0.00"); 
							tide=k*tide+b;
							sTide=dformat.format(tide);
						}
						
						java.sql.Date stationTime = new java.sql.Date(dateTime.getTime());						
						DateFormat df2 = DateFormat.getDateTimeInstance();
						
						DateFormat df0=new SimpleDateFormat("yyyy-MM-dd");
						Date today=new Date();
						String tideDate=df0.format(stationTime);
						String todayDate=df0.format(today);
						if(tideDate.compareTo(todayDate)>=0){
							PreparedStatement prep = null;
							PreparedStatement prep2 = null;
							String delSql="delete from tb_PredictionData where preTime=to_date(?,'yyyy-mm-dd hh24:mi:ss') and StationId=?";
							prep=conn.prepareStatement(delSql);
							prep.setString(1, df2.format(dateTime).toString());
							prep.setString(2,StationId);
							prep.executeUpdate();
							String sql = "insert into tb_PredictionData (StationId,StationName,preTime,preTide) values(?,?,to_date(?,'yyyy-mm-dd hh24:mi:ss'),?)";
							prep2 = conn.prepareStatement(sql);
							prep2.setString(1, StationId);	
							prep2.setString(2, StationCn);												
							prep2.setString(3, df2.format(dateTime).toString());
							prep2.setString(4, sTide);
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
		System.out.println(StationCn+"预测完成");
		return true;
	}
	private Date GetDate(double h) {
		double m = h - (int) h;
		m *= 60;

		Calendar cal = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			cal = Calendar.getInstance();
			cal.setTime(sdf.parse(endDate));
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
		try{
			CreateTQ();
		}catch(Exception e){
			return false;
		}
		return true;
	}
	private Integer[] getRKLL(){
		//取大通流量
		PreparedStatement pre = null;
		ResultSet result = null;
		String sql = "select to_char(SJ,'yyyy/MM/dd HH:mm:ss'),RKLL from t_szhd_rgswllxx t where MC='大通' and SJ>=to_date(?,'yyyy-MM-dd hh:mi:ss') and SJ<to_date(?,'yyyy-MM-dd hh:mi:ss') order by SJ";
		Integer[] RKLL=new Integer[5];
		try {
			pre = conn.prepareStatement(sql);
			pre.setString(1,startDate);
			pre.setString(2,endDate);
			result = pre.executeQuery();
			
			int x=0;
			while (result.next()) {
				if(result.getString(2)==null)
				{
					System.out.println("没有入库大通流量");
					try {
						throw new Exception();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try{
					RKLL[x++]=Integer.parseInt(result.getString(2));
					
				}catch(Exception e){
					System.out.println("大通流量出错");
				}
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return RKLL;
		
	}
	private void CreateTQ() {
		// 2017-4-19 15:22:35 TODO:前两天的实测潮位写入TQ
		// 1.获取前3天实测数据  2.组织写入TQ 总条数 时间 rkll
		
		ArrayList<String> tqRows=new ArrayList<String>();
		int daySpan=0;
		try {
			daySpan = CommonMethod.daysBetween("2010/1/1",new SimpleDateFormat("yyyy/MM/dd").format(new Date()));
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//float hourSpan=daySpan*24+tempHour+tempMinute/60f;
		//第一条数据从00:04:00开始
		
		float first=daySpan*24+4/60f; 
		Calendar calendar=Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 4);
        calendar.set(Calendar.SECOND, 0);
        double rkll=CommonMethod.getInterpolationByDate(calendar.getTime(), RKLLs);
		tqRows.add(first+" "+rkll/10000);
		
		int twoDayCursor=2;
		while(twoDayCursor<=480){

			float hourSpan=first+6*twoDayCursor/60f;
			
			calendar.add(Calendar.MINUTE, 6);
			rkll=CommonMethod.getInterpolationByDate(calendar.getTime(), RKLLs);
			System.out.println(calendar.getTime()+" "+hourSpan+" "+rkll);
			tqRows.add(hourSpan+" "+rkll/10000);
			twoDayCursor++;
		}
		// count tqRows写入TQ
		String TQFilePath = keepPath + "\\TQ";
		File TQFile = new File(TQFilePath);
		if (TQFile.exists()) {
			TQFile.delete();
		}
		FileWriter tq_fw = null;

		try {
			TQFile.createNewFile();
			tq_fw = new FileWriter(TQFile, true);
			tq_fw.write(String.valueOf(480)); // 两天总数（每六分钟一条）
			tq_fw.write("\n");

			Iterator<String> it = tqRows.iterator();
			while (it.hasNext()) {
				tq_fw.write(it.next());
				tq_fw.write("\n");
			}

			tq_fw.flush();
			tq_fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	// 查询预测流量
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
		
		
		JSONObject jsonObj = null;
		
		PreparedStatement pre = null;
		ResultSet result = null;
		try {
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
	 
}
