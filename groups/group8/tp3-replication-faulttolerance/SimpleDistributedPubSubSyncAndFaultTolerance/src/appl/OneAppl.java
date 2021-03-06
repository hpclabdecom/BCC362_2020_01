package appl;
import core.Message;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class OneAppl {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new OneAppl(true);
	}

	public OneAppl(boolean flag){

	// String hostPrimary = "10.128.0.20";
	// String hostBackup = "10.128.0.21";
	// String hostClient = "10.128.0.20";
		
		String hostPrimary = "localhost";
		int portPrimary = 8080;

		String hostBackup = "localhost";
		int portBackup = 8081;
		
		String hostClient = "localhost";

		PubSubClient clientA = new PubSubClient(hostClient, 8082);
		PubSubClient clientB = new PubSubClient(hostClient, 8083);
		PubSubClient clientC = new PubSubClient(hostClient, 8084);

		clientA.subscribe(hostPrimary, portPrimary);
		clientB.subscribe(hostPrimary, portPrimary);
		clientC.subscribe(hostPrimary, portPrimary);

		clientA.subscribe(hostBackup, portBackup);
		clientB.subscribe(hostBackup, portBackup);
		clientC.subscribe(hostBackup, portBackup);

		Thread accessOne = new requestAcquire(clientA, "ClientA",  "-acquire-", "X", hostPrimary, portPrimary);
		Thread accessTwo = new requestAcquire(clientB, "ClientB",  "-acquire-", "X", hostPrimary, portPrimary);
		Thread accessThree = new requestAcquire(clientC, "ClientC",  "-acquire-", "X", hostBackup, portBackup);
		// Thread accessFour = new requestAcquire(clientA, "ClientA",  "-acquire-", "X", hostBackup, portBackup);
		// Thread accessFive = new requestAcquire(clientB, "ClientB",  "-acquire-", "X", hostBackup, portBackup);
		// Thread accessSix = new requestAcquire(clientC, "ClientC",  "-acquire-", "X", hostBackup, portBackup);

		int seconds = (int) (Math.random()*(10000 - 1000)) + 1000;
		System.out.println("Starting in " + seconds/1000 + " seconds...\n");
		try { Thread.currentThread().sleep(seconds);
		}catch (Exception ignored) {}

		accessOne.start();
		accessTwo.start();
		accessThree.start();
		// accessFour.start();
		// accessFive.start();
		// accessSix.start();
		
		try{
			accessOne.join();
			accessTwo.join();
			accessThree.join();
			// accessFour.join();
			// accessFive.join();
			// accessSix.join();
		}catch (Exception ignored){}

		clientA.unsubscribe(hostPrimary, portPrimary);
		clientB.unsubscribe(hostPrimary, portPrimary);
		clientC.unsubscribe(hostPrimary, portPrimary);
		
		// clientA.unsubscribe(hostBackup, portBackup);
		// clientB.unsubscribe(hostBackup, portBackup);
		// clientC.unsubscribe(hostBackup, portBackup);

		clientA.stopPubSubClient();	
		clientB.stopPubSubClient();	
		clientC.stopPubSubClient();	
	}
}

class requestAcquire extends Thread {
	PubSubClient client;
	String clientName;
	String action;
	String resource;
	String hostBroker;
	int portBroker;

	public requestAcquire(PubSubClient client, String clientName, String action, String resource, String hostBroker, int portBroker) {
		this.client = client;
		this.clientName = clientName;
		this.action = action;
		this.resource = resource;
		this.hostBroker = hostBroker;
		this.portBroker = portBroker;
	}

	public void run() {	
		Thread access = new ThreadWrapper(client, clientName.concat(action).concat(resource), hostBroker, portBroker);
		access.start();

		try {
			access.join();
		} catch (Exception ignored) {}
		

			List<Message> logs = client.getLogMessages();
			List<String> acquires = new ArrayList<String>();

			while(true){

			Iterator<Message> it = logs.iterator();				
			while(it.hasNext()){
				Message log = it.next();
				String content = log.getContent();
				if (content.contains("-acquire-")){
					acquires.add(content);
				}
			}

			System.out.print("\nORDEM DE CHEGADA MANTIDA PELO BROKER: " + acquires + " \n");
			
			while (!acquires.isEmpty()){
				
				String firstClient = acquires.get(0);
				boolean hasRelease = false;

				while(!hasRelease){
					int randomInterval = getRandomInteger(1000, 10000);

					if(firstClient.startsWith(clientName)){
							access = new ThreadWrapper(client, "use"+resource, hostBroker, portBroker);
							access.start();
							try {
								access.join();
							} catch (Exception ignored) {}
							
							System.out.println("___________________________");
							System.out.println(firstClient.split("-")[0] + " pegou o recurso " + resource);

							System.out.println("Aguardando " + randomInterval/1000 + " segundos...\n");
							try {
								Thread.currentThread().sleep(randomInterval);
							}catch (Exception ignored) {}
							
							access = new ThreadWrapper(client, clientName.concat("-release-").concat(resource), hostBroker, portBroker);
							access.start();
							hasRelease = true;
					}else{
							break;
					}
				}						

				if (!acquires.isEmpty()){
					acquires.remove(0);
				}
			}
		}	
	}
	
	public int getRandomInteger(int minimum, int maximum){ 
		return ((int) (Math.random()*(maximum - minimum))) + minimum; 
	}

}

class ThreadWrapper extends Thread{
	PubSubClient c;
	String msg;
	String host;
	int port;
	
	public ThreadWrapper(PubSubClient c, String msg, String host, int port){
		this.c = c;
		this.msg = msg;
		this.host = host;
		this.port = port;
	}

	public void run(){
		c.publish(msg, host, port);
	}
}

