package net.nodedevelopment.nodetv;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** GitHub release checker + verified APK handoff. No silent installs or hidden downloads. */
public final class UpdateClient {
    public static final String OWNER="nodedevfivem";
    public static final String REPO="NODE-TV-Android";
    private static final String API="https://api.github.com/repos/"+OWNER+"/"+REPO+"/releases/latest";
    private static final long MAX_APK=180L*1024*1024;
    public interface Progress {void progress(long complete,long total);}
    public static final class Release {
        public final String version,releasePage,apkUrl,shaUrl,shaDigest,apkName;
        public final long size;
        Release(String v,String page,String apk,String sha,String digest,String filename,long bytes){
            version=v;releasePage=page;apkUrl=apk;shaUrl=sha;shaDigest=digest;apkName=filename;size=bytes;
        }
    }
    private static String readSmall(String target,int limit) throws Exception {
        HttpURLConnection c=connection(target);
        try {
            if(c.getResponseCode()!=200)throw new Exception("HTTP "+c.getResponseCode());
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
            try(InputStream in=c.getInputStream()){
                while((n=in.read(buf))!=-1){if(out.size()+n>limit)throw new Exception("Response exceeds limit");out.write(buf,0,n);}
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }finally{c.disconnect();}
    }
    private static HttpURLConnection connection(String target) throws Exception {
        if(!target.startsWith("https://"))throw new Exception("Only secure downloads allowed");
        HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();
        c.setInstanceFollowRedirects(true);c.setConnectTimeout(14000);c.setReadTimeout(24000);
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("User-Agent","NODE-TV-Android/0.1.0");
        return c;
    }
    public static int compareVersions(String a,String b){
        String[] aa=a.replaceFirst("^[vV]","").split("\\.");
        String[] bb=b.replaceFirst("^[vV]","").split("\\.");
        for(int i=0;i<3;i++){
            int x=0,y=0;try{if(i<aa.length)x=Integer.parseInt(aa[i]);}catch(Exception ignored){}
            try{if(i<bb.length)y=Integer.parseInt(bb[i]);}catch(Exception ignored){}
            if(x!=y)return Integer.compare(x,y);
        }
        return 0;
    }
    public static Release check(String currentVersion) throws Exception {
        JSONObject release=new JSONObject(readSmall(API,512*1024));
        if(release.optBoolean("draft",true)||release.optBoolean("prerelease",true))return null;
        String version=release.optString("tag_name","");
        if(!version.matches("v\\d+\\.\\d+\\.\\d+"))return null;
        if(compareVersions(version,currentVersion)<=0)return null;
        String filename="NODE-TV-Android-"+version+".apk";
        String shaname="NODE-TV-Android-"+version+"-SHA256.txt";
        String apk="",checksum="",digest="";long size=0;
        JSONArray assets=release.optJSONArray("assets");
        if(assets==null)return null;
        for(int i=0;i<assets.length();i++){
            JSONObject item=assets.getJSONObject(i);
            if(filename.equals(item.optString("name"))){
                apk=item.optString("browser_download_url");size=item.optLong("size");digest=item.optString("digest");
            }else if(shaname.equals(item.optString("name")))checksum=item.optString("browser_download_url");
        }
        // Require publisher checksum or asset digest, never install unverified data.
        if(apk.isEmpty()||size<=0||size>MAX_APK||(checksum.isEmpty()&&!digest.startsWith("sha256:")))return null;
        String required="https://github.com/"+OWNER+"/"+REPO+"/releases/download/"+version+"/";
        if(!apk.startsWith(required)||(!checksum.isEmpty()&&!checksum.startsWith(required)))return null;
        return new Release(version,release.optString("html_url"),apk,checksum,digest,filename,size);
    }
    public static File downloadVerified(Context context,Release release,Progress p) throws Exception {
        String expected=release.shaDigest.startsWith("sha256:")?release.shaDigest.substring(7).trim().toLowerCase(Locale.ROOT):"";
        if(!release.shaUrl.isEmpty()){
            String txt=readSmall(release.shaUrl,2048).trim();
            String[] lines=txt.split("\\r?\\n");
            String fromFile="";
            for(String line:lines){
                String[] bits=line.trim().split("\\s+",2);
                if(bits.length>=1&&bits[0].matches("(?i)[a-f0-9]{64}")&&(bits.length==1||bits[1].contains(release.apkName))) {
                    fromFile=bits[0].toLowerCase(Locale.ROOT);break;
                }
            }
            if(fromFile.isEmpty())throw new Exception("Release SHA-256 file is invalid");
            if(!expected.isEmpty()&&!expected.equals(fromFile))throw new Exception("Release checksum mismatch");
            expected=fromFile;
        }
        if(!expected.matches("[a-f0-9]{64}"))throw new Exception("Publisher checksum unavailable");
        File dir=new File(context.getCacheDir(),"updates");
        if(!dir.exists()&&!dir.mkdirs())throw new Exception("Cannot create update directory");
        File file=new File(dir,release.apkName);
        File partial=new File(dir,release.apkName+".part");
        HttpURLConnection c=connection(release.apkUrl);
        try {
            if(c.getResponseCode()!=200)throw new Exception("Download failed (HTTP "+c.getResponseCode()+")");
            if(!"https".equalsIgnoreCase(c.getURL().getProtocol()))throw new Exception("Insecure redirect blocked");
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            long total=0;byte[] buf=new byte[64*1024];int n;
            try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(partial)){
                while((n=in.read(buf))!=-1){
                    total+=n;if(total>MAX_APK||total>release.size)throw new Exception("Unexpected download size");
                    out.write(buf,0,n);md.update(buf,0,n);
                    if(p!=null)p.progress(total,release.size);
                }
                out.getFD().sync();
            }
            if(total!=release.size)throw new Exception("Incomplete update download");
            StringBuilder digest=new StringBuilder();for(byte b:md.digest())digest.append(String.format(Locale.ROOT,"%02x",b&0xff));
            if(!digest.toString().equals(expected))throw new Exception("SHA-256 verification failed");
            if(file.exists()&&!file.delete())throw new Exception("Cannot replace old update");
            if(!partial.renameTo(file))throw new Exception("Cannot finalize update");
            return file;
        }catch(Exception ex){partial.delete();throw ex;}
        finally{c.disconnect();}
    }
    public static void install(Context ctx,File apk) {
        Uri uri=FileProvider.getUriForFile(ctx,ctx.getPackageName()+".fileprovider",apk);
        Intent i=new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri,"application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }
    public static boolean canInstall(Context ctx){
        return Build.VERSION.SDK_INT<26||ctx.getPackageManager().canRequestPackageInstalls();
    }
    public static Intent requestInstallPermission(Context ctx){
        return new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:"+ctx.getPackageName()));
    }
}
