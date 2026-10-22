package net.nodedevelopment.nodetv;

import org.json.JSONException;
import org.json.JSONObject;

public final class Channel {
    public final String title, url, category, logoUrl, tvgId, region;

    public Channel(String title, String url, String category, String logoUrl, String tvgId, String region) {
        this.title = title; this.url = url; this.category = category;
        this.logoUrl = logoUrl; this.tvgId = tvgId; this.region = region;
    }
    public String key() { return url; }
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("title", title);obj.put("url", url);obj.put("category", category);
        obj.put("logo", logoUrl);obj.put("tvg", tvgId);obj.put("region", region);
        return obj;
    }
    public static Channel fromJson(JSONObject obj) {
        return new Channel(obj.optString("title"),obj.optString("url"),obj.optString("category"),
                obj.optString("logo"),obj.optString("tvg"),obj.optString("region"));
    }
}
