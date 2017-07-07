package pl.pszczola3mk.dbReport.business;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.SchemaUpdateScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.pszczola3mk.dbReport.service.ReportCreatorDBService;

@Component
public class ReportCreatorBusiness {

	private static final Logger log = LoggerFactory.getLogger(ReportCreatorBusiness.class);
	@Autowired
	private ReportCreatorDBService service;

	public void checkConnection(String url, String userName, String password) {
		this.service.checkConnection(url, userName, password);
	}

	public List<Map<String, Object>> executeSql(String sql, String url, String userName, String password) throws SQLException {
		List<Map<String, Object>> result = this.service.executeSql(sql, url, userName, password);
		return result;
	}

	public String translateFromHqlToSql(String hqlQueryText, List<Path> jarPath, String componentName, String url, String userName, String password) throws Exception {
		//
		Configuration cfg = new Configuration();
		getHibernateConfig(jarPath, componentName, url, userName, password, cfg);
		//
		String result = "";
		ServiceRegistryBuilder registry = new ServiceRegistryBuilder();
		registry.applySettings(cfg.getProperties());
		ServiceRegistry serviceRegistry = registry.buildServiceRegistry();
		SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistry);
		if (hqlQueryText != null && hqlQueryText.trim().length() > 0) {
			QueryTranslatorFactory translatorFactory = new ASTQueryTranslatorFactory();
			SessionFactoryImplementor factory = (SessionFactoryImplementor) sessionFactory;
			QueryTranslator translator = translatorFactory.createQueryTranslator(hqlQueryText, hqlQueryText, Collections.EMPTY_MAP, factory,null);
			translator.compile(Collections.EMPTY_MAP, false);
			result = translator.getSQLString();
			BasicFormatterImpl bfi = new BasicFormatterImpl();
			result = bfi.format(result);
		}
		log.info(result);
		return result;
	}

	public String generateScript(List<Path> jarPath, String componentName, String url, String userName, String password) throws Exception {
		//
		Dialect dialect = new PostgreSQL82Dialect();
		Connection connection = DriverManager.getConnection("jdbc:postgresql://" + url, userName, password);
		Configuration cfg = new Configuration();
		getHibernateConfig(jarPath, componentName, url, userName, password, cfg);
		//
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

	private void getHibernateConfig(List<Path> jarPath, String componentName, String url, String userName, String password, Configuration cfg)
			throws ClassNotFoundException, SQLException, IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Properties prop = new Properties();
		prop.put("hibernate.connection.url", "jdbc:postgresql://" + url);
		prop.put("hibernate.connection.username", userName);
		prop.put("hibernate.connection.password", password);
		prop.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL82Dialect");
		prop.put("hibernate.connection.driver_class", "org.postgresql.Driver");
		cfg.setProperties(prop);
		Class.forName("org.postgresql.Driver");
		log.info("Component: " + componentName);
		String pack1 = "pg.cui.components." + componentName + ".dataAccessModel";
		String pack2 = "pg.cui.components." + componentName + ".dataAccessModelReadOnly";

		/*URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class clazz = URLClassLoader.class;
		// Use reflection
		Method method = clazz.getDeclaredMethod("addURL", new Class[]{URL.class});
		method.setAccessible(true);
		Object[] o = jarPath.stream().map(j -> {
			try {
				return new File(j.toString()).toURL();
			} catch (MalformedURLException e1) {
				log.error("URL read error", e1);
				return null;
			}
		}).toArray();
		method.invoke(classLoader, o); */
		for (Path path : jarPath) {
			URL jarUrl = new URL("jar:file:" + path + "!/");
			URL[] urls = {jarUrl};
			ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
			// Add the conf dir to the classpath
			// Chain the current thread classloader
			URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{jarUrl}, currentThreadClassLoader);
			// Replace the thread classloader - assumes
			// you have permissions to do so
			Thread.currentThread().setContextClassLoader(urlClassLoader);
			String pathToJar = path.toString();
			JarFile jarFile = new JarFile(pathToJar);
			Enumeration<JarEntry> e = jarFile.entries();
			while (e.hasMoreElements()) {
				JarEntry je = e.nextElement();
				if (je.isDirectory() || !je.getName().endsWith(".class")) {
					continue;
				}
				// -6 because of .class
				String className = je.getName().substring(0, je.getName().length() - 6);
				className = className.replace('/', '.');
				if (className.contains(pack1) || className.contains(pack2)) {
					Class c = Thread.currentThread().getContextClassLoader().loadClass(className);
					cfg.addAnnotatedClass(c);
				}
			}
		}


	}
}
