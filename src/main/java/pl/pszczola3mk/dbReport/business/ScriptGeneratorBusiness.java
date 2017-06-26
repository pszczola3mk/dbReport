package pl.pszczola3mk.dbReport.business;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.SchemaUpdateScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.pszczola3mk.dbReport.model.User;

@Component
public class ScriptGeneratorBusiness {

	private static final Logger log = LoggerFactory.getLogger(ScriptGeneratorBusiness.class);

	public String generateScript(Path jarPath, String componentName, User user) throws Exception {
		Configuration cfg = new Configuration();
		Properties prop = new Properties();
		prop.put("hibernate.connection.url", "jdbc:postgresql://" + user.getUrl().getValue());
		prop.put("hibernate.connection.username", user.getUserName().getValue());
		prop.put("hibernate.connection.password", user.getPassword().getValue());
		prop.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL82Dialect");
		prop.put("hibernate.connection.driver_class", "org.postgresql.Driver");
		cfg.setProperties(prop);
		Class.forName("org.postgresql.Driver");
		Connection connection = DriverManager.getConnection("jdbc:postgresql://" + user.getUrl().getValue(), user.getUserName().getValue(), user.getPassword().getValue());
		Dialect dialect = new PostgreSQL82Dialect();
		log.info("Component: " + componentName);
		String pack1 = "pg.cui.components." + componentName + ".dataAccessModel";
		String pack2 = "pg.cui.components." + componentName + ".dataAccessModelReadOnly";
		String pathToJar = jarPath.toString();
		JarFile jarFile = new JarFile(pathToJar);
		Enumeration<JarEntry> e = jarFile.entries();
		URL[] urls = { new URL("jar:file:" + pathToJar + "!/") };
		URLClassLoader cl = URLClassLoader.newInstance(urls);
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		while (e.hasMoreElements()) {
			JarEntry je = e.nextElement();
			if (je.isDirectory() || !je.getName().endsWith(".class")) {
				continue;
			}
			// -6 because of .class
			String className = je.getName().substring(0, je.getName().length() - 6);
			className = className.replace('/', '.');
			if (className.contains(pack1) || className.contains(pack2)) {
				Class c = cl.loadClass(className);
				cfg.addAnnotatedClass(c);


			}
		}
		Thread.currentThread().setContextClassLoader(cl);
		cfg.buildMappings();
		Iterator<PersistentClass> classMappings = cfg.getClassMappings();
		Iterator<Table> tableMappings = cfg.getTableMappings();
		List<SchemaUpdateScript> scripts = cfg.generateSchemaUpdateScriptList(dialect, new DatabaseMetadata(connection, dialect, cfg));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String result = "Nazwa klasy: V" + sdf.format(new Date()) + "__" + componentName + "_<nazwa_migracji>.java\n";
		Formatter formatter = FormatStyle.DDL.getFormatter();
		for (SchemaUpdateScript script : scripts) {
			result = result + formatter.format(script.getScript()) + ";\n";
		}
		log.info(result);
		return result;
	}
}
