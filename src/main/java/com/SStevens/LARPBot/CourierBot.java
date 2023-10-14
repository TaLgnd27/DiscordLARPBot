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
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.spi.InetAddressResolver;
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

    public static ScheduledFuture<?> setup(){
        //start bot loop
        while(true) {
            try {
                bot = new CourierBot();
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }

        //schedule bot checks
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        botHandler = new BotHandler();
        return scheduler.scheduleAtFixedRate(botHandler, 0, 1, TimeUnit.HOURS);
    }
}
