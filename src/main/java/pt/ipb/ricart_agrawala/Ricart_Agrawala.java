//Students: Aziz Ibragimov, Alexandre Allies
package pt.ipb.ricart_agrawala;

import org.jgroups.*;

import javax.swing.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedList;

public class Ricart_Agrawala extends ReceiverAdapter {
    private static String user_name; //works as process id
    private static String channelName;
    private static Object[] options = {"YES","NO"};
    private static Object[] possibleNames = { "1", "2", "3" };
    private static int enterCriticalSection; //0 is "yes" and 1 is "no"
    private JChannel channel;
    //We implemented time as a queue that would be transmitted to every process
    private LinkedList<String> queue = new LinkedList<>();
    private int numberOfUsers = possibleNames.length;
    private int counter = 0; //counter of the replies

    private void start() throws Exception {
            channel = new JChannel().setReceiver(this);
            channel.connect(channelName);
            eventloop();
            channel.close();
    }

    private void criticalSection() throws Exception {
        System.out.println("Entered critical section " + new Timestamp(System.currentTimeMillis()));
        Thread.sleep(3000); //instead of this we can invoke any real criticalSection
        finishCriticalSection();
        System.out.println("Quit critical section");
    }

    private void finishCriticalSection() throws Exception {
        queue.removeFirst();
            //queue will be empty only for us,but we still need to clean it for others
            if(!queue.isEmpty()) {
                String queueString = String.join(" ", queue);
                System.out.println(queueString);
                Message msg = new Message(null, "QUEUE " + queueString);
                channel.send(msg);
            }
            else{
                Message msg = new Message(null,"QUEUE " + "CLEAN" );
                channel.send(msg);
            }
    }
    private void eventloop() throws InterruptedException {
        while(true) {
            enterCriticalSection = JOptionPane.showOptionDialog(null, "["+ user_name + "] "+"Enter critical section?", "Attention",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, options[1]);     //default value is "no"
            if(enterCriticalSection == -1) break;
            switch (enterCriticalSection) {
                case 0: //case "yes"
                        try {
                            System.out.print("> ");
                            System.out.flush();
                            String line = "";
                            line = "[" + user_name + "] " + "REQUEST ";
                            Message msg = new Message(null, line);
                            //check if u are the first in the queue and then proceed doing the cs.
                            if(queue.isEmpty()){
                                queue.add(user_name);
                                String queueString = String.join(" ", queue);
                                Message msg1 = new Message(null,"QUEUE " + queueString);
                                channel.send(msg1);
                                channel.send(msg);
                                while(counter < numberOfUsers - 1);
                                counter = 0;
                                criticalSection();
                            }
                            else {
                                if(!(queue.peekLast() == user_name)) queue.add(user_name);
                                String queueString = String.join(" ", queue);
                                Message msg1 = new Message(null,"QUEUE " + queueString);
                                channel.send(msg1);
                                if(!queue.isEmpty()) while(!queue.peekFirst().equals(user_name));
                                channel.send(msg);
                                while (counter < numberOfUsers - 1) ;
                                counter = 0;
                                criticalSection();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    break;
                case 1://case "no"
                    Thread.sleep(5000);
                    break;
            }
        }
    }

    private void createPopUp(String message){
        JFrame jf = new JFrame("The message arrived!");
        JDialog dialog = new JDialog(jf,false); // Sets its owner but makes it non-modal
        dialog.setTitle("The message!");
        dialog.setLocation(600, 100);
        JOptionPane optionPane = new JOptionPane(message,JOptionPane.WARNING_MESSAGE);
        dialog.getContentPane().add(optionPane); // Adds the JOptionPane to the dialog
        dialog.pack(); // Packs the dialog so that the JOptionPane can be seen
        dialog.setVisible(true); // Shows the dialog
        try {
            //Process sleeps for 333 milliseconds then terminates the pop-up
            Thread.sleep(300);
            dialog.dispose();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }
    public static void main(String[] args) throws Exception {
        channelName = JOptionPane.showInputDialog("Please enter the channel name:");
        Object userName = JOptionPane.showInputDialog(null,
                "Choose your user name", "User name",
                JOptionPane.INFORMATION_MESSAGE, null,
                possibleNames, possibleNames[0]);
        user_name = userName.toString();
        System.out.println("Your username is " + user_name);
        if(channelName.length() > 0) new Ricart_Agrawala().start();
        else{
            JOptionPane.showMessageDialog(null, "Please enter the valid channel name!", "Alert", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void receive(Message msg) {
        String message = msg.getObject();
        if(!message.substring(0, 3).contains(user_name)) {
            createPopUp(message);
            if(message.contains("QUEUE")){
                if(message.contains("CLEAN")) {
                    queue = new LinkedList<>();
                }
                else {
                    String queueString = message.substring(6);
                    String[] queueArray = queueString.split(" ");
                    queue = new LinkedList<>(Arrays.asList(queueArray));
                }
            }
            if(message.contains("REQUEST")){
                Message msg1 = new Message(null,"[" + user_name + "] " + "REPLY " + "to " + message.substring(1, 2));
                    try {
                        channel.send(msg1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            if(message.contains("REPLY")){
                if(message.contains(user_name)) counter++;
            }
        }
    }
    @Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }
}
