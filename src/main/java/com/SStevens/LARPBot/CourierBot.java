package com.SStevens.LARPBot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    public static void main(String[] args){
        try {
            bot = new CourierBot();
        } catch (LoginException e) {
            System.out.println("ERROR: Bot token is invalid!");
        }

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        @SuppressWarnings("unused")
        final ScheduledFuture<?> botEventHandler = scheduler.scheduleAtFixedRate(CourierBot::run, 0, 1, TimeUnit.HOURS);

    }

    public static void saveLink(String link){
        try {
            FileWriter myWriter = new FileWriter("savedLink.txt");
            myWriter.write(link);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            e.printStackTrace();
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
        Document doc = null;
        try {
            doc = Jsoup.connect("https://lasthopelarp.proboards.com/board/6/upcoming-events").userAgent("mozilla/17.0").get();
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            saveLink(link);
        } else {
            System.out.println("Invalid");
        }
    }
}
