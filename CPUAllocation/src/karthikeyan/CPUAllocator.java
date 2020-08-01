package karthikeyan;
import java.io.FileReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

enum ServerType {
	
	Large(1), Largex(2), Large2x(4), Large4x(8), Large8x(16), Large10x(32);  
	
	private final int CPU;
	
	public int getCPUs() {
		return this.CPU;
	}
	
	private ServerType(int cpu) {
		this.CPU = cpu;
	}
}

final class Server {
	final ServerType type;
	final double costPerHour;
	final String regionId;
	int instanceCount;
	
	Server(ServerType type, double costPerHour, String regionId) {
		this.type = type;
		this.costPerHour = costPerHour;
		this.regionId = regionId;
		initInstanceCount();
	}
	
	void initInstanceCount() {
		this.instanceCount = 0;
	}
}

class CPUAllocator {
	Map<String, ArrayList<Server>> allocation;
	ArrayList<Server> servers;
	
	CPUAllocator() {
		allocation = new HashMap<String, ArrayList<Server>>();
		servers = new ArrayList<Server>();
	}
	
	void initServers() {
		servers.forEach(server -> {
			server.initInstanceCount();
		});
		allocation.entrySet().forEach(region -> {
			region.getValue().clear();
		});
	}
	
	void readInstancesData() throws Exception {
		try (FileReader fr = new FileReader("resources/instances.json")) {
			JSONObject jo = (JSONObject) new JSONParser().parse(fr);
			Map instances = (Map) jo;
			Iterator<Map.Entry> instanceItr = instances.entrySet().iterator();
			while (instanceItr.hasNext()) {
				Map.Entry regionPair = instanceItr.next();
				String regionId = (String) regionPair.getKey();
				Iterator<Map.Entry> serverItr = ((Map) regionPair.getValue()).entrySet().iterator();
				ArrayList<Server> tempServers = new ArrayList<Server>();
				while (serverItr.hasNext()) {
					Map.Entry serverPair = serverItr.next();
					ServerType type = null;
					switch ((String) serverPair.getKey()) {
					case "large":
						type = ServerType.Large;
						break;
					case "xlarge":
						type = ServerType.Largex;
						break;
					case "2xlarge":
						type = ServerType.Large2x;
						break;
					case "4xlarge":
						type = ServerType.Large4x;
						break;
					case "8xlarge":
						type = ServerType.Large8x;
						break;
					case "10xlarge":
						type = ServerType.Large10x;
						break;
					}
					double cost = (double) serverPair.getValue();
					Server server = new Server(type, cost, regionId);
					tempServers.add(server);
				}
				servers.addAll(tempServers);
			}
		} catch (ParseException pe) {
			System.out.println("Improper input format");
			System.out.println("ParseException: " + pe.getMessage());
		}
	}
	
	class CompareServers implements Comparator<Server> {
		@Override
		public int compare(Server s1, Server s2) {
			if (s1.costPerHour/s1.type.getCPUs() > s2.costPerHour/s2.type.getCPUs())
				return 1;
			return -1;
		}
	}
	
	void sortServers() {
		//servers.sort(new CompareServers());
		CompareServers cmp = new CompareServers();
		for (int i = 0; i < servers.size(); i++) {
			for (int j = i + 1; j < servers.size(); j++) {
				if (cmp.compare(servers.get(i), servers.get(j)) == 1) {
					Collections.swap(servers, i, j);
				}
			}
		}
	}
	
	void writeAllocation(int hours) {
		JSONArray cpuAllocation = new JSONArray();
		for (Map.Entry<String, ArrayList<Server>> region: allocation.entrySet()) {
			JSONObject jRegion = new JSONObject();
			jRegion.put("region", region.getKey());
			JSONArray serversList = new JSONArray();
			double cost = 0;
			for (Server server: region.getValue()) {
				JSONObject jServer = new JSONObject();
				String type = null;
				switch (server.type) {
				case Large:
					type = "large";
					break;
				case Largex:
					type = "xlarge";
					break;
				case Large2x:
					type = "2xlarge";
					break;
				case Large4x:
					type = "4xlarge";
					break;
				case Large8x:
					type = "8xlarge";
					break;
				case Large10x:
					type = "10xlarge";
					break;
				}
				jServer.put(type, server.instanceCount);
				serversList.add(jServer);
				cost += server.instanceCount * server.costPerHour * hours;
			}
			BigDecimal bd = new BigDecimal(cost).setScale(2, RoundingMode.HALF_UP);
			cost = bd.doubleValue();
			jRegion.put("total_cost", "$" + cost);
			jRegion.put("servers", serversList);
			cpuAllocation.add(jRegion);
		}
		try (PrintWriter pw = new PrintWriter("resources/JSONExample.json")) {
			System.out.println(cpuAllocation.toJSONString());
			pw.write(cpuAllocation.toJSONString());
		} catch(Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
	}
	
	double getCosts(int hours, int cpus) {
		//System.out.println("Minimum cost");
		initServers();
		sortServers();
		double totalCost = 0;
		double leastCost = Double.MAX_VALUE;
		Server leastCostServer = null;
		for (Server server: servers) {
			if (cpus == 0) {
				break;
			}
			int serverCount = (int) cpus / server.type.getCPUs();
			double serverCost = serverCount * server.costPerHour * hours;
			totalCost += serverCost;
			server.instanceCount = serverCount;
			if (serverCount > 0) {
				allocation.putIfAbsent(server.regionId, new ArrayList<Server>());
				allocation.get(server.regionId).add(server);
			}
			cpus = cpus - serverCount * server.type.getCPUs();
			if (server.costPerHour < leastCost) {
				leastCost = server.costPerHour;
				leastCostServer = server;
			}
		}
		if (cpus > 0) {
			leastCostServer.instanceCount++;
			allocation.putIfAbsent(leastCostServer.regionId, new ArrayList<Server>());
			allocation.get(leastCostServer.regionId).remove(leastCostServer);
			allocation.get(leastCostServer.regionId).add(leastCostServer);
			totalCost += leastCostServer.costPerHour * hours;
		}
		writeAllocation(hours);
		return totalCost;
	}
	
	void getCosts(int hours, double price) {
		//System.out.println("Maximum CPUs");
		initServers();
		sortServers();
		for (Server server: servers) {
			if (price <= 0)
				break;
			double serverCountDouble = price / (server.costPerHour * hours);
			int serverCount = (int) serverCountDouble;
			server.instanceCount = serverCount;
			if (serverCount > 0) {
				if (!allocation.containsKey(server.regionId)) {
					allocation.put(server.regionId, new ArrayList<Server>());
				}
				allocation.get(server.regionId).add(server);
			}
			price = price - serverCount * server.costPerHour * hours;
		}
		writeAllocation(hours);
	}
	
	void getCosts(int hours, int cpus, double price) {
		double totalCost = getCosts(hours, cpus);
		if (totalCost <= price) {
			getCosts(hours, price);
		} else {
			initServers();
		}
		writeAllocation(hours);
	}
	
	public static void main(String[] args) {
		CPUAllocator cpuAllocator = new CPUAllocator();
		try {
			cpuAllocator.readInstancesData();
		} catch(Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
		cpuAllocator.getCosts(24, 115);
		cpuAllocator.getCosts(24, 207.85);
		cpuAllocator.getCosts(24, 115, 207.85);
	}
}

