package pl.pszczola3mk.dbReport.business;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
import pl.pszczola3mk.dbReport.model.LogsMethod;

@Component
public class LogAnalyzeBusiness {

	private static final Logger log = LoggerFactory.getLogger(LogAnalyzeBusiness.class);
	private Path path = null;
	private Map<String, LogsMethod> methods = new HashMap<>();
	private Map<String, LogsMethod> beans = new HashMap<>();

	public List<LogsMethod> logAnalyze(String beanNameArg, String userName, String password, String host, String filePath, boolean withRefresh) throws Exception {
		String line = null;
		if (withRefresh || this.path == null) {
			this.path = uploadFileAnalyze(userName, password, host, filePath);
			//this.path = FileSystems.getDefault().getPath("/home/pszczola/PG/tmp", "testlog.tmp");
			FileInputStream inputStream = null;
			Scanner sc = null;
			inputStream = new FileInputStream(this.path.toFile());
			sc = new Scanner(inputStream, "UTF-8");
			while (sc.hasNextLine()) {
				try {
					line = sc.nextLine();
					if (line.contains(" STOP ")) {
						//log.info("STOP " + context.getMethod().getName() + ": " + (diff / (1000 * 1000)) + "ms " + (diff / (1000 * 1000 * 1000)) + "s " + " list size: " + size);
						String[] split = line.split(" STOP ")[1].split(" ");
						String beanName = line.split("STOP")[0].split("INFO")[1].trim().split(" ")[0].trim().replace("[", "").replace("]", "");
						String name = split[0].replace(":", "");
						Integer timeInMilis = Integer.parseInt(split[1].replace("ms", ""));
						LogsMethod logsMethod = this.methods.get(name);
						if (logsMethod == null) {
							logsMethod = new LogsMethod(name, timeInMilis, beanName);
							this.methods.put(name, logsMethod);
						} else {
							logsMethod.increase(timeInMilis);
						}
						LogsMethod beanMethod = this.beans.get(beanName);
						if (beanMethod == null) {
							beanMethod = new LogsMethod(null, timeInMilis, beanName);
							this.beans.put(beanName, beanMethod);
						} else {
							beanMethod.increase(timeInMilis);
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
		/*for (String s : this.methods.keySet()) {
			LogsMethod logsMethod = this.methods.get(s);
			log.info(logsMethod.toString());
		}
		for (String s : this.beans.keySet()) {
			LogsMethod logsMethod = this.beans.get(s);
			log.info(logsMethod.toString());
		} */
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
					if (buf[0] == ' ')
						break;
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
				uploadedFile = Files.createTempFile("scriptGenerator", "log.tmp");
				fos = new FileOutputStream(uploadedFile.toFile());
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
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
			return uploadedFile;
		} catch (Exception e) {
			try {
				if (fos != null)
					fos.close();
			} catch (Exception ee) {
			}
			throw e;
		}
	}

	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		//          1 for error,
		//          2 for fatal error,
		//          -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;
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
