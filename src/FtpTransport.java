import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;


public class FtpTransport {
	private String ext_ftp_url;
	private int ext_ftp_port;
	private String ext_ftp_user;
	private String ext_ftp_pass;
	
	private String int_ftp_url;
	private int int_ftp_port;
	private String int_ftp_user;
	private String int_ftp_pass;
	
	private String transport_tmp_dic;
	
	
	/**
	 * 构造FTP传输工具
	 * @param ext_ftp_url 外网FTP地址
	 * @param ext_ftp_port 外网FTP端口
	 * @param ext_ftp_user 外网FTP用户名
	 * @param ext_ftp_pass 外网FTP密码
	 * @param int_ftp_url 内网FTP地址
	 * @param int_ftp_port 内网FTP端口
	 * @param int_ftp_user 内网FTP用户名
	 * @param int_ftp_pass 内网FTP密码
	 */
	public FtpTransport(String ext_ftp_url, int ext_ftp_port,String ext_ftp_user, String ext_ftp_pass, String int_ftp_url,int int_ftp_port, String int_ftp_user, String int_ftp_pass,String transport_tmp_dic) {
		this.ext_ftp_url = ext_ftp_url;
		this.ext_ftp_port = ext_ftp_port;
		this.ext_ftp_user = ext_ftp_user;
		this.ext_ftp_pass = ext_ftp_pass;
		this.int_ftp_url = int_ftp_url;
		this.int_ftp_port = int_ftp_port;
		this.int_ftp_user = int_ftp_user;
		this.int_ftp_pass = int_ftp_pass;
		this.transport_tmp_dic = transport_tmp_dic;
		
	}

	/**
	 * 开始转发附件内容
	 * @param message
	 */
	public void ftpStart() {
		FTPClient ext_ftp = new FTPClient();
		FTPClient int_ftp = new FTPClient();
		try{
		
			//连接外网
			ext_ftp.connect(ext_ftp_url,ext_ftp_port);
			ext_ftp.setControlEncoding("GBK");
//			ext_ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
			//登录
			if(!ext_ftp.login(ext_ftp_user, ext_ftp_pass)){
				System.out.println("登录外网FTP服务器失败");
				return;
			}
			System.out.println("外网FTP服务器连接成功!");
			
			int_ftp.connect(int_ftp_url,int_ftp_port);
        	if(!int_ftp.login(int_ftp_user, int_ftp_pass)){
				System.out.println("登录内网FTP服务器失败");
				return;
			}
        	
			System.out.println("内网FTP服务器连接成功!");
			
			List<FtpObject> fileList = genFilelist(ext_ftp);
			for(FtpObject fo : fileList){
				System.out.println("路径：" + fo.getPath() + "\t文件名：" + fo.getName());
			}
			
			int_ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
			
			for(FtpObject fo : fileList){
        		File file = new File(transport_tmp_dic + fo.getName());
            	OutputStream os = new FileOutputStream(file);
        		if(ext_ftp.retrieveFile(fo.getPath() + "" + new String(fo.getName().getBytes("GBK"),"iso-8859-1"), os)){
            		os.close();
        		}
        		
        		if(!fo.getPath().equals("/")){
        			createDir(int_ftp, fo.getPath());
        		}
        		
        		int_ftp.changeWorkingDirectory("/");
        		InputStream is = new FileInputStream(file);
        		if(int_ftp.storeFile(fo.getPath() + "" + new String(fo.getName().getBytes("GBK"),"iso-8859-1"), is)){
        			System.out.println("文件" + fo.getName() + "同步完成!");
        			is.close();
        		}else{
        			System.out.println("文件" + fo.getName() + "同步失败!");
        		}
        		
        		ext_ftp.deleteFile(fo.getPath() + "" + new String(fo.getName().getBytes("GBK"),"iso-8859-1"));
//        		if(!fo.getPath().equals("/") && ext_ftp.listFiles(fo.getPath()).length == 0){//如果目录空了那么删除掉目录
//        			ext_ftp.deleteFile(fo.getPath());
//        		}
        		
        		file.delete();
        		int_ftp.changeWorkingDirectory("/");
			}
			System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " \t本次操作完成,等待下一次操作开始..\n----------------------------------------------");
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try {
				ext_ftp.disconnect();
				int_ftp.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	
	//获取全部要同步的文件列表
	public List<FtpObject> genFilelist(FTPClient ext_ftp){
		List<FtpObject> list = new ArrayList<FtpObject>();
		recursion(ext_ftp,list,"/");
		return list;
	}
	
	//迭代
	public void recursion(FTPClient ext_ftp,List<FtpObject> fileList,String path){
		try {
			FTPFile[] files = ext_ftp.listFiles(path);
			for(FTPFile file : files){
				if(file.isFile()){
					fileList.add(new FtpObject(path,file.getName()));
				}else if(file.isDirectory()){
					recursion(ext_ftp, fileList,path + file.getName() + "/");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//根据路径创建目录,并会把工作目录自动转换到最后子目录
    private boolean createDir(FTPClient ftpClient,String path) throws IOException {
        if(path == null|| path == "" || ftpClient == null)
        {
            return false;
        }
        String[] paths = path.split("/");
        for (int i = 0; i < paths.length; i++){
        	boolean created = false;
        	if(!paths[i].equals("")){
        		FTPFile[] flst = ftpClient.listFiles(paths[i]);
        		for(FTPFile file : flst){
        			if(file.isDirectory() && file.getName() == paths[i]){
        				created = true;
        				ftpClient.changeWorkingDirectory("" + paths[i]);
        				continue;
        			}
        		}
        		
        		if(!created){//未创建
        			ftpClient.makeDirectory(paths[i]);
        			ftpClient.changeWorkingDirectory(paths[i]);
        		}
        	}
        }
        return true;
    }

}
