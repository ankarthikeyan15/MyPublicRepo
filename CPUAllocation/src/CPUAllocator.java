import java.io.FileReader;
import java.util.ArrayList;
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
	
	public Server(ServerType type, double costPerHour, String regionId) {
		this.type = type;
		this.costPerHour = costPerHour;
		this.regionId = regionId;
	}
}

final class Region {
	final ArrayList<Server> servers;
	
	public Region(ArrayList<Server> servers) {
		this.servers = servers;
	}
}

class CPUAllocator {
	ArrayList<Region> regions;
	ArrayList<Server> servers;
	
	void readInstancesData() throws Exception {
		try (FileReader fr = new FileReader("instances.json")) {
			JSONObject jo = (JSONObject) new JSONParser().parse(fr);
		} catch(ParseException pe) {
			
		}
	}
	
	public static void main(String[] args) {
		CPUAllocator cpuAllocator = new CPUAllocator();
		try {
			cpuAllocator.readInstancesData();
		} catch(Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
	}
}

