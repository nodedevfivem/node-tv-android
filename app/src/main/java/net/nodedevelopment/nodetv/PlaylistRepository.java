package net.nodedevelopment.nodetv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaylistRepository {
    public static final String WORLD_URL = "https://iptv-org.github.io/iptv/index.m3u";
    public static final String PH_URL = "https://iptv-org.github.io/iptv/countries/ph.m3u";
    private static final int MAX_CHANNELS = 24000;
    private static final int MAX_BYTES = 25 * 1024 * 1024;
    private static final Pattern ATTR = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");

    public static List<Channel> fetch(boolean philippines) throws IOException {
        String url = philippines ? PH_URL : WORLD_URL;
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(22000);
        c.setRequestProperty("User-Agent", "NODE-TV-Android/0.1.0");
        c.setRequestProperty("Accept", "audio/x-mpegurl, application/x-mpegURL, */*");
        try {
            if (c.getResponseCode() != 200) throw new IOException("Channel directory unavailable (HTTP " + c.getResponseCode() + ")");
            long length = c.getContentLengthLong();
            if (length > MAX_BYTES) throw new IOException("Directory exceeds size limit");
            try (InputStream in = c.getInputStream()) {
                byte[] data = new byte[8192]; int n, total = 0;
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                while ((n = in.read(data)) >= 0) {
                    total += n;
                    if (total > MAX_BYTES) throw new IOException("Directory exceeds size limit");
                    out.write(data, 0, n);
                }
                return parse(new String(out.toByteArray(),StandardCharsets.UTF_8),philippines?"Philippines":"Worldwide");
            }
        } finally {c.disconnect();}
    }

    public static List<Channel> parse(String m3u, String region) {
        ArrayList<Channel> channels = new ArrayList<>();
        String title = "",category = "Other",logo = "",tvgId = "";
        boolean expectStream = false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(m3u.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8));
        try {
            String line;
            while ((line=reader.readLine()) != null && channels.size() < MAX_CHANNELS) {
                line=line.trim();
                if (line.startsWith("#EXTINF")) {
                    int comma=line.lastIndexOf(',');
                    title=comma>=0 ? line.substring(comma+1).trim() : "Unnamed Channel";
                    category="Other";logo="";tvgId="";
                    Matcher m=ATTR.matcher(line.substring(0,comma>=0?comma:line.length()));
                    while (m.find()) {
                        switch(m.group(1).toLowerCase(Locale.ROOT)) {
                            case "group-title":category=m.group(2).trim();break;
                            case "tvg-logo":logo=m.group(2).trim();break;
                            case "tvg-id":tvgId=m.group(2).trim();break;
                        }
                    }
                    expectStream=true;
                } else if (!line.isEmpty() && !line.startsWith("#") && expectStream) {
                    if (line.startsWith("https://") || line.startsWith("http://")) {
                        channels.add(new Channel(title.isEmpty()?"Unknown Channel":title,line,
                                category.isEmpty()?"Other":category,logo,tvgId,region));
                    }
                    expectStream=false;
                }
            }
        }catch(IOException ignored) {} // In-memory reader
        Collections.sort(channels,Comparator.comparing(ch->ch.title.toLowerCase(Locale.ROOT)));
        return channels;
    }
}
