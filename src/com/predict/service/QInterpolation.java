package com.predict.service;

import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;
import java.util.Map;

public class QInterpolation {

    public QInterpolation() {

    }

    public static void main(String[] args) {
        Map<Date,Integer> map=new TreeMap<Date,Integer>();
        Calendar now=Calendar.getInstance();

        now.set(2017,4,12,12,0,0);
        map.put(now.getTime(),29200);
        now.set(2017,4,13,12,0,0);
        map.put(now.getTime(),29500);
        now.set(2017,4,14,12,0,0);
        map.put(now.getTime(),30200);
        now.set(2017,4,15,12,0,0);
        map.put(now.getTime(),30000);
        now.set(2017,4,16,12,0,0);
        map.put(now.getTime(),30200);
        
        Calendar instance=Calendar.getInstance();
        instance.set(2010,0,1,0,0,0);
        
        Calendar now2=Calendar.getInstance();
        now2.set(2017,4,13,12,8,0);
        System.out.println(qInterpolation(now2.getTime(),instance.getTime(),map));
        
        /*
        Date dt=new Date();
        Calendar c=Calendar.getInstance();
		 c.setTime(dt);
		 c.add(Calendar.DAY_OF_MONTH, -5);
		 
		 
		 System.out.println(now.getTime());
*/
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
