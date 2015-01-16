import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;


public class Startup {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			FtpTransport ft = new FtpTransport(getProperty("Extranet.FTP.URL"), Integer.parseInt(getProperty("Extranet.FTP.PORT")), getProperty("Extranet.FTP.USER"), getProperty("Extranet.FTP.PASS"), getProperty("Intranet.FTP.URL"), Integer.parseInt(getProperty("Intranet.FTP.PORT")), getProperty("Intranet.FTP.USER"), getProperty("Intranet.FTP.PASS"),getProperty("Transport.TMP.DIC"));
			long interval = Long.parseLong(getProperty("Transport.Interval"));
			while(true){
				ft.ftpStart();
				Thread.sleep(interval);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * 获取系统全局配置
	 * @param key
	 * @return
	 */
	public static String getProperty(String key){
		try {
			Properties props = new Properties();
			InputStream fis = new FileInputStream(new File(Startup.class.getResource("/").getPath() + "application.properties"));
			props.load(fis);
			return props.getProperty(key);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
