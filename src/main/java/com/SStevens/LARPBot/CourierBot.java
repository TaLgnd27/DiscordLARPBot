package com.SStevens.LARPBot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Logger.GLOBAL_LOGGER_NAME;

public class CourierBot {

    private final ShardManager shardManager;

    private final Dotenv config;

    public CourierBot() throws LoginException {
        config = Dotenv.configure().load();
        String token = config.get("TOKEN");

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
        builder.setStatus(OnlineStatus.IDLE);
        builder.setActivity(Activity.listening("the greatest information networks"));
        shardManager = builder.build();
    }

    @SuppressWarnings("unused")
    public ShardManager getShardManager(){
        return shardManager;
    }

    @SuppressWarnings("unused")
    public Dotenv getConfig(){
        return config;
    }

    static CourierBot bot;
    private final static Logger LOGGER =
            Logger.getLogger(GLOBAL_LOGGER_NAME);
    public static void main(String[] args){
        ScheduledFuture<?> botHandler = setup();

        //exit check
        Scanner in = new Scanner(System.in);
        boolean isExit = false;
        while (!isExit){
            try {
                botHandler.get();
            } catch (Exception e) {
                botHandler = setup();
            }
            if(in.nextLine().equalsIgnoreCase("exit")){
                isExit = true;
            }
        }
        System.exit(0);
    }

    public static void saveLink(String link){
        try {
            FileWriter myWriter = new FileWriter("savedLink.txt");
            myWriter.write(link);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.toString());
        }
    }
    public static String loadLink(){
        StringBuilder out = new StringBuilder();
        try {
            File myObj = new File("savedLink.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                out.append(myReader.nextLine());
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, e.toString());
        }
        return out.toString();
    }

    public static String getEventLink() {
        Document doc = null;
        try {
            doc = Jsoup.connect("https://lasthopelarp.proboards.com/board/6/upcoming-events").userAgent("mozilla/17.0").get();
        } catch (IOException e) {
            bot.shardManager.shutdown();
            LOGGER.log(Level.WARNING, e.toString());
        }

        assert doc != null;
        Elements temp = doc.select("tr.item.thread.first");
        temp = temp.select("span.link.target > a");

        return "https://lasthopelarp.proboards.com" + temp.attr("href");
    }

    public static boolean isValidLink(String s){
        boolean out = true;
        Document doc = null;
        try {
            doc = Jsoup.connect(s).userAgent("mozilla/17.0").get();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.toString());
        }

        assert doc != null;
        Elements temp = doc.select("div.message");
        String text = (temp.get(0)).text();

        if(text.charAt(0) != '='){
            out = false;
        }

        if(s.equals(loadLink())){
            out = false;
        }
        return out;
    }

    public static void run() {
        String link = getEventLink();
        if(isValidLink(link)){
            System.out.println(link);
            String message = "@everyone I've been looking for you. Got something I'm supposed to deliver - your hands only.\n" +
                    link;
            List<TextChannel> channels = bot.shardManager.getTextChannelsByName("Announcements", true);
            for (TextChannel channel:
                    channels) {
                try {
                    channel.sendMessage(message).queue();
                } catch (Throwable t){
                    LOGGER.log(Level.WARNING, t.toString());
                }
            }
            saveLink(link);
        } else {
            System.out.println("Invalid");
        }
    }

    public static void ipNotif(){
        //get ip
        String ip = "Unknown IP, eat shit";
        try {
            URL whatismyip = new URI("https://checkip.amazonaws.com").toURL();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            ip = in.readLine();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.toString());
        }
        System.out.println(ip);
        User usr = bot.shardManager.retrieveUserById("343771474202722304").complete();
        String finalIp = ip;
        usr.openPrivateChannel().flatMap(channel -> channel.sendMessage("Here is my new IP: " + finalIp)).queue();

    }

    public static boolean isInternet(){
        System.out.println("Waiting for network");
        try {
            URL url = new URI("http://www.google.com").toURL();
            URLConnection connection = url.openConnection();
            connection.connect();
            System.out.println("Internet is connected");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static ScheduledFuture<?> setup(){
        //wait for network
        while(true){
            if (isInternet()) break;
        }

        //start bot
        try {
            bot = new CourierBot();
        } catch (LoginException e) {
            System.out.println("ERROR: Bot token is invalid!");
        }

        //send IP
        ipNotif();

        //schedule bot checks
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        return scheduler.scheduleAtFixedRate(CourierBot::run, 0, 30, TimeUnit.MINUTES);
    }
}
