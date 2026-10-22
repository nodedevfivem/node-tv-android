package net.nodedevelopment.nodetv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UserStore {
    private final SharedPreferences pref;
    private final LinkedHashMap<String,Channel> favorites = new LinkedHashMap<>();
    private final LinkedHashMap<String,Channel> recent = new LinkedHashMap<>();
    public UserStore(Context c) {
        pref=c.getSharedPreferences("node_tv_user",Context.MODE_PRIVATE);
        load("favorites_json",favorites);load("recent_json",recent);
    }
    private void load(String key, Map<String,Channel> into) {
        try {
            JSONArray arr=new JSONArray(pref.getString(key,"[]"));
            for (int i=0;i<arr.length();i++) {
                Channel c=Channel.fromJson(arr.getJSONObject(i));
                if (c.url.startsWith("http://") || c.url.startsWith("https://")) into.put(c.key(),c);
            }
        }catch(Exception ignored) {} // Malformed old preferences should not crash launch.
    }
    private void save(String key, Map<String,Channel> map) {
        JSONArray arr = new JSONArray();
        for(Channel c:map.values()) {try{arr.put(c.toJson());}catch(Exception ignored){}}
        pref.edit().putString(key,arr.toString()).apply();
    }
    public synchronized boolean isFavorite(Channel c){return favorites.containsKey(c.key());}
    public synchronized void toggleFavorite(Channel c) {
        if(favorites.containsKey(c.key())) favorites.remove(c.key());else favorites.put(c.key(),c);
        save("favorites_json",favorites);
    }
    public synchronized List<Channel> favorites(){return new ArrayList<>(favorites.values());}
    public synchronized void watched(Channel c) {
        recent.remove(c.key());
        LinkedHashMap<String,Channel> updated=new LinkedHashMap<>();updated.put(c.key(),c);
        for(Channel item:recent.values()) {if(updated.size()>=30)break;updated.put(item.key(),item);}
        recent.clear();recent.putAll(updated);save("recent_json",recent);
        try {pref.edit().putString("last_channel", c.toJson().toString()).apply();}
        catch(Exception ignored) { /* Keep recent history even if one channel has invalid metadata. */ }
    }
    public synchronized List<Channel> recent(){return new ArrayList<>(recent.values());}
    public Channel lastChannel(){try{return Channel.fromJson(new JSONObject(pref.getString("last_channel","{}")));}catch(Exception ex){return null;}}
    public int theme(){return pref.getInt("theme",0);}
    public void setTheme(int i){pref.edit().putInt("theme",i).apply();}
    public int lastRegion(){return pref.getInt("region",1);}
    public void setLastRegion(int value){pref.edit().putInt("region",value).apply();}
    /** Last mandatory version successfully observed in a published GitHub Release.
     * Keep the required-update gate across app restarts or temporary network outages.
     * Once this installed app reaches that version, clear the requirement.
     */
    public String requiredVersion(){return pref.getString("required_version", "");}
    public void setRequiredVersion(String version){
        pref.edit().putString("required_version", version).apply();
    }
    public void clearRequiredVersion(){pref.edit().remove("required_version").apply();}
    public long snoozeUntil(){return pref.getLong("update_snooze_until",0L);}
    public void snooze(){pref.edit().putLong("update_snooze_until",System.currentTimeMillis()+24L*60*60*1000).apply();}
}
