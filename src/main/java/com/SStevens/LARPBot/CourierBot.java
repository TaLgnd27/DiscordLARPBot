package com.SStevens.LARPBot;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.Route;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Scanner;

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

    public ShardManager getShardManager(){
        return shardManager;
    }

    public Dotenv getConfig(){
        return config;
    }

    public static void main(String[] args) throws InterruptedException {
        CourierBot bot = null;
        try {
            bot = new CourierBot();
        } catch (LoginException e) {
            System.out.println("ERROR: Bot token is invalid!");
        }

        while(true){
            runLoop();
            //run loop
            File f = new File("savedLink.txt");
            String link;
            if(f.exists()){
                link = loadLink();
            } else {
                link = getEventLink();
            }
            if(isValidLink(link)){
                String message = "@everyone I've been looking for you. Got something I'm supposed to deliver - your hands only.\n" +
                        link;
                List<TextChannel> channels = bot.shardManager.getTextChannelsByName("Announcements", true);
                for (TextChannel channel:
                     channels) {
                    channel.sendMessage(message).queue();
                }
                saveLink(link);
            } else {
                System.out.println("Invalid");
            }

            Thread.sleep(600000);
        }

    }

    public static void saveLink(String link){
        File f = new File("savedLink.txt");
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
        String out = "";
        try {
            File myObj = new File("savedLink.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                out += myReader.nextLine();
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
        }
        return out;
    }

    public static String getEventLink() {
        Document doc = null;
        try {
            doc = Jsoup.connect("https://lasthopelarp.proboards.com/board/6/upcoming-events").userAgent("mozilla/17.0").get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Elements temp = doc.select("tr.item.thread.first");
        temp = temp.select("span.link.target > a");
        String out = "https://lasthopelarp.proboards.com" + temp.attr("href");

        return out;
    }

    public static boolean isValidLink(String s){
        boolean out = true;
        Document doc = null;
        try {
            doc = Jsoup.connect(s).userAgent("mozilla/17.0").get();
        } catch (IOException e) {
            e.printStackTrace();
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

    public static void runLoop(){

    }

}
