package appl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import core.Message;

public class OneAppl {

	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		new OneAppl(true);
	}

	public OneAppl() {
		PubSubClient client = new PubSubClient();
		client.startConsole();
	}

	public OneAppl(boolean flag) throws InterruptedException {
		Scanner reader = new Scanner(System.in); // Reading from System.in
		
		String brokerAddress = "10.128.0.2";
		int brokerPort = 8080;

		String clientAddress = "10.128.0.6";

		PubSubClient nirvana = new PubSubClient(clientAddress, 8084);
		PubSubClient metallica = new PubSubClient(clientAddress, 8082);
		PubSubClient seether = new PubSubClient(clientAddress, 8083);

		nirvana.subscribe(brokerAddress, brokerPort);
		metallica.subscribe(brokerAddress, brokerPort);
		seether.subscribe(brokerAddress, brokerPort);
		//Todos desse Client vao se conectar no broker secundario
		brokerAddress = "10.128.0.3";

		Thread accessOne = new ThreadSincronized(nirvana, "x", "nirvana", brokerAddress, brokerPort, 1);
		Thread accessTwo = new ThreadSincronized(metallica, "x", "metallica", brokerAddress, brokerPort, 1);
		Thread accessThree = new ThreadSincronized(nirvana, "x", "nirvana", brokerAddress, brokerPort, 2);

		accessOne.start();
		accessTwo.start();
		accessThree.start();

		try {
			accessTwo.join();
			accessOne.join();
			accessThree.join();
		} catch (Exception e) {

		}

		try {
			Thread.currentThread().sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Thread accessFour = new ThreadSincronized(metallica, "x", "metallica", brokerAddress, brokerPort, 2);
		Thread accessFive = new ThreadSincronized(seether, "x", "seether", brokerAddress, brokerPort, 1);
		Thread accessSix = new ThreadSincronized(metallica, "x", "metallica", brokerAddress, brokerPort, 3);
		Thread accessSeven = new ThreadSincronized(nirvana, "x", "nirvana", brokerAddress, brokerPort, 3);

		accessFour.start();
		accessFive.start();
		accessSix.start();
		accessSeven.start();
		try {
			accessFour.join();
			accessFive.join();
			accessSix.join();
			accessSeven.join();
		} catch (Exception e) {

		}
	
		List<Message> log = nirvana.getLogMessages();
		List<Message> logAcquire = new ArrayList<Message>(); 
		List<Message> logRelease = new ArrayList<Message>(); 
		Iterator<Message> it = log.iterator();
		it = log.iterator();
		while (it.hasNext()) {
			Message aux = it.next();
			String content = aux.getContent();
			String[] parts2 = content.split("_");
			try {
				if (parts2[1].equals("acquire")) {
					logAcquire.add(aux);
				}
				else{
					logRelease.add(aux);
				}
			} catch(Exception e){
			} 
		}
		System.out.println("Log acquire:\n");
		it = logAcquire.iterator();
		while (it.hasNext()){
			Message aux = it.next();
			String content = aux.getContent();
			System.out.print(content + " | ");
		}

		System.out.println("\n\nLog release:\n");
		it = logRelease.iterator();
		while (it.hasNext()){
			Message aux = it.next();
			String content = aux.getContent();
			System.out.print(content + " | ");
		}



	}

	class ThreadWrapper extends Thread {
		PubSubClient c;
		String msg;
		String host;
		int port;

		public ThreadWrapper(PubSubClient c, String msg, String host, int port) {
			this.c = c;
			this.msg = msg;
			this.host = host;
			this.port = port;
		}

		public void run() {
			c.publish(msg, host, port);
		}
	}

	class ThreadSincronized extends Thread {
		PubSubClient c;
		String topic;
		String client_name;
		String host;
		int port;

		public ThreadSincronized(PubSubClient c, String topic, String client_name, String host, int port, int id) {
			this.c = c;
			this.topic = topic;
			this.client_name = Integer.toString(id).concat(client_name);
			this.host = host;
			this.port = port;

		}

		public void run() {

			Thread access = new ThreadWrapper(c, client_name.concat("_acquire_x"), host, port);
			access.start();
			try {
				access.join();
			} catch (Exception e) {
			}

			List<Message> log = c.getLogMessages();
			// CopyOnWriteArrayList<Message> log2 = new CopyOnWriteArrayList <Message>();
			while (true) {
				try {
					Thread.currentThread().sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} // 1 segundo

				log = c.getLogMessages();
				// log2 = new CopyOnWriteArrayList <Message>();
				// log2.addAll(log);
				Iterator<Message> it = log.iterator();
				it = log.iterator();
				while (it.hasNext()) {
					Message i = it.next();
					String content = i.getContent();
					try {
						String[] parts = content.split("_");
						/*
						 * if (parts[1].equals("acquire")) { log2.add(content); }
						 */

						if (parts[1].equals("release")) {
							Iterator<Message> it2 = log.iterator();
							while (it2.hasNext()) {
								Message j = it2.next();
								String content2 = j.getContent();
								String[] parts2 = content2.split("_");

								if (parts2[0].equals(parts[0])) {
									log.remove(j);
									log.remove(i);
									break;
								}
							}
						}
					} catch (Exception e) {
						log.remove(i);
					}
				}
				String first = null;
				try {
					first = log.get(0).getContent();
				} catch (Exception e) {
					System.out.print("Caiu na exception X\n");
					continue;
				}

				if (first.equals(client_name + "_acquire_x")) {

					System.out.print("\nAcessando X\n");
					try {
						Thread.currentThread().sleep(2000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} // 1 segundo

					/*
					 * try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e1) { // TODO
					 * Auto-generated catch block e1.printStackTrace(); }
					 */
					access = new ThreadWrapper(c, client_name.concat("_release_x"), host, port);
					access.start();
					try {
						access.join();
					} catch (Exception e) {
					}
					break;
				}
			}
			log = c.getLogMessages();
			Iterator<Message> it = log.iterator();
			it = log.iterator();
			while (it.hasNext()) {
				Message aux = it.next();
				System.out.print(aux.getContent() + aux.getLogId() + " | ");
			}
			System.out.println();
		}
	}

}