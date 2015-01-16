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
	 * ����FTP���乤��
	 * @param ext_ftp_url ����FTP��ַ
	 * @param ext_ftp_port ����FTP�˿�
	 * @param ext_ftp_user ����FTP�û���
	 * @param ext_ftp_pass ����FTP����
	 * @param int_ftp_url ����FTP��ַ
	 * @param int_ftp_port ����FTP�˿�
	 * @param int_ftp_user ����FTP�û���
	 * @param int_ftp_pass ����FTP����
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
	 * ��ʼת����������
	 * @param message
	 */
	public void ftpStart() {
		FTPClient ext_ftp = new FTPClient();
		FTPClient int_ftp = new FTPClient();
		try{
		
			//��������
			ext_ftp.connect(ext_ftp_url,ext_ftp_port);
			ext_ftp.setControlEncoding("GBK");
//			ext_ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
			//��¼
			if(!ext_ftp.login(ext_ftp_user, ext_ftp_pass)){
				System.out.println("��¼����FTP������ʧ��");
				return;
			}
			System.out.println("����FTP���������ӳɹ�!");
			
			int_ftp.connect(int_ftp_url,int_ftp_port);
        	if(!int_ftp.login(int_ftp_user, int_ftp_pass)){
				System.out.println("��¼����FTP������ʧ��");
				return;
			}
        	
			System.out.println("����FTP���������ӳɹ�!");
			
			List<FtpObject> fileList = genFilelist(ext_ftp);
			for(FtpObject fo : fileList){
				System.out.println("·����" + fo.getPath() + "\t�ļ�����" + fo.getName());
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
        			System.out.println("�ļ�" + fo.getName() + "ͬ�����!");
        			is.close();
        		}else{
        			System.out.println("�ļ�" + fo.getName() + "ͬ��ʧ��!");
        		}
        		
        		ext_ftp.deleteFile(fo.getPath() + "" + new String(fo.getName().getBytes("GBK"),"iso-8859-1"));
//        		if(!fo.getPath().equals("/") && ext_ftp.listFiles(fo.getPath()).length == 0){//���Ŀ¼������ôɾ����Ŀ¼
//        			ext_ftp.deleteFile(fo.getPath());
//        		}
        		
        		file.delete();
        		int_ftp.changeWorkingDirectory("/");
			}
			System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " \t���β������,�ȴ���һ�β�����ʼ..\n----------------------------------------------");
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
	
	
	//��ȡȫ��Ҫͬ�����ļ��б�
	public List<FtpObject> genFilelist(FTPClient ext_ftp){
		List<FtpObject> list = new ArrayList<FtpObject>();
		recursion(ext_ftp,list,"/");
		return list;
	}
	
	//����
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
	
	//����·������Ŀ¼,����ѹ���Ŀ¼�Զ�ת���������Ŀ¼
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
        		
        		if(!created){//δ����
        			ftpClient.makeDirectory(paths[i]);
        			ftpClient.changeWorkingDirectory(paths[i]);
        		}
        	}
        }
        return true;
    }

}
