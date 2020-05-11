package com.clinomics.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.activation.MimetypesFileTypeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

	public static List<String> getFileList(String filePath) {
		File path = new File(filePath);
		File[] arrFiles = path.listFiles();
		List<String> rtn = new ArrayList<String>();
		if (arrFiles != null) {
			List<File> listFile = Arrays.asList(arrFiles);
			rtn = listFile.stream().map(f -> f.getName()).collect(Collectors.toList());
		}
		return rtn;
	}
	/*
	 * file의 mimeType Get
	 */
	public static String getMimeType(String filename) {
		File f = new File(filename);
		String mimeType = new MimetypesFileTypeMap().getContentType(f);
		if(mimeType != null && !"".equals(mimeType)){
			String[] tmp = mimeType.split("/");

			if(tmp.length > 0){
				//for(int i=0; tmp.length > i; i++) {
					//System.out.println("("+i+") ContentType : " + tmp[i]);
				//}
				mimeType = tmp[0];
			}
		}
		logger.info("mimeType" + mimeType);
		return mimeType;
	}

	/*확장자를 제거한 파일명*/
	public static String getFileNameWithoutExt(String filename) {
		if (filename == null) {
            return "";
        }
        final int index = filename.lastIndexOf(".");
        if (index == -1) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
	}
	
	/*파일명에서 확장자만 알아내기*/
	public static String getFileNameExt(String filename) {
		if (filename == null) {
            return "";
        }
		final int index = filename.lastIndexOf(".");
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
	}
	
	public static String moveFile(String fileName, String beforeFilePath, String afterFilePath) {

		String path = afterFilePath;
		String filePath = path + "/" + fileName;
		File dir = new File(path);
		if (!dir.exists()) { // 폴더 없으면 폴더 생성
			dir.mkdirs();
		}
		try {
			File file = new File(beforeFilePath + "/" + fileName);
			if (file.renameTo(new File(filePath))) { // 파일 이동
				return filePath; // 성공시 성공 파일 경로 return
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean createFolder(String path) {
		File folder = new File(path);

		if (!folder.exists()) {
			folder.mkdir();
			return true;
		}
		return false;
	}
}
