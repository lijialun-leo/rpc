package main.java.balancing;

import java.util.List;

public class RoundRobin {
	
	 private static Integer pos = 0;

	 public static String getServer(List<String> list){
	 	String server = null;
        synchronized (pos)
        {
            if (pos > (list.size()-1))
                pos = 0;
            server = list.get(pos);
            pos ++;
        }
        //System.out.println("server :" + server);
        return server;
	 }

}
