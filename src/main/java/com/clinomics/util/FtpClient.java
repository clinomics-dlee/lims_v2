package com.clinomics.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpClient {

	public List<String> getFileList() {
		FTPClient ftp = null;
		List<String> rtnFiles = new ArrayList<String>();
		try {
			ftp = new FTPClient();
			ftp.setControlEncoding("UTF-8");

			ftp.connect("ftp.somehost");
			ftp.login("user", "pass");
			ftp.changeWorkingDirectory("/dbdump");
			FTPFile[] files = ftp.listFiles();
			
			
			for (FTPFile file : files) {
				rtnFiles.add(file.getName());
			}
			
			ftp.logout();
		} catch (SocketException e) {
			System.out.println("Socket:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO:" + e.getMessage());
		} finally {
			if (ftp != null && ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException e) {
				}
			}
		}
		return rtnFiles;
	}
	
	public void doDownload(String fileName, String path) {
		FTPClient ftp = null;
		try {
			ftp = new FTPClient();
			ftp.setControlEncoding("UTF-8");

			ftp.connect("ftp.somehost");
			ftp.login("user", "pass");
			ftp.changeWorkingDirectory("/dbdump");
			//ftp.listFiles();

			File f = new File("d:\\dbdump", "oradump1_200605.tmp");
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(f);
				boolean isSuccess = ftp.retrieveFile("oradump1.tmp", fos);
				if (isSuccess) {
					// 다운로드 성공
				} else {
					// 다운로드 실패
				}
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			} finally {
				if (fos != null)
					try {
						fos.close();
					} catch (IOException ex) {
						
					}
			}
			ftp.logout();
		} catch (SocketException e) {
			System.out.println("Socket:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO:" + e.getMessage());
		} finally {
			if (ftp != null && ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException e) {
				}
			}
		}

	}
}
