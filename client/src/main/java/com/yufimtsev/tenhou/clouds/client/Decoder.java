package com.yufimtsev.tenhou.clouds.client;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class Decoder {

    public static String parseAuth(String authMessage) {
        Document document = Jsoup.parse(authMessage);
        Elements helo = document.getElementsByTag("helo");
        if (helo.size() > 0) {
            return helo.get(0).attr("auth");
        }
        return null;
    }

    public static String parseChat(String chatMessage) {
        Document document = Jsoup.parse(chatMessage);
        Elements messages = document.getElementsByTag("chat");
        if (messages.size() > 0) {
            Element message = messages.get(0);
            try {
                String uname = URLDecoder.decode(message.attr("uname"), "UTF-8");
                String lobby = URLDecoder.decode(message.attr("lobby"), "UTF-8");
                String text = URLDecoder.decode(message.attr("text"), "UTF-8");
                if (lobby.length() > 0) {
                    return "LOBBY " + lobby;
                } else if (uname.length() > 0 ) {
                    return uname + ": " + text;
                } else {
                    return text;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return "";
            }
        }
        return "";
    }
}
