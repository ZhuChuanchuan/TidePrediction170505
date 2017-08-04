package com.predict.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class CommonMethod {
	// /计算两个字符串时间的间隔天数
	public static int daysBetween(String smdate, String bdate) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		Calendar cal = Calendar.getInstance();
		cal.setTime(sdf.parse(smdate));
		long time1 = cal.getTimeInMillis();
		cal.setTime(sdf.parse(bdate));
		long time2 = cal.getTimeInMillis();
		long between_days = (time2 - time1) / (1000 * 3600 * 24);

		return Integer.parseInt(String.valueOf(between_days));
	}
	
	/**
	 * 获取某一时刻的插值流量
	 * @param date 查询的时刻
	 * @param rklls 前5天入库流量
	 * @return
	 */
	public static double getInterpolationByDate(Date queryDate,Integer[] rklls ){
		Map<Date,Integer> map=new TreeMap<Date,Integer>();
        Calendar now=Calendar.getInstance();
        
        Date dt = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(dt);
		
		//NT 与大通相差2day
		now.set(2017,4,29,24,0,0);
        map.put(now.getTime(),28000);
		now.set(2017,4,30,24,0,0);
        map.put(now.getTime(),28100);
        now.set(2017,4,31,24,0,0);
        map.put(now.getTime(),28200);
        now.set(2017,5,1,24,0,0);
        map.put(now.getTime(),28300);
        now.set(2017,5,2,24,0,0);
        map.put(now.getTime(),28400);
        now.set(2017,5,3,24,0,0);
        map.put(now.getTime(),28500);
		/*
		c.add(Calendar.DAY_OF_YEAR, -5);
		now.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),12,0,0);
		System.out.println(c.get(Calendar.YEAR)+" "+c.get(Calendar.MONTH)+" "+c.get(Calendar.DAY_OF_MONTH));
        map.put(now.getTime(),rklls[0]);
        
        c.add(Calendar.DAY_OF_YEAR, 1);
		now.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),12,0,0);
		System.out.println(c.get(Calendar.YEAR)+" "+c.get(Calendar.MONTH)+" "+c.get(Calendar.DAY_OF_MONTH));
        map.put(now.getTime(),rklls[1]);
        
        c.add(Calendar.DAY_OF_YEAR, 1);
		now.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),12,0,0);
		System.out.println(c.get(Calendar.YEAR)+" "+c.get(Calendar.MONTH)+" "+c.get(Calendar.DAY_OF_MONTH));
        map.put(now.getTime(),rklls[2]);
        
        c.add(Calendar.DAY_OF_YEAR, 1);
		now.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),12,0,0);
		System.out.println(c.get(Calendar.YEAR)+" "+c.get(Calendar.MONTH)+" "+c.get(Calendar.DAY_OF_MONTH));
        map.put(now.getTime(),rklls[3]);
        
        c.add(Calendar.DAY_OF_YEAR, 1);
		now.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),12,0,0);
		System.out.println(c.get(Calendar.YEAR)+" "+c.get(Calendar.MONTH)+" "+c.get(Calendar.DAY_OF_MONTH));
        map.put(now.getTime(),rklls[4]);
        */
        Calendar instance=Calendar.getInstance();
        instance.set(2010,0,1,0,0,0); //自2010 1 1起
        //System.out.println(queryDate+" "+qInterpolation(queryDate,instance.getTime(),map));
		return qInterpolation(queryDate,instance.getTime(),map);
	}
	private static  double getHours(Date date1,Date date2){
        long time=date1.getTime()-date2.getTime();
        double hours=(time/1000)/3600.0;
        return hours;
    }
    public static  double qInterpolation(Date date,Date instance,Map<Date,Integer> qMap){
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
