package core.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import core.Message;

public class Client {
	
	private Socket s;
	
	public Client(String ip, int port) throws IOException{
		try{
			s = new Socket(ip, port);
			s.setSoTimeout(3000);
		}catch (IOException e){
			System.out.println("[CLIENT] Client cannot connect with " + ip + " on port: " + port);
			throw e;
		}
	}
	
	public Message sendReceive(Message msg) throws IOException{
		try{
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
			
			out.writeObject(msg);
			out.flush();
						
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
			Message response = (Message) in.readObject();

			
			in.close();
			out.close();			
			s.close();
			return response;
		}
		catch (IOException e){
			System.out.println("[CLIENT] Client cannot send/receive a message: " + msg.getContent());
			throw e;
		}
		catch (ClassNotFoundException e) {
			System.out.println("[CLIENT] Client cannot send/receive a message: " + msg.getContent());
			return null;
		}

	}

}
