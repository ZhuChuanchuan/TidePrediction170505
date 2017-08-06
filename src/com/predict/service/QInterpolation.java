package com.predict.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;
import java.util.Map;

public class QInterpolation {

    public QInterpolation() {

    }

    public static void main(String[] args) {
    	
    	//1，读入全年大通流量数据
    	Integer[] DaTong=ReadDaTong();
    	
    	//2，构造线性插值函数
        Map<Date,Integer> map=new TreeMap<Date,Integer>();
        Calendar now=Calendar.getInstance();
        now.set(2013,0,1,0,0,0);
        for(int i=0;i<DaTong.length;i++){
    		//System.out.println(DaTong[i]);
            map.put(now.getTime(),DaTong[i]);
            now.add(Calendar.DAY_OF_YEAR, 1);
            
    	}
          
        Calendar instance=Calendar.getInstance();
        instance.set(2010,0,1,0,0,0);
        
        
        BufferedWriter writer=null;
        try {
        	File file=new File("大通2013插值后.txt");
            if(file.exists()){
            	file.delete();
            }
			file.createNewFile();
			writer=new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        Calendar start=Calendar.getInstance();
        start.set(2013,0,1,0,0,0);
        Calendar end=Calendar.getInstance();
        end.set(2013,11,31,0,0,0);
        
        int count=0;
        //3，获取插值，写入文件
        try {
    		while(start.compareTo(end)<0){
    			double datong=qInterpolation(start.getTime(),instance.getTime(),map)/10000;
    			//System.out.println(start.getTime()+" "+String.valueOf(datong));
    			writer.write(String.valueOf(datong));
        		writer.write("\n");
            	
            	start.add(Calendar.MINUTE, 30);
            	count++;
            }
    		//System.out.println(start.getTime()+" "+datong);
			writer.close();
			System.out.println("共有插值后总数"+count);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
        
//        System.out.println(qInterpolation(now2.getTime(),instance.getTime(),map));
//        
        /*
        Date dt=new Date();
        Calendar c=Calendar.getInstance();
		 c.setTime(dt);
		 c.add(Calendar.DAY_OF_MONTH, -5);
		 
		 
		 System.out.println(now.getTime());
*/
    }
    private static Integer[] ReadDaTong() {
    	FileReader preResult_fr;
		BufferedReader preResult_br;
		String preResultPath = "DaTong2013.txt";
		int count = 0;
		Integer[] DaTong=new Integer[365];
		try {
			preResult_fr = new FileReader(preResultPath);
			preResult_br = new BufferedReader(preResult_fr);
			String strLine = "";
			try {
				while ((strLine = preResult_br.readLine()) != null) {
					DaTong[count++]=Integer.parseInt(strLine);
				}
				System.out.println("共大通天数条数："+count);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return DaTong;
	}

	private static double getHours(Date date1,Date date2){
        long time=date1.getTime()-date2.getTime();
        double hours=(time/1000)/3600.0;
        return hours;
    }
    public static double qInterpolation(Date date,Date instance,Map<Date,Integer> qMap){
        double k=0;
        double b=0;
        double h=getHours(date,instance);
        Date dl=new Date();
        int ql=0;
        Date dr=new Date();
        int qr=0;
        long time=date.getTime();
        //遍历qMap
        for (Map.Entry<Date, Integer> entry : qMap.entrySet()) {
            //判斷date位于哪個區間内
            if (time<=entry.getKey().getTime()){
                dr=entry.getKey();
                qr=entry.getValue();
                break;
            }
        }
        dl=new Date(dr.getTime()-24*3600*1000);
        ql=qMap.get(dl);
        k=(qr-ql)/(getHours(dr,dl));
        b=qr-k*getHours(dr,instance);
        return k*h+b;
    }
}
