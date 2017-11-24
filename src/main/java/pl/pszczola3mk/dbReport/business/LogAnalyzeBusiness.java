package pl.pszczola3mk.dbReport.business;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tukaani.xz.XZInputStream;
import pl.pszczola3mk.dbReport.model.FileForCompare;
import pl.pszczola3mk.dbReport.model.LogsMethod;
import pl.pszczola3mk.dbReport.model.MethodInvoke;

@Component
public class LogAnalyzeBusiness {

	private static final Logger log = LoggerFactory.getLogger(LogAnalyzeBusiness.class);
	private Path path = null;
	private Map<String, LogsMethod> methods = new HashMap<>();
	private Map<String, LogsMethod> beans = new HashMap<>();
	private List<FileForCompare> filesForCompare = new ArrayList<>();

	public List<FileForCompare> compare(String methodName) {
		List<FileForCompare> files = new ArrayList<>();
		for (FileForCompare fileForCompare : this.filesForCompare) {
			if (fileForCompare.getLogsMethod().getMethodName().equals(methodName)) {
				files.add(fileForCompare);
			}
		}
		return files;
	}

	public List<LogsMethod> trackPersonInvokes(String personNumber) {
		Map<String, LogsMethod> map = new HashMap<>();
		for (LogsMethod lm : this.methods.values()) {
			for (MethodInvoke mi : lm.getInvokeList()) {
				if (mi.getPersonId().equals(personNumber)) {
					LogsMethod method = map.get(mi.getBeanName() + " " + mi.getMethodName());
					if (method != null) {
						method.increase(mi.getDurationInMilis(), mi.getPersonId());
					} else {
						method = new LogsMethod(lm.getBeanName(), lm.getMethodName(), mi.getPersonId(), mi.getParams(), mi.getInvokeDate());
						method.increase(mi.getDurationInMilis(), mi.getPersonId());
						map.put(mi.getBeanName() + " " + mi.getMethodName(), method);
					}
				}
			}
		}
		return map.values().stream().collect(Collectors.toList());
	}

	public List<LogsMethod> logAnalyze(String beanNameArg, String userName, String password, String host, String filePath, boolean withRefresh, List<String> methodsNamesForCompare) throws Exception {
		String line = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
		if (withRefresh || this.path == null) {
			this.methods = new HashMap<>();
			this.beans = new HashMap<>();
			if (host.equals("local")) {
				File f = new File(filePath);
				this.path = f.toPath();
			} else {
				this.path = uploadFileAnalyze(userName, password, host, filePath);
			}
			FileInputStream inputStream = null;
			Scanner sc = null;
			inputStream = new FileInputStream(this.path.toFile());
			sc = new Scanner(inputStream, "UTF-8");
			while (sc.hasNextLine()) {
				try {
					line = sc.nextLine();
					if (line.contains(" STOP ")) {
						//log.info("STOP ["+personNumber+"] " + context.getMethod().getName() + " " + (diff / (1000 * 1000)) + "ms " + (diff / (1000 * 1000 * 1000)) + "s " + " list size: " + size);
						String[] split = line.split(" STOP ")[1].split(" ");
						String beanName = line.split("STOP")[0].split("INFO")[1].trim().split(" ")[0].trim().replace("[", "").replace("]", "");
						String personId = split[0].replace("[", "").replace("]", "");
						String name = split[1].replace(":", "");
						Integer timeInMilis = Integer.parseInt(split[2].replace("ms", ""));
						LogsMethod logsMethod = this.methods.get(beanName + " " + name);
						if (logsMethod != null) {
							logsMethod.increase(timeInMilis, personId);
						}
						//only for beans stats
						LogsMethod beansMethod = this.beans.get(beanName);
						if (beansMethod != null) {
							beansMethod.increase(timeInMilis, null);
						} else {
							beansMethod = new LogsMethod(beanName, name, null, null, null);
							beansMethod.increase(timeInMilis, null);
							this.beans.put(beanName, beansMethod);
						}
					}
					if (line.contains(" START ")) {
						//log.info("START ["+personNumber+"] " + context.getMethod().getName() + " " + params);
						//2017-10-30 00:00:04,660 INFO  [pg.cui.components.studentManager.businessObjects.StudentManagerBean] (default task-176) START: searchMyStudentMessage params:
						String[] split = line.split(" START ")[1].split(" ");
						String date = line.split("START")[0].split("INFO")[0].trim();
						Date invokeDate = sdf.parse(date);
						String beanName = line.split("START")[0].split("INFO")[1].trim().split(" ")[0].trim().replace("[", "").replace("]", "");
						String personId = split[0].replace("[", "").replace("]", "");
						String name = split[1].replace(":", "");
						String params = split[2];
						LogsMethod beansMethod = this.methods.get(beanName + " " + name);
						if (beansMethod == null) {
							this.methods.put(beanName + " " + name, new LogsMethod(beanName, name, personId, params, invokeDate));
						}
					}
				} catch (Exception e) {
					log.error("Error w przetwarzaniu linii: " + line);
				}
			}
			if (inputStream != null) {
				inputStream.close();
			}
			if (sc != null) {
				sc.close();
			}
		}
		if (methodsNamesForCompare != null && methodsNamesForCompare.size() > 0) {
			for (String method : methodsNamesForCompare) {
				LogsMethod logsMethod = this.methods.get(method);
				FileForCompare mfc = new FileForCompare();
				String[] splitPath = filePath.split("/");
				String orgFileName = splitPath[splitPath.length - 1];
				mfc.setFileName(orgFileName);
				mfc.setLogsMethod(logsMethod);
				this.filesForCompare.add(mfc);
			}
		}
		if (beanNameArg != null) {
			return this.methods.values().stream().filter(m -> m.getBeanName().equals(beanNameArg)).collect(Collectors.toList());
		} else {
			return this.beans.values().stream().collect(Collectors.toList());
		}
	}

	private Path uploadFileAnalyze(String userName, String password, String host, String filePath) throws Exception {
		FileOutputStream fos = null;
		Path uploadedFile = null;
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(userName, host, 22);
			session.setPassword(password);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			String command = "scp -f " + filePath;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();
			channel.connect();
			byte[] buf = new byte[1024];
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}
				// read '0644 '
				in.read(buf, 0, 5);
				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ') {
						break;
					}
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}
				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}
				log.info("filesize=" + filesize + ", file=" + file);
				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
				// read a content of lfile
				String[] splitPath = filePath.split("/");
				String orgFileName = splitPath[splitPath.length - 1];
				uploadedFile = Files.createTempFile("sg" + orgFileName, "log.tmp");
				fos = new FileOutputStream(uploadedFile.toFile());
				int foo;
				while (true) {
					if (buf.length < filesize) {
						foo = buf.length;
					} else {
						foo = (int) filesize;
					}
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L) {
						break;
					}
				}
				fos.close();
				fos = null;
				if (checkAck(in) != 0) {
					System.exit(0);
				}
				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}
			session.disconnect();
			log.info("file: " + uploadedFile.toAbsolutePath());
			if (filePath.endsWith("xz")) {
				uploadedFile = Paths.get(extractXz(uploadedFile.toString()));
			}
			return uploadedFile;
		} catch (Exception e) {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (Exception ee) {
			}
			throw e;
		}
	}

	private String extractXz(String filePath) throws IOException {
		try {
			String decFilepath = filePath + "Dec";
			FileInputStream fin = new FileInputStream(filePath);
			BufferedInputStream in = new BufferedInputStream(fin);
			FileOutputStream out = new FileOutputStream(decFilepath);
			XZInputStream xzIn = new XZInputStream(in);
			final byte[] buffer = new byte[8192];
			int n = 0;
			while (-1 != (n = xzIn.read(buffer))) {
				out.write(buffer, 0, n);
			}
			out.close();
			xzIn.close();
			return decFilepath;
		} catch (Exception e) {
			log.error("Decompress error:", e);
			throw e;
		}
	}

	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		//          1 for error,
		//          2 for fatal error,
		//          -1
		if (b == 0) {
			return b;
		}
		if (b == -1) {
			return b;
		}
		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}
}
