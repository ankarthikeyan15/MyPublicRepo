/**
* <h1>CPU Allocator!</h1>
* The CPUAllocator program implements an instance allocation application
* for a Cloud Operator that allocates the resources according to the 
* following specifications:
* <p>
* Each server has X number of CPUs. The available server types are
* large=1, xlarge=2, 2xlarge=4, 4xlarge=8, 8xlarge=16 and 10xlarge=32.
* <p>
* Cost per hour for each server varies based on the data centre region.
* User can request servers with:
* 1. minimum N CPUs for H hours
* 2. maximum price they are willing to pay
* 3. both 1 and 2
* <p>
* Input:
* 1. hours - number of hours the requester wants to use the servers (int)
* 2. cpus - minimum number of CPUs the requester needs (int)
* 3. price - maximum price the requester is able to pay (float) 
* <p>
* Output:
* A List of dictionaries of allocated servers with regions as the keys.
* The list should be sorted based by the total cost per region.
* 
* @author  Karthikeyan Narayanan
* @version 1.0
* @since   2020-08-01
*/
package karthikeyan;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Server types available in the Cloud
 */
enum ServerType {

    /**
     * ServerType large
     */
    Large(1),
    /**
     * ServerType xlarge
     */
    Largex(2),
    /**
     * ServerType 2xlarge
     */
    Large2x(4),
    /**
     * ServerType 4xlarge
     */
    Large4x(8),
    /**
     * ServerType 8xlarge
     */
    Large8x(16),
    /**
     * ServerType 10xlarge
     */
    Large10x(32);

    private final int CPU;

    /**
     * Returns the number of CPUs for a given Server type
     * 
     * @return an integer containing number of CPUs
     */
    public int getCPUs() {
	return this.CPU;
    }

    private ServerType(int cpu) {
	this.CPU = cpu;
    }
}

/**
 * Server is the class which encapsulates the cloud server's regionId, type and
 * cost per hour for using the server
 * 
 */
final class Server {
    final ServerType type;
    final double costPerHour;
    final String regionId;
    int instanceCount;

    /**
     * Server constructor
     * 
     * @param type        ServerType to be set for server
     * @param costPerHour costPerHour to be set for server
     * @param regionId    region ID of the server to be set
     */
    Server(ServerType type, double costPerHour, String regionId) {
	this.type = type;
	this.costPerHour = costPerHour;
	this.regionId = regionId;
	initInstanceCount();
    }

    /**
     * Initializes the instance count of the server
     */
    void initInstanceCount() {
	this.instanceCount = 0;
    }
}

/**
 * CPUAllocator is the class which encapsulates methods to read user
 * requirements and allocates the servers
 *
 */
class CPUAllocator {

    Map<String, ArrayList<Server>> allocation;
    ArrayList<Server> servers;

    /**
     * CPUAllocator constructor
     */
    CPUAllocator() {
	allocation = new HashMap<String, ArrayList<Server>>();
	servers = new ArrayList<Server>();
    }

    /**
     * Initializes the servers and allocation result
     */
    void initServers() {
	servers.forEach(server -> {
	    server.initInstanceCount();
	});
	allocation.clear();
    }

    /**
     * Reads the instances data from given JSON and updates servers list
     * 
     * @throws Exception
     */
    void readInstancesData() throws Exception {
	try (FileReader fr = new FileReader("resources/instances.json")) {
	    JSONObject jo = (JSONObject) new JSONParser().parse(fr);
	    Map instances = (Map) jo;
	    Iterator<Map.Entry> instanceItr = instances.entrySet().iterator();
	    // iterate for each region JSON object
	    while (instanceItr.hasNext()) {
		Map.Entry regionPair = instanceItr.next();
		String regionId = (String) regionPair.getKey();
		Iterator<Map.Entry> serverItr = ((Map) regionPair.getValue())
			.entrySet().iterator();
		ArrayList<Server> tempServers = new ArrayList<Server>();
		// iterate for each server JSON object in the region
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
		// add parsed servers data to servers list
		servers.addAll(tempServers);
	    }
	} catch (ParseException pe) {
	    System.out.println(
		    "Enter the Instance details as JSON in instances.json");
	    System.out.println("ParseException: " + pe.getMessage());
	}
    }

    /**
     * Compares servers to sort by (cost/CPUs) of each server
     *
     */
    class CompareServers implements Comparator<Server> {
	@Override
	public int compare(Server s1, Server s2) {
	    if (s1.costPerHour / s1.type.getCPUs() > s2.costPerHour
		    / s2.type.getCPUs()) {
		return 1;
	    }
	    return -1;
	}
    }

    /**
     * Sort the servers in the list
     */
    void sortServers() {
	// servers.sort(new CompareServers());
	CompareServers cmp = new CompareServers();
	for (int i = 0; i < servers.size(); i++) {
	    for (int j = i + 1; j < servers.size(); j++) {
		if (cmp.compare(servers.get(i), servers.get(j)) == 1) {
		    Collections.swap(servers, i, j);
		}
	    }
	}
    }

    /**
     * Writes the allocated servers and their cost as JSON to output and
     * serverAllocation.json file
     * 
     * @param hours number of hours the user requested the servers for
     */
    void writeAllocation(int hours) {
	JSONArray cpuAllocation = new JSONArray();
	for (Map.Entry<String, ArrayList<Server>> region : allocation
		.entrySet()) {
	    JSONObject jRegion = new JSONObject();
	    JSONArray serversList = new JSONArray();
	    double cost = 0;
	    // create JSON Object for servers in each region
	    for (Server server : region.getValue()) {
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
		// calculate cost for servers in a region
		cost += server.instanceCount * server.costPerHour * hours;
	    }
	    // round cost to 2 decimal places
	    BigDecimal bd = new BigDecimal(cost).setScale(2,
		    RoundingMode.HALF_UP);
	    cost = bd.doubleValue();
	    jRegion.put("total_cost", "$" + cost);
	    jRegion.put("servers", serversList);
	    jRegion.put("region", region.getKey());
	    cpuAllocation.add(jRegion);
	}
	// sort the allocated servers data by the total cost per region
	cpuAllocation.sort(new Comparator<JSONObject>() {
	    @Override
	    public int compare(JSONObject jo1, JSONObject jo2) {
		String val = ((String) jo1.get("total_cost")).substring(1);
		double val1 = Double.parseDouble(val);
		val = ((String) jo2.get("total_cost")).substring(1);
		double val2 = Double.parseDouble(val);
		if (val1 > val2) {
		    return 1;
		}
		return -1;
	    }
	});
	// write the output JSON to serverAllocation.json
	try (PrintWriter pw = new PrintWriter(
		"resources/serverAllocation.json")) {
	    System.out.println(cpuAllocation.toJSONString());
	    pw.write(cpuAllocation.toJSONString());
	} catch (Exception e) {
	    System.out.println("Exception: " + e.getMessage());
	}
    }

    /**
     * Allocates servers for given minimum number of CPUs and number of hours
     * required, and returns the minimum cost for the given requirements
     * 
     * @param hours number of hours the user requested the servers for
     * @param cpus  minimum number of CPUs the user requested
     * @return
     */
    double getCosts(int hours, int cpus) {
	initServers();
	// sort servers by (cost/CPUs) of each server
	sortServers();
	double totalCost = 0;
	// stores the server with lowest cost
	Server leastCostServer = null;
	for (Server server : servers) {
	    if (cpus == 0) {
		break;
	    }
	    int serverCount = (int) cpus / server.type.getCPUs();
	    double serverCost = serverCount * server.costPerHour * hours;
	    // update total servers cost
	    totalCost += serverCost;
	    server.instanceCount = serverCount;
	    if (serverCount > 0) {
		allocation.putIfAbsent(server.regionId,
			new ArrayList<Server>());
		allocation.get(server.regionId).add(server);
	    }
	    // update remaining cpus after allocation
	    cpus = cpus - serverCount * server.type.getCPUs();
	    if (Objects.isNull(leastCostServer)
		    || server.costPerHour < leastCostServer.costPerHour) {
		leastCostServer = server;
	    }
	}
	// if cpus remain > 0, add one server with the lowest cost to the list
	if (cpus > 0) {
	    leastCostServer.instanceCount++;
	    allocation.putIfAbsent(leastCostServer.regionId,
		    new ArrayList<Server>());
	    allocation.get(leastCostServer.regionId).remove(leastCostServer);
	    allocation.get(leastCostServer.regionId).add(leastCostServer);
	    totalCost += leastCostServer.costPerHour * hours;
	}
	// writeAllocation(hours);
	return totalCost;
    }

    /**
     * Allocates servers for given maximum payable price and number of hours
     * required, such that maximum possible CPUs are allocated
     * 
     * @param hours number of hours the user requested the servers for
     * @param price maximum payable price for the servers
     */
    void getCosts(int hours, double price) {
	initServers();
	// sort servers by (cost/CPUs) of each server
	sortServers();
	for (Server server : servers) {
	    if (price <= 0)
		break;
	    double serverCountDouble = price / (server.costPerHour * hours);
	    int serverCount = (int) serverCountDouble;
	    server.instanceCount = serverCount;
	    if (serverCount > 0) {
		allocation.putIfAbsent(server.regionId,
			new ArrayList<Server>());
		allocation.get(server.regionId).add(server);
	    }
	    // update remaining price after allocation
	    price = price - serverCount * server.costPerHour * hours;
	}
	// writeAllocation(hours);
    }

    /**
     * Allocates servers with given minimum number of CPUs, for given maximum
     * payable price and number of hours required
     * 
     * @param hours number of hours the user requested the servers for
     * @param cpus  minimum number of CPUs the user requested
     * @param price maximum payable price for the servers
     */
    void getCosts(int hours, int cpus, double price) {
	double totalCost = getCosts(hours, cpus);
	/* check if minimum CPUs required could be allocated for the maximum
	payable price */
	if (totalCost <= price) {
	    getCosts(hours, price);
	} else {
	    initServers();
	}
	// writeAllocation(hours);
    }

    /*
     * Reads user input and performs allocation operation based on user inputs
     */
    public static void main(String[] args) {
	CPUAllocator cpuAllocator = new CPUAllocator();
	try {
	    cpuAllocator.readInstancesData();
	} catch (Exception e) {
	    System.out.println("Exception: " + e.getMessage());
	}
	Scanner scanner = new Scanner(System.in);
	int choice;
	do {
	    System.out.println("Enter the cpu requirements and price"
		    + "in cloudResource.properties and pick a choice.");
	    System.out.println("Enter the choice:\n"
		    + "1. Calculate Cost for the given data\n" + "2. Exit");
	    choice = scanner.nextInt();
	    if (choice == 2) {
		break;
	    }
	    if (choice != 1) {
		System.out.println("Please enter 1 or 2");
		continue;
	    }
	    try (InputStream input = new FileInputStream(
		    "resources/cloudResource.properties")) {
		Properties properties = new Properties();
		properties.load(input);
		// read the input properties
		String hours = (String) properties.get("hours");
		String cpus = (String) properties.get("minCPUs");
		String price = (String) properties.get("maxPrice");
		if (Objects.isNull(hours)) {
		    System.out.println("Please enter the hours requirement!");
		    continue;
		}
		// allocate based on input parameters
		if (Objects.isNull(price)) {
		    if (Objects.isNull(cpus)) {
			System.out.println(
				"Please enter the cpus/price requirements!");
			continue;
		    }
		    cpuAllocator.getCosts(Integer.parseInt(hours),
			    Integer.parseInt(cpus));
		} else {
		    if (Objects.isNull(cpus)) {
			cpuAllocator.getCosts(Integer.parseInt(hours),
				Double.parseDouble(price));
		    } else {
			cpuAllocator.getCosts(Integer.parseInt(hours),
				Integer.parseInt(cpus),
				Double.parseDouble(price));
		    }
		}
		// write the server allocation output
		cpuAllocator.writeAllocation(Integer.parseInt(hours));
	    } catch (Exception e) {
		System.out.println("Exception: " + e.getMessage());
	    }
	} while (true);
	System.out.println("CPU Allocator terminated!");
    }
}
