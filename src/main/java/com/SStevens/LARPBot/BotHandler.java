package com.SStevens.LARPBot;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.SStevens.LARPBot.CourierBot.*;

class BotHandler implements Runnable {
    private final static Logger LOGGER =
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    String ip;

    @Override
    public void run() {
            try {
                String link = getEventLink();
                if (isValidLink(link)) {
                    System.out.println(java.time.LocalDateTime.now()+ ": " + link);
                    String message = "@everyone I've been looking for you. Got something I'm supposed to deliver - your hands only.\n" +
                            link;
                    List<TextChannel> channels = bot.getShardManager().getTextChannelsByName("Announcements", true);
                    for (TextChannel channel :
                            channels) {
                        try {
                            channel.sendMessage(message).queue();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println(java.time.LocalDateTime.now() + ": Saving Link");
                    saveLink(link);
                } else {
                    System.out.println(java.time.LocalDateTime.now() + ": Invalid");
                }
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, Arrays.toString(t.getStackTrace()));
            }
    }
}