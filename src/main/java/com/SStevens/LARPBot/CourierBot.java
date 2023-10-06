package com.SStevens.LARPBot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CourierBot {

    private final ShardManager shardManager;

    private final Dotenv config;

    public CourierBot() {
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
    static BotHandler botHandler;

    public static void main(String[] args){
        ScheduledFuture<?> botHandler = setup();

        //exit check
        Scanner in = new Scanner(System.in);
        boolean isExit = false;
        while (!isExit){
            if(in.nextLine().equalsIgnoreCase("exit")){
                isExit = true;
            }
        }
        System.out.println("Exiting program");
        botHandler.cancel(true);
        System.exit(0);
    }

    public static void saveLink(String link){
        try {
            FileWriter myWriter = new FileWriter("savedLink.txt");
            myWriter.write(link);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            e.printStackTrace();
        }
        return out.toString();
    }

    public static String getEventLink() {
        Document doc;
        try {
            doc = Jsoup.connect("https://lasthopelarp.proboards.com/board/6/upcoming-events").userAgent("mozilla/17.0").get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements temp = doc.select("tr.item.thread.first");
        temp = temp.select("span.link.target > a");

        return "https://lasthopelarp.proboards.com" + temp.attr("href");
    }

    public static boolean isValidLink(String s){
        boolean out = true;
        Document doc;
        try {
            doc = Jsoup.connect(s).userAgent("mozilla/17.0").get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

    public static String getIP(){
        //get ip
        try {
            URL whatismyip = new URI("https://checkip.amazonaws.com").toURL();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            return  in.readLine();
        } catch (Exception e) {
            return  "IP Unknown";
        }
    }

    public static void ipNotify(){
        String ip = getIP();
        System.out.println(ip);
        User usr = bot.shardManager.retrieveUserById("343771474202722304").complete();
        usr.openPrivateChannel().flatMap(channel -> channel.sendMessage("Here is my new IP: " + ip)).queue();

    }

    public static boolean isInternet(){
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
        System.out.println("Waiting for network");
        while(true){
            if (isInternet()) break;
        }

        //start bot
        try {
            bot = new CourierBot();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //schedule bot checks
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        botHandler = new BotHandler();
        return scheduler.scheduleAtFixedRate(botHandler, 0, 10, TimeUnit.SECONDS);
    }
}
