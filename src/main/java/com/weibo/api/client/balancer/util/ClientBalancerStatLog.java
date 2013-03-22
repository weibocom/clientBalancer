package com.weibo.api.client.balancer.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Copy from commons/trunk, and remove the statistic of ehcache
 * @author fishermen
 *
 */
public class ClientBalancerStatLog{
	
	private static AtomicLong count = new AtomicLong(0);
	private static AtomicLong errorCount = new AtomicLong(0);
	private static Map<String, AtomicLong> statVars = new ConcurrentHashMap<String, AtomicLong>();
	private static Map<String, AtomicLong> lastStatVars = new ConcurrentHashMap<String, AtomicLong>();
	private static Map<String, AtomicLong> maxStatVars = new ConcurrentHashMap<String, AtomicLong>();
	private static AtomicBoolean outOfMemory = new AtomicBoolean(false);
	
	private static Map<String, ProcessStat> processStats = new ConcurrentHashMap<String, ProcessStat>();
	private static Map<String, ProcessStat> processStatsLast = new ConcurrentHashMap<String, ProcessStat>();
	private static Map<String, ProcessStat> processStatsMax = new ConcurrentHashMap<String, ProcessStat>();
	
	private static AtomicBoolean pausePrint = new AtomicBoolean(false);
	private static Map<String, ThreadPoolExecutor> executors = new ConcurrentHashMap<String, ThreadPoolExecutor>();
	
	private static Set<String> cacheStatKeys = new HashSet<String>();
	
	public static void setPausePrint(boolean print) {
		pausePrint.set(print);
	}
	
	public static long inc() {
		return count.incrementAndGet();
	}

	public static long get() {
		return count.get();
	}
	
	public static long dec() {
		return count.decrementAndGet();
	}
	
	public static synchronized void registerVar(String var) {
		if(statVars.get(var) == null){
			statVars.put(var, new AtomicLong(0));
			lastStatVars.put(var, new AtomicLong(0));
			maxStatVars.put(var, new AtomicLong(0));
		}		
	}
	
	public static void registerExecutor(String name, ThreadPoolExecutor executor){
		executors.put(name, executor);
	}
	
	public static long inc(String var) {
		return inc(var, 1);
	}

	public static long inc(String var, int value) {
		AtomicLong c = statVars.get(var);
		if(c == null){
			registerVar(var);
			c = statVars.get(var);
		}
		
		long r = c.addAndGet(value);
		if (r < 0) {
			r = 0;
			c.set(0);
		}
		return r;
	}
	
	public static long dec(String var) {
		AtomicLong c = statVars.get(var);
		if (c != null)
			return c.decrementAndGet();
		else
			return 0;
	}
	
	public static long inc(int delta) {
		return count.addAndGet(delta);
	}
	
	public static void incProcessTime(String var, int processCount, long processTime){
		incProcessTime(processStats, var, processCount, processTime);
		incProcessTime(processStatsLast, var, processCount, processTime);
	}
	
	private static void incProcessTime(Map<String, ProcessStat> pstats, String var, int processCount, long processTime){
		ProcessStat ps = pstats.get(var);
		if(ps == null){
			ps = new ProcessStat();
			pstats.put(var, ps);
		}
		ps.addStat(processCount, processTime);
	}

	public static long incError() {
		return errorCount.incrementAndGet();
	}

	public static long decError() {
		return errorCount.decrementAndGet();
	}

	public static long getError() {
		return errorCount.get();
	}
	
	
	public static long incError(int delta) {
		return errorCount.addAndGet(delta);
	}
	
	
	
	public static void addCacheStatKeySuffix(String keySuffix){
		cacheStatKeys.add(keySuffix);
	}
	
	private static long startTime = SystemTimer.currentTimeMillis();
	private static long lastCount = 0;
	private static long cnt = 0;
	private static long lastTime = 0;
	private static long max = 0;
	public static String getStatStr() {	
		try {
				
			long time2 = System.currentTimeMillis();
			
			cnt = count.get();
			long cur = (cnt - lastCount) * 1000l / (time2 - lastTime);
			if (cur > max)  max = cur;
			
			//.statLog.info("---------------------------");
			//ClientBalancerLog.statLog.info("JAVA HEAP: " + memoryReport() + ", UP TIME: " + ((time2 - startTime) / 1000) + ", min: " + ((time2 - startTime) / 60000));
			SortedSet<String> keys = new TreeSet<String>(statVars.keySet());
			StringBuilder sb = new StringBuilder(512).append("cBalancer_stat[");
			boolean firstLoop = true;
			for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
				String var = iterator.next();
				AtomicLong c = statVars.get(var);
				AtomicLong last1 = lastStatVars.get(var);
				AtomicLong m1 = maxStatVars.get(var);
				
				long cnt1 = c.get();
				if (cnt1 == 0)
					continue;
				long max1 = m1.get();
				long lastCount1 = last1.get();
				
				long avg1 = cnt1 * 1000l / (time2 - startTime);
				long cur1 = (cnt1 - lastCount1) * 1000l / (time2 - lastTime);
				if (cur1 > max1)  max1 = (int) cur1;

				if (!firstLoop)
					sb.append(",");
				else
					firstLoop = false;

				// json-style output
				sb.append("{\"").append(var).append("\":[").append(cnt1).append(",")
					.append(avg1).append(",").append(cur1).append(",").append(max1).append("]}\n");
				
				m1.set(max1);
				last1.set(cnt1);
			}
			sb.append("]\r\n");
			//sb.append(sb.toString());
			
			//stat process time
			if(processStats.size() > 0){
				sb.append(statProcessSt().toString()).append("\r\n");
			}
			
			//stat executors
			sb.append("cBalancer_pool:[ ");
			for(Map.Entry<String, ThreadPoolExecutor> entry : executors.entrySet()){
				sb.append(statExecutor(entry.getKey(), entry.getValue())).append(", ");
			}
			sb.append(" ]").toString();
			
			lastTime = time2;
			lastCount = cnt;
			
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	private static StringBuilder statProcessSt(){
		StringBuilder pstatSb = new StringBuilder(processStats.size() * 64).append("cBalancer_processStat: ");
		for(Map.Entry<String, ProcessStat> entry : processStats.entrySet()){
			String psKey = entry.getKey();
			ProcessStat ps = entry.getValue();
			ProcessStat psLast = processStatsLast.get(psKey);
			ProcessStat psMax = processStatsMax.get(psKey);
			if(psMax == null || psMax.getAvgTime() < psLast.getAvgTime()){
				processStatsMax.put(psKey, psLast);
				psMax = processStatsMax.get(psKey);
			}
			
			if(ps.getAvgTime() > 0){
				pstatSb.append(entry.getKey()).append("{")
					.append(ps.getCount()).append("=").append(ps.getAvgTime()).append(",")
					.append(psLast.getCount()).append("=").append(psLast.getAvgTime()).append(",")
					.append(psMax.getCount()).append("=").append(psMax.getAvgTime()).append("},\n ");
			}
			//reset last stat
			processStatsLast.put(psKey, new ProcessStat());
		}
		if (pstatSb.lastIndexOf(",") > 0) {
			pstatSb.delete(pstatSb.lastIndexOf(","), pstatSb.length() - 1);
		}
		return pstatSb;
	}
	
	public static boolean isOutOfMemory() {
		return outOfMemory.get();
	}
	
	private static String statExecutor(String name, ThreadPoolExecutor executor){
		StringBuilder strBuf = new StringBuilder(32);
		strBuf.append(name).append("{").append(executor.getQueue().size()).append(",")
			.append(executor.getCompletedTaskCount()).append(",")
			.append(executor.getTaskCount()).append(",")
			.append(executor.getActiveCount()).append(",")
			.append(executor.getCorePoolSize()).append("}\n");
		return strBuf.toString();
	}
	
//	private String statEhCache(String name, Ehcache ehcache){
//		return new StringBuilder().append("EhCache: ").append(ehcache.getStatistics()).toString();	
//		
//	}
	
	public static class ProcessStat{
		 public AtomicLong count = new AtomicLong();
		 public AtomicLong time = new AtomicLong();
		 
		 public ProcessStat(){
		 }
		 
		 private void addStat(int pcount, long ptime){
			 this.count.addAndGet(pcount);
			 this.time.addAndGet(ptime);
		 }
		 
		 private long getCount(){
			 return count.get();
		 }
		 
		 private long getAvgTime(){
			 if(this.count.get() > 0){
				 return this.time.get() / this.count.get();
			 }
			 return 0;
		 }
	}
	
}
