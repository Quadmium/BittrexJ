import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import java.text.DecimalFormat;
import javax.mail.*;
import javax.mail.internet.*;
import com.sun.mail.util.MailSSLSocketFactory;

public class DriverAlarm
{
    private static BittrexUtils bittrex;
    private static ArrayList<String> uuidList = new ArrayList<String>();
    private static DecimalFormat d = new DecimalFormat("#");
    private static String market = "SLG";
    private static ArrayList<Map<String, Double>> prevStatsList = new ArrayList<Map<String, Double>>();

    public DriverAlarm()
    {
    }

    public static void main(String[] args)
    {
        bittrex = new BittrexUtils("BTC-" + market, "myapikey", "myapisecret");
        d.setMaximumFractionDigits(8);
        d.setMinimumFractionDigits(8);
        System.out.println("Open orders:");
        System.out.println(bittrex.getCommunicator().getAllOpenOrders().toString().replace(",", "\r\n"));
        Scanner in = new Scanner(System.in);
        System.out.println("Enter uuid(s) to watch, N to exit: ");
        String input = in.next();
        while(!input.toLowerCase().equals("n"))
        {
            uuidList.add(input);
            prevStatsList.add(null);
            input = in.next();
        }

        Timer timer = new Timer(300000, (e) -> tick());
        timer.start();
        tick();
    }

    private static void tick()
    {
        System.out.println("==============Tick==============");
        try {
            tickWatcher();
        } catch(Exception e) {System.out.println(e);}
    }

    private static void tickWatcher()
    {
        for(int i=0; i<uuidList.size(); i++)
        {
            String uuid = uuidList.get(i);
            Map<String, Double> prevStats = prevStatsList.get(i);
            if(prevStats == null && bittrex.getCommunicator().getAllOpenOrders().toString().contains(uuid))
            {
                prevStatsList.set(i, bittrex.getOrderStatistics(uuid));
                prevStats = prevStatsList.get(i);
                System.out.println(uuid + " New Status:\r\n" + (prevStats != null ? prevStats.toString().replace(",", "\r\n") : "Filled"));
                continue;
            }
            else if(prevStats == null)
                continue;
    
            Map<String, Double> curStats = bittrex.getOrderStatistics(uuid);
    
            if(!bittrex.getCommunicator().getAllOpenOrders().toString().contains(uuid))
            {
                prevStatsList.set(i, null);
                prevStats = prevStatsList.get(i);
                sendNotification(i);
                continue;
            }
    
            if(curStats == null ||
                (double)curStats.get("Quantity") != (double)prevStats.get("Quantity") ||
                (double)curStats.get("QuantityRemaining") != (double)prevStats.get("QuantityRemaining") ||
                (double)curStats.get("Limit") != (double)prevStats.get("Limit"))
            {
                prevStatsList.set(i, curStats);
                prevStats = prevStatsList.get(i);
                System.out.println(uuid + " New Status:\r\n" + (prevStats != null ? prevStats.toString().replace(",", "\r\n") : "Filled"));
                sendNotification(i);
            }
            
            System.out.println("Processed: " + uuid);
        }
    }

    private static void sendNotification(int index)
    {
        String uuid = uuidList.get(index);
        Map<String, Double> prevStats = prevStatsList.get(index);
        // Recipient's email ID needs to be mentioned.
        String to = "";//change accordingly

        // Sender's email ID needs to be mentioned
        String from = "";//change accordingly
        final String username = "";//change accordingly
        final String password = "";//change accordingly

        // Assuming you are sending email through relay.jangosmtp.net
        String host = "smtp.gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");
        try{
            MailSSLSocketFactory socketFactory= new MailSSLSocketFactory();
            socketFactory.setTrustAllHosts(true);
            props.put("mail.smtp.ssl.socketFactory", socketFactory);
        }catch(Exception e){}

        // Get the Session object.
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            // Create a default MimeMessage object.
            Message message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(to));

            // Set Subject: header field
            message.setSubject("Bittrex - " + uuid);

            // Now set the actual message
            message.setText(uuid + " Order status: \r\n" + (prevStats != null ? prevStats.toString().replace(",", "\r\n") : "Filled"));

            // Send message
            Transport.send(message);

            System.out.println("Sent message.");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}