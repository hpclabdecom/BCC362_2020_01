package pub;

import java.util.Set;
import java.util.SortedSet;

import core.Message;
import core.MessageImpl;
import core.PubSubCommand;

public class WakeUpCommand implements PubSubCommand{

    @Override
    public Message execute(Message m, SortedSet<Message> log, Set<String> subscribers) {
        Message response = new MessageImpl();

        // separar a mensagem de release e dar marcar o log como acquire_finished

		// conteudo da mensagem: release_clientAddress_resource

		// formato do recurso : clientAddress_resource
		String[] content = m.getContent().split("_");
		String resource = content[1] + "_" + content[2];

		synchronized(log){

			for( Message message : log ) {
				// firstAcquire
				if(message.getType().equals("notify") && message.getContent().equals("acquire_" + resource)){
					message.setType("acquire_finished");
					break;
				}
			}

			response.setContent("Released resource: " + m.getContent());

			response.setType("notify_ack");

			m.setType("notify");
			log.add(m);
			
			for(Message message : log)
				System.out.print(message.getContent() + " | ");
			System.out.print("\n");

			log.notifyAll(); // quem estiver segurando este log vai acordar.
		
		}
	
		return response;

    }

}